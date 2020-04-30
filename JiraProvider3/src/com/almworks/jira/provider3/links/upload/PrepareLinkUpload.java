package com.almworks.jira.provider3.links.upload;

import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.jira.provider3.services.upload.CollectUploadContext;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;

import java.util.Collection;
import java.util.Collections;

public class PrepareLinkUpload implements UploadUnit.Factory {
  public static final PrepareLinkUpload INSTANCE = new PrepareLinkUpload();

  @Override
  public void collectRelated(ItemVersion trunk, CollectUploadContext context) throws UploadUnit.CantUploadException {
    requestLinkedIssue(trunk, Link.SOURCE, context);
    requestLinkedIssue(trunk, Link.TARGET, context);
  }

  private void requestLinkedIssue(ItemVersion link, ItemAttribute issueReference, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ItemVersion issue = issueReference.readValue(link);
    Integer id = issue.getValue(Issue.ID);
    if (id != null) return; // Already uploaded
    context.requestUpload(issue.getItem(), true);
  }

  @Override
  public Collection<? extends UploadUnit> prepare(ItemVersion link, LoadUploadContext context) throws UploadUnit.CantUploadException {
    LinkInfo linkInfo = LinkInfo.create(link, context);
    if (linkInfo == null) return null;
    SyncState state = link.getSyncState();
    switch (state) {
    case NEW:
      return Collections.singleton(new CreateLink(linkInfo));
    case LOCAL_DELETE:
      return Collections.singleton(new DeleteLink(linkInfo));
    case SYNC:
    case EDITED:
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT:
    default:
      throw UploadUnit.CantUploadException.create("Not uploadable link state", state, link);
    }
  }
}
