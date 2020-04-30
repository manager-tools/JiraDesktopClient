package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.slaves.SimpleDependent;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JRAttachment;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AttachmentsField implements JsonIssueField {
  private static final JsonIssueField DELEGATE = new SimpleDependent(ServerAttachment.TYPE, ServerAttachment.ISSUE, JRAttachment.PARTIAL_JSON_CONVERTOR, null).toField(true);
  private static final HintValue<Boolean> NOT_LOADED = HintValue.flag("attachments.load.notLoaded");

  public static final AttachmentsField INSTANCE = new AttachmentsField();

  private AttachmentsField() {
  }

  @Nullable
  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    return DELEGATE.loadValue(jsonValue);
  }

  @Nullable
  @Override
  public Collection<? extends ParsedValue> loadNull() {
    return NOT_LOADED.singleton();
  }

  public void maybeLoadAdditional(EntityHolder issue, RestSession session, ProgressInfo progressInfo) {
    if (!NOT_LOADED.isValueSet(issue)) return; // all loaded
    LogHelper.debug("Attachments REST API is disabled", issue.getScalarValue(ServerIssue.KEY));
  }
}
