package com.almworks.jira.provider3.links.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.jira.provider3.schema.LinkType;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.schema.ServerLink;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.*;

class LinkInfo {
  private static final LocalizedAccessor.Value P_NOT_SUBMITTED = JiraLinks.I18N.getFactory("upload.create.notSubmitted.short");
  private final CreateIssueUnit mySource;
  private final CreateIssueUnit myTarget;
  private final String myOutward;
  private final int myTypeId;
  private final long myItem;

  private LinkInfo(CreateIssueUnit source, CreateIssueUnit target, String outward, Integer typeId, long item) {
    mySource = source;
    myTarget = target;
    myOutward = outward;
    myTypeId = typeId;
    myItem = item;
  }

  public static LinkInfo create(ItemVersion link, LoadUploadContext context) throws UploadUnit.CantUploadException {
    CreateIssueUnit source = getLinkEnd(link, context, Link.SOURCE);
    CreateIssueUnit target = getLinkEnd(link, context, Link.TARGET);
    if (source == null || target == null) return null;
    ItemVersion type = link.readValue(Link.LINK_TYPE);
    if (type == null) throw UploadUnit.CantUploadException.create("Missing link type", link);
    Integer typeId = type.getValue(LinkType.ID);
    if (typeId == null) {
      LogHelper.error("Missing type id", type);
      throw UploadUnit.CantUploadException.internalError();
    }
    String outward = type.getValue(LinkType.OUTWARD_DESCRIPTION);
    return new LinkInfo(source, target, outward, typeId, link.getItem());
  }

  public long getItem() {
    return myItem;
  }

  @Nullable
  private static CreateIssueUnit getLinkEnd(ItemVersion link, LoadUploadContext context, ItemAttribute reference) throws UploadUnit.CantUploadException {
    try {
      return CreateIssueUnit.getExisting(reference.readValue(link), context);
    } catch (UploadUnit.CantUploadException e) {
      throw new UploadUnit.CantUploadException("Missing link end " + reference, e);
    }
  }

  @Nullable
  public Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException {
    if (myTarget.getIssueId() != null && mySource.getIssueId() != null) { // Don't load anything if a link cannot exist
      Map<UploadUnit, ConnectorException> targetProblems = myTarget.loadServerState(session, transaction, context, purpose);
      Map<UploadUnit, ConnectorException> sourceProblems = mySource.loadServerState(session, transaction, context, purpose);
      HashMap<UploadUnit, ConnectorException> allProblems = Collections15.hashMap();
      if (targetProblems != null) allProblems.putAll(targetProblems);
      if (sourceProblems != null) allProblems.putAll(sourceProblems);
      return allProblems;
    }
    return null;
  }

  @NotNull
  public List<EntityHolder> findLinks(EntityTransaction transaction) {
    Integer sourceId = mySource.getIssueId();
    Integer targetId = myTarget.getIssueId();
    if (sourceId == null || targetId == null) return Collections.emptyList();
    return ServerLink.findLink(transaction, sourceId, targetId, myTypeId);
  }

  @SuppressWarnings("unchecked")
  public JSONObject createLink(UploadContext context) throws UploadProblem.Thrown {
    Integer sourceId = mySource.getIssueId();
    Integer targetId = myTarget.getIssueId();
    if (sourceId == null || targetId == null) {
      if (context.isFailed(mySource) || context.isFailed(myTarget)) throw UploadProblem.fatal(P_NOT_SUBMITTED.create(), null).toException();
      throw UploadProblem.notNow("Link end is not created yet: " + mySource + " " + myTarget).toException();
    }
    JSONObject createLink = new JSONObject();
    createLink.put("type", UploadJsonUtil.object("id", myTypeId));
    createLink.put("inwardIssue", UploadJsonUtil.object("id", sourceId));
    createLink.put("outwardIssue", UploadJsonUtil.object("id", targetId));
    return createLink;
  }

  public Collection<Pair<Long, String>> getMasterItems() {
    ArrayList<Pair<Long,String>> masters = Collections15.arrayList();
    masters.addAll(mySource.getMasterItems());
    masters.addAll(myTarget.getMasterItems());
    return masters;
  }

  public boolean isServerStateLoaded(EntityTransaction transaction) {
    EntityHolder source = mySource.findIssue(transaction);
    EntityHolder target = myTarget.findIssue(transaction);
    return source != null || target != null;
  }

  public String getDisplayableName() {
    String source = mySource.getDisplayableName();
    String target = myTarget.getDisplayableName();
    return source + " " + myOutward + " " + target;
  }
}
