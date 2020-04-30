package com.almworks.jira.provider3.gui.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class ResolvedField {
  private static final TypedKey<Map<String, FieldEditor>> EDITORS = TypedKey.create("fieldEditors");

  public static final Convertor<ResolvedField, String> GET_DISPLAY_NAME = new Convertor<ResolvedField, String>() {
    @Override
    public String convert(ResolvedField value) {
      return value.getDisplayName();
    }
  };
  public static final Convertor<ResolvedField, Long> GET_ITEM = new Convertor<ResolvedField, Long>() {
    @Override
    public Long convert(ResolvedField value) {
      return value != null ? value.getItem() : 0l;
    }
  };
  public static final Convertor<ResolvedField, String> GET_JIRA_ID = new Convertor<ResolvedField, String>() {
    @Override
    public String convert(ResolvedField value) {
      return value != null ? value.getJiraId() : "";
    }
  };

  public static final Comparator<ResolvedField> BY_DISPLAY_NAME = Containers.convertingComparator(GET_DISPLAY_NAME, String.CASE_INSENSITIVE_ORDER);
  public static final CanvasRenderer<ResolvedField> RENDERER = Renderers.convertingCanvasRenderer(Renderers.canvasToString(), GET_DISPLAY_NAME);

  public static Condition<ResolvedField> filterFields(final Collection<ServerFields.Field> fields) {
    return new Condition<ResolvedField>() {
      @Override
      public boolean isAccepted(ResolvedField value) {
        Static field = Util.castNullable(Static.class, value);
        return field != null && fields.contains(field.myField);
      }
    };
  }

  private final long myItem;

  protected ResolvedField(long item) {
    myItem = item;
  }

  public final long getItem() {
    return myItem;
  }

  public abstract String getJiraId();

  @NotNull
  public abstract String getDisplayName();

  public abstract boolean isStatic();

  @Nullable
  public static ResolvedField load(ItemVersion fieldItem) {
    ServerFields.Field field = ServerFields.find(fieldItem);
    if (field != null) return new Static(fieldItem.getItem(), field);
    String id = fieldItem.getValue(CustomField.ID);
    String displayName = fieldItem.getValue(CustomField.NAME);
    if (displayName == null || displayName.isEmpty()) displayName = id;
    if (id != null) return new Custom(fieldItem.getItem(), id, displayName);
    LogHelper.error("Not a field", fieldItem);
    return null;
  }

  @NotNull
  public static ArrayList<ResolvedField> load(VersionSource source, LongList fieldItems) {
    ArrayList<ResolvedField> result = Collections15.arrayList();
    for (ItemVersion field : source.readItems(fieldItems)) {
      ResolvedField resolvedField = load(field);
      if (resolvedField != null) result.add(resolvedField);
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    ResolvedField other = Util.castNullable(ResolvedField.class, obj);
    return other != null && myItem == other.myItem;
  }

  @Override
  public int hashCode() {
    return (int) myItem;
  }

  public static void addEditor(EditModelState model, ResolvedField field, FieldEditor editor) {
    addEditor(model, field.getJiraId(), editor);
  }

  public static void addEditor(EditModelState model, String fieldId, FieldEditor editor) {
    Map<String, FieldEditor> editors = model.getValue(EDITORS);
    if (editors == null) {
      editors = Collections15.hashMap();
      model.putHint(EDITORS, editors);
    }
    if (!editors.containsKey(fieldId)) editors.put(fieldId, editor);
    else LogHelper.error("Already registered field editor", fieldId, editor, model);
  }

  /**
   * Find an editor by field if it has been {@link #addEditor(com.almworks.items.gui.edit.EditModelState, ResolvedField, com.almworks.items.gui.edit.FieldEditor) registered} in the model
   * @param model model to search in
   * @param editorClass expected editor class
   * @param fieldId field's JIRA ID
   * @return editor for the specified field or null if no such editor registered
   */
  @Nullable
  public static <T extends FieldEditor> T findEditor(EditModelState model, Class<T> editorClass, String fieldId) {
    Map<String, FieldEditor> editors = getEditorsMap(model);
    for (Map.Entry<String, FieldEditor> entry : editors.entrySet()) {
      if (entry.getKey().equals(fieldId)) {
        FieldEditor editor = entry.getValue();
        T casted = Util.castNullable(editorClass, editor);
        if (casted == null) LogHelper.error("Wrong editor class", editorClass, fieldId, editor);
        return casted;
      }
    }
    LogHelper.assertError(editorClass == FieldEditor.class, "Editor not found", editorClass, fieldId);
    return null;
  }

  @Nullable
  public static ResolvedField loadStatic(DBReader reader, ServerFields.Field field) {
    long item = field.findItem(reader);
    return item > 0 ? new Static(item, field) : null;
  }

  @NotNull
  public static Map<String, FieldEditor> getEditorsMap(EditModelState model) {
    Map<String, FieldEditor> editors = model.getValue(EDITORS);
    if (editors == null) {
      LogHelper.error("No field editors found");
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(editors);
  }

  @NotNull
  public static Map<String, ResolvedField> loadAllById(ItemVersion connection) {
    HashMap<String, ResolvedField> result = Collections15.hashMap();
    List<ResolvedField> fields = loadAll(connection);
    for (ResolvedField field : fields) result.put(field.getJiraId(), field);
    return result;
  }

  public static List<ResolvedField> loadAll(ItemVersion connection) {
    ArrayList<ResolvedField> result = Collections15.arrayList();
    for (ServerFields.Field field : ServerFields.ALL_FIELDS) {
      ResolvedField resolvedField = loadStatic(connection.getReader(), field);
      if (resolvedField != null) result.add(resolvedField);
    }
    for (ItemVersion field : connection.readItems(CustomField.queryKnownKey(connection))) {
      ResolvedField resolvedField = load(field);
      if (resolvedField != null) result.add(resolvedField);
    }
    return result;
  }

  public static boolean containsStatic(Collection<ResolvedField> fields, ServerFields.Field field) {
    return findStatic(fields, field) != null;
  }

  @Nullable("When not found")
  public static ResolvedField findStatic(Collection<ResolvedField> fields, ServerFields.Field field) {
    for (ResolvedField resolvedField : fields) {
      Static staticField = Util.castNullable(Static.class, resolvedField);
      if (staticField != null && staticField.myField.equals(field)) return resolvedField;
    }
    return null;
  }

  private static class Static extends ResolvedField {
    private final ServerFields.Field myField;

    private Static(long item, ServerFields.Field field) {
      super(item);
      myField = field;
    }

    @Override
    public String getJiraId() {
      return myField.getJiraId();
    }

    @NotNull
    @Override
    public String getDisplayName() {
      return myField.getDefaultDisplayName();
    }

    @Override
    public boolean isStatic() {
      return true;
    }

    @Override
    public String toString() {
      return "Static" + myField;
    }
  }

  private static class Custom extends ResolvedField {
    private final String myJiraId;
    private final String myDisplayName;

    private Custom(long item, String jiraId, @NotNull String displayName) {
      super(item);
      myJiraId = jiraId;
      myDisplayName = displayName;
    }

    @Override
    public String getJiraId() {
      return myJiraId;
    }

    @NotNull
    @Override
    public String getDisplayName() {
      return myDisplayName;
    }

    @Override
    public boolean isStatic() {
      return false;
    }

    @Override
    public String toString() {
      return "CustomField[" + myJiraId + "," + myDisplayName + "]";
    }
  }
}
