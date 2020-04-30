package com.almworks.jira.provider3.services.upload;

import com.almworks.api.engine.SyncProblem;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBItemType;
import com.almworks.items.sync.SyncManager;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.services.upload.queue.UploadQueue;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.model.SetHolderModel;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;

import java.util.Map;

public class JiraUploadComponent {
  public static final Role<JiraUploadComponent> ROLE = Role.role(JiraUploadComponent.class);
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(JiraUploadComponent.class.getClassLoader(), "com/almworks/jira/provider3/services/upload/message");

  private final SyncManager mySyncManager;
  private final Map<DBItemType, UploadUnit.Factory> myFactories = Collections15.hashMap();

  public JiraUploadComponent(SyncManager syncManager) {
    mySyncManager = syncManager;
  }

  public JiraUploadFacade startUpload(ProgressInfo progress, SetHolderModel<SyncProblem> problems, UploadQueue uploadQueue, JiraConnection3 connection, LongList items)
    throws InterruptedException
  {
    JiraUploadFacade result = new JiraUploadFacade(progress, problems, uploadQueue, mySyncManager, connection);
    Map<DBItemType, UploadUnit.Factory> factoryMap;
    synchronized (myFactories) {
      factoryMap = Collections15.hashMap(myFactories);
    }
    result.start(items, factoryMap);
    return result;
  }

  public void registerUploadFactory(DBItemType type, UploadUnit.Factory factory) {
    if (type == null || factory == null) {
      LogHelper.error("Null value", type, factory);
      return;
    }
    UploadUnit.Factory prev;
    synchronized (myFactories) {
      if (!myFactories.containsKey(type)) {
        myFactories.put(type, factory);
        return;
      } else prev = myFactories.get(type);
    }
    LogHelper.error("Redefinition of upload factory", type, factory, prev);
  }
}
