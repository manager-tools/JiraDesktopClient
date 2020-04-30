package com.almworks.jira.provider3.attachments.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.dump.RequestDumper;
import com.almworks.api.http.HttpMethodFactoryException;
import com.almworks.api.http.HttpUtils;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.remotedata.issue.AddEditSlaveUnit;
import com.almworks.jira.provider3.remotedata.issue.SlaveIds;
import com.almworks.jira.provider3.remotedata.issue.edit.CreateIssueUnit;
import com.almworks.jira.provider3.remotedata.issue.edit.EditIssue;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.services.upload.*;
import com.almworks.jira.provider3.sync.download2.rest.JRAttachment;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Util;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;

public class PrepareAttachmentsUpload implements UploadUnit.Factory {
  public static final UploadUnit.Factory INSTANCE = new PrepareAttachmentsUpload();

  @Override
  public void collectRelated(ItemVersion attachment, CollectUploadContext context) throws UploadUnit.CantUploadException {
    ItemVersion issue = attachment.readValue(Attachment.ISSUE);
    if (issue != null && issue.getValue(Issue.ID) == null) context.requestUpload(issue.getItem(), true);
  }

  @Override
  public Collection<? extends UploadUnit> prepare(ItemVersion attachment, LoadUploadContext context) throws UploadUnit.CantUploadException {
    SyncState state = attachment.getSyncState();
    UploadUnit unit;
    ItemVersion issue = attachment.readValue(Attachment.ISSUE);
    switch (state) {
    case NEW:
      AttachmentValues change = AttachmentValues.load(attachment);
      SlaveIds slaveIds = SlaveIds.markUpload(context, attachment, ServerAttachment.ISSUE, ServerAttachment.TYPE, ServerAttachment.ID);
      unit = new AddAttachment(attachment, change, slaveIds, CreateIssueUnit.getExisting(issue, context));
      break;
    case LOCAL_DELETE:
      change = AttachmentValues.load(attachment);
      unit = new DeleteAttachment(attachment.getItem(), change, CreateIssueUnit.getExisting(issue, context));
      break;
    case SYNC:
    case EDITED:
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT:
    default:
      throw UploadUnit.CantUploadException.create("Not uploadable attachment state", state, attachment);
    }
    return Collections.singleton(unit);
  }

  public static void findFailedUploads(EntityWriter writer) {
    FailedAttachment.findFailedUploads(writer);
  }

  private static class AddAttachment extends AddEditSlaveUnit<AttachmentValues> {
    private static final LocalizedAccessor.Value M_FAILURE_SHORT = JiraAttachments.I18N.getFactory("attachment.upload.add.failure.short");
    private static final LocalizedAccessor.MessageStr M_FAILURE_FULL = JiraAttachments.I18N.messageStr("attachment.upload.add.failure.full");

    public AddAttachment(ItemVersion attachment, AttachmentValues change, SlaveIds slaveIds, CreateIssueUnit issue) {
      super(attachment.getItem(), issue, null, change, slaveIds);
    }

    @Override
    protected UploadProblem checkForConflict(@Nullable EntityHolder issue, @NotNull AttachmentValues base) {
      return null;
    }

    @Override
    protected Collection<? extends UploadProblem> doPerform(RestSession session, int issueId, @Nullable EditIssue edit, @Nullable AttachmentValues base,
      final AttachmentValues change)
      throws ConnectorException
    {
      RestSession.Request request = new RestSession.Request(session.getRestResourcePath("api/2/issue/" + issueId + "/attachments"), "uploadAttachment", null) {
        @Override
        protected HttpMethodBase create(String url) throws HttpMethodFactoryException {
          PostMethod post = HttpUtils.createPost(url);
          HttpMethodParams params = new HttpMethodParams();
          FilePart filePart;
          try {
            filePart = new FilePart("file", change.getAttachmentName(), change.getFile());
            filePart.setCharSet(null);
            String mimeType = change.getMimeType();
            if (mimeType != null)
              filePart.setContentType(mimeType);
          } catch (FileNotFoundException e) {
            throw new HttpMethodFactoryException(e.getMessage(), e);
          }
          MultipartRequestEntity entity = new MultipartRequestEntity(new Part[] {filePart}, params);
          post.setRequestEntity(entity);
          return post;
        }

        @Override
        protected void doDumpRequest(RequestDumper dumper) {
          File file = change.getFile();
          dumper.addMessage(
            "Uploading attachment: name=" + change.getAttachmentName() + " mime-type=" + change.getMimeType() + " size=" + file.length() + " isFile=" + file.isFile() + " path=" +
              file.getPath());
        }
      };
      request.addRequestHeader("X-Atlassian-Token", "nocheck");
      RestResponse response = session.perform(request, RequestPolicy.NEEDS_LOGIN);
      Integer id;
      if (!response.isSuccessful()) {
        LogHelper.debug("Upload attachment failed", issueId, change, response.getStatusCode());
        return UploadProblem.serverError(M_FAILURE_SHORT.create(), response.getStatusCode(), response.getStatusText()).toCollection() ;
      } else {
        Object json = null;
        JSONArray array;
        try {
          json = response.getJSON();
          array = Util.castNullable(JSONArray.class, json);
          LogHelper.assertWarning(array != null, "JSON array expected");
        } catch (ParseException e) {
          LogHelper.debug(e, json);
          array = null;
        }
        if (array == null) return UploadProblem.parseProblem(M_FAILURE_SHORT.create()).toCollection();
        id = JRAttachment.ID.getValue(array.get(0));
      }
      if (id == null) return UploadProblem.fatal("", null).toCollection();
      change.setId(id);
      return null;
    }

    @Override
    protected void doFinishUpload(PostUploadContext context, EntityHolder issue, long item, AttachmentValues change, boolean newSlave) {
      Integer id = change.getId();
      if (id == null) {
        context.addMessage(this, UploadProblem.fatal(M_FAILURE_SHORT.create(), M_FAILURE_FULL.formatMessage(change.getAttachmentName())));
        return;
      }
      EntityHolder attachment = ServerAttachment.find(issue, id);
      if (attachment != null) attachment.setItem(item); // Set attachment item only if it is found
      // Confirm upload if the change is actually uploaded (has ID). See https://jira.almworks.com/browse/JCO-1455, https://jira.atlassian.com/browse/JRA-27637
      context.reportUploaded(item, SyncAttributes.INVISIBLE);
      context.reportUploaded(item, Attachment.LOCAL_FILE);
    }
  }
}
