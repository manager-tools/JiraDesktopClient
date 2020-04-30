package com.almworks.jira.provider3.sync.download2.meta.core;

import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoadedMetaKey<T> extends TypedKey<T> {
  private final LocalizedAccessor.Value myMissingMessage;

  public LoadedMetaKey(@NotNull String name, LocalizedAccessor.Value missingMessage) {
    super(name, null, null);
    myMissingMessage = missingMessage;
  }

  /**
   * @param missingMessage required if the data may be absent (due to errors our other reasons)
   */
  public static <T> LoadedMetaKey<T> createMetaKey(String name, LocalizedAccessor.Value missingMessage) {
    return new LoadedMetaKey<T>(name, missingMessage);
  }

  @Nullable
  public LocalizedAccessor.Value getMissingMessage() {
    return myMissingMessage;
  }
}
