package com.almworks.util.fileformats;

/**
 * :todoc:
 *
 * @author sereda
 */
public class FileFormatException extends Exception {
  public FileFormatException() {
    super();
  }

  public FileFormatException(String message) {
    super(message);
  }

  public FileFormatException(Throwable cause) {
    super(cause);
  }

  public FileFormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
