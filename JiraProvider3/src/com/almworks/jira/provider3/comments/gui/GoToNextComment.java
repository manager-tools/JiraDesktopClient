package com.almworks.jira.provider3.comments.gui;

import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.util.List;

class GoToNextComment extends SimpleAction {
  public static final AnAction FORWARD = new GoToNextComment("Next", Icons.ARROW_DOWN, "Go to next unresolved conflict", 1);
  public static final AnAction BACKWARD = new GoToNextComment("Previous", Icons.ARROW_UP, "Go to previous unresolved conflict", -1);

  private final int myDirection;

  private GoToNextComment(String name, Icon icon, String shortDescription, int direction) {
    super(name, icon);
    myDirection = direction;
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, shortDescription);
    watchRole(ResolveCommentForm.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    ResolveCommentForm form = context.getSourceObject(ResolveCommentForm.ROLE);
    context.updateOnChange(form.getSelectionModifiable());
    List<SlaveMergeValue<MergeCommentVersion>> allComments = form.getAllComments();
    SlaveMergeValue<MergeCommentVersion> current = form.getCurrentComment();
    for (SlaveMergeValue<MergeCommentVersion> comment : allComments) {
      if (comment == current) continue;
      if (form.isUnresolved(comment)) return;
    }
    context.setEnabled(false);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    ResolveCommentForm form = context.getSourceObject(ResolveCommentForm.ROLE);
    gotoNext(form, form.getCurrentComment(), myDirection);
  }

  public static boolean gotoNext(ResolveCommentForm form, SlaveMergeValue<MergeCommentVersion> comment, int direction) {
    List<SlaveMergeValue<MergeCommentVersion>> allComments = form.getAllComments();
    int size = allComments.size();
    if (size == 0) return false;
    int index = allComments.indexOf(comment);
    if (index < 0) return false;
    for (int i = (size + index + direction) % size; i != index; i = (size +  i+ direction) % size) {
      SlaveMergeValue<MergeCommentVersion> other = allComments.get(i);
      if (form.isUnresolved(other)) {
        form.selectComment(other);
        return true;
      }
    }
    return false;
  }
}
