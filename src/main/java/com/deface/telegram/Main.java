package com.deface.telegram;

import com.deface.telegram.config.AppConfig;

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
    } catch (IllegalStateException e) {
      System.err.println("Configuration error: " + e.getMessage());
      System.err.println("Set required environment variables or provide values in .env/application.properties.");
      System.exit(1);
    }
  }

  private static String safe(String value) {
    return value == null ? "(unset)" : value;
  }
}
