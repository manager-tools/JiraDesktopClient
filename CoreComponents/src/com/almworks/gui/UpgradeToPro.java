package com.almworks.gui;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.install.Setup;
import com.almworks.util.Terms;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.TextContentBuilder;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAbstractAction;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.PresentationKey;

/**
 * This class is used to show variations of the Upgrade to Pro dialog.
 */
public class UpgradeToPro {
  private static final String PRO_NAME = Setup.getProductName() + " Pro";

  private static final String UPGRADE_NOW =
    "<br><br>" +
      "Upgrading to " + PRO_NAME + " gives you:" +
      "<br>- Unlimited " + Terms.ref_ConnectionType + " connections;" +
      "<br>- Unlimited $(app.term.artifacts);" +
      "<br>- Timely customer support and updates.";

  private static final String UPGRADE_NOW_USERS =
    "<br><br>" +
      "Upgrading to " + PRO_NAME + " allows you to work with " + Terms.ref_ConnectionType + " servers " +
      "that run on any license. Commercial license users also benefit from " +
      "the timely customer support and updates.";

  public static void showApproachingLimit(int passed, int limit, String connectionName) {
    final String message =
      "<html>There are " + passed + "+ " + Terms.ref_artifacts + " in " + Terms.ref_ConnectionType + " already. " +
        "Please note that connection \"" + connectionName + "\" will work until there are " + limit + " " + Terms.ref_artifacts + " on the server. " +
        "Synchronization will be disabled once this limit is exceeded." +
        "<br><br>" +
        "Please consider upgrading to " + PRO_NAME + "." + UPGRADE_NOW; 

    setupAndShowDialog("Approaching " + Terms.ref_Artifact + " Limit", message, true);
  }

  public static void showLimitExceeded(int limit, String connectionName) {
    final String message =
      "<html>There are more than " + limit + " " + Terms.ref_artifacts + " in " + Terms.ref_ConnectionType + ". " +
        "Connection \"" + connectionName + "\" is offline, synchronization is disabled." +
        "<br><br>" +
        "Please upgrade to " + PRO_NAME + "." + UPGRADE_NOW;

    setupAndShowDialog(Terms.ref_Artifact + " Limit Exceeded", message, true);
  }

  public static void showUserLimitExceeded(String connectionName) {
    final String message =
      "<html>There are too many users in " + Terms.ref_ConnectionType + ". " +
        "Connection \"" + connectionName + "\" is offline, synchronization is disabled." +
        "<br><br>" +
        "Please upgrade to " + PRO_NAME + "." + UPGRADE_NOW_USERS;

    setupAndShowDialog("User Limit Exceeded", message, true);
  }

  public static void showGeneralDownload(String title, String message) {
    setupAndShowDialog(title, message, false);
  }

  private static void setupAndShowDialog(final String title, final String message, final boolean upgrade) {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        setupDialog(Local.parse(title), Local.parse(message), upgrade).showWindow();
      }
    });
  }

  private static DialogBuilder setupDialog(String title, String message, final boolean upgrade) {
    final DialogManager dialogManager = Context.require(DialogManager.ROLE);
    final DialogBuilder builder = dialogManager.createBuilder("upgradeToPro");
    builder.setIgnoreStoredSize(true);
    builder.setTitle(title);
    builder.setModal(true);
    TextContentBuilder tcb = new TextContentBuilder(title, message);
    if (upgrade) {
      tcb.appendLink(Setup.getUrlDownload(), "Download " + PRO_NAME + " (with 30-day free trial)", 12);
      tcb.appendLink(Setup.getUrlPurchase(), "Purchase", 0);
      tcb.appendLink(Setup.getUrlRfq(), "Request a quote", 0);
    }
    builder.setContent(tcb.getComponent());
    builder.setCancelAction("Close");
    builder.setOkAction(new AnAbstractAction("Go to Download") {
      {
        final String tooltip = "Open web site to download "
          + (upgrade ? PRO_NAME : Setup.getProductName());
        setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, tooltip);
      }

      public void perform(ActionContext context) throws CantPerformException {
        ExternalBrowser.openURL(Setup.getUrlDownload(), true);
      }
    });
    return builder;
  }
}