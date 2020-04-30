package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.edit.PrepareIssueUpload;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class IssueFieldDescriptor {
  private static final LocalizedAccessor.Message3 M_CONFLICT = PrepareIssueUpload.I18N.message3("field.conflict");
  protected static final LocalizedAccessor.Value M_NO_VALUE = PrepareIssueUpload.I18N.getFactory("field.noValue");
  protected static final String SET = "set";

  private final String myFieldId;
  private final String myDisplayName;
  private final boolean myHasDisplayName;

  protected IssueFieldDescriptor(String fieldId, @Nullable String displayName) {
    myFieldId = fieldId;
    if (displayName == null || displayName.isEmpty()) {
      myDisplayName = fieldId;
      myHasDisplayName = false;
    } else {
      myDisplayName = displayName;
      myHasDisplayName = true;
    }
  }

  protected static String createConflictMessage(String displayName, String expectedText, String actualText) {
    return M_CONFLICT.formatMessage(displayName, expectedText, actualText);
  }

  public String getFieldId() {
    return myFieldId;
  }

  @NotNull
  public abstract EntityKey<?> getIssueEntityKey();

  public abstract JsonIssueField createDownloadField();

  @Nullable("When nothing to upload and nothing to check")
  public abstract IssueFieldValue load(ItemVersion trunk, ItemVersion base);

  public String getDisplayName() {
    return myDisplayName;
  }

  public boolean hasDisplayName() {
    return myHasDisplayName;
  }
}
