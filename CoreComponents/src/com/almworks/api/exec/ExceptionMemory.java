package com.almworks.api.exec;

import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ExceptionMemory {
  Role<ExceptionMemory> ROLE = Role.role(ExceptionMemory.class);

  boolean remembers(@NotNull ExceptionHash hash);

  void remember(@NotNull Collection<ExceptionHash> hashes);
}
