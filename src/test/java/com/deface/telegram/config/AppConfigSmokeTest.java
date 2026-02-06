package com.deface.telegram.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigSmokeTest {
  @Test
  void loadsFromTestProperties() {
    boolean envOverrides = System.getenv("TELEGRAM_BOT_TOKEN") != null
        || System.getenv("DEFACE_ENDPOINT") != null
        || System.getenv("ALLOWED_FILTER_NAMES") != null
        || System.getenv("ALLOWED_PASTE_STYLES") != null
        || System.getenv("DEFAULT_FILTER_NAME") != null
        || System.getenv("DEFAULT_PASTE_STYLE") != null;
    boolean dotenvPresent = Files.exists(Path.of(".env"));

    Assumptions.assumeTrue(!envOverrides && !dotenvPresent,
        "Environment or .env overrides are set; skipping property-based smoke test.");

    AppConfig config = AppConfig.load();

    assertEquals("test-token", config.getTelegramBotToken());
    assertEquals("https://example.test/api/deface-image", config.getDefaceEndpoint());
    assertEquals("blur", config.getDefaultFilterName());
    assertEquals("feathered", config.getDefaultPasteStyle());
    assertTrue(config.getAllowedFilterNames().contains("blur"));
    assertTrue(config.getAllowedPasteStyles().contains("hard"));
  }
}
