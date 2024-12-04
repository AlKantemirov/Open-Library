package com.alkantemirov.openlibrary;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import kotlin.text.Charsets;

public class DBUploader implements Runnable {
    private final String uploadUrl;
    private final String postUploadUrl;
    private final String bookTitle;
    private final String bookAuthor;
    private final File coverFile;
    private final File thumbnailFile;
    private final File txtFile;
    private final String boundary = "*****ZZZ";
    private final String lineEnd = "\r\n";
    private final String twoHyphens = "--";

    public DBUploader(File coverFile, File thumbnailFile, File txtFile, String bookTitle, String bookAuthor) {
        this.uploadUrl = Configuration.UPLOAD_URL;
        this.postUploadUrl = Configuration.POST_UPLOAD_URL;
        this.coverFile = coverFile;
        this.thumbnailFile = thumbnailFile;
        this.txtFile = txtFile;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
    }

    @Override
    public void run() {
        if (uploadFiles() == HttpURLConnection.HTTP_OK) updateDB();
    }
    public int uploadFiles() {
        HttpURLConnection connection;
        DataOutputStream outputStream;
        FileInputStream coverFileInputStream;
        FileInputStream thumbnailFileInputStream;
        FileInputStream txtFileInputStream;
        int responseCode = 0;
        try {
            URL url = new URL(uploadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            outputStream = new DataOutputStream(connection.getOutputStream());

            if (coverFile != null) {
                coverFileInputStream = new FileInputStream(coverFile);
                writeHeader(outputStream, coverFileInputStream, "cover", coverFile.getName(), "image/png");
                coverFileInputStream.close();
            }
            if (thumbnailFile != null) {
                thumbnailFileInputStream = new FileInputStream(thumbnailFile);
                writeHeader(outputStream, thumbnailFileInputStream, "thumbnail", thumbnailFile.getName(), "image/png");
                thumbnailFileInputStream.close();
            }
            if (txtFile != null) {
                txtFileInputStream = new FileInputStream(txtFile);
                writeHeader(outputStream, txtFileInputStream, "txt", txtFile.getName(), "text/plain");
                txtFileInputStream.close();
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            outputStream.flush();
            outputStream.close();

            responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.i("DBUploader", "files uploaded");
            } else {
                Log.e("DBUploader", "err uploading. code: " + responseCode);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.e("DBUploader", "response: " + response);
            }
        } catch (IOException ioe) {
            Log.e("DBUploader", "err: " + ioe);
        }
        return responseCode;
    }
    private void updateDB() {
        try {
            URL url = new URL(postUploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String postTitle = !bookTitle.isEmpty() ? URLEncoder.encode(bookTitle, Charsets.UTF_8.name()) : "";
            String postAuthor = !bookAuthor.isEmpty() ? URLEncoder.encode(bookAuthor, Charsets.UTF_8.name()) : "";
            String postPreview = coverFile != null && !coverFile.getName().isEmpty() ?
                    URLEncoder.encode(coverFile.getName(), Charsets.UTF_8.name()) : "";
            String postLocation = txtFile != null && !txtFile.getName().isEmpty() ?
                    URLEncoder.encode(txtFile.getName(), Charsets.UTF_8.name()) : "";
            String postData = "title=" + postTitle + "&author=" + postAuthor +
                    "&preview=" + postPreview + "&location=" + postLocation;
            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes());
                os.flush();
            }
            int responseCode = connection.getResponseCode();
            Log.i("DBUploader","response Code: " + responseCode);
        } catch (IOException ioe) {
            Log.e("DBUploader", "err: " + ioe);
        }
    }
    private void writeHeader(DataOutputStream outputStream, FileInputStream fileInputStream,
                             String name, String filename, String extension) throws IOException {
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", name, filename, lineEnd));
        outputStream.writeBytes(String.format("Content-Type: %s%s", extension, lineEnd));
        outputStream.writeBytes(lineEnd);
        int bytesRead;
        byte[] buffer = new byte[4096];
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.writeBytes(lineEnd);
    }
}