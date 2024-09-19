package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ChatGPT {
    public static String search(String text, String version, String apiKey, int max_tokens, double temperature) throws Exception {
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
        System.out.println("Response Code: " + responseCode);

        String toReturn = "";

        if (responseCode == 200) {
            String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                    .reduce((a, b) -> a + b).get();
            toReturn = new JSONObject(output).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            String errorResponse = errorReader.lines().reduce((a, b) -> a + b).get();
            toReturn = "Error Response: " + errorResponse;
        }

        return toReturn;
    }


    public static String fetchContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        StringBuilder content = new StringBuilder();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            Document doc = Jsoup.parse(content.toString());

            Element mainContent = doc.select("div.main-content").first(); // Example selector
            if (mainContent != null) {
                return mainContent.text();
            } else {
                return doc.body().text(); // Fallback to entire body text
            }
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            String errorResponse = errorReader.lines().reduce((a, b) -> a + b).get();
            return "Error Response: " + errorResponse;
        }
    }

    public static String searchOnWebPage(String question, String webUrl, String version, String apiKey, int max_tokens, double temperature) throws Exception {
        String webContent = fetchContent(webUrl);
        String prompt = "Based on the following web content, answer the question: " + question + "\n\nWeb content:\n" + webContent;
        return search(prompt, version, apiKey, max_tokens, temperature);
    }


    public static void main(String[] args) throws Exception {
        /*
        System.out.println(search("Cual es la densidad de etiopia",
                "gpt-3.5-turbo",
                "",
                4000,
                1.0));*/

        System.out.println(searchOnWebPage("Entrepreneur cuantos seguidores tiene en Instagram?",
                "https://www.crehana.com/blog/transformacion-digital/blogs-mas-famosos/",
                "gpt-3.5-turbo",
                " ",
                4000,
                1.0));


    }
}
