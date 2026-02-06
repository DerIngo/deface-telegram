package com.deface.telegram.deface;

import com.deface.telegram.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public final class DefaceClient {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final Logger logger = LoggerFactory.getLogger(DefaceClient.class);

  private final HttpClient httpClient;
  private final String endpoint;
  private final String defaultFilterName;
  private final String defaultPasteStyle;

  public DefaceClient(AppConfig config) {
    Objects.requireNonNull(config, "config");
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
    this.endpoint = config.getDefaceEndpoint();
    this.defaultFilterName = config.getDefaultFilterName();
    this.defaultPasteStyle = config.getDefaultPasteStyle();
  }

  public byte[] defaceImage(byte[] imageBytes) throws IOException, InterruptedException {
    return defaceImage(imageBytes, defaultFilterName, defaultPasteStyle);
  }

  public byte[] defaceImage(byte[] imageBytes, String filterName, String pasteStyle)
      throws IOException, InterruptedException {
    Objects.requireNonNull(imageBytes, "imageBytes");

    String url = buildUrl(endpoint, filterName, pasteStyle);
    logger.info("Sending image to deface API with filter={} paste={}", filterName, pasteStyle);
    String boundary = "----DefaceBoundary" + UUID.randomUUID();
    byte[] body = buildMultipartBody(boundary, imageBytes);

    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        .build();

    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      String contentType = response.headers().firstValue("Content-Type").orElse("unknown");
      String bodyPreview = truncateBytes(response.body(), 8192);
      logger.error("Deface API returned status {} content-type={} body={}",
          response.statusCode(), contentType, bodyPreview);
      throw new IOException("Deface API returned status " + response.statusCode());
    }
    logger.info("Deface API responded with {} bytes", response.body().length);
    return response.body();
  }

  private static String buildUrl(String endpoint, String filterName, String pasteStyle) {
    String separator = endpoint.contains("?") ? "&" : "?";
    return endpoint
        + separator
        + "filter_name=" + urlEncode(filterName)
        + "&paste_ellipse_name=" + urlEncode(pasteStyle);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static byte[] buildMultipartBody(String boundary, byte[] imageBytes) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    output.write("Content-Disposition: form-data; name=\"input_file\"; filename=\"image.jpg\"\r\n"
        .getBytes(StandardCharsets.UTF_8));
    output.write("Content-Type: application/octet-stream\r\n\r\n"
        .getBytes(StandardCharsets.UTF_8));
    output.write(imageBytes);
    output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

    return output.toByteArray();
  }

  private static String truncateBytes(byte[] bytes, int limit) {
    if (bytes == null || bytes.length == 0) {
      return "";
    }
    int length = Math.min(bytes.length, limit);
    String text = new String(bytes, 0, length, StandardCharsets.UTF_8);
    if (bytes.length > limit) {
      return text + "...(truncated)";
    }
    return text;
  }
}
