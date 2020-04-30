package com.almworks.items.sync.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AggregatingEditCommit implements EditCommit {
  private final List<EditCommit> myChildren = Collections15.arrayList();
  // Guarded by myChildren
  private final List<ThreadGate> myGates = Collections15.arrayList();

  public AggregatingEditCommit addProcedure(@Nullable ThreadGate notificationGate, EditCommit procedure) {
    if (procedure == null) return this;
    if (notificationGate == null) notificationGate = ThreadGate.STRAIGHT;
    synchronized (myChildren) {
      myChildren.add(procedure);
      myGates.add(notificationGate);
    }
    return this;
  }

  @Override
  public void onCommitFinished(final boolean success) {
    EditCommit[] procedures;
    ThreadGate[] gates;
    synchronized (myChildren) {
      assert myChildren.size() == myGates.size();
      procedures = myChildren.toArray(new EditCommit[myChildren.size()]);
      gates = myGates.toArray(new ThreadGate[myGates.size()]);
    }
    Throwable exception = null;
    for (int i = 0; i < procedures.length; i++) {
      final EditCommit procedure = procedures[i];
      ThreadGate gate = gates[i];
      try {
        if (ThreadGate.isRightNow(gate)) procedure.onCommitFinished(success);
        else gate.execute(new Runnable() {
          @Override
          public void run() {
            procedure.onCommitFinished(success);
          }
        });
      } catch (Throwable e) {
        Log.error(e);
        exception = e;
      }
    }
    ExceptionUtil.rethrowNullable(exception);
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    EditCommit[] commits;
    synchronized (myChildren) {
      commits = myChildren.toArray(new EditCommit[myChildren.size()]);
    }
    for (EditCommit commit : commits) commit.performCommit(drain);
  }

  public static AggregatingEditCommit toAggregating(EditCommit commit) {
    if (commit instanceof AggregatingEditCommit) return (AggregatingEditCommit) commit;
    AggregatingEditCommit composite = new AggregatingEditCommit();
    composite.addProcedure(null, commit);
    return composite;
  }

  public AggregatingEditCommit addDelete(final long item) {
    return addProcedure(null, new Adapter() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        drain.changeItem(item).delete();
      }
    });
  }

  public void updateValues(final long item, final AttributeMap values) {
    addProcedure(null, new Adapter() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        ItemVersionCreator creator = drain.changeItem(item);
        SyncUtils.copyValues(creator, values);
      }
    });
  }

  public void addCreateCopyConnection(final Map<DBAttribute<?>, Object> newItemValues, final long copyConnectionFrom) {
    addProcedure(null, new Adapter() {
      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        Long connection = drain.forItem(copyConnectionFrom).getValue(SyncAttributes.CONNECTION);
        if (connection == null) {
          Log.error("Missing connection " + copyConnectionFrom);
          throw new DBOperationCancelledException();
        }
        ItemVersionCreator creator = drain.createItem();
        creator.setValue(SyncAttributes.CONNECTION, connection);
        SyncUtils.copyValues(creator, newItemValues);
      }
    });
  }
}
