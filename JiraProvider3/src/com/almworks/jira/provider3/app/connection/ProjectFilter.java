package com.almworks.jira.provider3.app.connection;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.LongSet;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class ProjectFilter {
  private final DBIdentifiedObject myConnection;
  private final DPConfiguredProjects myProjects;
  private final BoolExpr<DP> myTerm;
  private final AtomicReference<LongList> myCurrentProjects = new AtomicReference<LongList>(null);

  public ProjectFilter(DBIdentifiedObject connection) {
    myConnection = connection;
    myProjects = new DPConfiguredProjects(connection);
    myTerm = myProjects.term();
  }

  public SimpleModifiable getModifiable() {
    return myProjects.myModifiable;
  }

  public BoolExpr<DP> getFilter() {
    return myTerm;
  }

  @NotNull
  public LongList getCurrentProjects() {
    LongList projects = myCurrentProjects.get();
    LogHelper.assertError(projects != null, "Projects filter not initialized");
    return Util.NN(projects, LongList.EMPTY);
  }

  public void initDB(DBDrain drain, long connectionItem) {
    LongList items = drain.forItem(connectionItem).getLongSet(Jira.PROJECT_FILTER);
    updateOnSuccess(drain, items);
  }

  public void update(DBDrain drain, EntityWriter writer, List<EntityHolder> projectFilter) {
    ItemVersionCreator connection = drain.changeItem(myConnection);
    LongArray newFilter = writer.getAllItems(projectFilter);
    newFilter.sortUnique();
    LongList prev = connection.getLongSet(Jira.PROJECT_FILTER);
    if (!Util.equals(prev, newFilter)) {
      updateOnSuccess(drain, newFilter);
      connection.setSet(Jira.PROJECT_FILTER, newFilter);
    }
  }

  private void updateOnSuccess(DBDrain drain, final LongList projectItems) {
    drain.finallyDo(ThreadGate.STRAIGHT, new Procedure<Boolean>() {
      @Override
      public void invoke(Boolean arg) {
        if (arg) {
          LongList prev = myCurrentProjects.get();
          if (!Util.equals(prev, projectItems)) {
            myCurrentProjects.set(projectItems);
            myProjects.myModifiable.fireChanged();
          }
        } else LogHelper.error("Init project filter transaction failed");
      }
    });
  }

  private static class DPConfiguredProjects extends DP {
    private final TypedKey<LongList> PROJECTS = TypedKey.create("projects");
    private final DBIdentifiedObject myConnection;
    private final SimpleModifiable myModifiable = new SimpleModifiable();

    private DPConfiguredProjects(DBIdentifiedObject connection) {
      myConnection = connection;
    }

    @Override
    public BoolExpr<DP> resolve(DBReader reader, @Nullable ResolutionSubscription subscription) {
      LongList projects = resolveCurrent(reader, subscription);
      if (projects.isEmpty()) return BoolExpr.TRUE();
      return DPEquals.equalOneOf(Issue.PROJECT, projects);
    }

    @SuppressWarnings( {"unchecked"})
    private LongList resolveCurrent(DBReader reader, ResolutionSubscription subscription) {
      if (subscription != null) myModifiable.addStraightListener(subscription.getLife(), subscription);
      Map cache = reader.getTransactionCache();
      LongList projects = PROJECTS.getFrom(cache);
      if (projects != null) return projects;
      long connection = reader.findMaterialized(myConnection);
      if (connection <= 0) projects = LongList.EMPTY;
      else projects = LongSet.create(reader.getValue(connection, Jira.PROJECT_FILTER));
      PROJECTS.putTo(cache, projects);
      return projects;
    }

    @Override
    public boolean accept(long item, DBReader reader) {
      Log.error("Unresolved DP");
      return false;
    }

    @Override
    protected boolean equalDP(DP other) {
      return this == other;
    }

    @Override
    protected int hashCodeDP() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "ConfiguredProjects(" + myConnection + ")";
    }
  }
}
