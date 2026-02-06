package com.deface.telegram.telegram;

import com.deface.telegram.config.AppConfig;
import com.deface.telegram.deface.DefaceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaceTelegramBot extends TelegramLongPollingBot {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
  private static final Pattern FILE_PATH_PATTERN = Pattern.compile("\"file_path\"\\s*:\\s*\"([^\"]+)\"");
  private static final Logger logger = LoggerFactory.getLogger(DefaceTelegramBot.class);

  private final AppConfig config;
  private final DefaceClient defaceClient;
  private final ChatSettingsStore settingsStore;
  private final HttpClient httpClient;

  public DefaceTelegramBot(AppConfig config, DefaceClient defaceClient, ChatSettingsStore settingsStore) {
    this.config = Objects.requireNonNull(config, "config");
    this.defaceClient = Objects.requireNonNull(defaceClient, "defaceClient");
    this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update == null || !update.hasMessage()) {
      return;
    }

    Message message = update.getMessage();
    if (message.hasText()) {
      logger.info("Received text message from chat {}", message.getChatId());
      handleCommand(message);
      return;
    }

    if (message.hasPhoto()) {
      logger.info("Received photo message from chat {}", message.getChatId());
      handlePhoto(message);
    }
  }

  @Override
  public String getBotUsername() {
    return config.getTelegramBotUsername().orElse("");
  }

  @Override
  public String getBotToken() {
    return config.getTelegramBotToken();
  }

  private void handleCommand(Message message) {
    String text = message.getText().trim();
    String command = extractCommand(text);
    if (command == null) {
      return;
    }

    if ("/start".equals(command)) {
      logger.info("Handling /start for chat {}", message.getChatId());
      reply(message.getChatId(), "Welcome. Send a photo to process or /help for commands.");
      return;
    }

    if ("/help".equals(command)) {
      logger.info("Handling /help for chat {}", message.getChatId());
      reply(message.getChatId(), buildHelpMessage());
      return;
    }

    if ("/status".equals(command)) {
      ChatSettingsStore.ChatSettings settings = settingsStore.get(message.getChatId());
      reply(message.getChatId(), "Filter: " + settings.filterName()
          + "\nPaste style: " + settings.pasteStyle());
      return;
    }

    if ("/filter".equals(command)) {
      String value = extractArgument(text);
      if (value == null) {
        reply(message.getChatId(), "Usage: /filter <name>");
        return;
      }
      if (!config.getAllowedFilterNames().contains(value)) {
        reply(message.getChatId(), "Invalid filter. Allowed: " + String.join(", ", config.getAllowedFilterNames()));
        return;
      }
      ChatSettingsStore.ChatSettings settings = settingsStore.updateFilter(message.getChatId(), value);
      reply(message.getChatId(), "Filter set to " + settings.filterName());
      return;
    }

    if ("/paste".equals(command)) {
      String value = extractArgument(text);
      if (value == null) {
        reply(message.getChatId(), "Usage: /paste <name>");
        return;
      }
      if (!config.getAllowedPasteStyles().contains(value)) {
        reply(message.getChatId(), "Invalid paste style. Allowed: "
            + String.join(", ", config.getAllowedPasteStyles()));
        return;
      }
      ChatSettingsStore.ChatSettings settings = settingsStore.updatePasteStyle(message.getChatId(), value);
      reply(message.getChatId(), "Paste style set to " + settings.pasteStyle());
    }
  }

  private void handlePhoto(Message message) {
    Long chatId = message.getChatId();
    Optional<PhotoSize> bestPhoto = selectBestPhoto(message.getPhoto());
    if (bestPhoto.isEmpty()) {
      reply(chatId, "I couldn't read that photo. Please try again.");
      return;
    }

    try {
      logger.info("Downloading Telegram file for chat {}", chatId);
      byte[] originalImage = downloadTelegramFile(bestPhoto.get().getFileId());
      ChatSettingsStore.ChatSettings settings = settingsStore.get(chatId);
      logger.info("Calling deface API for chat {} filter={} paste={}", chatId, settings.filterName(),
          settings.pasteStyle());
      byte[] processedImage = defaceClient.defaceImage(originalImage, settings.filterName(), settings.pasteStyle());
      sendPhoto(chatId, processedImage);
      logger.info("Processed image sent for chat {}", chatId);
    } catch (Exception e) {
      String refId = java.util.UUID.randomUUID().toString();
      logger.error("Failed to process image for chat {} refId={}", chatId, refId, e);
      reply(chatId, "Sorry, I couldn't process that image right now. Ref: " + refId);
    }
  }

  private Optional<PhotoSize> selectBestPhoto(List<PhotoSize> photos) {
    if (photos == null || photos.isEmpty()) {
      return Optional.empty();
    }
    return photos.stream().max(Comparator.comparingLong(this::scorePhoto));
  }

  private long scorePhoto(PhotoSize photo) {
    Integer fileSize = photo.getFileSize();
    if (fileSize != null) {
      return fileSize;
    }
    Integer width = photo.getWidth();
    Integer height = photo.getHeight();
    if (width == null || height == null) {
      return 0;
    }
    return (long) width * height;
  }

  private byte[] downloadTelegramFile(String fileId) throws IOException, InterruptedException {
    String filePath = fetchFilePath(fileId);
    String url = "https://api.telegram.org/file/bot" + config.getTelegramBotToken() + "/" + filePath;
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .GET()
        .build();

    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() != 200) {
      logger.error("Telegram file download failed with status {}", response.statusCode());
      throw new IOException("Telegram file download failed with status " + response.statusCode());
    }
    return response.body();
  }

  private String fetchFilePath(String fileId) throws IOException, InterruptedException {
    String url = "https://api.telegram.org/bot" + config.getTelegramBotToken()
        + "/getFile?file_id=" + URLEncoder.encode(fileId, StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      logger.error("Telegram getFile failed with status {}", response.statusCode());
      throw new IOException("Telegram getFile failed with status " + response.statusCode());
    }
    Matcher matcher = FILE_PATH_PATTERN.matcher(response.body());
    if (!matcher.find()) {
      logger.error("Telegram getFile response missing file_path");
      throw new IOException("Telegram getFile response missing file_path");
    }
    return matcher.group(1);
  }

  private void sendPhoto(Long chatId, byte[] imageBytes) throws TelegramApiException {
    InputFile inputFile = new InputFile(new ByteArrayInputStream(imageBytes), "processed.jpg");
    SendPhoto sendPhoto = new SendPhoto(chatId.toString(), inputFile);
    execute(sendPhoto);
  }

  private void reply(Long chatId, String text) {
    SendMessage response = new SendMessage(chatId.toString(), text);
    try {
      execute(response);
    } catch (TelegramApiException e) {
      logger.error("Failed to send message to chat {}", chatId, e);
    }
  }

  private String extractCommand(String text) {
    if (!text.startsWith("/")) {
      return null;
    }
    String firstToken = text.split("\\s+", 2)[0];
    int atIndex = firstToken.indexOf('@');
    if (atIndex > 0) {
      return firstToken.substring(0, atIndex);
    }
    return firstToken;
  }

  private String extractArgument(String text) {
    String[] parts = text.split("\\s+", 2);
    if (parts.length < 2) {
      return null;
    }
    String candidate = parts[1].trim();
    return candidate.isEmpty() ? null : candidate;
  }

  private String buildHelpMessage() {
    return "Commands:\n"
        + "/start - start the bot\n"
        + "/help - show this help\n"
        + "/filter <name> - set filter\n"
        + "/paste <name> - set paste style\n"
        + "/status - show current settings\n"
        + "Allowed filters: " + String.join(", ", config.getAllowedFilterNames()) + "\n"
        + "Allowed paste styles: " + String.join(", ", config.getAllowedPasteStyles()) + "\n"
        + "Send a photo to process it with the current settings.";
  }
}
