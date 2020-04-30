package com.almworks.spi.provider.wizard;

import com.almworks.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
* Used to simulate non-local returns.
*/
public final class CheckFailed extends Exception {
  private final String myExplanation;

  public CheckFailed(@NotNull String message, @Nullable String explanation) {
    super(message);
    myExplanation = explanation;
  }

  public CheckFailed(@NotNull String message) {
    this(message, null);
  }

  public Pair<String, String> asStringPair() {
    return Pair.create(getMessage(), myExplanation);
  }
}
