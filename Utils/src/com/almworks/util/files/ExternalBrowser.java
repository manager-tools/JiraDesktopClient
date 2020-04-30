package com.almworks.util.files;

import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.DialogsUtil;
import org.almworks.util.RuntimeInterruptedException;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author dyoma
 */
public class ExternalBrowser {
  private static final Pattern HAS_PROTOCOL = Pattern.compile("^\\w{3,}://");
  private static final Pattern IS_MAILTO = Pattern.compile("^mailto:");
  private static final Pattern FTP_PROTOCOL = Pattern.compile("^ftp\\.", Pattern.CASE_INSENSITIVE);

  /**
   * On Mac OS X the {@code MRJFileUtils.openURL()} method
   * used by {@link BrowserLauncher} doesn't "get" URLs without
   * a protocol, like a simple "www.google.com". This method
   * tries to guess the potocol for such URLs.
   * @param url The source URL.
   * @return On non-Mac systems, returns {@code url}. On Mac
   * systems prepends the guessed protocol name if needed.
   */
  private static String protocolize(String url) {
    if(!Env.isMac()) {
      return url;
    }
    if(HAS_PROTOCOL.matcher(url).find()) {
      return url;
    }
    if(IS_MAILTO.matcher(url).find()) {
      return url;
    }
    if(FTP_PROTOCOL.matcher(url).find()) {
      return "ftp://" + url;
    }
    return "http://" + url;
  }

  private String myUrl;
  private boolean myEncoded;
  private Procedure<IOException> myExceptionHandler;

  public void setUrl(String url, boolean encoded) {
    myUrl = protocolize(url);
    myEncoded = encoded;
  }

  public void setExceptionHandler(Procedure<IOException> exceptionHandler) {
    myExceptionHandler = exceptionHandler;
  }

  public void setDialogHandler(final String title, final String message) {
    myExceptionHandler = new Procedure<IOException>() {
      public void invoke(IOException arg) {
        DialogsUtil.showException(title, message, arg);
      }
    };
  }

  public void openBrowser() {
    assert myUrl != null;
    assert myExceptionHandler != null;
    ThreadGate.LONG(ExternalBrowser.class).execute(new Runnable() {
      public void run() {
        try {
          runRegisteredProgram(myUrl, myEncoded);
        } catch (IOException e) {
          myExceptionHandler.invoke(e);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    });
  }

  private static void runRegisteredProgram(String cmdLine, boolean encoded) throws IOException, InterruptedException {
    Threads.assertLongOperationsAllowed();
    BrowserLauncher.openURL(cmdLine, encoded);
  }

  public static void openURL(String url, boolean encoded) {
    ExternalBrowser browser = createOpenInBrowser(url, encoded);
    browser.openBrowser();
  }

  public static ExternalBrowser createOpenInBrowser(String url, boolean encoded) {
    ExternalBrowser browser = new ExternalBrowser();
    browser.setUrl(url, encoded);
    browser.setDialogHandler(L.dialog("Open Browser"), L.content("Failed to open browser"));
    return browser;
  }
}
