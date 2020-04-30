package com.almworks.api.connector.http.dump;

import com.almworks.api.http.HttpResponseData;
import com.almworks.util.commons.ProcedureE;
import org.apache.commons.httpclient.URI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ResponseDumper implements HttpResponseData {
  private final HttpResponseData myResponse;

  public ResponseDumper(HttpResponseData response) {
    myResponse = response;
  }

  public String getCharset() {
    return myResponse.getCharset();
  }

  @Override
  public String getFullContentType() {
    return myResponse.getFullContentType();
  }

  @Override
  public void readStream(ProcedureE<InputStream, IOException> reader) throws IOException {
    myResponse.readStream(reader);
  }

  @Override
  public void releaseConnection() {
    onCloseStream();
    myResponse.releaseConnection();
  }

  protected void onCloseStream() {}

  public long getContentLength() {
    return myResponse.getContentLength();
  }

  public int getStatusCode() {
    return myResponse.getStatusCode();
  }

  public URI getLastURI() {
    return myResponse.getLastURI();
  }

  public String getStatusText() {
    return myResponse.getStatusText();
  }

  public Map<String, String> getResponseHeaders() {
    return myResponse.getResponseHeaders();
  }

  public String getContentType() {
    return myResponse.getContentType();
  }

  public String getContentFilename() {
    return myResponse.getContentFilename();
  }
}
