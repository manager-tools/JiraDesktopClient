package com.almworks.util.threads;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({METHOD, TYPE, CONSTRUCTOR})
public @interface CanBlock {
  String value() default "should not be called from AWT thread";
  String becauseOf() default "";
}
