package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.UiItem;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.sync.*;
import com.almworks.jira.provider3.gui.edit.editors.UploadChangeTask;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;

public class DeleteSlavesCommit implements EditCommit, Runnable {
  private final Engine myEngine;
  private final LongList mySlaves;
  private final Component myComponent;
  @Nullable
  private final UploadChangeTask myUpload;
  private final DBAttribute<Long>[] myMasterRefs;

  protected DeleteSlavesCommit(Engine engine, LongList slaves, Component component, boolean upload, DBAttribute<Long>[] masterRefs) {
    myEngine = engine;
    mySlaves = slaves;
    myComponent = component;
    myMasterRefs = masterRefs;
    myUpload = upload ? new UploadChangeTask() : null;
  }

  public static EditCommit create(Engine engine, LongList slaves, Component component, boolean upload, DBAttribute<Long> ... masterRefs) {
    return new DeleteSlavesCommit(engine, slaves, component, upload, ArrayUtil.arrayCopy(masterRefs));
  }

  public static boolean perform(LongArray slaves, ActionContext context, boolean upload, DBAttribute<Long> ... masterRefs) throws CantPerformException {
    EditCommit commit = DeleteSlavesCommit.create(context.getSourceObject(Engine.ROLE), slaves, context.getComponent(), upload, masterRefs);
    return context.getSourceObject(SyncManager.ROLE).commitEdit(slaves, commit);
  }

  public static boolean perform(Collection<? extends UiItem> slaves, ActionContext context, boolean upload, DBAttribute<Long> ... masterRefs) throws CantPerformException {
    return perform(ItemActionUtils.collectItems(slaves), context, upload, masterRefs);
  }

  protected LongList getSlaves() {
    return mySlaves;
  }

  @Override
  public void performCommit(EditDrain drain) throws DBOperationCancelledException {
    for (ItemVersionCreator creator : drain.changeItems(getSlaves())) {
      addSlaveToUpload(creator);
      creator.delete();
    }
  }

  protected void addSlaveToUpload(ItemVersion slave) {
    if (myUpload != null) myUpload.addSlave(slave, myMasterRefs);
  }

  @Override
  public void run() {
    DialogsUtil.showErrorMessage(myComponent, "Remove issue links failed", "Remove Links");
  }

  @Override
  public void onCommitFinished(boolean success) {
    if (!success) ThreadGate.AWT.execute(this);
    else upload();
  }

  private void upload() {
    if (myUpload != null)
      myUpload.perform(myEngine);
  }
}
