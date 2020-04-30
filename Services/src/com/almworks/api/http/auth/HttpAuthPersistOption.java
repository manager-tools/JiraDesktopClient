package com.almworks.api.http.auth;

/**
 * :todoc:
 *
 * @author sereda
 */
public class HttpAuthPersistOption {
  public static final HttpAuthPersistOption KEEP_ON_DISK = new HttpAuthPersistOption("keepOnDisk");
  public static final HttpAuthPersistOption KEEP_IN_MEMORY = new HttpAuthPersistOption("keepInMemory");
  public static final HttpAuthPersistOption DONT_KEEP = new HttpAuthPersistOption("dontKeep");

  private final String myName;

  private HttpAuthPersistOption(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }
}
