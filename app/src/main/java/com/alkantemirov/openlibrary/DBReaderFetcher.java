package com.alkantemirov.openlibrary;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DBReaderFetcher implements Runnable {
    private final int conn_timeout = 500;
    private final int max_attempts = 5;
    private int code = 200;
    private int attemptsCount = 0;
    private final String destinationUrl;
    private String result;
    private int linesCount;
    private final int maxLinesCount = 50;
    private long pageNo = 1;
    private boolean isEnd;

    public DBReaderFetcher(String fileName) {
        destinationUrl = Configuration.TEXTS_DIR + fileName;
        pageNo = PreferencesManager.getInstance().getValue(fileName);
    }

    @Override
    public void run() {
        fetch();
    }
    private void fetch() {
        linesCount = 0;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(destinationUrl).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                for (int i = 0; i < maxLinesCount * pageNo - 1; i++) in.readLine();
                while ((inputLine = in.readLine()) != null && linesCount < maxLinesCount) {
                    response.append(inputLine).append("\n");
                    linesCount++;
                }

                if (in.readLine() == null) isEnd = true;

                in.close();

                result = response.toString();
            } else {
                if (!checkRemainingAttempts(503)) return;
                System.out.println("GET request failed");
                synchronized (this){
                    wait(conn_timeout);
                    fetch();
                }
            }
        } catch (Exception e) {
            if (!checkRemainingAttempts(503)) return;
            Log.e("DBReaderFetcher", "err: " + e);
            try {
                synchronized (this) {
                    wait(conn_timeout);
                    fetch();
                }
            } catch (InterruptedException ie) {
                Log.e("DBReaderFetcher", "err: " + ie);
            }
        }
    }
    public int getCode() {return code;}
    public String getResult() {return result;}
    public boolean isEndReached() {return isEnd;}
    private boolean checkRemainingAttempts(int code) {
        if (attemptsCount > max_attempts) {
            this.code = code;
            return false;
        }
        attemptsCount++;
        return true;
    }
}
