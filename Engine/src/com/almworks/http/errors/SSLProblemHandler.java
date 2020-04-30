package com.almworks.http.errors;

import com.almworks.api.application.viewer.JEditorPaneWrapper;
import com.almworks.api.application.viewer.LinksEditorKit;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.application.TextDecoratorRegistryImpl;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.properties.Role;
import com.almworks.util.text.TextUtil;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class SSLProblemHandler {
  public static final Role<SSLProblemHandler> ROLE = Role.role(SSLProblemHandler.class);

  private static final LocalizedAccessor I18N = CurrentLocale.createAccessor(SSLProblemHandler.class.getClassLoader(), "com/almworks/http/errors/message");
  private static final String SNI_PROPERTY = "jsse.enableSNIExtension";

  private final SNIErrorHandler mySNIErrorHandler;
  private final DialogManager myDialogManager;
  private final AtomicBoolean myInteracting = new AtomicBoolean(false);
  private volatile boolean mySNIConfigured = false;
  private volatile boolean mySNIIgnored = false;

  public SSLProblemHandler(DialogManager dialogManager) {
    myDialogManager = dialogManager;
    mySNIErrorHandler = new SNIErrorHandler(this::handleSNIError);
  }

  public SNIErrorHandler getSNIErrorHandler() {
    return mySNIErrorHandler;
  }

  private void handleSNIError(URI uri) {
    if (mySNIConfigured || mySNIIgnored) return;
    if ("false".equals(Env.getString(SNI_PROPERTY))) {
      LogHelper.error("SNI property does has effect", Env.getString(SNI_PROPERTY), uri);
      return;
    }
    showSNIError(uri);
  }

  /**
   * Shows dialog which explains SNI problem
   * @param uri source that caused the problem
   */
  @ThreadSafe
  public void showSNIError(URI uri) {
    if (!myInteracting.compareAndSet(false, true)) return;
    SwingUtilities.invokeLater(() -> {
      try {
        boolean reconfigure = Form.askUser(myDialogManager.createBuilder("SSLProblems.SNIDialog"), uri);
        if (reconfigure) {
          mySNIConfigured = true;
          Env.changeProperties(Collections.singletonMap(SNI_PROPERTY, "false"));
        } else mySNIIgnored = true;
      } finally {
        myInteracting.set(false);
      }
    });
  }

  private static class Form {
    private JEditorPane myProblem = new JEditorPane();
    private JScrollPane myScrollPane = new JScrollPane(myProblem);

    public Form(String uri) {
      myProblem.setContentType("text/html");
      myProblem.setEditable(false);
      myScrollPane.setPreferredSize(new Dimension(800, 600));
      String text = I18N.getString("problem.SNI.text")
              .replaceAll("ARG_FONT", String.valueOf(new JLabel().getFont().getSize()))
              .replaceAll("ARG_URL_SHORT", TextUtil.truncateChars(uri, 30))
              .replaceAll("ARG_URL", uri);
      TextDecoratorRegistryImpl decorators = new TextDecoratorRegistryImpl();
      decorators.start();
      new JEditorPaneWrapper(myProblem, true, LinksEditorKit.create(decorators, true)).setText(text);
      UIUtil.scrollToTop(myProblem);
    }

    public static boolean askUser(DialogBuilder builder, URI uri) {
      DialogResult<Boolean> dr = new DialogResult<>(builder);
      DialogResult.configureYesNo(dr, "Reconfigure Client for Jira", "Ignore");
      dr.pack();
      Form form = new Form(uri.toString());
      return Boolean.TRUE.equals(dr.showModal("Network Problem", form.myScrollPane));
    }
  }
}
