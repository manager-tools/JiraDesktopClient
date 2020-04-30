package com.almworks.jira.provider3.gui.actions.source;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.util.sources.AbstractItemSource;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class LoadSubtasksSource extends AbstractItemSource {
  private static final TypedKey<LongSet> SUBTASKS = TypedKey.create("subtasks");
  private final TypedKey<LoadItemsStage.Context> CONTEXT = TypedKey.create("currentStage");
  private final JiraConnection3 myConnection;
  private final long myParent;

  public LoadSubtasksSource(JiraConnection3 connection, long parent) {
    super("Sub-Tasks");
    myConnection = connection;
    myParent = parent;
  }

  public void reload(@NotNull ItemsCollector collector) {
    Progress progress = getProgressDelegate(collector);
    LoadItemsStage.Context context = new LoadItemsStage.Context(progress, collector, myConnection);
    LoadParent stage1 = new LoadParent(context, myParent);
    collector.putValue(CONTEXT, context);
    progress.setStarted();
    context.perform(null, stage1);
  }

  public void stop(@NotNull ItemsCollector collector) {
    LoadItemsStage.Context context = collector.getValue(CONTEXT);
    if (context != null) {
      collector.putValue(CONTEXT, null);
      context.stop();
    }
  }

  @SuppressWarnings( {"SynchronizationOnLocalVariableOrMethodParameter"})
  private static LoadItemsStage readSubtasks(LoadItemsStage.Context context, ItemVersion parent) {
    UserDataHolder data = context.getUserData();
    LongSet loaded = data.getUserData(SUBTASKS);
    while (loaded == null) {
      loaded = new LongSet();
      if (!data.putIfAbsent(SUBTASKS, loaded)) loaded = data.getUserData(SUBTASKS);
    }
    LoadItemsStage next;
    ArrayList<String> keysToLoad = Collections15.arrayList();
    LongSet remove;
    synchronized (loaded) {
      remove = LongSet.copy(loaded);
    }
    for (ItemVersion subtask : parent.readItems(Issue.getSubtasks(parent))) {
      remove.remove(subtask.getItem());
      if (ItemDownloadStage.getValue(subtask) == ItemDownloadStage.DUMMY) {
        String key = subtask.getValue(Issue.KEY);
        if (key == null) LogHelper.error("Missing key", subtask);
        else keysToLoad.add(key);
      } else {
        context.addItem(subtask);
        synchronized (loaded) {
          loaded.add(subtask.getItem());
        }
      }
    }
    if (!remove.isEmpty()) {
      synchronized (loaded) {
        loaded.removeAll(remove);
      }
      context.removeAll(remove);
    }
    if (keysToLoad.isEmpty()) next = null;
    else next = new LoadItemsStage.ItemSourceStage(context, IssuesByKeyItemSource.create(context.getConnection(), keysToLoad));
    return next;
  }

  private static class LoadParent implements LoadItemsStage, ReadTransaction<Object> {
    private final Context myContext;
    private final long myParent;

    public LoadParent(Context context, long parent) {
      myContext = context;
      myParent = parent;
    }

    public void perform() {
      myContext.getDatabase().readBackground(this);
    }

    @Override
    public void stop() {
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      ItemVersion parent = SyncUtils.readTrunk(reader, myParent);
      String parentKey = parent.getValue(Issue.KEY);
      ItemDownloadStage stage = ItemDownloadStage.getValue(parent);
      myContext.addItem(myParent, reader);
      LoadItemsStage next;
      if (stage == ItemDownloadStage.DUMMY) {
        if (parentKey != null) next = loadParent(parentKey);
        else next = null;
      } else next = readSubtasks(myContext, parent);
      if (next == null) myContext.setDone();
      else myContext.perform(this, next);
      return null;
    }

    private LoadItemsStage loadParent(String parentKey) {
      if (parentKey == null || parentKey.trim().isEmpty()) {
        myContext.reportError("Missing parent key");
        return null;
      }
      ItemSource source = IssuesByKeyItemSource.create(myContext.getConnection(), parentKey);
      return new WaitForParent(myContext, source, myParent);
    }
  }

  private static class WaitForParent implements ItemsCollector, LoadItemsStage, DBListener {
    private final ItemSource mySource;
    private final Context myContext;
    private final long myParent;

    private WaitForParent(Context context, ItemSource source, long parent) {
      myContext = context;
      mySource = source;
      myParent = parent;
    }

    @Override
    public void perform() {
      myContext.getDatabase().addListener(myContext.getLife(), this);
      mySource.reload(this);
      ProgressSource progress = mySource.getProgress(this);
      myContext.delegateProgress(0.1, progress);
    }

    @Override
    public void stop() {
      mySource.stop(this);
    }

    @Override
    public void addItem(long item, DBReader reader) {
      myContext.addItem(item, reader);
    }

    @Override
    public <T> T getValue(TypedKey<T> key) {
      return myContext.getCollector().getValue(key);
    }

    @Override
    public <T> T putValue(TypedKey<T> key, @Nullable T value) {
      return myContext.getCollector().putValue(key, value);
    }

    @Override
    public void removeItem(long item) {
      myContext.removeItem(item);
    }

    @Override
    public void reportError(String error) {
      myContext.reportError(error);
    }

    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      if (event.getAffectedSorted().binarySearch(myParent) < 0) return;
      ItemVersion parent = SyncUtils.readTrunk(reader, myParent);
      if (ItemDownloadStage.getValue(parent) == ItemDownloadStage.DUMMY) return;
      LoadItemsStage next = readSubtasks(myContext, parent);
      if (myContext.isCurrentStage(this)) myContext.perform(this, next);
    }
  }
}
