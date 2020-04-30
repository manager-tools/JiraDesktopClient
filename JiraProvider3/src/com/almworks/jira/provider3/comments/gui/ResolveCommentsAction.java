package com.almworks.jira.provider3.comments.gui;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.gui.edit.merge.MergeControl;
import com.almworks.items.gui.edit.merge.MergeValue;
import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.util.L;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ResolveCommentsAction extends SimpleAction {
  public static final AnAction INSTANCE = new ResolveCommentsAction();

  private ResolveCommentsAction() {
    super("Merge Comments");
    setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ENTER);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip(Local.parse("Merge Comments")));
    watchModifiableRole(MergeControl.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    MergeControl control = context.getSourceObject(MergeControl.ROLE);
    List<SlaveMergeValue<MergeCommentVersion>> comments = select(control.getAllValues());
    context.setEnabled(comments.isEmpty() ? EnableState.INVISIBLE : EnableState.ENABLED);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    MergeControl control = context.getSourceObject(MergeControl.ROLE);
    List<SlaveMergeValue<MergeCommentVersion>> comments = select(control.getAllValues());
    CantPerformException.ensureNotEmpty(comments);
    DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("jira.merge.resolveComment");
    Map<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion> resolved =
      ResolveCommentForm.showWindow(builder, comments, control.getConfig("CommentMerge"));
    for (Map.Entry<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion> entry : resolved.entrySet()) {
      SlaveMergeValue<MergeCommentVersion> comment = entry.getKey();
      comment.setResolution(entry.getValue());
    }
  }

  public static List<SlaveMergeValue<MergeCommentVersion>> select(List<MergeValue> values) {
    ArrayList<SlaveMergeValue<MergeCommentVersion>> result = Collections15.arrayList();
    for (MergeValue value : values) {
      SlaveMergeValue<?> slaveValue = Util.castNullable(SlaveMergeValue.class, value);
      if (slaveValue == null) continue;
      SlaveMergeValue<MergeCommentVersion> comments = slaveValue.cast(MergeCommentVersion.class);
      if (comments != null) result.add(comments);
    }
    return result;
  }
}
