package com.almworks.util.threads;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({METHOD})
public @interface AvoidLocks {
  String value() default "avoid calling this method under locks - deadlocks are possible";
}
