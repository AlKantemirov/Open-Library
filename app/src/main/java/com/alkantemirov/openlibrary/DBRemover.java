package com.alkantemirov.openlibrary;

import java.net.HttpURLConnection;
import java.net.URL;

public class DBRemover implements Runnable {
    private final String removeUrl;
    private int targetId;
    private final int conn_timeout = 500;
    private final int max_attempts = 5;
    private int attemptsCount = 0;
    public DBRemover(int targetId) {
        removeUrl = Configuration.REMOVE_URL;
        this.targetId = targetId;
    }
    @Override
    public void run() {
        remove();
    }
    private void remove() {
        StringBuilder sn = new StringBuilder();
        sn.append(removeUrl).append(targetId);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(sn.toString()).openConnection();
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
