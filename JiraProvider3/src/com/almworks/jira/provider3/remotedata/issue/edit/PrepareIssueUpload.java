package com.almworks.jira.provider3.remotedata.issue.edit;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.remotedata.issue.misc.EditWatchersUnit;
import com.almworks.jira.provider3.remotedata.issue.misc.VoteIssueUnit;
import com.almworks.jira.provider3.schema.*;
import com.almworks.jira.provider3.services.upload.CollectUploadContext;
import com.almworks.jira.provider3.services.upload.LoadUploadContext;
import com.almworks.jira.provider3.services.upload.UploadUnit;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PrepareIssueUpload implements UploadUnit.Factory {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(PrepareIssueUpload.class.getClassLoader(), "com/almworks/jira/provider3/remotedata/issue/message");
  public static final PrepareIssueUpload INSTANCE = new PrepareIssueUpload();
  @SuppressWarnings("unchecked")
  private static final List<DBAttribute<Long>> MASTER_REFERENCES = Collections15.unmodifiableListCopy(Comment.ISSUE, Worklog.ISSUE, Link.SOURCE.getAttribute(),
    Link.TARGET.getAttribute(), Attachment.ISSUE);

  @Override
  public void collectRelated(ItemVersion trunk, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ensureParentIsRequested(trunk, context);
    collectSlaves(trunk, context);
  }

  private void ensureParentIsRequested(ItemVersion trunk, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ItemVersion parent = trunk.readValue(Issue.PARENT);
    if (parent == null) return;
    Integer parentId = parent.getValue(Issue.ID);
    boolean needsParent = parentId == null || IssueType.isSubtask(parent.switchToServer().readValue(Issue.ISSUE_TYPE), true);
    if (!needsParent) return;
    long parentItem = parent.getItem();
    context.requestUpload(parentItem, true);
  }

  private void collectSlaves(ItemVersion issue, CollectUploadContext context) {
    for (DBAttribute<Long> masterReference : MASTER_REFERENCES) {
      for (ItemVersion slave : issue.readItems(issue.getSlaves(masterReference))) {
        SyncState state = slave.getSyncState();
        switch (state) {
        case NEW:
        case EDITED:
        case LOCAL_DELETE:
          context.requestUpload(slave.getItem(), false);
          break;
        case SYNC:
        case DELETE_MODIFIED:
        case MODIFIED_CORPSE:
        case CONFLICT:
          break;
        default:
          LogHelper.error("Unknown state", state);
        }
      }
    }
  }

  @Override
  public Collection<? extends UploadUnit> prepare(ItemVersion item, LoadUploadContext context) throws UploadUnit.CantUploadException {
    SyncState state = item.getSyncState();
    CreateIssueUnit create;
    switch (state) {
    case SYNC: return Collections.emptyList();
    case NEW:
    case EDITED:
      ArrayList<UploadUnit> result = Collections15.arrayList();
      EditIssue editIssue;
      if(state == SyncState.EDITED) {
        create = CreateIssueUnit.getExisting(item, context);
        if (create == null) {
          LogHelper.error("Not submitted", item);
          throw UploadUnit.CantUploadException.internalError();
        }
        Pair<EditIssue,List<UploadUnit>> pair = EditIssue.load(item, create, context);
        editIssue = pair.getFirst();
        result.addAll(pair.getSecond());
        result.add(editIssue);
      } else if (state == SyncState.NEW) {
        Pair<EditIssue, List<UploadUnit>> pair = CreateIssueUnit.submitIssue(item, context);
        if (pair == null) return Collections.emptyList();
        editIssue = pair.getFirst();
        result.add(editIssue.getCreate());
        result.addAll(pair.getSecond());
        result.add(editIssue);
      } else {
        LogHelper.error("Unknown state", state);
        throw UploadUnit.CantUploadException.internalError();
      }
      List<UploadUnit> units = loadPostEditUnits(editIssue, item);
      result.addAll(units);
      return result;
    case LOCAL_DELETE:
      throw new UploadUnit.CantUploadException("Delete issue is not supported");
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT:
      throw new UploadUnit.CantUploadException("Conflict detected");
    default:
      LogHelper.error("Unknown state", state);
      throw UploadUnit.CantUploadException.internalError();
    }
  }

  private List<UploadUnit> loadPostEditUnits(EditIssue editIssue, ItemVersion item) {
    ArrayList<UploadUnit> result = Collections15.arrayList();
    result.add(new AssignIssueUnit(editIssue));
    result.addAll(VoteIssueUnit.load(editIssue, item));
    result.addAll(EditWatchersUnit.load(editIssue, item));
    return result;
  }

  public static void findFailedUploads(EntityWriter writer) {
    FailedIssue.findFailedUploads(writer);
  }
}
