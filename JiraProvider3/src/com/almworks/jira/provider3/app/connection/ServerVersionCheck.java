package com.almworks.jira.provider3.app.connection;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.util.SyncNotAllowedException;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.io.persist.PersistableBoolean;
import com.almworks.util.io.persist.PersistableLong;
import org.almworks.util.Const;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerVersionCheck implements ConnectorManager.CheckServer {
  public static final String EARLIEST_SUPPORTED_VERSION = "5.0";
  public static final String NEXT_CHECK = "NC";
  public static final String SUPPORTED_FLAG = "S";
  private final Store myStore;
  private final PersistableLong myNextCheck = new PersistableLong();
  private final PersistableBoolean mySupported = new PersistableBoolean();
  private final AtomicBoolean myLoaded = new AtomicBoolean(false);

  public ServerVersionCheck(Store store) {
    myStore = store;
  }

  @Override
  public void check(RestSession session) throws ConnectorException {
    ensureLoaded();
    boolean supported;
    if (System.currentTimeMillis() < myNextCheck.access()) supported = mySupported.access();
    else {
      try {
        supported = RestServerInfo.isJiraVersionOrLater(session, EARLIEST_SUPPORTED_VERSION);
      } catch (ConnectorException e) {
        myNextCheck.set(0l);
        throw e;
      }
      myNextCheck.set(System.currentTimeMillis() + Const.DAY);
      StoreUtils.storePersistable(myStore, NEXT_CHECK, myNextCheck);
    }
    if (supported != mySupported.access()) {
      mySupported.set(supported);
      StoreUtils.storePersistable(myStore, SUPPORTED_FLAG, mySupported);
    }
    if (Boolean.FALSE.equals(supported)) throw notSupported();
  }

  private void ensureLoaded() {
    if (myLoaded.get()) return;
    if (!StoreUtils.restorePersistable(myStore, NEXT_CHECK, myNextCheck)) myNextCheck.set(0l);
    if (!StoreUtils.restorePersistable(myStore, SUPPORTED_FLAG, mySupported)) mySupported.set(false);
    myLoaded.set(true);
  }

  public static ConnectorException notSupported() {
    return SyncNotAllowedException.longReason("Server version is not supported", serverUnsupportedMessage(EARLIEST_SUPPORTED_VERSION));
  }

  private static String serverUnsupportedMessage(String earliestSupportedVersion) {
    return Local.parse(
      "<html><body>This version of " + Terms.ref_Deskzilla + " is incompatible with " + Terms.ref_ConnectionType + " versions earlier than "
        + earliestSupportedVersion + "." + "<br>Please use an earlier version of " + Terms.ref_Deskzilla + ".");
  }
}
