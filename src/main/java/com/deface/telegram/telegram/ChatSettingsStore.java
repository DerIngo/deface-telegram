package com.deface.telegram.telegram;

import com.deface.telegram.config.AppConfig;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ChatSettingsStore {
  private final ConcurrentMap<Long, ChatSettings> settingsByChat = new ConcurrentHashMap<>();
  private final AppConfig config;

  public ChatSettingsStore(AppConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  public ChatSettings get(Long chatId) {
    return settingsByChat.computeIfAbsent(chatId, id -> new ChatSettings(
        config.getDefaultFilterName(),
        config.getDefaultPasteStyle()
    ));
  }

  public ChatSettings updateFilter(Long chatId, String filterName) {
    if (!config.getAllowedFilterNames().contains(filterName)) {
      throw new IllegalArgumentException("Invalid filter. Allowed: "
          + String.join(", ", config.getAllowedFilterNames()));
    }
    return settingsByChat.compute(chatId, (id, existing) -> {
      ChatSettings current = existing == null
          ? new ChatSettings(config.getDefaultFilterName(), config.getDefaultPasteStyle())
          : existing;
      return new ChatSettings(filterName, current.pasteStyle());
    });
  }

  public ChatSettings updatePasteStyle(Long chatId, String pasteStyle) {
    if (!config.getAllowedPasteStyles().contains(pasteStyle)) {
      throw new IllegalArgumentException("Invalid paste style. Allowed: "
          + String.join(", ", config.getAllowedPasteStyles()));
    }
    return settingsByChat.compute(chatId, (id, existing) -> {
      ChatSettings current = existing == null
          ? new ChatSettings(config.getDefaultFilterName(), config.getDefaultPasteStyle())
          : existing;
      return new ChatSettings(current.filterName(), pasteStyle);
    });
  }

  public record ChatSettings(String filterName, String pasteStyle) {
  }
}
