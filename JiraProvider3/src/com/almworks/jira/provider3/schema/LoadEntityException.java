package com.almworks.jira.provider3.schema;

import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

public class LoadEntityException extends Exception {
  public LoadEntityException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  public static LoadEntityException create(Object ... message) {
    StringBuilder builder = new StringBuilder();
    Throwable cause = LogHelper.buildMessage(builder, message);
    return new LoadEntityException(builder.toString(), cause);
  }
}
