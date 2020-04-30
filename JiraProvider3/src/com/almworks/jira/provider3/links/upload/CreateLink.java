package com.almworks.jira.provider3.links.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadContext;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.jira.provider3.sync.schema.ServerLink;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CreateLink implements UploadUnit {
  private static final Pattern LOCATION = Pattern.compile("/rest/api/2/issueLink/(\\d+)$");
  private static final LocalizedAccessor.Value P_NOT_CONFIRMED_SHORT = JiraLinks.I18N.getFactory("upload.create.notConfirmed.short");
  private final LinkInfo myLinkInfo;
  private Integer myLinkId;

  public CreateLink(LinkInfo linkInfo) {
    myLinkInfo = linkInfo;
  }

  @Override
  public boolean isDone() {
    return myLinkId != null;
  }

  @Override
  public boolean isSurelyFailed(UploadContext context) {
    return false;
  }

  @Override
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    return myLinkInfo.loadServerState(session, transaction, context, purpose);
  }

  @Override
  public UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context) {
    List<EntityHolder> links = myLinkInfo.findLinks(transaction);
    if (!links.isEmpty()) {
      EntityHolder link = links.get(0);
      myLinkId = link.getScalarValue(ServerLink.ID);
      LogHelper.assertError(myLinkId != null, "No link id", link);
    }
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown {
    JSONObject createLink = myLinkInfo.createLink(context);
    RestResponse response = session.restPostJson("api/2/issueLink", createLink, RequestPolicy.NEEDS_LOGIN);
    if (!response.isSuccessful()) throw getFailure(response);
    String location = response.getResponseHeader("Location");
    if (location == null) {
      LogHelper.error("No created link returned");
      throw UploadProblem.fatal(P_NOT_CONFIRMED_SHORT.create(), null).toException();
    }
    Matcher m = LOCATION.matcher(location);
    if (!m.find()) {
      LogHelper.error("Unexpected link location", location);
      throw UploadProblem.fatal(P_NOT_CONFIRMED_SHORT.create(), null).toException();
    }
    try {
      myLinkId = Integer.parseInt(m.group(1));
    } catch (NumberFormatException e){
      LogHelper.error(e);
      throw UploadProblem.fatal(P_NOT_CONFIRMED_SHORT.create(), null).toException();
    }
    return null;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (myLinkInfo.isServerStateLoaded(transaction)) {
      if (!myLinkInfo.findLinks(transaction).isEmpty())  context.reportUploaded(myLinkInfo.getItem(), SyncSchema.INVISIBLE);
    }
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myLinkInfo.getMasterItems();
  }

  private UploadProblem.Thrown getFailure(RestResponse response) {
    int code = response.getStatusCode();
    String failure;
    switch (code) { // https://developer.atlassian.com/static/rest/jira/5.2.5.html#id328560
    case 400:
      LogHelper.error("Should not happen", code);
      failure =  "Internal error (" + code + ")";
      break;
    case 401:
      failure =  "No link issue permission";
      break;
    case 404:
      failure =  "Issue linking is disabled";
      break;
    default:
      failure =  "Link issues failed (" + code + ")";
    }
    return UploadProblem.fatal(failure, "Failed to create new link: " + failure).toException();
  }
}
