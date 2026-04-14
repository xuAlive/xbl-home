package com.xu.home.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtil {



    public static String sendGet(String urlString, Map<String, String> headers) throws Exception {
        var url = new URL(urlString);
        var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Clear default headers
        connection.setRequestProperty("User-Agent", "");
        connection.setRequestProperty("Accept", "");

        // Set custom headers
        if (headers != null) {
            for (var entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // Get response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                var response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            }
        } else {
            throw new Exception("GET request failed with response code: " + responseCode);
        }
    }

    public static String sendPost(String urlString, String body, Map<String, String> headers) throws Exception {
        var url = new URL(urlString);
        var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Clear default headers
        connection.setRequestProperty("User-Agent", "");
        connection.setRequestProperty("Accept", "");

        // Set custom headers
        if (headers != null) {
            for (var entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // Write body
        if (body != null && !body.isEmpty()) {
            try (var os = connection.getOutputStream()) {
                os.write(body.getBytes());
                os.flush();
            }
        }

        // Get response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (var in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                var response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            }
        } else {
            throw new Exception("POST request failed with response code: " + responseCode);
        }
    }


}
