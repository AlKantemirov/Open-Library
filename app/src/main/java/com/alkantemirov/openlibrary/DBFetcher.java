package com.alkantemirov.openlibrary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

public class DBFetcher implements Runnable {
    private final String booksUrl;
    private final String authorsUrl;
    private final int conn_timeout = 500;
    private final int max_attempts = 5;
    private int code = 200;
    private int attemptsCount = 0;

    public DBFetcher() {
        booksUrl = Configuration.FETCH_URL;
        authorsUrl = Configuration.FETCH_AUTHORS_URL;
    }

    @Override
    public void run() {
        if (!BookImage.bookArrayList.isEmpty() && !AuthorImage.authorArrayList.isEmpty()) return;
        fetchData(booksUrl, BookImage.bookArrayList, BookImage.class);
        fetchData(authorsUrl, AuthorImage.authorArrayList, AuthorImage.class);
    }
    private <T> void fetchData(String url, List<T> list, Class<T> clazz) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                response.append("]");
                in.close();

                ObjectMapper om = new ObjectMapper();
                CollectionType listType = om.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
                List<T> items = om.readValue(response.toString(), listType);
                list.addAll(items);
            } else {
                if (!checkRemainingAttempts(503)) return;
                System.out.println("GET request failed");
                synchronized (this){
                    wait(conn_timeout);
                    fetchData(url, list, clazz);
                }
            }
        } catch (Exception e) {
            if (!checkRemainingAttempts(503)) return;
            e.printStackTrace();
            try {
                synchronized (this) {
                    wait(conn_timeout);
                    fetchData(url, list, clazz);
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
    public int getCode() {return code;}
    private boolean checkRemainingAttempts(int code) {
        if (attemptsCount > max_attempts) {
            this.code = code;
            return false;
        }
        attemptsCount++;
        return true;
    }
}
