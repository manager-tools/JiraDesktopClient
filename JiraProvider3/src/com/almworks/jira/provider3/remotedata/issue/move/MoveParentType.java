package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.download2.rest.JRIssueType;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collection;

class MoveParentType extends BaseMoveUnit {
  private static final String P_OPERATION = "operation";
  private static final String V_PARENT = "move.subtask.parent.operation.name";
  private static final String V_TYPE = "move.subtask.type.operation.name";
  private static final LocalizedAccessor.Value M_NO_OPTION_PARENT = MoveLoader.I18N.getFactory("upload.problem.move.changeParent.noOption.parent");
  private static final LocalizedAccessor.Value M_NO_OPTION_TYPE = MoveLoader.I18N.getFactory("upload.problem.move.changeParent.noOption.type");
  private static final LocalizedAccessor.Message2 M_NO_OPTION_FULL = MoveLoader.I18N.message2("upload.problem.move.changeParent.noOption.full");
  private static final LocalizedAccessor.Value M_OPERATION = MoveLoader.I18N.getFactory("upload.move.operation");
  private final CreateIssueUnit myNewParent;
  private final int myNewTypeId;
  private final ArrayList<IssueFieldValue> myValues;

  public MoveParentType(CreateIssueUnit create, UploadUnit prevStep, int stepIndex, CreateIssueUnit newParent, int newTypeId, ArrayList<IssueFieldValue> values) {
    super(prevStep, create, stepIndex, MoveWizard.generic(), true);
    myNewParent = newParent;
    myNewTypeId = newTypeId;
    myValues = values;
  }

  @Override
  protected Collection<? extends UploadProblem> doPerform(RestSession session, UploadContext context, int issueId)
    throws ConnectorException, UploadProblem.Thrown
  {
    String newParentKey = myNewParent.getIssueKey();
    if (newParentKey == null) return UploadProblem.notNow("New parent is not submitted yet").toCollection();
    Pair<Boolean, Boolean> changeParentType;
    try {
      changeParentType = checkCurrentState(session, issueId);
    } catch (ParseException e) {
      LogHelper.warning("Move subtask problem", e.getMessage());
      return UploadProblem.parseProblem(M_OPERATION.create()).toCollection();
    }
    if (changeParentType.getFirst()) changeParent(session, issueId, newParentKey);
    if (changeParentType.getSecond()) changeType(session, issueId, context);
    markSuccess();
    return null;
  }

  private void changeType(RestSession session, int issueId, UploadContext context) throws UploadProblem.Thrown, ConnectorException {
    HtmlWizard wizard = startMove(session, issueId, V_TYPE, M_NO_OPTION_TYPE);

    FormWrapper form;
    try {
      form = wizard.findMandatoryForm("MoveSubTaskType.jspa");
    } catch (HtmlWizard.NoFormException e) {
      LogHelper.warning("Change type problem", e.getMessage());
      throw UploadProblem.internalError().toException();
    }
    form.setNNToStringValue("issuetype", myNewTypeId);
    wizard.submit(form);

    finishMove(wizard, myValues, context);
  }

  private void changeParent(RestSession session, int issueId, String newParentKey) throws ConnectorException, UploadProblem.Thrown {
    HtmlWizard wizard = startMove(session, issueId, V_PARENT, M_NO_OPTION_PARENT);
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm("MoveSubTaskParent.jspa?", "id=" + issueId);
    } catch (HtmlWizard.NoFormException e) {
      LogHelper.warning("Change parent problem", e.getMessage());
      throw UploadProblem.internalError().toException();
    }
    form.setNNToStringValue("parentIssue", newParentKey);
    Document document = wizard.submit(form);
    String key = MoveWizard.extractKey(document);
    LogHelper.assertWarning(Util.equals(key, getIssue().getIssueKey()), "Issue key changed by change parent", key, getIssue().getIssueKey());
    getIssue().issueKeyUpdated(key);
  }

  private HtmlWizard startMove(RestSession session, int issueId, String operation, LocalizedAccessor.Value displayableOperation) throws ConnectorException, UploadProblem.Thrown {
    HtmlWizard wizard = HtmlWizard.start(session, "secure/MoveSubTaskChooseOperation!default.jspa?decorator=none&id=" + issueId);
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm("MoveSubTaskChooseOperation.jspa?", "id=" + issueId);
    } catch (HtmlWizard.NoFormException e) {
      LogHelper.warning("Start move problem", e.getMessage());
      throw cantStartProblem().toException();
    }
    ensureHasParamValue(wizard, form, operation, displayableOperation);
    form.setValue(P_OPERATION, operation);
    wizard.submit(form);
    return wizard;
  }

  private Pair<Boolean, Boolean> checkCurrentState(RestSession session, int issueId) throws ConnectorException, ParseException {
    RestResponse response = session.restGet("api/2/issue/" + issueId + "?fields=parent%2Cissuetype", RequestPolicy.SAFE_TO_RETRY);
    response.ensureSuccessful();
    boolean changeParent = false;
    boolean changeType = false;
    JSONObject issue = response.getJSONObject();
    JSONObject parent = JRIssue.PARENT.getValue(issue);
    if (parent == null) LogHelper.warning("Issue has no parent", issueId);
    else {
      Integer parentId = JRIssue.ID.getValue(parent);
      if (parentId == null) LogHelper.error("Missing parent issue id", parent);
      else changeParent =  !Util.equals(myNewParent.getIssueId(), parentId);
    }
    JSONObject type = JRIssue.ISSUE_TYPE.getValue(issue);
    if (type == null) LogHelper.error("Missing issue type", issueId);
    else {
      Integer typeId = JRIssueType.ID.getValue(type);
      if (typeId == null) LogHelper.error("Missing issue type id", type);
      else changeType = !Util.equals(myNewTypeId, typeId);
    }
    return Pair.create(changeParent, changeType);
  }

  private void ensureHasParamValue(HtmlWizard wizard, FormWrapper form, String value, LocalizedAccessor.Value displayableOperation) throws UploadProblem.Thrown {
    Element element = JDOMUtils.searchElement(form.getElement(), "input", "value", value);
    if (element != null) return;
    Element head = JDOMUtils.searchElement(wizard.getRootElement(), "head");
    Element title = null;
    if (head != null) title = JDOMUtils.searchElement(head, "title");
    String pageTitle = title != null ? title.getTextTrim() : "page";
    LogHelper.debug("Missing move subtask option", value);
    throw UploadProblem.fatal(displayableOperation.create(), M_NO_OPTION_FULL.formatMessage(value, pageTitle)).toException();
  }

  @Override
  public String toString() {
    return "MoveParentType";
  }
}
