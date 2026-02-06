package com.deface.telegram;

import com.deface.telegram.config.AppConfig;
import com.deface.telegram.deface.DefaceClient;
import com.deface.telegram.telegram.ChatSettingsStore;
import com.deface.telegram.telegram.DefaceTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public final class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private Main() {
  }

  public static void main(String[] args) {
    try {
      AppConfig config = AppConfig.load();

      logger.info("Deface Telegram Bot starting");
      logger.info("DEFACE_ENDPOINT={}", safe(config.getDefaceEndpoint()));
      logger.info("DEFAULT_FILTER_NAME={}", config.getDefaultFilterName());
      logger.info("DEFAULT_PASTE_STYLE={}", config.getDefaultPasteStyle());

      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      DefaceClient defaceClient = new DefaceClient(config);
      ChatSettingsStore settingsStore = new ChatSettingsStore(config);
      botsApi.registerBot(new DefaceTelegramBot(config, defaceClient, settingsStore));
      logger.info("Telegram bot registered and polling");
    } catch (IllegalStateException e) {
      logger.error("Configuration error: {}", e.getMessage(), e);
      logger.error("Set required environment variables or provide values in .env/application.properties.");
      System.exit(1);
    } catch (TelegramApiException e) {
      logger.error("Failed to start Telegram bot", e);
      System.exit(1);
    }
  }

  private static String safe(String value) {
    return value == null ? "(unset)" : value;
  }
}
