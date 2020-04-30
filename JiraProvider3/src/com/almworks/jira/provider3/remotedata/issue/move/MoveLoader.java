package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.HistoryRecord;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.AttributeMap;
import com.almworks.jira.provider3.remotedata.issue.MoveIssueStep;
import com.almworks.jira.provider3.remotedata.issue.StepLoader;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class MoveLoader implements StepLoader {
  public static final StepLoader INSTANCE = new MoveLoader();
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(MoveLoader.class.getClassLoader(), "com/almworks/jira/provider3/remotedata/issue/move/message");

  private MoveLoader() {
  }

  @Override
  public UploadUnit loadStep(ItemVersion trunk, HistoryRecord record, CreateIssueUnit create, LoadUploadContext context, @Nullable UploadUnit prevStep, int stepIndex)
    throws UploadUnit.CantUploadException {
    DBReader reader = trunk.getReader();
    MoveIssueStep step = MoveIssueStep.load(reader, record.getDataStream());
    if (step == null) throw UploadUnit.CantUploadException.internalError();
    AttributeMap postState = step.getState();
    ArrayList<IssueFieldValue> values = EditIssue.loadValues(context, postState, trunk.switchToServer());
    int expectedType = step.getExpectedTypeId(reader);
    long newParentItem = step.getNewParent();
    long prevParentItem = step.getPreviousParent();
    CreateIssueUnit newParent;
    if (newParentItem > 0) {
      newParent = CreateIssueUnit.getExisting(trunk.forItem(newParentItem), context);
      if (newParent == null) {
        LogHelper.warning("New parent is not submitted and not prepared yet", trunk, newParentItem);
        throw UploadUnit.CantUploadException.create("Move to not submitted parent is not supported yet");
      }
    } else newParent = null;
    Integer prevParentId =  prevParentItem > 0 ? trunk.forItem(prevParentItem).getValue(Issue.ID) : null;
    if (prevParentId != null) {
      return newParent != null ?
        new MoveParentType(create, prevStep, stepIndex, newParent, expectedType, values) :
        new MoveFromSubtask(create, prevStep, stepIndex, values);
    } else {
      return newParent != null ? new MoveToSubtask(create, prevStep, stepIndex, newParent, values) :
        new GenericMove(create, prevStep, stepIndex, values);
    }
  }
}
