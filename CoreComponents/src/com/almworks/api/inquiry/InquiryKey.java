package com.almworks.api.inquiry;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

public class InquiryKey <T> extends TypedKey<T> {
  protected InquiryKey(String name, @NotNull Class<T> keyClass) {
    super(name, keyClass, null);
  }

  public static <T> InquiryKey<T> inquiryKey(String name, @NotNull Class<T> keyClass) {
    return new InquiryKey<T>(name, keyClass);
  }
}
