package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;

import java.util.ArrayList;
import java.util.Collection;

class MoveToSubtask extends BaseMoveUnit {
  private final CreateIssueUnit myNewParent;
  private final ArrayList<IssueFieldValue> myValues;

  public MoveToSubtask(CreateIssueUnit create, UploadUnit prevStep, int stepIndex, CreateIssueUnit newParent, ArrayList<IssueFieldValue> values) {
    super(prevStep, create, stepIndex, MoveWizard.convertToSubtask(), true);
    myNewParent = newParent;
    myValues = values;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId)
    throws ConnectorException, UploadProblem.Thrown {
    Integer newTypeId = CreateIssueUnit.findChangeId(IssueFields.ISSUE_TYPE, myValues);
    if (newTypeId == null) {
      LogHelper.warning("Convertor to subtask missing new type");
      return UploadProblem.internalError().toCollection();
    }
    String newParentKey = myNewParent.getIssueKey();
    if (newParentKey == null) return UploadProblem.notNow("Parent is not uploaded yet").toCollection();
    HtmlWizard wizard = HtmlWizard.start(session, "secure/ConvertIssue.jspa?decorator=none&id=" + issueId);
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm("ConvertIssueSetIssueType.jspa");
    } catch (HtmlWizard.NoFormException e) {
      LogHelper.warning("Move to subtask", e.getMessage());
      return cantStartProblem().toCollection();
    }
    form.setValue("parentIssueKey", newParentKey);
    form.setNNToStringValue("issuetype", newTypeId);
    wizard.submit(form);
    finishMove(wizard, myValues, context);
    markSuccess();
    return null;
  }

  @Override
  public String toString() {
    return "MoveToSubtask";
  }
}
