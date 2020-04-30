package com.almworks.jira.provider3.remotedata.issue.fields;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.remotedata.issue.EditIssueRequest;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.services.upload.PostUploadContext;
import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.jira.provider3.sync.download2.details.fields.DependentField;
import com.almworks.jira.provider3.sync.download2.details.fields.ScalarField;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.download2.rest.JsonEntityParser;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

public class CascadeFieldDescriptor extends IssueFieldDescriptor {
  private final EntityKey<Entity> myKey;
  private final JsonEntityParser myParser;
  private final DBAttribute<Long> myAttribute;

  public CascadeFieldDescriptor(String fieldId, @Nullable String displayName, EntityKey<Entity> key, JsonEntityParser parser) {
    super(fieldId, displayName);
    myKey = key;
    myParser = parser;
    myAttribute = ServerJira.toLinkAttribute(key);
  }

  @NotNull
  @Override
  public EntityKey<?> getIssueEntityKey() {
    return myKey;
  }

  @Override
  public JsonIssueField createDownloadField() {
    ValueSupplement<Entity> supplement = myParser.getParser().getSupplement();
    if (supplement != null) return new DependentField(myKey, myParser.createConvertor(), supplement);
    return ScalarField.entity(myKey, myParser.createConvertor());
  }

  @Override
  public IssueFieldValue load(ItemVersion trunk, ItemVersion base) {
    CascadeValue change = CascadeValue.read(trunk.readValue(myAttribute));
    CascadeValue expected = CascadeValue.read(base.readValue(myAttribute));
    return new MyFieldValue(this, change, expected);
  }

  @Override
  public String toString() {
    return "Cascade [" + myKey + "]";
  }

  private CascadeValue getValue(EntityHolder issue) {
    return CascadeValue.read(issue.getReference(myKey));
  }

  private String getConflictMessage(CascadeValue expected, CascadeValue actual) {
    return createConflictMessage(getDisplayName(), CascadeValue.getDisplayableValue(expected), CascadeValue.getDisplayableValue(actual));
  }

  private static class CascadeValue { // todo implement IssueFormValue
    private final int myId;
    private final String myName;
    @Nullable("When top-level value")
    private final CascadeValue myParent;

    private CascadeValue(int id, String name, CascadeValue parent) {
      if (name == null) name = String.valueOf(id);
      myId = id;
      myName = name;
      myParent = parent;
    }

    @Nullable
    public static CascadeValue read(@Nullable ItemVersion cascadeItem) {
      if (cascadeItem == null || cascadeItem.getItem() <= 0) return null;
      Integer id = cascadeItem.getValue(CustomField.ENUM_ID);
      String name = cascadeItem.getValue(CustomField.ENUM_DISPLAY_NAME);
      ItemVersion parent = cascadeItem.readValue(CustomField.ENUM_PARENT);
      if (id == null) {
        LogHelper.error("Missing cascade id", cascadeItem);
        return null;
      }
      return new CascadeValue(id, name, read(parent));
    }

    @Nullable
    public static CascadeValue read(@Nullable EntityHolder holder) {
      if (holder == null) return null;
      Integer id = holder.getScalarValue(ServerCustomField.ENUM_ID);
      String name = holder.getScalarValue(ServerCustomField.ENUM_DISPLAY_NAME);
      if (id == null) {
        LogHelper.error("Missing cascade id", holder);
        return null;
      }
      EntityHolder parent = holder.getReference(ServerCustomField.ENUM_PARENT);
      return new CascadeValue(id, name, read(parent));
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      CascadeValue other = Util.castNullable(CascadeValue.class, obj);
      return other != null && other.myId == myId && Util.equals(other.myParent, myParent);
    }

    @Override
    public int hashCode() {
      return myId;
    }

    @Override
    public String toString() {
      return (myParent != null ? myParent.toString() + "/" : "") + myName + "(" + myId + ")";
    }

    public static String getDisplayableValue(@Nullable CascadeValue value) {
      return value != null ? value.myName : M_NO_VALUE.create();
    }

    public String[] toFormValue() {
      String ownId = String.valueOf(myId);
      return myParent == null ? new String[] {ownId, "-1"} : new String[] {String.valueOf(myParent.myId), ownId};
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
      JSONObject own = new JSONObject();
      own.put("id", String.valueOf(myId));
      if (myParent == null) return own;
      JSONObject parent = myParent.toJson();
      parent.put("child", own);
      return parent;
    }
  }

  private static class MyFieldValue extends BaseValue {
    private final CascadeFieldDescriptor myDescriptor;
    private final CascadeValue myChange;
    private final CascadeValue myExpected;

    public MyFieldValue(CascadeFieldDescriptor descriptor, CascadeValue change, CascadeValue expected) {
      super(true);
      myDescriptor = descriptor;
      myChange = change;
      myExpected = expected;
    }

    @Override
    public IssueFieldDescriptor getDescriptor() {
      return myDescriptor;
    }

    @NotNull
    @Override
    public String[] getFormValue(RestServerInfo serverInfo) {
      return myChange == null ? new String[] {"-1", "-1"} : myChange.toFormValue();
    }

    @Override
    public boolean isChanged() {
      return !Util.equals(myChange, myExpected);
    }

    @Override
    public String checkInitialState(EntityHolder issue) {
      CascadeValue server = myDescriptor.getValue(issue);
      if (Util.equals(server, myExpected)) return null;
      return myDescriptor.getConflictMessage(myExpected, server);
    }

    @Override
    public void addChange(EditIssueRequest edit) throws UploadProblem.Thrown {
      String fieldId = myDescriptor.getFieldId();
      if (edit.needsUpload(fieldId, SET, needsUpload(edit.getServerInfo()))) {
        JSONObject value = myChange == null ? null : myChange.toJson();
        //noinspection unchecked
        edit.addEdit(this, fieldId, UploadJsonUtil.singleObjectElementArray(SET, value));
      }
    }

    @Override
    protected void doFinishUpload(long issueItem, EntityHolder issue, PostUploadContext context) {
      CascadeValue server = myDescriptor.getValue(issue);
      if (Util.equals(myChange, server)) context.reportUploaded(issueItem, myDescriptor.myAttribute);
      else LogHelper.debug("Not uploaded", issueItem, this, server);
    }

    @Override
    public String toString() {
      return "Upload " + myDescriptor + "[" + myExpected + "->" + myChange + "]";
    }
  }
}
