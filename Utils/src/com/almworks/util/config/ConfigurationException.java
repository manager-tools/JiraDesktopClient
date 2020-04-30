package com.almworks.util.config;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ConfigurationException extends Exception {
  public ConfigurationException(Throwable cause) {
    super(cause);
  }

  public ConfigurationException(String message) {
    super(message);
  }


  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
