package com.deface.telegram.telegram;

import com.deface.telegram.config.AppConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

public final class DefaceTelegramBot extends TelegramLongPollingBot {
  private final AppConfig config;
  private final ChatSettingsStore settingsStore;

  public DefaceTelegramBot(AppConfig config, ChatSettingsStore settingsStore) {
    this.config = Objects.requireNonNull(config, "config");
    this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore");
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update == null || !update.hasMessage()) {
      return;
    }

    Message message = update.getMessage();
    if (!message.hasText()) {
      return;
    }

    String text = message.getText().trim();
    String command = extractCommand(text);
    if (command == null) {
      return;
    }

    if ("/start".equals(command)) {
      reply(message.getChatId(), "Welcome. Send /help for available commands.");
      return;
    }

    if ("/help".equals(command)) {
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

  @Override
  public String getBotUsername() {
    return config.getTelegramBotUsername().orElse("");
  }

  @Override
  public String getBotToken() {
    return config.getTelegramBotToken();
  }

  private void reply(Long chatId, String text) {
    SendMessage response = new SendMessage(chatId.toString(), text);
    try {
      execute(response);
    } catch (TelegramApiException e) {
      System.err.println("Failed to send message: " + e.getMessage());
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
        + "Allowed paste styles: " + String.join(", ", config.getAllowedPasteStyles());
  }
}
