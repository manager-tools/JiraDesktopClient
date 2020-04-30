package com.almworks.jira.provider3.comments.gui;

import com.almworks.items.gui.edit.merge.MergeValue;
import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.util.LogHelper;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;

class MarkMerged extends SimpleAction {
  public static final AnAction MARK_EDITED = new MarkMerged("Mark merged", null, "Mark current local version as conflict resolution", SlaveMergeValue.ResolutionKind.EDIT);
  public static final AnAction DISCARD = new MarkMerged("Discard Local", Icons.ACTION_DISCARD, "Discard local changes", SlaveMergeValue.ResolutionKind.DISCARD);
//  public static final AnAction CONVERT_TO_NEW = new MarkMerged("Add as New Comment", Icons.ACTION_COMMENT_ADD, "Add local comment version as new comment", SlaveMergeValue.ResolutionKind.COPY_NEW);

  private final SlaveMergeValue.ResolutionKind myResolutionKind;

  MarkMerged(String name, Icon icon, String shortDescription, SlaveMergeValue.ResolutionKind resolutionKind) {
    super(name, icon);
    myResolutionKind = resolutionKind;
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, shortDescription);
    watchRole(ResolveCommentForm.ROLE);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ResolveCommentForm form = context.getSourceObject(ResolveCommentForm.ROLE);
    context.updateOnChange(form.getSelectionModifiable());
    SlaveMergeValue<MergeCommentVersion> current = form.getCurrentComment();
    if (current.isResolved()) {
      context.setEnabled(EnableState.DISABLED);
      return;
    }
    boolean copyNew = myResolutionKind == SlaveMergeValue.ResolutionKind.COPY_NEW;
    boolean discard = myResolutionKind == SlaveMergeValue.ResolutionKind.DISCARD;
    if (copyNew || discard) {
      MergeCommentVersion resolution = form.getCurrentResolution();
      if (resolution == null) {
        context.setEnabled(EnableState.ENABLED);
        return;
      }
      context.setEnabled(discard == resolution.isDeleted() ? EnableState.DISABLED : EnableState.ENABLED);
      return;
    }
    context.setEnabled(EnableState.DISABLED);
    MergeCommentVersion remote = current.getVersion(MergeValue.REMOTE);
    if (remote.isDeleted()) {
      if (myResolutionKind == SlaveMergeValue.ResolutionKind.EDIT) return;
    }
    context.setEnabled(EnableState.ENABLED);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ResolveCommentForm form = context.getSourceObject(ResolveCommentForm.ROLE);
    int version;
    switch (myResolutionKind) {
    case COPY_NEW:
    case EDIT: version = MergeValue.LOCAL; break;
    case DISCARD: version = MergeValue.REMOTE; break;
    default:
      LogHelper.error("Unknown kind", myResolutionKind);
      return;
    }
    SlaveMergeValue<MergeCommentVersion> comment = form.setResolveCurrent(version);
    GoToNextComment.gotoNext(form, comment, 1);
  }
}
