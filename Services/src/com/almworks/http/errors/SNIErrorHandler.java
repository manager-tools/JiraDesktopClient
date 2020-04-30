package com.almworks.http.errors;

import com.almworks.util.LogHelper;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Suggests to workaround SNI extensions<br>
 * See: http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html <br>
 *
 */
public class SNIErrorHandler {
  private static final Pattern WEBKIT_MESSAGE = Pattern.compile(".*url:\\s+\\[([^\\]]+)],\\s+.*message:\\s+\\[([^\\]]+)],.*");

  @Nullable
  private final Consumer<URI> myConsumer;

  public SNIErrorHandler(@Nullable Consumer<URI> consumer) {
    myConsumer = consumer;
  }

  /**
   * Process log records from com.sun.webkit.network.URLLoader#notifyDidFail
   * Looks for messages like: errorCode: [99], url: [https://git.e-soft.ca/jira/secure/Dashboard.jspa], message: [handshake alert:  unrecognized_name], data: [0x00007F84205A5870]
   * handshake alert:  unrecognized_name - means that server does not handle SSL/TLS extensions
   *
   * @param logRecord log record to analyze
   */
  public void checkWebkitMessage(LogRecord logRecord) {
    String url = isMyWebkitMessage(logRecord);
    if (url == null) return;
    problematicServer(url);
  }

  /**
   * Analyses log record and returns URL if SNI problem is logged
   * @param logRecord record to analyse
   * @return null if unrelated record or problematic URL if the record contains SNI error
   */
  public static String isMyWebkitMessage(LogRecord logRecord) {
    String message = logRecord.getMessage();
    if (!message.startsWith("errorCode: [")) return null;
    Matcher m = WEBKIT_MESSAGE.matcher(message);
    if (!m.matches()) return null;
    String url = m.group(1);
    String error = m.group(2);
    return isMyProblem(error) ? url : null;
  }

  private static boolean isMyProblem(String error) {
    return error.contains("handshake alert") && error.contains("unrecognized_name");
  }

  public void checkException(Throwable exception, String failedUrl) {
    SSLProtocolException sslProtocolException = Util.castNullable(SSLProtocolException.class, ExceptionUtil.findException(exception, t -> t instanceof SSLProtocolException));
    if (sslProtocolException == null) return;
    if (!isMyProblem(sslProtocolException.getMessage())) return;
    problematicServer(failedUrl);
  }

  private void problematicServer(String url) {
    LogHelper.warning("Problematic server detected: ", url);
    if (myConsumer == null) return;
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      LogHelper.error(e);
      return;
    }
    myConsumer.accept(uri);
  }
}
