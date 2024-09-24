package org.example;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for managing OAuth tokens using Zoho's OAuth API.
 * It handles the generation of new access tokens, refreshing expired tokens, and caching tokens
 * to optimize the token management process.
 *
 * The class supports both generating tokens using an authorization code and refreshing tokens using
 * a refresh token. It stores tokens in memory, but this can be replaced with persistent storage
 * (e.g., Redis) if needed.
 */
public class TokenService {

    private static final String TOKEN_URL = "https://accounts.zoho.com/oauth/v2/token";
    private static final String REDIRECT_URI = "https://www.zoho.com";

    // In-memory cache to store tokens (could be replaced with a more persistent storage like Redis)
    private static Map<String, TokenInfo> tokenCache = new HashMap<>();

    /**
     * Class to store token information and expiration time.
     * It holds both the access token and refresh token, as well as the expiration time for the access token.
     */
    static class TokenInfo {
        String accessToken;
        String refreshToken;
        long expirationTime;

        /**
         * Constructor to create an instance of TokenInfo.
         *
         * @param accessToken    The OAuth access token.
         * @param refreshToken   The OAuth refresh token.
         * @param expirationTime The expiration time (in milliseconds) of the access token.
         */
        TokenInfo(String accessToken, String refreshToken, long expirationTime) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expirationTime = expirationTime;
        }

        /**
         * Checks whether the access token is still valid based on its expiration time.
         *
         * @return True if the access token is still valid, otherwise false.
         */
        boolean isAccessTokenValid() {
            return System.currentTimeMillis() < expirationTime;
        }
    }

    /**
     * Generates an OAuth token by sending a POST request to Zoho's OAuth API, or reuses an existing valid token
     * if it is cached and still valid. The method takes the authorization code, client ID, and client secret,
     * and returns a JSONObject containing the access token or an error message if the request fails.
     *
     * If a valid token exists in the cache, it will be reused. If the token is expired, the refresh token will be
     * used to obtain a new access token.
     *
     * @param code          The authorization code required for generating the OAuth token.
     * @param client_id     The client ID for OAuth token generation.
     * @param client_secret The client secret for OAuth token generation.
     * @return              A JSONObject containing the OAuth access token or an error message in case of failure.
     */
    public static JSONObject generateTkn(String code, String client_id, String client_secret) {
        try {
            // Check if there is a valid token in the cache
            if (tokenCache.containsKey(client_id)) {
                TokenInfo tokenInfo = tokenCache.get(client_id);
                if (tokenInfo.isAccessTokenValid()) {
                    JSONObject tokenResponse = new JSONObject();
                    tokenResponse.put("access_token", tokenInfo.accessToken);
                    tokenResponse.put("refresh_token", tokenInfo.refreshToken);
                    return tokenResponse;
                } else {
                    // Access token has expired, refresh the token
                    return refreshAccessToken(client_id, client_secret, tokenInfo.refreshToken);
                }
            }

            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String data = "code=" + code
                    + "&grant_type=authorization_code"
                    + "&client_id=" + client_id
                    + "&client_secret=" + client_secret
                    + "&redirect_uri=" + REDIRECT_URI;

            OutputStream os = conn.getOutputStream();
            byte[] input = data.getBytes("utf-8");
            os.write(input, 0, input.length);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            String jsonResponseStr = response.toString().trim();
            JSONObject jsonResponse = new JSONObject(jsonResponseStr);

            // Cache the new access and refresh tokens
            String accessToken = jsonResponse.getString("access_token");
            String refreshToken = jsonResponse.getString("refresh_token");
            long expiresIn = jsonResponse.getLong("expires_in") * 1000; // Convert seconds to milliseconds
            long expirationTime = System.currentTimeMillis() + expiresIn;

            tokenCache.put(client_id, new TokenInfo(accessToken, refreshToken, expirationTime));

            return jsonResponse;

        } catch (IOException e) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Refreshes the access token using the refresh token when the access token has expired.
     * The method sends a POST request to Zoho's OAuth API and returns the new access token.
     *
     * @param client_id     The client ID for OAuth token generation.
     * @param client_secret The client secret for OAuth token generation.
     * @param refresh_token The refresh token to use for refreshing the access token.
     * @return              A JSONObject containing the new access token or an error message in case of failure.
     */
    public static JSONObject refreshAccessToken(String client_id, String client_secret, String refresh_token) {
        try {
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String data = "refresh_token=" + refresh_token
                    + "&grant_type=refresh_token"
                    + "&client_id=" + client_id
                    + "&client_secret=" + client_secret;

            OutputStream os = conn.getOutputStream();
            byte[] input = data.getBytes("utf-8");
            os.write(input, 0, input.length);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            String jsonResponseStr = response.toString().trim();
            JSONObject jsonResponse = new JSONObject(jsonResponseStr);

            // Cache the new access token
            String accessToken = jsonResponse.getString("access_token");
            long expiresIn = jsonResponse.getLong("expires_in") * 1000; // Convert seconds to milliseconds
            long expirationTime = System.currentTimeMillis() + expiresIn;

            // Update the cache with the new access token
            tokenCache.put(client_id, new TokenInfo(accessToken, refresh_token, expirationTime));

            return jsonResponse;

        } catch (IOException e) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Main method for testing the TokenService class.
     * It generates an OAuth token using the authorization code and prints the token details.
     *
     * @param args Command-line arguments (not used).
     * @throws Exception If any error occurs during token generation.
     */
    public static void main(String[] args) throws Exception {
        System.out.println(generateTkn("your_code", "your_client_id", "your_client_secret"));
    }
}
