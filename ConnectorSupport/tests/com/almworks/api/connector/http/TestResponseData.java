package com.almworks.api.connector.http;

import com.almworks.api.http.HttpResponseData;
import com.almworks.util.commons.ProcedureE;
import com.almworks.util.io.IOUtils;
import org.apache.commons.httpclient.URI;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
* @author Igor Sereda
*/
class TestResponseData implements HttpResponseData {
  private final String myResponse;

  public TestResponseData(String response) {
    myResponse = response;
  }

  @Override
  public String getContentFilename() {
    return null;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public String getFullContentType() {
    return null;
  }

  @Override
  public long getContentLength() {
    return myResponse.length();
  }

  @Override
  public void readStream(ProcedureE<InputStream, IOException> reader) throws IOException {
    ByteArrayInputStream stream = new ByteArrayInputStream(myResponse.getBytes("UTF-8"));
    reader.invoke(stream);
    IOUtils.closeStreamIgnoreExceptions(stream);
  }

  @NotNull
  @Override
  public Map<String, String> getResponseHeaders() {
    return new HashMap<String, String>();
  }

  @Override
  public URI getLastURI() {
    return null;
  }

  @Override
  public int getStatusCode() {
    throw new RuntimeException("Not implemented");
  }

  @NotNull
  @Override
  public String getStatusText() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public String getCharset() {
    return "UTF-8";
  }

  @Override
  public void releaseConnection() {
  }
}
