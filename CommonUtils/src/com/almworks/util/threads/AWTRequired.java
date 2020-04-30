package com.almworks.util.threads;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, TYPE, FIELD, CONSTRUCTOR, PARAMETER})
public @interface AWTRequired {
  String value() default "method MUST be called in AWT thread";
}