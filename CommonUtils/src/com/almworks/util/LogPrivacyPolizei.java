package com.almworks.util;

import org.jetbrains.annotations.NotNull;

public interface LogPrivacyPolizei {
  @NotNull String examine(@NotNull String messageToBeLogged);
}
