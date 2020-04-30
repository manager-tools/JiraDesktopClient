package com.almworks.api.connector.http;

import com.almworks.api.http.HttpUtils;
import com.almworks.util.GlobalLogPrivacy;
import com.almworks.util.LogPrivacyPolizei;
import org.almworks.util.Util;
import org.apache.commons.httpclient.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class DumpUtils {
  private static final SimpleDateFormat DAY_DIR_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final Date ourDumpDay = new Date();
  private static final SimpleDateFormat FILENAME_FORMAT = new SimpleDateFormat("DDD-HHmmss-SSS");
  private static final SimpleDateFormat TIMESTAMP_IN_FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private static final Comparator<NameValuePair> NVP_COMPARATOR = new Comparator<NameValuePair>() {
    public int compare(NameValuePair o1, NameValuePair o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  };

  private static String getDumpHost(URI uri) {
    String host;
    if (uri == null) {
      host = "unknown.host";
    } else {
      try {
        host = uri.getHost();
      } catch (URIException e) {
        host = "invalid.hostname";
      }
    }
    return host;
  }

  private static File makeDumpDir(File parent, String dir) throws DumpFileException {
    File result = new File(parent, dir);
    if (!result.exists())
      result.mkdir();
    if (!result.isDirectory())
      throw new DumpFileException("can't create directory " + result);
    return result;
  }

  private static String getDumpDayDirName() {
    String day;
    synchronized (DAY_DIR_FORMAT) {
      day = DAY_DIR_FORMAT.format(ourDumpDay);
    }
    return day;
  }

  private static String getDumpFilename(String script, URI uri) {
    String filename;
    synchronized (FILENAME_FORMAT) {
      filename = FILENAME_FORMAT.format(new Date());
    }
    String suffix;
    if (script != null) suffix = script.replace(':', '_');
    else suffix = getDumpScript(uri);
    filename = filename + '.' + suffix;
    return filename;
  }

  private static String getDumpScript(URI uri) {
    String script;
    if (uri == null) {
      script = "unknown";
    } else {
      try {
        String path = Util.NN(uri.getPath());
        int k = path.lastIndexOf('/');
        if (k >= 0)
          path = path.substring(k + 1);
        k = path.indexOf('.');
        if (k >= 0)
          path = path.substring(0, k);
        if (path.length() == 0)
          path = "index";
        script = path;
      } catch (URIException e) {
        script = "invalid";
      }
    }
    return script;
  }

  public static File getDumpFile(File logDir, String script, URI uri) throws DumpFileException {
    if (logDir == null)
      throw new DumpFileException("no connector log dir");
    if (!logDir.isDirectory())
      logDir.mkdirs();
    if (!logDir.isDirectory())
      throw new DumpFileException("cannot create log dir");
    String host = getDumpHost(uri);
    File hostDir = makeDumpDir(logDir, host);
    String day = getDumpDayDirName();
    File dayDir = makeDumpDir(hostDir, day);
    String filename = getDumpFilename(script, uri);
    return new File(dayDir, filename);
  }

  public static String privacy(String string, @Nullable LogPrivacyPolizei polizei) {
    String s = string;
    if (polizei != null) {
      s = polizei.examine(s);
    }
    return GlobalLogPrivacy.examineLogString(s);
  }

  static void dumpCookies(PrintStream out, List<String> afterDump, LogPrivacyPolizei polizei) {
    for (String string : afterDump) {
      string = privacy(string, polizei);
      out.print("   ");
      out.println(string);
    }
  }

  public static URI getDumpURI(String requestUrl) {
    URI uri;
    try {
      uri = requestUrl == null ? null : new URI(requestUrl, true);
    } catch (URIException e) {
      uri = null;
    }
    return uri;
  }

  public static void writeRequestData(PrintStream out, String requestUrl, List<String> cookiesBeforeRequest, Cookie[] cookiesAfter, @Nullable Map<String, String> requestHeaders,
    @Nullable List<NameValuePair> postParameters, @Nullable byte[] rawRequest, LogPrivacyPolizei polizei) {
    String timestamp;
    synchronized (TIMESTAMP_IN_FILE_FORMAT) {
      timestamp = TIMESTAMP_IN_FILE_FORMAT.format(new Date());
    }
    out.println("Dump time: " + timestamp);
    String url = requestUrl == null ? "unknown" : requestUrl;
    out.println("URL: " + privacy(url, polizei) + "\n");
    if (cookiesAfter.length > 0 || (cookiesBeforeRequest != null && !cookiesBeforeRequest.isEmpty())) {
      List<String> afterDump = HttpUtils.cookieDump(cookiesAfter);
      if (cookiesBeforeRequest == null || afterDump.equals(cookiesBeforeRequest)) {
        out.println("Cookies:");
        dumpCookies(out, afterDump, polizei);
      } else {
        out.println("Cookies Before:");
        dumpCookies(out, cookiesBeforeRequest, polizei);
        out.println("");
        out.println("Cookies After:");
        dumpCookies(out, afterDump, polizei);
      }
      out.println("");
    }
    if (requestHeaders != null) {
      out.println("Headers:");
      writeHeaders(requestHeaders, out, polizei);
    }
    if (postParameters != null) {
      out.println("POST Parameters:");
      Collections.sort(postParameters, NVP_COMPARATOR);
      for (NameValuePair pair : postParameters) {
        out.println("   " + privacy(pair.getName() + "=" + pair.getValue(), polizei));
      }
      out.println("");
    }
    if (rawRequest != null) {
      out.println("REQUEST (" + rawRequest.length + " bytes) :");
      out.println(privacy(new String(rawRequest), polizei));
      out.println("---");
      out.println("");
    }
    URI uri = getDumpURI(requestUrl);
    String query = null;
    if (uri != null)
      query = uri.getEscapedQuery();

    if (query != null && query.length() > 0) {
      String[] params = query.split("\\&");
      if (params != null && params.length > 0) {
        out.println("GET Parameters:");
        for (String param : params) {
          out.println("   " + privacy(param, polizei));
        }
      }
    }

    out.println();
    out.println();
  }

  private static void writeHeaders(Map<String, String> headers, PrintStream out, LogPrivacyPolizei polizei) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      out.println("   " + privacy(entry.getKey() + ": " + entry.getValue(), polizei));
    }
    out.println();
  }

  public static void addHttpReport(String method, URI uri, HttpVersion version, Header[] requestHeaders, StatusLine response, Header[] responseHeaders, List<String> target,
    @Nullable  LogPrivacyPolizei polizei) {
    String url = uri == null ? "?" : uri.getEscapedURI();
    target.add("");
    target.add(method + " " + privacy(url, polizei) + " " + version);
    if (requestHeaders != null) {
      for (Header header : requestHeaders) {
        target.add("   " + privacy(String.valueOf(header).trim(), polizei));
      }
    }
    if (response != null) {
      target.add("");
      target.add(Util.NN(String.valueOf(response)).trim());
      if (responseHeaders != null) {
        for (Header header : responseHeaders) {
          target.add("   " + privacy(String.valueOf(header).trim(), polizei));
        }
      }
    }
  }

  public static void writeHttpReport(PrintStream out, @Nullable List<String> report) {
    if (report == null) return;
    out.println("++++++++++++++++++ HTTP report ++++++++++++++++++");
    for (String s : report) {
      out.println(s);
    }
    out.println();
    out.println("+++++++++++++++++++++++++++++++++++++++++++++++++");
    out.println();
  }

  public static void startReply(PrintStream out, @Nullable String message, boolean success, @Nullable Throwable exception) {
    out.println("Reply: " + (success ? "successful" : "failed"));
    if (message != null) {
      out.println("Message: " + message);
    }
    if (exception != null) {
      out.println("Exception: " + exception);
      exception.printStackTrace(out);
    }
    out.println();
//    if (myResponseHeaders != null) {
//      out.println("Response Headers:");
//      writeHeaders(myResponseHeaders, out);
//    }
    out.println("================================================================================");
    out.println();
  }

  public static class DumpFileException extends Exception {
    public DumpFileException(String message) {
      super(message);
    }
  }
}
