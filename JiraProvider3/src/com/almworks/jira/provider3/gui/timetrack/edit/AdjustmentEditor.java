package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.timetrack.RemainEstimateEditor;
import com.almworks.jira.provider3.gui.timetrack.TimeUtils;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;
import java.util.List;

class AdjustmentEditor extends NestedModelEditor {
  private static final RemainEstimateEditor ESTIMATE = RemainEstimateEditor.INSTANCE;
  private static final RemainingAdjustmentEditor ADJUSTMENT = new RemainingAdjustmentEditor();

  public static final AdjustmentEditor INSTANCE = new AdjustmentEditor();

  private AdjustmentEditor() {
    super(NameMnemonic.rawText("Adjust Estimate"));
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare)
  {
    LongList issues = parent.getEditingItems();
    DefaultEditModel.Child child = DefaultEditModel.Child.editItems(parent, issues, false);
    return Pair.create(child, Arrays.asList(ESTIMATE, ADJUSTMENT));
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    DefaultEditModel.Child child = getNestedModel(model);
    return ADJUSTMENT.isAdjustSetTo(child) && ESTIMATE.isChanged(child);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    DefaultEditModel.Child nested = getNestedModel(verifyContext.getModel());
    if (nested == null) return;
    DataVerification childContext = verifyContext.subContext(nested);
    ADJUSTMENT.verifyData(childContext);
    if (ADJUSTMENT.isAdjustSetTo(nested)) ESTIMATE.verifyData(childContext);
  }

  @Override
  public void commit(CommitContext context) {
    LogHelper.error("Should not happen");
  }

  public void commitAdjust(CommitContext parentContext, LongList worklogs) {
    DefaultEditModel.Child nested = getNestedModel(parentContext.getModel());
    if (nested == null) return;
    CommitContext context = parentContext.subContext(nested);
    ItemVersionCreator unsafeIssue = context.getDrain().unsafeChange(context.getItem());
    unsafeIssue.setValue(Issue.LOCAL_WORKLOGS, true);
    int adjust = ADJUSTMENT.getAdjust(nested);
    switch (adjust) {
    case RemainingAdjustmentEditor.ADJUST_DONT_CHANGE: unsafeIssue.setValue(Issue.LOCAL_REMAIN_ESTIMATE, null); break;
    case RemainingAdjustmentEditor.ADJUST_SET: ESTIMATE.commit(context); break;
    case RemainingAdjustmentEditor.ADJUST_AUTO:
      TimeUtils.commitAutoAdjust(unsafeIssue, worklogs);
      break;
    default: LogHelper.error("Unknown adjustment", adjust);
    }
  }

  public void attach(Lifespan life, EditItemModel model, WorklogForm form) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested == null) return;
    form.attachAdjustment(life, nested, ESTIMATE, ADJUSTMENT);
  }
}
