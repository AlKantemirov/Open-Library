package com.alkantemirov.openlibrary;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import kotlin.text.Charsets;

public class DBUpdater implements Runnable {
    private final String updateUrl;
    private int targetId;
    private String newTitle;
    private String newAuthor;
    private final int conn_timeout = 500;
    private final int max_attempts = 5;
    private int attemptsCount = 0;

    public DBUpdater(int targetId, String newTitle, String newAuthor) {
        updateUrl = Configuration.UPDATE_URL;
        this.targetId = targetId;
        this.newTitle = newTitle;
        this.newAuthor = newAuthor;
    }

    @Override
    public void run() {
        remove();
    }
    private void remove() {
        StringBuilder sb = new StringBuilder();
        sb.append(updateUrl).append(targetId);
        try {
            sb.append("&title=").append(URLEncoder.encode(newTitle, Charsets.UTF_8.name()));
            sb.append("&author=").append(URLEncoder.encode(newAuthor, Charsets.UTF_8.name()));
        } catch (UnsupportedEncodingException uee) {
            Log.e("DBUploader", "err: " + uee);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(sb.toString()).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
            } else {
                if (!checkRemainingAttempts()) return;
                System.out.println("GET request failed");
                synchronized (this) {
                    wait(conn_timeout);
                    remove();
                }
            }
        } catch (Exception e) {
            if (!checkRemainingAttempts()) return;
            e.printStackTrace();
            try {
                synchronized (this) {
                    wait(conn_timeout);
                    remove();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
    private boolean checkRemainingAttempts() {
        if (attemptsCount > max_attempts) {
            return false;
        }
        attemptsCount++;
        return true;
    }
}
