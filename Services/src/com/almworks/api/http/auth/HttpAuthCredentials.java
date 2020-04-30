package com.almworks.api.http.auth;

public class HttpAuthCredentials {
  private final String myUsername;
  private final String myPassword;

  public HttpAuthCredentials(String username, String password) {
    myPassword = password;
    myUsername = username;
  }

  public String getPassword() {
    return myPassword;
  }

  public String getUsername() {
    return myUsername;
  }

  public String toString() {
    return myUsername + ":***";
  }
}
