package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.Base64;

import static org.example.TokenService.generateTkn;

/**
 * This class provides functionality for connecting to an external API (such as ManageEngine ServiceDesk Plus)
 * using an OAuth token generated from Zoho's API. It sends a GET request to the specified URL and returns
 * the response in JSON format, along with metadata like status and error messages if applicable.
 */
public class SDKConnectorService {

    /**
     * Sends a GET request to the specified URL using an OAuth token generated through Zoho's API
     * and returns the result as a JSONObject.
     *
     * @param url           The URL to which the GET request will be sent.
     * @param code          The authorization code required for OAuth token generation.
     * @param client_id     The client ID for OAuth token generation.
     * @param client_secret The client secret for OAuth token generation.
     * @return              A JSONObject containing the API response, including status, HTTP status code,
     *                      the response data (if successful), and any error messages.
     * @throws Exception    If an error occurs during token generation or while sending the HTTP request.
     */
    public static JSONObject invokeGetTkn(String url, String code, String client_id, String client_secret) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .header("Accept", "application/vnd.manageengine.sdp.v3+json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Zoho-oauthtoken " + generateTkn(code, client_id, client_secret).getString("access_token"))
                .build();

        JSONObject jsonResponse = new JSONObject();

        try {
            Response output = client.newCall(request).execute();

            if (output.isSuccessful()) {
                jsonResponse.put("status", "success");
                jsonResponse.put("statusCode", output.code());
                jsonResponse.put("data", new JSONObject(output.body().string()));
            } else {
                jsonResponse.put("status", "error");
                jsonResponse.put("statusCode", output.code());
                jsonResponse.put("message", "Error response from server: " + output.code());
            }
        } catch (Exception e) {
            jsonResponse.put("status", "exception");
            jsonResponse.put("message", "Exception while making the API request: " + e.getMessage());
        }

        return jsonResponse;
    }

    /**
     * The main method for testing the SDKConnectorService. It is currently empty
     * and can be filled with test cases or examples of how to use invokeGetTkn method.
     *
     * @param args Command-line arguments (not used).
     * @throws Exception If an error occurs during the execution of test cases or examples.
     */
    public static void main(String[] args) throws Exception {

    }
}
