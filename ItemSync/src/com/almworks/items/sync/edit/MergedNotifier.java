package com.almworks.items.sync.edit;

import com.almworks.items.api.DBWriter;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.TypedKey;

class MergedNotifier implements Procedure<Boolean> {
  private static final TypedKey<MergeEventImpl> MERGE_EVENT = TypedKey.create("mergeEvent");
  private final SyncManagerImpl myManager;
  private final MergeEventImpl myEvent;
  private final long myIcn;
  private boolean myFired = false;

  public MergedNotifier(SyncManagerImpl manager, MergeEventImpl event, long icn) {
    myManager = manager;
    myEvent = event;
    myIcn = icn;
  }

  public static void appendSuccessfulMerge(SyncManagerImpl manager, DBWriter writer, MergeEventImpl merged) {
    MergeEventImpl event = MERGE_EVENT.getFrom(writer.getTransactionCache());
    if (event == null) {
      event = new MergeEventImpl();
      writer.finallyDo(ThreadGate.LONG, new MergedNotifier(manager, event, writer.getTransactionIcn()));
    }
    event.addEvent(merged);
  }

  @Override
  public void invoke(Boolean arg) {
    if (!Boolean.TRUE.equals(arg)) return;
    if (myFired) return;
    myFired = true;
    if (!myEvent.isEmpty()) myManager.notifyMerged(myIcn, myEvent);
  }
}
