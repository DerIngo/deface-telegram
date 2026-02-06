package com.deface.telegram.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class AppConfig {
  private final String telegramBotToken;
  private final String telegramBotUsername;
  private final String defaceEndpoint;
  private final String defaultFilterName;
  private final String defaultPasteStyle;
  private final java.util.List<String> allowedFilterNames;
  private final java.util.List<String> allowedPasteStyles;

  private AppConfig(
      String telegramBotToken,
      String telegramBotUsername,
      String defaceEndpoint,
      String defaultFilterName,
      String defaultPasteStyle,
      java.util.List<String> allowedFilterNames,
      java.util.List<String> allowedPasteStyles
  ) {
    this.telegramBotToken = telegramBotToken;
    this.telegramBotUsername = telegramBotUsername;
    this.defaceEndpoint = defaceEndpoint;
    this.defaultFilterName = defaultFilterName;
    this.defaultPasteStyle = defaultPasteStyle;
    this.allowedFilterNames = allowedFilterNames;
    this.allowedPasteStyles = allowedPasteStyles;
  }

  public static AppConfig load() {
    Properties properties = loadProperties("application.properties");
    Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();

    String telegramBotToken = resolve("TELEGRAM_BOT_TOKEN", dotenv, properties, null);
    String telegramBotUsername = resolve("TELEGRAM_BOT_USERNAME", dotenv, properties, null);
    String defaceEndpoint = resolve("DEFACE_ENDPOINT", dotenv, properties, null);
    String defaultFilterName = resolve("DEFAULT_FILTER_NAME", dotenv, properties, null);
    String defaultPasteStyle = resolve("DEFAULT_PASTE_STYLE", dotenv, properties, null);
    String allowedFilterNamesRaw = resolve("ALLOWED_FILTER_NAMES", dotenv, properties, null);
    String allowedPasteStylesRaw = resolve("ALLOWED_PASTE_STYLES", dotenv, properties, null);

    requirePresent(telegramBotToken, "TELEGRAM_BOT_TOKEN");
    requirePresent(defaceEndpoint, "DEFACE_ENDPOINT");

    requirePresent(allowedFilterNamesRaw, "ALLOWED_FILTER_NAMES");
    requirePresent(allowedPasteStylesRaw, "ALLOWED_PASTE_STYLES");

    var allowedFilterNames = parseList(allowedFilterNamesRaw, "ALLOWED_FILTER_NAMES");
    var allowedPasteStyles = parseList(allowedPasteStylesRaw, "ALLOWED_PASTE_STYLES");

    if (defaultFilterName == null) {
      defaultFilterName = "blur";
    }
    if (defaultPasteStyle == null) {
      defaultPasteStyle = "feathered";
    }

    validateInList("DEFAULT_FILTER_NAME", defaultFilterName, allowedFilterNames);
    validateInList("DEFAULT_PASTE_STYLE", defaultPasteStyle, allowedPasteStyles);

    return new AppConfig(
        telegramBotToken,
        telegramBotUsername,
        defaceEndpoint,
        defaultFilterName,
        defaultPasteStyle,
        allowedFilterNames,
        allowedPasteStyles
    );
  }

  public String getTelegramBotToken() {
    return telegramBotToken;
  }

  public Optional<String> getTelegramBotUsername() {
    return Optional.ofNullable(telegramBotUsername);
  }

  public String getDefaceEndpoint() {
    return defaceEndpoint;
  }

  public String getDefaultFilterName() {
    return defaultFilterName;
  }

  public String getDefaultPasteStyle() {
    return defaultPasteStyle;
  }

  public java.util.List<String> getAllowedFilterNames() {
    return allowedFilterNames;
  }

  public java.util.List<String> getAllowedPasteStyles() {
    return allowedPasteStyles;
  }

  private static String resolve(
      String key,
      Dotenv dotenv,
      Properties properties,
      String defaultValue
  ) {
    String envValue = normalize(System.getenv(key));
    if (envValue != null) {
      return envValue;
    }

    String dotenvValue = normalize(dotenv.get(key));
    if (dotenvValue != null) {
      return dotenvValue;
    }

    String propertyValue = normalize(properties.getProperty(key));
    if (propertyValue != null) {
      return propertyValue;
    }

    return defaultValue;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void requirePresent(String value, String key) {
    if (value == null) {
      throw new IllegalStateException("Missing required configuration: " + key);
    }
  }

  private static void validateInList(String key, String value, java.util.List<String> allowed) {
    if (!allowed.contains(value)) {
      throw new IllegalStateException(
          "Invalid " + key + ": " + value + ". Allowed values: " + String.join(", ", allowed)
      );
    }
  }

  private static java.util.List<String> parseList(String raw, String key) {
    String[] parts = raw.split(",");
    java.util.List<String> values = new java.util.ArrayList<>();
    for (String part : parts) {
      String normalized = normalize(part);
      if (normalized != null) {
        values.add(normalized);
      }
    }
    if (values.isEmpty()) {
      throw new IllegalStateException("Missing or empty configuration: " + key);
    }
    return java.util.List.copyOf(values);
  }

  private static Properties loadProperties(String resourceName) {
    Properties properties = new Properties();
    try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (input != null) {
        properties.load(input);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load " + resourceName, e);
    }
    return properties;
  }
}
