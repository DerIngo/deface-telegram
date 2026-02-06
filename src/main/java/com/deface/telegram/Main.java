package com.deface.telegram;

import com.deface.telegram.config.AppConfig;
import com.deface.telegram.telegram.ChatSettingsStore;
import com.deface.telegram.telegram.DefaceTelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public final class Main {
  private Main() {
  }

  public static void main(String[] args) {
    try {
      AppConfig config = AppConfig.load();

      System.out.println("Deface Telegram Bot starting");
      System.out.println("DEFACE_ENDPOINT=" + safe(config.getDefaceEndpoint()));
      System.out.println("DEFAULT_FILTER_NAME=" + config.getDefaultFilterName());
      System.out.println("DEFAULT_PASTE_STYLE=" + config.getDefaultPasteStyle());

      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      ChatSettingsStore settingsStore = new ChatSettingsStore(config);
      botsApi.registerBot(new DefaceTelegramBot(config, settingsStore));
    } catch (IllegalStateException e) {
      System.err.println("Configuration error: " + e.getMessage());
      System.err.println("Set required environment variables or provide values in .env/application.properties.");
      System.exit(1);
    } catch (TelegramApiException e) {
      System.err.println("Failed to start Telegram bot: " + e.getMessage());
      System.exit(1);
    }
  }

  private static String safe(String value) {
    return value == null ? "(unset)" : value;
  }
}
