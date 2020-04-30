package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EditRequest extends EditIssueRequest {
  /**
   * Argument can be 'edit issue', 'create issue' or workflow action name.
   */
  private static final LocalizedAccessor.MessageStr M_GENERIC_ERROR_SHORT = PrepareIssueUpload.I18N.messageStr("upload.problem.unknownError.short");
  private static final LocalizedAccessor.MessageIntStr M_REMOTE_ERROR_FULL = PrepareIssueUpload.I18N.messageIntStr("upload.problem.remoteError.full");
  private static final TypedKey<RestServerInfo> SERVER_INFO = TypedKey.create("serverInfo");

  private final ParsedIssueFields myFields;
  private final ArrayList<IssueFieldValue> myValues = Collections15.arrayList();
  private final ArrayList<UploadProblem> myProblems = Collections15.arrayList();
  private final JSONObject myUpdate = new JSONObject();
  private final boolean myCreate;

  private boolean myAnyNotDone = false;

  public EditRequest(ParsedIssueFields fields, boolean create, RestServerInfo serverInfo) {
    super(serverInfo);
    myFields = fields;
    myCreate = create;
  }

  /**
   * An unit that asks server info must call {@link #ensureHasServerInfo(com.almworks.jira.provider3.services.upload.UploadContext, com.almworks.restconnector.RestSession)}
   */
  @NotNull
  public static RestServerInfo getServerInfo(UploadContext context) throws UploadProblem.Thrown {
    RestServerInfo serverInfo = context.getUserData().getUserData(SERVER_INFO);
    if (serverInfo == null) {
      LogHelper.error("Missing server info");
      throw UploadProblem.internalError().toException();
    }
    return serverInfo;
  }

  /**
   * @return if a value has temporary problem returns the problem
   */
  @Nullable
  public UploadProblem fillFields(List<IssueFieldValue> values) {
    for (IssueFieldValue value : values) {
      try {
        value.addChange(this);
      } catch (UploadProblem.Thrown thrown) {
        UploadProblem problem = thrown.getProblem();
        if (problem.isTemporary()) return problem;
        myProblems.add(problem);
      }
    }
    return null;
  }

  @Override
  public boolean isCreate() {
    return myCreate;
  }

  @Override
  public ParsedIssueFields.Info getFieldInfo(String fieldId) {
    return myFields.getFieldInfo(fieldId);
  }

  @Override
  public void addEdit(IssueFieldValue value, String fieldId, JSONArray change) {
    //noinspection unchecked
    myUpdate.put(fieldId, change);
    myValues.add(value);
    if (value.needsUpload(getServerInfo())) myAnyNotDone = true;
  }

  @Override
  public boolean hasOperation(String fieldId, String operation) {
    return myFields.hasOperation(fieldId, operation);
  }

  public void addUpdate(JSONObject outer) {
    //noinspection unchecked
    outer.put("update", myUpdate);
  }

  public boolean isAnyNotDone() {
    return myAnyNotDone;
  }

  public Collection<? extends UploadProblem> getProblems() {
    return myProblems;
  }

  private static final ArrayKey<String> ERROR_MESSAGES = ArrayKey.textArray("errorMessages");
  private static final JSONKey<JSONObject> ERRORS = JSONKey.object("errors");
  public boolean processResponse(CreateIssueUnit create, @Nullable RestResponse response, @Nullable ConnectorException failure, String displayableOperation) {
    boolean success = failure == null && response != null && response.isSuccessful();
    if (failure != null) myProblems.add(UploadProblem.exception(failure));
    for (IssueFieldValue value : myValues) value.uploadDone(success);
    if (!success && response != null) {
      int statusCode = response.getStatusCode();
      try {
        JSONObject message = JSONKey.ROOT_OBJECT.getValue(response.getJSON());
        if (message != null) {
          ArrayList<String> errors = Collections15.arrayList();
          errors.addAll(ERROR_MESSAGES.list(message));
          JSONObject responseErrors = ERRORS.getValue(message);
          if (responseErrors != null)
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) responseErrors).entrySet()) errors.add(entry.getKey() + ": " + entry.getValue());
          String details = TextUtil.separate(errors, "\n");
          String shortText = M_GENERIC_ERROR_SHORT.formatMessage(displayableOperation);
          String fullText = shortText + ".\n\n" + M_REMOTE_ERROR_FULL.formatMessage(statusCode, details);
          myProblems.add(UploadProblem.fatal(shortText, fullText));
          LogHelper.warning("Error uploading", statusCode, create, message, displayableOperation);
        } else {
          if (LogHelper.isDebugLoggable()) LogHelper.debug(response.getString());
          myProblems.add(UploadProblem.fatal(M_GENERIC_ERROR_SHORT.formatMessage(displayableOperation), null));
        }
      } catch (ConnectionException e) {
        LogHelper.warning("Edit issue failed", displayableOperation, create, statusCode, response.getLastUrl());
        LogHelper.debug(e);
        myProblems.add(UploadProblem.fatal(M_GENERIC_ERROR_SHORT.formatMessage(displayableOperation), null));
      } catch (ParseException e) {
        LogHelper.warning("Failed to parse", displayableOperation);
        myProblems.add(UploadProblem.parseProblem(displayableOperation));
      }
    }
    return success;
  }

  public void addProblem(UploadProblem problem) {
    myProblems.add(problem);
  }

  public static void ensureHasServerInfo(UploadContext context, RestSession session) throws ConnectorException {
    if (context.getUserData().getUserData(EditRequest.SERVER_INFO) == null) {
      RestServerInfo serverInfo = RestServerInfo.get(session);
      context.getUserData().putUserData(EditRequest.SERVER_INFO, serverInfo);
    }
  }
}
