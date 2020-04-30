package com.almworks.util.threads;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({METHOD, TYPE})
public @interface ThreadSafe {
  String value() default "concurrent access to the class/method is supported";
}
