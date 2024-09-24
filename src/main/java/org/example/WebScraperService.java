package org.example;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is responsible for scraping web content from a given URL.
 * It makes a GET request to the specified URL, extracts the main content of the page, and
 * returns the content and other metadata in a JSON format.
 */
public class WebScraperService {

    /**
     * Fetches and parses the content of a web page by sending a GET request to the specified URL.
     * It extracts the main content, page title, and content length, and returns the data in a JSONObject.
     *
     * @param urlString The URL of the web page to fetch and scrape content from.
     * @return          A JSONObject containing the page content, title, content length, and additional metadata.
     * @throws Exception If an error occurs during the HTTP request or content parsing.
     */
    public static JSONObject fetchContent(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        // Preparar el JSON de respuesta
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("url", urlString);
        jsonResponse.put("responseCode", responseCode);

        StringBuilder content = new StringBuilder();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            Document doc = Jsoup.parse(content.toString());

            Element mainContent = doc.select("div.main-content").first();
            if (mainContent != null) {
                jsonResponse.put("content", mainContent.text());
            } else {
                jsonResponse.put("content", doc.body().text());
            }
            jsonResponse.put("title", doc.title());
            jsonResponse.put("contentLength", content.length());
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            String errorResponse = errorReader.lines().reduce((a, b) -> a + b).get();
            jsonResponse.put("error", errorResponse);
        }

        return jsonResponse;
    }

    /**
     * Main method for testing the WebScraperService. It scrapes content from a sample URL
     * and prints the main content of the page.
     *
     * @param args Command-line arguments (not used).
     * @throws Exception If an error occurs during web scraping or printing the content.
     */
    public static void main(String[] args) throws Exception {
        String url = "https://www.crehana.com/blog/transformacion-digital/blogs-mas-famosos/";
        JSONObject response = fetchContent(url);
        System.out.println(response.getString("content"));
    }
}
