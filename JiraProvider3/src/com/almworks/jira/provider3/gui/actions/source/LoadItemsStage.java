package com.almworks.jira.provider3.gui.actions.source;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.progress.ProgressUtil;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

interface LoadItemsStage {
  void stop();

  void perform();

  class Context {
    private final JiraConnection3 myConnection;
    private final AtomicReference<Progress> myLeftProgress = new AtomicReference<Progress>();
    private final Progress myWholeProgress;
    private final ItemsCollector myCollector;
    private final AtomicReference<LoadItemsStage> myCurrentStage = new AtomicReference<LoadItemsStage>();
    private final DetachComposite myLife = new DetachComposite();
    private final UserDataHolder myUserData = new UserDataHolder();

    public Context(Progress progress, ItemsCollector collector, JiraConnection3 connection) {
      myCollector = collector;
      myConnection = connection;
      myLeftProgress.set(progress);
      myWholeProgress = progress;
    }

    public ItemsCollector getCollector() {
      return myCollector;
    }

    public JiraConnection3 getConnection() {
      return myConnection;
    }

    public Database getDatabase() {
      return myConnection.getDatabase();
    }

    public void stop() {
      myLife.detach();
      LoadItemsStage stage = myCurrentStage.getAndSet(null);
      if (stage != null) stage.stop();
      setDone();
    }

    public void setDone() {
      myLeftProgress.set(null);
      myWholeProgress.setDone();
    }

    public void perform(@Nullable LoadItemsStage prev, @Nullable LoadItemsStage next) {
      if (!myCurrentStage.compareAndSet(prev, next)) {
        LoadItemsStage actual = myCurrentStage.get();
        if (actual != null) LogHelper.error("Wrong stage", prev, actual, next);
        return;
      }
      if (next == null) setDone();
      else next.perform();
    }

    public void delegateProgress(ProgressSource progress) {
      Progress left = myLeftProgress.getAndSet(null);
      if (left != null) left.delegate(progress);
    }

    public void delegateProgress(double part, ProgressSource progress) {
      if (part <= 0) return;
      if (part >= 1) {
        delegateProgress(progress);
        return;
      }
      while (true) {
        Progress left = myLeftProgress.get();
        if (left == null) return;
        Pair<Progress,Progress> pair = ProgressUtil.splitProgress(left, "", part, "", 1 - part);
        if (myLeftProgress.compareAndSet(left, pair.getSecond())) {
          pair.getFirst().delegate(progress);
          return;
        }
      }
    }

    public void reportError(String error) {
      myCollector.reportError(error);
    }

    public Lifespan getLife() {
      return myLife;
    }

    public void addItem(long item, DBReader reader) {
      myCollector.addItem(item, reader);
    }

    public void removeItem(long item) {
      myCollector.removeItem(item);
    }

    public void addItem(ItemVersion item) {
      addItem(item.getItem(), item.getReader());
    }

    public boolean isCurrentStage(LoadItemsStage stage) {
      return stage == myCurrentStage.get();
    }

    public UserDataHolder getUserData() {
      return myUserData;
    }

    public void removeAll(LongList remove) {
      if (remove == null) return;
      for (int i = 0; i < remove.size(); i++) removeItem(remove.get(i));
    }
  }

  class ItemSourceStage implements LoadItemsStage {
    private final Context myContext;
    private final ItemSource mySource;

    public ItemSourceStage(Context context, ItemSource source) {
      myContext = context;
      mySource = source;
    }

    @Override
    public void perform() {
      ItemsCollector collector = myContext.getCollector();
      mySource.reload(collector);
      ProgressSource progress = mySource.getProgress(collector);
      myContext.delegateProgress(progress);
    }

    @Override
    public void stop() {
      mySource.stop(myContext.getCollector());
    }
  }
}
