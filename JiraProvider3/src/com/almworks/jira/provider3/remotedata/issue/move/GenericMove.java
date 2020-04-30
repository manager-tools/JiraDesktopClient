package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LocalLog;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;

import java.util.*;

class GenericMove extends BaseMoveUnit {
  private static final LocalLog log = LocalLog.topLevel("GenericMove");
  public static final String START_MOVE = "MoveIssue.jspa";
  private static final LocalizedAccessor.Message2 M_PROJECT_TYPE_FAILURE = MoveLoader.I18N.message2("upload.problem.move.generic.projectType");
  private final ArrayList<IssueFieldValue> myValues;

  public GenericMove(CreateIssueUnit issue, UploadUnit prevStep, int stepIndex, ArrayList<IssueFieldValue> values) {
    super(prevStep, issue, stepIndex, MoveWizard.generic(), false);
    myValues = values;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId) throws ConnectorException, UploadProblem.Thrown {
    EntityFieldDescriptor.MyValue<Integer> projectValue = IssueFields.PROJECT.findValue(myValues);
    EntityFieldDescriptor.MyValue<Integer> typeValue = IssueFields.ISSUE_TYPE.findValue(myValues);
    Integer pid = projectValue == null ? null : projectValue.getChangeId();
    Integer typeId = typeValue == null ? null : typeValue.getChangeId();
    if (pid == null && typeId == null) {
      log.warning("No actual move");
      markSuccess();
      return null;
    }
    HtmlWizard wizard = HtmlWizard.start(session, "secure/MoveIssue!default.jspa?id=" + issueId);
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm(START_MOVE);
    } catch (HtmlWizard.NoFormException e) {
      log.warning("Generic move problem", e.getMessage());
      return cantStartProblem().toCollection();
    }
    form.setNNToStringValue("pid", pid);
    form.setNNToStringValue("issuetype", typeId);
    log.debug("Moving issues:", issueId, "to project:", pid, "type:", typeId);
    wizard.submit(form);

    if (wizard.findForm(START_MOVE) != null) {
      log.warning("Move has not moved to next form", projectValue, typeValue);
      addWarning(M_PROJECT_TYPE_FAILURE.formatMessage(getValueDisplayName(projectValue), getValueDisplayName(typeValue)));
    }
    finishMove(wizard, myValues, context);
    markSuccess();
    return null;
  }

  private String getValueDisplayName(EntityFieldDescriptor.MyValue<?> value) {
    List<Pair<?, String>> values = value == null ? Collections.emptyList()
      : Arrays.asList(value.getChange(), value.getExpected());
    for (Pair<?, String> p : values) {
      if (p == null) continue;
      String name = p.getSecond();
      if (name != null) return name;
      else break;
    }
    return "-";
  }

  @Override
  public String toString() {
    return "GenericMove";
  }
}
