package com.nhom1.auction.client.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ImageUploadService {

  private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";
  private static final Path LOCAL_CONFIG_PATH = Path.of("config.local.properties");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public CompletableFuture<String> upload(File imageFile) {
    String apiKey = resolveApiKey();
    if (imageFile == null || !imageFile.exists() || !imageFile.isFile()) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid image file"));
    }

    return CompletableFuture.supplyAsync(() -> doUpload(apiKey.trim(), imageFile));
  }

  private String resolveApiKey() {
    String envKey = System.getenv("IMGBB_API_KEY");
    if (envKey != null && !envKey.isBlank()) {
      return envKey.trim();
    }

    if (Files.exists(LOCAL_CONFIG_PATH)) {
      Properties properties = new Properties();
      try (InputStream input = Files.newInputStream(LOCAL_CONFIG_PATH)) {
        properties.load(input);
        String fileKey = properties.getProperty("imgbb.api.key");
        if (fileKey != null && !fileKey.isBlank()) {
          return fileKey.trim();
        }
      } catch (IOException ignored) {
      }
    }

    throw new IllegalStateException("IMGBB_API_KEY is not configured");
  }

  private String doUpload(String apiKey, File imageFile) {
    try {
      byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
      String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
      String body =
          "key="
              + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
              + "&image="
              + URLEncoder.encode(encodedImage, StandardCharsets.UTF_8);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(IMGBB_UPLOAD_URL))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("imgBB upload failed with status " + response.statusCode());
      }

      JsonNode root = OBJECT_MAPPER.readTree(response.body());
      boolean success = root.path("success").asBoolean(false);
      String displayUrl = root.path("data").path("display_url").asText("");
      if (!success || displayUrl.isBlank()) {
        throw new IllegalStateException("imgBB upload failed: invalid response");
      }
      return displayUrl;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new CompletionException("Failed to upload image to imgBB", ex);
    }
  }
}
