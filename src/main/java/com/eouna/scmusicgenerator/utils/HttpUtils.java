package com.eouna.scmusicgenerator.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Http请求的工具
 *
 * @author CCL
 */
public class HttpUtils {

  private static final String LINE_FEED = "\r\n";

  /** 发送GET请求 */
  public static String sendGet(String urlStr) {
    StringBuilder response = new StringBuilder();
    try {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      int responseCode = conn.getResponseCode();
      System.out.println("GET Response Code: " + responseCode);

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }

      in.close();
      conn.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return response.toString();
  }

  /**
   * 发送POST请求，支持上传文件
   *
   * @param urlStr 请求地址
   * @param fieldName 字段名（一般是"file"）
   * @param uploadFile 要上传的文件
   * @param extraFields 附加字段(可以为空)
   */
  public static String sendPostWithFile(
      String urlStr, String fieldName, File uploadFile, Map<String, String> extraFields) {
    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
    String charset = "UTF-8";
    return executePost(urlStr, boundary, charset, extraFields, uploadFile, true, null);
  }

  /**
   * 发送POST请求，不带文件，支持附加字段
   *
   * @param urlStr 请求地址
   * @param extraFields 附加字段
   */
  public static String sendPostWithoutFile(String urlStr, Map<String, String> extraFields) {
    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
    String charset = "UTF-8";
    return executePost(urlStr, boundary, charset, extraFields, null, false, null);
  }

  /**
   * 发送POST请求，支持文件上传并接收文件（如图片文件等）
   *
   * @param urlStr 请求地址
   * @param fieldName 文件字段名（一般是"file"）
   * @param uploadFile 要上传的文件
   * @param extraFields 附加字段(可以为空)
   * @param responseFile 输出接收的文件（例如：接收一个图片文件）
   */
  public static boolean sendPostWithFileAndReceiveFile(
      String urlStr,
      String fieldName,
      File uploadFile,
      Map<String, String> extraFields,
      File responseFile) {
    String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
    String charset = "UTF-8";
    String response =
        executePost(urlStr, boundary, charset, extraFields, uploadFile, true, responseFile);
    return !response.isEmpty();
  }

  /** 通用POST请求处理逻辑（包括文件上传及附加字段） */
  private static String executePost(
      String urlStr,
      String boundary,
      String charset,
      Map<String, String> extraFields,
      File uploadFile,
      boolean isFileUpload,
      File responseFile) {
    StringBuilder response = new StringBuilder();
    try {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setUseCaches(false);
      conn.setDoOutput(true);
      conn.setDoInput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

      OutputStream outputStream = conn.getOutputStream();
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);

      // 写附加字段（普通表单参数）
      if (extraFields != null) {
        for (String name : extraFields.keySet()) {
          String value = extraFields.get(name);
          writer.append("--").append(boundary).append(LINE_FEED);
          writer
              .append("Content-Disposition: form-data; name=\"")
              .append(name)
              .append("\"")
              .append(LINE_FEED);
          writer.append("Content-Type: text/plain; charset=").append(charset).append(LINE_FEED);
          writer.append(LINE_FEED);
          writer.append(value).append(LINE_FEED);
          writer.flush();
        }
      }

      // 如果是文件上传，处理文件字段
      if (isFileUpload && uploadFile != null) {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED);
        writer
            .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(fileName)
            .append("\"")
            .append(LINE_FEED);
        writer
            .append("Content-Type: ")
            .append(HttpURLConnection.guessContentTypeFromName(fileName))
            .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
      }

      // 结束multipart
      writer.append("--").append(boundary).append("--").append(LINE_FEED);
      writer.close();

      // 读取响应
      int responseCode = conn.getResponseCode();
      System.out.println("POST Response Code: " + responseCode);

      // 如果接收到文件响应
      if (responseFile != null && responseCode == HttpURLConnection.HTTP_OK) {
        InputStream inputStreamFromResponse = conn.getInputStream();
        FileOutputStream fileOutputStream = new FileOutputStream(responseFile);
        byte[] bufferResponse = new byte[4096];
        int bytesReadResponse;
        while ((bytesReadResponse = inputStreamFromResponse.read(bufferResponse)) != -1) {
          fileOutputStream.write(bufferResponse, 0, bytesReadResponse);
        }
        fileOutputStream.close();
        inputStreamFromResponse.close();
      }

      // 读取返回的内容
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }

      in.close();
      conn.disconnect();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return response.toString();
  }
}
