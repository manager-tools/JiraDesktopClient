package com.almworks.jira.provider3.links.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
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
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class DeleteLink implements UploadUnit {
  private static final LocalizedAccessor.MessageStr P_NOT_ALLOWED = JiraLinks.I18N.messageStr("upload.delete.notAllowed.short");
  private static final LocalizedAccessor.Message2 P_GENERIC = JiraLinks.I18N.message2("upload.delete.generic.short");
  private final LinkInfo myLinkInfo;
  private final IntArray myLinkIds = new IntArray();
  private boolean myDone = false;

  public DeleteLink(LinkInfo link) {
    myLinkInfo = link;
  }

  @Override
  public boolean isDone() {
    return myDone;
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
    for (EntityHolder link : links) {
      Integer id = link.getScalarValue(ServerLink.ID);
      if (id != null) myLinkIds.add(id);
      else LogHelper.error("Missing ID", link);
    }
    return null;
  }

  @Override
  public Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException {
    ArrayList<UploadProblem> problems = Collections15.arrayList();
    for (IntIterator cursor : myLinkIds) {
      int linkId = cursor.value();
      String path = "api/2/issueLink/" + linkId;
      RestResponse response = session.restDelete(path, RequestPolicy.SAFE_TO_RETRY);
      int code = response.getStatusCode();
      if (code / 100 != 2)
        switch (code) {
        case 400: LogHelper.warning("Invalid link id", linkId, path, code); break;
        case 401:
          problems.add(UploadProblem.fatal(P_NOT_ALLOWED.formatMessage(myLinkInfo.getDisplayableName()), null));
          break;
        case 404: LogHelper.warning("Failed to delete link", linkId, myLinkInfo, code); break; // May be the link just doesn't exist.
        default:
          problems.add(UploadProblem.fatal(P_GENERIC.formatMessage(myLinkInfo.getDisplayableName(), String.valueOf(code)), null));
          break;
        }
    }
    myDone = true;
    return problems;
  }

  @Override
  public void finishUpload(EntityTransaction transaction, PostUploadContext context) {
    if (myLinkInfo.isServerStateLoaded(transaction)) {
      if (myLinkInfo.findLinks(transaction).isEmpty())  context.reportUploaded(myLinkInfo.getItem(), SyncSchema.INVISIBLE);
    }
  }

  @NotNull
  @Override
  public Collection<Pair<Long, String>> getMasterItems() {
    return myLinkInfo.getMasterItems();
  }
}
