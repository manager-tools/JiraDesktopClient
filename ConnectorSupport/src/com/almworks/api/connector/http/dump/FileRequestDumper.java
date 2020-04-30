package com.almworks.api.connector.http.dump;

import com.almworks.api.connector.http.DumpUtils;
import com.almworks.api.connector.http.HttpDumper;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpReportAcceptor;
import com.almworks.api.http.HttpResponseData;
import com.almworks.util.LogPrivacyPolizei;
import com.almworks.util.commons.ProcedureE;
import com.almworks.util.io.DelegatingInputStream;
import org.almworks.util.Collections15;
import org.apache.commons.httpclient.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class FileRequestDumper extends RequestDumper implements HttpReportAcceptor {
  private final List<HttpDumper.DumpSpec> myDumperSpec;
  private final HttpMaterial myMaterial;
  private final String myScript;
  @Nullable
  private final LogPrivacyPolizei myPolizei;
  private final String myRequestUrl;
  private final List<String> myCookiesBefore;
  private final Map<String, String> myRequestHeaders = Collections15.hashMap();
  private final StringBuilder myMessage = new StringBuilder();
  private byte[] myRawRequest = null;
  private List<NameValuePair> myPostParameters;
  private ArrayList<String> myHttpReport;

  public FileRequestDumper(List<HttpDumper.DumpSpec> dumperSpec, HttpMaterial material, String script, @Nullable  LogPrivacyPolizei polizei, String url, List<String> cookiesBefore) {
    myDumperSpec = dumperSpec;
    myMaterial = material;
    myScript = script;
    myPolizei = polizei;
    myRequestUrl = url;
    myCookiesBefore = cookiesBefore;
  }

  @Override
  public void report(String method, URI uri, HttpVersion version, Header[] requestHeaders, StatusLine response, Header[] responseHeaders) {
    myHttpReport = Collections15.arrayList();
    DumpUtils.addHttpReport(method, uri, version, requestHeaders, response, responseHeaders, myHttpReport, myPolizei);
  }

  @Override
  public void finishWithException(@Nullable Throwable e) {
    for (HttpDumper.DumpSpec spec : myDumperSpec) {
      if(spec.getLevel() == HttpDumper.DumpLevel.NONE) continue;
      HttpDump.dumpAndClose(spec, this, e);
    }
  }


  @NotNull
  @Override
  public ResponseDumper responseObtained(@NotNull HttpResponseData response) {
    final HashMap<HttpDumper.DumpSpec, HttpDump> dumps = Collections15.hashMap();
    for (HttpDumper.DumpSpec spec : myDumperSpec) {
      if (dumps.containsKey(spec)) continue;
      HttpDump dump = HttpDump.dumpSuccess(spec, this);
      if (dump != null) dumps.put(spec, dump);
    }
    return new ResponseDumper(response) {
      @Override
      public void readStream(final ProcedureE<InputStream, IOException> reader) throws IOException {
        super.readStream(new ReaderWrapper(reader, dumps.values()));
        releaseConnection();
      }

      @Override
      protected void onCloseStream() {
        for (Map.Entry<HttpDumper.DumpSpec, HttpDump> entry : dumps.entrySet()) {
          entry.getValue().closeStream();
        }
      }
    };
  }

  @Override
  public void addRequestHeaders(@Nullable Map<String, String> requestHeaders) {
    if (requestHeaders != null) myRequestHeaders.putAll(requestHeaders);
  }

  @Override
  public void setRawRequest(byte[] bytes) {
    myRawRequest = bytes;
  }

  @Override
  public void setPostParameters(List<NameValuePair> parametersList) {
    myPostParameters = parametersList;
  }

  @Override
  public void addMessage(String message) {
    if (message == null || message.isEmpty()) return;
    if (myMessage.length() > 0) myMessage.append("\n");
    myMessage.append(message);
  }

  String getRequestUrl() {
    return myRequestUrl;
  }

  String getScript() {
    return myScript;
  }

  Cookie[] getCookiesAfter() {
    return myMaterial.getHttpClient().getState().getCookies();
  }

  LogPrivacyPolizei getPolizei() {
    return myPolizei;
  }

  List<String> getRequestCookies() {
    return myCookiesBefore;
  }

  @Nullable
  Map<String,String> getRequestHeaders() {
    return myRequestHeaders;
  }

  @Nullable
  byte[] getRawRequest() {
    return myRawRequest;
  }

  @Nullable
  List<NameValuePair> getPostParameters() {
    return myPostParameters;
  }

  List<String> getHttpReport() {
    return myHttpReport;
  }

  @Nullable
  String getMessage() {
    return myMessage.length() == 0 ? null : myMessage.toString();
  }

  private static class ReaderWrapper implements ProcedureE<InputStream, IOException> {
    private final ProcedureE<InputStream, IOException> myReader;
    private final Collection<HttpDump> myDumps;

    public ReaderWrapper(ProcedureE<InputStream, IOException> reader, Collection<HttpDump> dumps) {
      myReader = reader;
      myDumps = dumps;
    }

    @Override
    public void invoke(InputStream stream) throws IOException {
      myReader.invoke(new StreamDumper(stream));
    }

    private class StreamDumper extends DelegatingInputStream {
      public StreamDumper(InputStream stream) {
        super(stream);
      }

      @Override
      protected void byteRead(int read) {
        for (HttpDump dump : myDumps) dump.byteRead(read);
      }

      @Override
      protected void arrayRead(byte[] b, int off, int read) {
        for (HttpDump dump : myDumps) dump.arrayRead(b, off, read);
      }
    }
  }
}
