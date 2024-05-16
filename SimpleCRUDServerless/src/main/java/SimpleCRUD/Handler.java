package SimpleCRUD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import SimpleCRUD.models.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;


public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        String httpMethod = input.getHttpMethod();
        String output;
        int statusCode = switch (httpMethod) {
            case "GET" -> {
                output = getUsers();
                yield 200;
            }
            case "POST" -> {
                output = createUser(input.getBody());
                yield 201;
            }
            case "PUT" -> {
                output = updateUser(input.getBody());
                yield 200;
            }
            case "DELETE" -> {
                output = deleteUser(input.getBody());
                yield 200;
            }
            default -> {
                output = "Invalid HTTP Method";
                yield 400;
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(output);

        return response;
    }

    private String getUsers() {
        List<User> users = dynamoDBMapper.scan(User.class, new DynamoDBScanExpression());
        return new Gson().toJson(users);
    }

    private String createUser(String requestBody) {
        User user = new Gson().fromJson(requestBody, User.class);
        dynamoDBMapper.save(user);
        return "User created: " + user.getId();
    }

    private String updateUser(String requestBody) {
        User updatedUser = new Gson().fromJson(requestBody, User.class);
        User existingUser = dynamoDBMapper.load(User.class, updatedUser.getId());
        if (existingUser != null) {
            existingUser.setEmpId(updatedUser.getEmpId());
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            dynamoDBMapper.save(existingUser);
            return "User updated: " + existingUser.getId();
        } else {
            return "User not found";
        }
    }

    private String deleteUser(String requestBody) {
        User userToDelete = new Gson().fromJson(requestBody, User.class);
        User existingUser = dynamoDBMapper.load(User.class, userToDelete.getId());
        if (existingUser != null) {
            dynamoDBMapper.delete(existingUser);
            return "User deleted: " + existingUser.getId();
        } else {
            return "User not found";
        }
    }

}
