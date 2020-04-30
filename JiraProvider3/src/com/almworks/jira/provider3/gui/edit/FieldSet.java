package com.almworks.jira.provider3.gui.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.LoadAttribute;
import com.almworks.items.gui.edit.editors.composition.MandatoryEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Containers;
import com.almworks.util.commons.Condition;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represent an issue fields to be edited.<br>
 * The aim of this class is to {@link #createEditors(com.almworks.items.sync.VersionSource, EditorsScheme) create editors}
 * to edit one or more issues. When creating editors it {@link #ATTRIBUTE_PROVIDES provides issue project and type} to the model, so other editors can obtain the values even when
 * project and issue type are not edited by the model.
 */
public class FieldSet {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(FieldSet.class.getClassLoader(), "com/almworks/jira/provider3/gui/edit/message");
  public static final LocalizedAccessor.Value M_IS_MANDATORY = I18N.getFactory("issue.field.editor.mandatory.message");

  /** This editor makes value of project and type available to other editors */
  private static final Map<ServerFields.Field, FieldEditor> ATTRIBUTE_PROVIDES;
  static {
    HashMap<ServerFields.Field, FieldEditor> map = Collections15.hashMap();
    map.put(ServerFields.PROJECT, EditMetaSchema.PROVIDE_PROJECT);
    map.put(ServerFields.ISSUE_TYPE, new LoadAttribute(Issue.ISSUE_TYPE));
    ATTRIBUTE_PROVIDES = Collections.unmodifiableMap(map);
  }

  private final JiraConnection3 myConnection;
  private final LongArray myFields = new LongArray();
  private final LongArray myMandatoryFields = new LongArray();
  @Nullable
  private Map<ResolvedField, FieldEditor> myEditors;
  private final List<FieldEditor> myAttributeProviders = Collections15.arrayList(ATTRIBUTE_PROVIDES.size());

  private FieldSet(JiraConnection3 connection) {
    myConnection = connection;
  }

  public static FieldSet create(@NotNull JiraConnection3 connection, LongList fields, @Nullable LongList mandatory) {
    LongArray fieldItems = LongArray.copy(fields);
    mandatory = Util.NN(mandatory, LongList.EMPTY);
    FieldSet result = new FieldSet(connection);
    for (LongIterator cursor : fields) {
      long field = cursor.value();
      if (field <= 0) continue;
      result.addField(field, mandatory.contains(field));
    }
    return result;
  }

  public void addField(long field, boolean mandatory) {
    if (myEditors != null) {
      LogHelper.error("Already created", field, mandatory);
      return;
    }
    if (field <= 0) {
      LogHelper.error("Missing fields", mandatory);
      return;
    }
    if (!myFields.addSorted(field)) {
      LogHelper.warning("Duplicated field", field, mandatory, myMandatoryFields.contains(field));
      return;
    }
    if (mandatory) myMandatoryFields.addSorted(field);
  }

  public void addFields(VersionSource source, List<ServerFields.Field> fields) {
    for (ServerFields.Field field : fields) addField(field.findItem(source.getReader()), false);
  }

  public static FieldSet allFields(JiraConnection3 connection, DBReader reader) {
    LongArray fields = LongArray.create(ServerFields.resolve(reader, ServerFields.EDITABLE_FIELDS).values());
    CustomFieldsComponent customFields = connection.getCustomFields();
    for (ItemVersion field : SyncUtils.readItems(reader, CustomField.queryKnownKey(reader, connection.getConnectionItem())))
      if (customFields.isEditable(field)) fields.add(field.getItem());
    fields.sortUnique();
    return create(connection, fields, null);
  }

  /**
   * Create field editors.
   * @param scheme configures editor that should be used for the fields
   */
  public void createEditors(VersionSource source, EditorsScheme scheme) {
    if (myEditors != null) {
      LogHelper.error("Already created");
      return;
    }
    TLongObjectHashMap<FieldEditor> attributeProviders = new TLongObjectHashMap<>();
    for (Map.Entry<ServerFields.Field, FieldEditor> entry : ATTRIBUTE_PROVIDES.entrySet()) {
      ServerFields.Field field = entry.getKey();
      long item = field.findItem(source);
      FieldEditor editor = entry.getValue();
      if (item <= 0) LogHelper.error("Field not found", field, editor);
      else attributeProviders.put(item, editor);
    }

    Map<ResolvedField, FieldEditor> result = Collections15.hashMap();
    for (ItemVersion item : source.readItems(myFields)) {
      ResolvedField field = ResolvedField.load(item);
      if (field == null) continue;
      FieldEditor editor = scheme.getEditor(myConnection, item);
      if (editor != null) {
        attributeProviders.remove(item.getItem());
        if (myMandatoryFields.contains(item.getItem())) editor = new MandatoryEditor(editor, M_IS_MANDATORY.create());
        result.put(field, editor);
      } else LogHelper.warning("No editor for", item, field);
    }
    myAttributeProviders.clear();
    //noinspection unchecked
    myAttributeProviders.addAll((List<FieldEditor>)(List<?>)Arrays.asList(attributeProviders.getValues()));
    myEditors = result;
  }

  /**
   * Adds to target list all fields that satisfy filter condition in order specified by order comparator. Then removes the added editors.<br>
   * Accessing editor via this method guarantees that no editor is obtained twice.<br>
   * Also registers editors in the model so they can be
   * {@link com.almworks.jira.provider3.gui.edit.ResolvedField#findEditor(com.almworks.items.gui.edit.EditModelState, Class, String)  found later by associated field}
   * @param model register editors in the model
   * @param target target list
   * @param order fields order
   * @param filter fields filter
   */
  public void extractEditors(EditModelState model, ArrayList<FieldEditor> target, Comparator<ResolvedField> order, Condition<ResolvedField> filter) {
    Map<ResolvedField, FieldEditor> editors = ensureCreated();
    if (editors == null) return;
    List<ResolvedField> fields = filter.select(editors.keySet());
    Collections.sort(fields, order);
    for (ResolvedField field : fields) {
      FieldEditor editor = editors.remove(field);
      addEditor(model, target, field, editor);
    }
  }

  public void extractEditor(EditModelState model, ArrayList<FieldEditor> target, String jiraId) {
    Map<ResolvedField, FieldEditor> editors = ensureCreated();
    if (editors == null) return;
    for (Iterator<Map.Entry<ResolvedField, FieldEditor>> iterator = editors.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry<ResolvedField, FieldEditor> entry = iterator.next();
      if (entry.getKey().getJiraId().equals(jiraId)) {
        addEditor(model, target, entry.getKey(), entry.getValue());
        iterator.remove();
        return;
      }
    }
  }

  /**
   * Extracts all editors (not yet extracted). Also adds attribute providers
   * @return all editors and attribute providers
   * @see #extractEditors(com.almworks.items.gui.edit.EditModelState, java.util.ArrayList, java.util.Comparator, com.almworks.util.commons.Condition)
   */
  public List<FieldEditor> extractAllEditors(EditItemModel model) {
    ArrayList<FieldEditor> result = Collections15.arrayList();
    extractEditors(model, result, Containers.<ResolvedField>noOrder(), Condition.<ResolvedField>always());
    addAttributeProviders(result);
    return result;
  }

  private void addEditor(EditModelState model, ArrayList<FieldEditor> target, ResolvedField field, FieldEditor editor) {
    if (editor == null) {
      LogHelper.error("No editor found", field);
      return;
    }
    target.add(editor);
    ResolvedField.addEditor(model, field, editor);
  }

  /**
   * Add attribute provider editor to the target list. This method can be used several times
   */
  public void addAttributeProviders(ArrayList<FieldEditor> target) {
    target.addAll(myAttributeProviders);
  }

  @Nullable
  private Map<ResolvedField, FieldEditor> ensureCreated() {
    Map<ResolvedField, FieldEditor> editors = myEditors;
    if (editors == null) LogHelper.error("Editor not created yet");
    return editors;
  }
}
