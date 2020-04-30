package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import javax.swing.*;
import java.util.List;

abstract class EditIssueScreen extends IssueScreen {
  public static final List<ServerFields.Field> STATIC_DEFAULT_ORDER = Collections15.unmodifiableListCopy(ServerFields.PARENT,
    ServerFields.SUMMARY, ServerFields.PROJECT, ServerFields.ISSUE_TYPE, ServerFields.SECURITY, ServerFields.PRIORITY, ServerFields.DUE, ServerFields.COMPONENTS,
    ServerFields.AFFECT_VERSIONS,
    ServerFields.FIX_VERSIONS, ServerFields.ASSIGNEE, ServerFields.REPORTER, ServerFields.ENVIRONMENT, ServerFields.DESCRIPTION, ServerFields.TIME_TRACKING);

  private final Factory<String> myName;
  private final String myLocalId;
  private final Icon myIcon;

  protected EditIssueScreen(Factory<String> name, String localId, Icon icon) {
    myName = name;
    myLocalId = localId;
    myIcon = icon;
  }

  public String getName() {
    return myName.create();
  }

  public abstract String getTooltip();

  public String getLocalId() {
    return myLocalId;
  }

  public void renderOn(Canvas canvas, CellState state) {
    canvas.setIcon(myIcon);
    canvas.appendText(myName.create());
    canvas.setToolTipText(myName.create());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    EditIssueScreen other = Util.castNullable(EditIssueScreen.class, obj);
    return other != null && myLocalId.equals(other.myLocalId);
  }

  @Override
  public int hashCode() {
    return myLocalId.hashCode();
  }
}
