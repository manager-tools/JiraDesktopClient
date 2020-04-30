package com.almworks.api.connector.http;

import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpReportAcceptor;
import com.almworks.api.http.HttpUtils;
import com.almworks.util.Enumerable;
import com.almworks.util.LogHelper;
import com.almworks.util.LogPrivacyPolizei;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;
import org.apache.commons.httpclient.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HttpDumper implements HttpReportAcceptor {

  private final List<DumpSpec> mySpecs;
  private final HttpMaterial myMaterial;

  @Nullable
  private Boolean mySuccess = null;

  @Nullable
  private Throwable myException = null;

  @Nullable
  private List<NameValuePair> myPostParameters = null;

  @Nullable
  private String myApplicationMessage = null;

  @Nullable
  private String myRequestUrl = null;

  @Nullable
  private String myResponse = null;

  @Nullable
  private String myScript = null;

  @Nullable
  private byte[] myRawRequest = null;

  @Nullable
  private Map<String, String> myHeaders = null;

  @Nullable
  private LogPrivacyPolizei myPolizei = null;

  @Nullable
  private Map<String, String> myResponseHeaders = null;

  @Nullable
  private List<String> myHttpReport = null;

  @Nullable
  private List<String> myCookiesBeforeRequest;

  public HttpDumper(HttpMaterial material, List<DumpSpec> specs) {
    mySpecs = specs;
    myMaterial = material;
  }

  public synchronized void clear() {
    myApplicationMessage = null;
    myException = null;
    myPostParameters = null;
    myRequestUrl = null;
    myResponse = null;
    myScript = null;
    mySuccess = null;
    myCookiesBeforeRequest = null;
  }

  public synchronized void dump() {
    try {
      dumpUnsafe();
    } catch (Exception e) {
      LogHelper.warning(e);
      // ignore
    }
  }

  public synchronized void setException(Throwable exception) {
    myException = exception;
  }

  public synchronized void setUrl(String url) {
    myRequestUrl = url;
  }

  public synchronized void setPostParameters(Collection<NameValuePair> postParameters) {
    myPostParameters = postParameters == null ? null : Collections15.arrayList(postParameters);
  }

  public synchronized void setScriptOverride(String script) {
    myScript = script;
  }

  public synchronized void setResponse(String loadedResponse) {
    myResponse = loadedResponse;
  }

  public synchronized void setSuccess(boolean success) {
    mySuccess = success;
  }

  private void dumpUnsafe() {
    if (mySpecs == null || mySpecs.isEmpty()) {
      return;
    }

    boolean success = mySuccess != null && mySuccess;

    for(final DumpSpec spec : mySpecs) {
      if(spec.getLevel() == DumpLevel.NONE) {
        continue;
      }
      if(success && spec.getLevel() == DumpLevel.ERRORS) {
        continue;
      }
      File dumpFile = null;
      FileOutputStream stream = null;
      PrintStream out = null;
      try {
        dumpFile = DumpUtils.getDumpFile(spec.getDir(), myScript, getDumpURI());
        assert dumpFile != null;
        stream = new FileOutputStream(dumpFile);
        out = new PrintStream(new BufferedOutputStream(stream));
        DumpUtils.writeRequestData(out, myRequestUrl, myCookiesBeforeRequest, myMaterial.getHttpClient().getState().getCookies(), myHeaders, myPostParameters, myRawRequest,
          myPolizei);
        writeHttpReport(out);
        writeReplyData(out, myResponse, myApplicationMessage, success, myException);
      } catch (DumpUtils.DumpFileException e) {
        LogHelper.debug("cannot create dump file: " + e.getMessage());
        return;
      } catch (IOException e) {
        LogHelper.debug("failed to write dump to " + dumpFile, e);
      } finally {
        IOUtils.closeStreamIgnoreExceptions(out);
        IOUtils.closeStreamIgnoreExceptions(stream);
      }
    }
  }

  private URI getDumpURI() {
    return DumpUtils.getDumpURI(myRequestUrl);
  }

  private void writeHttpReport(PrintStream out) {
    List<String> report;
    synchronized (this) {
      report = myHttpReport;
      myHttpReport = null;
    }
    DumpUtils.writeHttpReport(out, report);
  }

  private void writeReplyData(PrintStream out, String reply, String message, boolean success, Throwable exception) {
    DumpUtils.startReply(out, message, success, exception);
    if (reply == null)
      out.println("-- There was no response --");
    else
      out.print(DumpUtils.privacy(reply, myPolizei));
  }

  public synchronized void setMessage(String message) {
    myApplicationMessage = message;
  }

  public void setHeaders(Map<String, String> headers) {
    myHeaders = headers == null || headers.size() == 0 ? null : Collections15.hashMap(headers);
  }

  public void setRawRequest(byte[] bytes) {
    myRawRequest = bytes;
  }

  public void setPrivacyPolizei(LogPrivacyPolizei polizei) {
    myPolizei = polizei;
  }

  public void setResponseHeaders(Map<String, String> headers) {
    myResponseHeaders = headers == null || headers.size() == 0 ? null : Collections15.hashMap(headers);
  }

  public void report(String method, URI uri, HttpVersion version, Header[] requestHeaders, StatusLine response,
    Header[] responseHeaders)
  {
    List<String> report;
    synchronized (this) {
      if (myHttpReport == null)
        myHttpReport = Collections15.arrayList();
      report = myHttpReport;
    }
    DumpUtils.addHttpReport(method, uri, version, requestHeaders, response, responseHeaders, report, myPolizei);
  }

  public void saveCookiesBeforeRequest() {
    Cookie[] cookies = myMaterial.getHttpClient().getState().getCookies();
    myCookiesBeforeRequest = HttpUtils.cookieDump(cookies);
  }


  public static final class DumpLevel extends Enumerable {
    public static final DumpLevel ALL = new DumpLevel("ALL");
    public static final DumpLevel ERRORS = new DumpLevel("ERRORS");
    public static final DumpLevel NONE = new DumpLevel("NONE");

    private DumpLevel(String name) {
      super(name);
    }
  }


  public static class DumpSpec {
    private final DumpLevel myLevel;
    private final File myDir;

    public DumpSpec(DumpLevel level, File dir) {
      myLevel = level;
      myDir = dir;
    }

    public DumpLevel getLevel() {
      return myLevel;
    }

    public File getDir() {
      return myDir;
    }

    public static List<DumpSpec> listOfTwo(DumpSpec spec1, DumpSpec spec2) {
      if(spec1 == null && spec2 == null) {
        return null;
      }
      if(spec1 != null) {
        return spec2 == null ? Collections.singletonList(spec1) : Collections15.arrayList(spec1, spec2);
      }
      return Collections.singletonList(spec2);
    }

    public static List<DumpSpec> listOfOne(DumpLevel level, File dumpDir) {
      if(level != null && level != DumpLevel.NONE && dumpDir != null) {
        return Collections.singletonList(new DumpSpec(level, dumpDir));
      }
      return null;
    }
  }
}
