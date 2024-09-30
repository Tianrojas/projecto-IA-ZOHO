package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.json.JSONArray;
import org.json.JSONObject;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O;
import static org.example.WebScraperService.fetchContent;

/**
 * This class provides functionality for interacting with the OpenAI GPT API
 * to execute queries and generate responses based on user input, either directly
 * or by using content from a web page.
 */
public class ChatGPTQueryService {

    /**
     * Sends a query to the OpenAI GPT API and returns a JSON response containing the result.
     *
     * @param text        The input query to be processed by the OpenAI model.
     * @param version     The version of the GPT model to use (e.g., "gpt-3.5-turbo", "gpt-4").
     * @param apiKey      The API key for authenticating the request to the OpenAI API.
     * @param max_tokens  The maximum number of tokens the API should return in the response.
     * @param temperature The sampling temperature (higher values produce more random responses).
     * @return            A JSONObject containing the API's response, including the processed content and the raw response.
     * @throws Exception  If an error occurs during the HTTP request or response parsing.
     */
    public static JSONObject search(String text, String version, String apiKey, int max_tokens, double temperature) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + apiKey);

        JSONObject data = new JSONObject();
        data.put("model", version);

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", text);
        messages.put(message);

        data.put("messages", messages);
        data.put("max_tokens", max_tokens);
        data.put("temperature", temperature);

        con.setDoOutput(true);
        con.getOutputStream().write(data.toString().getBytes());

        int responseCode = con.getResponseCode();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("responseCode", responseCode);

        if (responseCode == 200) {
            String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                    .reduce((a, b) -> a + b).get();
            JSONObject responseJson = new JSONObject(output);

            String content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            jsonResponse.put("content", content);
            jsonResponse.put("rawResponse", responseJson);
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            String errorResponse = errorReader.lines().reduce((a, b) -> a + b).get();

            jsonResponse.put("error", errorResponse);
        }
        return jsonResponse;
    }

    /**
     * Retrieves the content from a specified web page and forms a prompt to ask the OpenAI GPT API based on that content.
     *
     * @param question    The question to ask, based on the web content.
     * @param webUrl      The URL of the web page from which content will be fetched.
     * @param version     The version of the GPT model to use (e.g., "gpt-3.5-turbo", "gpt-4").
     * @param apiKey      The API key for authenticating the request to the OpenAI API.
     * @param max_tokens  The maximum number of tokens the API should return in the response.
     * @param temperature The sampling temperature (higher values produce more random responses).
     * @return            A JSONObject containing the API's response, including the processed content and the raw response.
     * @throws Exception  If an error occurs during the HTTP request, web scraping, or response parsing.
     */
    public static JSONObject searchOnWebPage(String question, String webUrl, String version, String apiKey, int max_tokens, double temperature) throws Exception {
        String webContent = fetchContent(webUrl).getString("content");
        String prompt = "Based on the following web content, answer the question: " + question + "\n\nWeb content:\n" + webContent;
        return search(prompt, version, apiKey, max_tokens, temperature);
    }

    /**
     * This method searches for a prompt in Pinecone and generates a response using OpenAI's GPT-4 model.
     * It first retrieves relevant information from Pinecone and then uses that information to formulate
     * a response based on the retrieved data.
     *
     * @param apiKey            The API key for accessing OpenAI's GPT-4 service.
     * @param version           The version of the OpenAI model to use (e.g., GPT-4).
     * @param temperature       The "temperature" parameter for controlling the randomness of the OpenAI model's responses.
     *                          Higher values (e.g., 1.0) make the output more random, while lower values (e.g., 0.2) make it more deterministic.
     * @param pineconeApiKey     The API key for accessing Pinecone's vector database service.
     * @param index             The index to search within Pinecone (e.g., the vector index name).
     * @param nameSpace         The namespace within Pinecone where the vectors are stored. This allows for segmentation of data.
     * @param prompt            The prompt/question for which the search is performed in Pinecone and used to generate a response from OpenAI.
     * @return                  A JSONObject containing the response generated by OpenAI using the knowledge retrieved from Pinecone, or an error message if no results were found.
     * @throws Exception        If an error occurs during the search or response generation process.
     */
    public static JSONObject searchOnPinecone(String apiKey, String version, Double temperature, String pineconeApiKey, String index, String nameSpace, String prompt) throws Exception {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(GPT_4_O)
                .temperature(temperature)
                .build();
        String queryWithKnowledge =  PineconService.searchVectorPinecone(apiKey,pineconeApiKey, index, nameSpace, prompt).getString("queryWithKnowledge");
        String responseWithKnowledge = chatModel.generate(queryWithKnowledge);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("responseWithKnowledge", responseWithKnowledge);
        return jsonResponse;
    }

    /**
     * Main method for testing the ChatGPTQueryService. It executes both direct queries and queries based on web content.
     * execute on console using java -cp "target/classes;target/dependency/*;target/chatgptconnection-1.0-SNAPSHOT.jar" org.example.ChatGPTQueryService
     *
     * @param args  Command-line arguments (not used).
     * @throws Exception  If any error occurs during the API interaction or content fetching.
     */
    public static void main(String[] args) throws Exception {
        //Set APIkey
        System.out.println(search("Cual es la densidad de etiopia",
                "gpt-4",
                "",
                4000,
                1.0).getString("content"));

        System.out.println(searchOnWebPage("Entrepreneur cuantos seguidores tiene en Instagram?",
                "https://www.crehana.com/blog/transformacion-digital/blogs-mas-famosos/",
                "gpt-3.5-turbo",
                "",
                4000,
                1.0).getString("content"));
        System.out.println(searchOnPinecone("",
                "gpt-4",
                1.0,
                "",
                "",
                "",
                "¿Cuáles son los 3 primeros pasos para empezar a utilizar el WonderVector5000?").getString("responseWithKnowledge"));

    }
}
