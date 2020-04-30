package com.almworks.api.http;

import com.almworks.util.properties.Role;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;

public interface HttpClientProvider {
  Role<HttpClientProvider> ROLE = Role.role("httpClientProvider");

  HttpClient createHttpClient();

  HttpClientProvider SIMPLE = new HttpClientProvider() {
    public HttpClient createHttpClient() {
      return new HttpClient(new SimpleHttpConnectionManager());
    }
  };
}
