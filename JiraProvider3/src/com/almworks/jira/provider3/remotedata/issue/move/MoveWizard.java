package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityFieldDescriptor;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LocalLog;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.text.TextUtil;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MoveWizard {
  private static final LocalLog log = LocalLog.topLevel("MoveWizard");
  private static final LocalizedAccessor.Value M_GENERIC_FAILURE_SHORT = MoveLoader.I18N.getFactory("upload.problem.move.genericFailure.short");
  private static final LocalizedAccessor.Value M_GENERIC_FAILURE_FULL = MoveLoader.I18N.getFactory("upload.problem.move.genericFailure.full");
  private static final LocalizedAccessor.Value M_NO_STATUS_SHORT = MoveLoader.I18N.getFactory("upload.problem.move.noStatus.short");
  private static final LocalizedAccessor.Value M_NO_STATUS_FULL = MoveLoader.I18N.getFactory("upload.problem.move.noStatus.full");
  private static final LocalizedAccessor.Message2 M_WRONG_STATUS = MoveLoader.I18N.message2("upload.problem.move.wrongStatus.full");
  private static final LocalizedAccessor.Value M_SERVER_MESSAGES_SHORT = MoveLoader.I18N.getFactory("upload.problem.move.failureMessages.short");
  private static final LocalizedAccessor.MessageIntStr M_SERVER_MESSAGES_FULL = MoveLoader.I18N.messageIntStr("upload.problem.move.failureMessages.full");
  private static final LocalizedAccessor.Value M_STATUS_NOT_CHANGED = MoveLoader.I18N.getFactory("upload.warning.move.statusNotChanged");
  private static final LocalizedAccessor.Value M_CONFIRM_FAILED_SHORT = M_GENERIC_FAILURE_SHORT;
  private static final LocalizedAccessor.MessageIntStr M_CONFIRM_HEADER = MoveLoader.I18N.messageIntStr("upload.problem.move.confirmFailed.error");
  private static final LocalizedAccessor.MessageStr M_CONFIRM_HEADER_FAILURE = MoveLoader.I18N.messageStr("upload.problem.move.confirmFailed.failure");
  private static final LocalizedAccessor.MessageIntStr M_CONFIRM_WARNINGS = MoveLoader.I18N.messageIntStr("upload.problem.move.confirmFailed.warnings");
  private static final LocalizedAccessor.Value M_CANT_COMPLETE_SHORT = MoveLoader.I18N.getFactory("upload.problem.move.cantComplete.short");
  private static final LocalizedAccessor.MessageIntStr M_CANT_COMPLETE_FULL = MoveLoader.I18N.messageIntStr("upload.problem.move.cantComplete.full");

  private final String myUpdateWorkflow;
  private final String myStatusParam;
  private final String myUpdateFields;
  private final String myConfirm;
  private final List<String> myWarnings = Collections15.arrayList();

  MoveWizard(String updateWorkflow, String statusParam, String updateFields, String confirm) {
    myUpdateWorkflow = updateWorkflow;
    myStatusParam = statusParam;
    myUpdateFields = updateFields;
    myConfirm = confirm;
  }

  @NotNull
  public static MoveWizard generic() {
    return new MoveWizard("MoveIssueUpdateWorkflow.jspa", "beanTargetStatusId", "MoveIssueUpdateFields.jspa", "MoveIssueConfirm.jspa");
  }

  @NotNull
  public static MoveWizard subtaskToIssue() {
    return new MoveWizard("ConvertSubTaskSetStatus.jspa", "targetStatusId", "ConvertSubTaskUpdateFields.jspa", "ConvertSubTaskConvert.jspa");
  }

  @NotNull
  public static MoveWizard convertToSubtask() {
    return new MoveWizard("ConvertIssueSetStatus.jspa", "targetStatusId", "ConvertIssueUpdateFields.jspa", "ConvertIssueConvert.jspa");
  }

  static String extractKey(Document document) throws UploadProblem.Thrown {
    String key = extractKey(document.getRootElement());
    if (key == null) {
      log.debug("New key not found after move");
      throw  genericFailure().toException();
    }
    return key;
  }

  private static final Pattern TICKET_TITLE_PATTERN = Pattern.compile("^\\[#(" + JiraPatterns.ISSUE_KEY_PATTERN + ")\\]\\s+");
  @Nullable
  private static String extractKey(Element root) {
    Element title = JDOMUtils.searchElement(root, "title");
    if (title != null) {
      String titleText = JDOMUtils.getTextTrim(title);
      Matcher matcher = TICKET_TITLE_PATTERN.matcher(titleText);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }

    Iterator<Element> ii = JDOMUtils.searchElementIterator(root, "a");
    while (ii.hasNext()) {
      Element a = ii.next();
      String atext = JDOMUtils.getTextTrim(a);
      if (JiraPatterns.canBeAnIssueKey(atext)) {
        String href = JDOMUtils.getAttributeValue(a, "href", null, false);
        if (href != null && href.endsWith("/" + atext)) {
          return atext;
        }
      }
    }

    return null;
  }


  public void maybeUpdateStatus(HtmlWizard wizard, ArrayList<IssueFieldValue> values, RestServerInfo serverInfo) throws UploadProblem.Thrown, ConnectorException {
    FormWrapper form = wizard.findForm(myUpdateWorkflow);
    if (form != null) {
      EntityFieldDescriptor.MyValue<Integer> status = IssueFields.STATUS.findValue(values);
      if (status == null) throw UploadProblem.fatal(M_NO_STATUS_SHORT.create(), M_NO_STATUS_FULL.create()).toException();
      if (!status.isChanged()) addWarning(M_STATUS_NOT_CHANGED.create());
      checkStatus(status, form.getParameterElement(myStatusParam));
      form.setValues(myStatusParam, status.getFormValue(serverInfo));
      log.debug("Uploading status", status.getChangeId());
      wizard.submit(form);
    } else log.debug("No change status form");
  }

  public boolean addWarning(String warning) {
    return myWarnings.add(warning);
  }

  private void checkStatus(EntityFieldDescriptor.MyValue<Integer> status, Element statusSelect) throws UploadProblem.Thrown {
    Pair<Integer,String> change = status.getChange();
    Integer setStatusId = change != null ? change.getFirst() : null;
    if (setStatusId == null) {
      log.warning("Missing issue status");
      throw UploadProblem.fatal(M_NO_STATUS_SHORT.create(), M_NO_STATUS_FULL.create()).toException();
    }
    StringBuilder validStatuses = new StringBuilder();
    for (Element option : JDOMUtils.searchElements(statusSelect, "option")) {
      String statusId = JDOMUtils.getAttributeValue(option, "value", "", false);
      if (statusId.isEmpty()) continue;
      int intId;
      try {
        intId = Integer.parseInt(statusId);
      } catch (NumberFormatException e){
        log.warning("Wrong status id on form", statusId);
        continue;
      }
      if (setStatusId == intId) return; // Status is valid
      if (validStatuses.length() > 0) validStatuses.append(", ");
      validStatuses.append(JDOMUtils.getText(option).trim());
    }
    addWarning(M_WRONG_STATUS.formatMessage(change.getSecond(), validStatuses.toString()));
  }

  public void updateFields(HtmlWizard wizard, ArrayList<IssueFieldValue> values, EditIssue edit, RestServerInfo serverInfo) throws UploadProblem.Thrown, ConnectorException {
    FormWrapper form;
    try {
      form = wizard.findMandatoryForm(myUpdateFields);
    } catch (HtmlWizard.NoFormException e) {
      log.warning("Update fields problem", e.getMessage());
      UploadProblem problem = myWarnings.isEmpty() ? UploadProblem.internalError()
        : UploadProblem.fatal(M_CANT_COMPLETE_SHORT.create(),
        M_CANT_COMPLETE_FULL.formatMessage(myWarnings.size(), TextUtil.separateToString(myWarnings, String.format("%n"))));
      throw problem.toException();
    }
    setFieldParameters(form, values, edit, serverInfo);
    wizard.submit(form);
  }

  private static final Set<String> IGNORE_PARAMS = Collections15.hashSet("id", "atl_token", "Next >>", "guid");
  private static void setFieldParameters(FormWrapper form, ArrayList<IssueFieldValue> ownValues, EditIssue edit, RestServerInfo serverInfo) throws UploadProblem.Thrown {
    FormParameters parameters = FormParameters.create(form, IGNORE_PARAMS, serverInfo);
    for (String param : parameters.getParameters()) {
      if (parameters.trySetValue(param, ownValues)) continue;
      if (!parameters.isRequired(param)) continue;
      if (edit == null) {
        log.error("Missing issue edit during move", param);
        continue;
      }
      if (!parameters.trySetValue(param, edit.getValues())) log.warning("Missing required field for move", param);
    }
  }

  public String confirmMove(HtmlWizard wizard) throws UploadProblem.Thrown, ConnectorException {
    FormWrapper form = wizard.findForm(myConfirm);
    if (form == null) throw findMoveFieldValuesError(wizard, myUpdateFields);
    RestResponse response = null;
    ConnectorException failure = null;
    try {
      response = wizard.submitGetResponse(form);
    } catch (ConnectorException e) {
      log.warning("Failed to submit move confirm");
      failure = e;
    }
    if (response == null || !response.isSuccessful()) {
      if (myWarnings.isEmpty() && response != null) response.ensureSuccessful();
      else {
        String warnings = M_CONFIRM_WARNINGS.formatMessage(myWarnings.size(), TextUtil.separate(myWarnings, "\n"));
        if (response != null) throw UploadProblem.fatal(M_CONFIRM_FAILED_SHORT.create(), M_CONFIRM_HEADER.formatMessage(response.getStatusCode(), response.getStatusText()) + "\n\n" + warnings).toException();
        if (failure != null) throw UploadProblem.fatal(M_CONFIRM_FAILED_SHORT.create(), M_CONFIRM_HEADER_FAILURE.formatMessage(failure.getMediumDescription()) + "\n\n" + warnings).toException();
        log.error("No response or failure");
        throw UploadProblem.fatal(M_CONFIRM_FAILED_SHORT.create(), warnings).toException();
      }
    }
    Document document = wizard.nextLoaded(response);
    return extractKey(document);
  }

  private static UploadProblem.Thrown findMoveFieldValuesError(HtmlWizard wizard, String updateFieldsAction) {
    FormWrapper form = wizard.findForm(updateFieldsAction);
    if (form == null) {
      log.debug("Unexpected move failure");
      return genericFailure().toException();
    }
    List<String> messages = Collections15.arrayList();
    for (Element errorElement : JDOMUtils.searchElements(form.getElement(), "span", "class", "errMsg")) {
      String text = JDOMUtils.getText(errorElement);
      text = text.trim();
      if (text.length() > 0) messages.add(text);
    }
    if (!messages.isEmpty()) {
      String serverMessage = TextUtil.separate(messages, "\n");
      log.debug("Move failure detected", serverMessage);
      return UploadProblem.fatal(M_SERVER_MESSAGES_SHORT.create(), M_SERVER_MESSAGES_FULL.formatMessage(messages.size(), serverMessage)).toException();
    }
    log.debug("Failed to find move errors");
    return genericFailure().toException();
  }

  private static UploadProblem genericFailure() {
    return UploadProblem.fatal(M_GENERIC_FAILURE_SHORT.create(), M_GENERIC_FAILURE_FULL.create());
  }
}
