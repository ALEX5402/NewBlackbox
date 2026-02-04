package top.niunaijun.blackbox.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class LogSender {
    private static final String TAG = "LogSender";
    private static final String API_URL_TEMPLATE = "https://logs-sender-api.vercel.app/api/%s/upload";

    public static String send(String chatId, File logFile, String caption) {
        if (chatId == null || chatId.isEmpty()) {
            Slog.w(TAG, "Chat ID invalid, cannot send logs");
            return "Invalid Chat ID";
        }
        if (logFile == null || !logFile.exists()) {
             Slog.w(TAG, "Log file not found: " + logFile);
             return "Log file not found";
        }

        String urlString = String.format(API_URL_TEMPLATE, chatId);
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String twoHyphens = "--";
        String lineEnd = "\r\n";

        try {
            Slog.d(TAG, "Sending logs to: " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

            
            if (caption != null) {
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"caption\"" + lineEnd);
                dos.writeBytes(lineEnd);
                
                
                dos.write(caption.getBytes("UTF-8"));
                dos.writeBytes(lineEnd);
            }

            
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + logFile.getName() + "\"" + lineEnd);
            dos.writeBytes("Content-Type: text/plain" + lineEnd);
            dos.writeBytes(lineEnd);

            FileInputStream fis = new FileInputStream(logFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            fis.close();
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            dos.flush();
            dos.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                Slog.d(TAG, "Logs sent successfully: " + responseCode);
                return null; 
            } else {
                 String errorMsg = "HTTP " + responseCode;
                 try {
                     BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                     String line;
                     StringBuilder response = new StringBuilder();
                     while ((line = br.readLine()) != null) {
                         response.append(line);
                     }
                     br.close();
                     errorMsg += ": " + response.toString();
                     Slog.e(TAG, "Failed to send logs. " + errorMsg);
                 } catch (Exception e) {
                     Slog.e(TAG, "Failed to send logs. Response Code: " + responseCode);
                 }
                 return errorMsg;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error sending logs: " + e.getMessage(), e);
            return "Exception: " + e.getMessage();
        }
    }
}
