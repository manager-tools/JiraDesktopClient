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

class MoveFromSubtask extends BaseMoveUnit {
  private final ArrayList<IssueFieldValue> myValues;

  public MoveFromSubtask(CreateIssueUnit create, UploadUnit prevStep, int stepIndex, ArrayList<IssueFieldValue> values) {
    super(prevStep, create, stepIndex, MoveWizard.subtaskToIssue(), true);
    myValues = values;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId)
    throws ConnectorException, UploadProblem.Thrown
  {
    HtmlWizard wizard = HtmlWizard.start(session, "secure/ConvertSubTask!default.jspa?decorator=none&id=" + issueId);
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm("ConvertSubTaskSetIssueType.jspa");
    } catch (HtmlWizard.NoFormException e) {
      LogHelper.warning("Start move from subtask problem", e.getMessage());
      return cantStartProblem().toCollection();
    }
    Integer typeId = CreateIssueUnit.findChangeId(IssueFields.ISSUE_TYPE, myValues);
    if (typeId == null) {
      LogHelper.warning("No type change");
      markSuccess();
      return null;
    }
    form.setNNToStringValue("issuetype", typeId);
    wizard.submit(form);

    finishMove(wizard, myValues, context);
    markSuccess();
    return null;
  }

  @Override
  public String toString() {
    return "MoveFromSubtask";
  }
}
