package com.almworks.util.properties;

/**
 * @author : Dyoma
 */
public interface StringLoader <T> {
  T restoreFromString(String string);
}
