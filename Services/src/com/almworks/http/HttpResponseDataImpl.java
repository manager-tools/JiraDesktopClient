package com.almworks.http;

import com.almworks.api.http.HttpLoader;
import com.almworks.api.http.HttpResponseData;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.ProcedureE;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseDataImpl implements HttpResponseData {
  private String myCharset = HttpLoader.DEFAULT_CHARSET;
  private InputStream myContentStream;
  private String myContentType;
  private String myFullContentType;
  private String myContentFilename;
  private long myContentLength = -1;
  private HashMap<String, String> myResponseHeaders = null;

  @Nullable
  private HttpMethodExecutor myExecutor;

  @Nullable
  private URI myLastURI;

  public HttpResponseDataImpl(HttpMethodExecutor executor) {
    myExecutor = executor;
  }

  @Override
  public String getCharset() {
    return myCharset;
  }

  // todo refactor - polymorphic transfer operation

  @Override
  public void readStream(ProcedureE<InputStream, IOException> reader) throws IOException {
    if (myContentStream == null) LogHelper.error("No input stream");
    else
      try {
        reader.invoke(myContentStream);
      } finally {
        myContentStream = null;
        releaseConnection();
      }
  }

  @Override
  public void releaseConnection() {
    HttpMethodExecutor executor = myExecutor;
    if (executor != null) {
      try {
        executor.dispose();
      } catch (Exception e) {
        Log.debug("cannot release connection", e);
      }
      myExecutor = null;
    }
  }


  public void setCharset(String charset) {
    try {
      if (charset == null || charset.length() == 0 || !Charset.isSupported(charset)) {
        Log.warn("charset " + charset + " is not supported by the operating system, falling back to " + HttpLoader
          .DEFAULT_CHARSET);
        charset = HttpLoader.DEFAULT_CHARSET;
      }
    } catch (java.nio.charset.IllegalCharsetNameException e) {
      Log.warn("illegal charset: " + charset, e);
      charset = HttpLoader.DEFAULT_CHARSET;
    }
    myCharset = charset;
  }

  public void setContentStream(InputStream contentStream) {
    myContentStream = contentStream;
  }

  public String getContentType() {
    return myContentType;
  }

  public String getFullContentType() {
    return myFullContentType;
  }

  public long getContentLength() {
    return myContentLength;
  }

  public String getContentFilename() {
    return myContentFilename;
  }


  @NotNull
  public Map<String, String> getResponseHeaders() {
    HashMap<String, String> headers = myResponseHeaders;
    if (headers == null) {
      return Collections15.emptyMap();
    } else {
      return Collections.unmodifiableMap(headers);
    }
  }

  @Nullable
  public URI getLastURI() {
    return myLastURI;
  }

  @Override
  public int getStatusCode() {
    HttpMethodExecutor executor = myExecutor;
    return executor != null ? executor.getStatusCode() : 0;
  }

  @NotNull
  @Override
  public String getStatusText() {
    HttpMethodExecutor executor = myExecutor;
    return executor != null ? executor.getStatusText() : "";
  }

  public void setLastURI(@Nullable URI lastURI) {
    myLastURI = lastURI;
  }

  void setResponseHeaders(Header[] headers) {
    myResponseHeaders = Collections15.hashMap();
    for (Header header : headers) {
      myResponseHeaders.put(header.getName(), header.getValue());
    }
  }

  void setContentType(String contentType) {
    myContentType = contentType;
  }

  void setFullContentType(String fullContentType) {
    myFullContentType = fullContentType;
  }

  void setContentFilename(String name) {
    myContentFilename = name;
  }

  void setContentLength(long length) {
    myContentLength = length;
  }

  static {
    assert Charset.isSupported(HttpLoader.DEFAULT_CHARSET);
  }
}
