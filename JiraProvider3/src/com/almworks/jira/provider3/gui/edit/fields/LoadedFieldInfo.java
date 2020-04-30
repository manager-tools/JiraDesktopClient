package com.almworks.jira.provider3.gui.edit.fields;

import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.edit.FieldSet;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LoadedFieldInfo {
  private static final TypedKey<LoadedFieldInfo> KEY = TypedKey.create("loadedFieldInfo");

  private final HashMap<Pair<Long, Long>, FieldInfoSet> myFields;
  private final FieldInfoSet myAllFieldsInfo;

  private LoadedFieldInfo(HashMap<Pair<Long, Long>, FieldInfoSet> fields) {
    myFields = fields;
    myAllFieldsInfo = FieldInfoSet.merge(fields.values());
  }

  @Nullable
  public static LoadedFieldInfo ensureLoaded(VersionSource source, EditItemModel anyModel) {
    EditItemModel root = anyModel.getRootModel();
    LoadedFieldInfo info = root.getValue(KEY);
    if (info == null) {
      long connectionItem = EngineConsts.getConnectionItem(root);
      if (connectionItem <= 0) {
        LogHelper.error("Missing connection");
        return null;
      }
      info = load(source.forItem(connectionItem));
      root.putHint(KEY, info);
    }
    return info;
  }

  @Nullable
  public static LoadedFieldInfo getInstance(EditItemModel model) {
    return model != null ? model.getRootModel().getValue(KEY) : null;
  }

  private static LoadedFieldInfo load(ItemVersion connection) {
    HashMap<Pair<Long, Long>, FieldInfoSet> fields = JiraFieldsInfo.loadCurrent(connection);
    return new LoadedFieldInfo(fields);
  }

  /**
   * @return all known fields. Mandatory mark is obtained from scope defined by project and type
   * @see #getOnlyInScope(Long, Long)
   */
  public FieldInfoSet getAllFields(Long project, Long type) {
    FieldInfoSet info = getOnlyInScope(project, type);
    FieldInfoSetBuilder result = new FieldInfoSetBuilder();
    result.merge(info, true);
    result.merge(myAllFieldsInfo, true);
    return result.build();
  }

  /**
   * @return all fields applicable in scope defined by project and type
   * @see #getAllFields(Long, Long)
   */
  @NotNull
  public FieldInfoSet getOnlyInScope(Long project, Long type) {
    FieldInfoSet info = myFields.get(Pair.create(project, type));
    if (info != null) return info;
    FieldInfoSetBuilder byProject = new FieldInfoSetBuilder();
    FieldInfoSetBuilder byType = new FieldInfoSetBuilder();
    for (Map.Entry<Pair<Long, Long>, FieldInfoSet> entry : myFields.entrySet()) {
      Long prj = entry.getKey().getFirst();
      Long tp = entry.getKey().getSecond();
      if (prj.equals(project)) byProject.merge(entry.getValue(), false);
      if (tp.equals(type)) byType.merge(entry.getValue(), false);
    }
    return (!byProject.isEmpty() ? byProject : byType).build();
  }

  public void checkMandatory(DataVerification context, Collection<FieldEditor> editors) {
    EditItemModel model = context.getModel();
    Long project = model.getSingleEnumValue(Issue.PROJECT);
    Long type = model.getSingleEnumValue(Issue.ISSUE_TYPE);
    if (project == null || type == null) {
      LogHelper.warning("Missing project/type", project, type);
      return;
    }
    FieldInfoSet info = getOnlyInScope(project, type);
    Map<String, FieldEditor> map = ResolvedField.getEditorsMap(model);
    for (Map.Entry<String, FieldEditor> entry : map.entrySet()) {
      Pair<NameMnemonic, Boolean> pair = info.getInfo(entry.getKey());
      if (pair == null) {
        LogHelper.debug("Missing field info for", entry.getKey());
        continue;
      }
      FieldEditor editor = entry.getValue();
      boolean mandatory = pair.getSecond();
      if (model.isEnabled(editor) && mandatory && editors.contains(editor) && !editor.hasValue(model)) {
        DataVerification.Problem problem = new DataVerification.Problem(model, editor.getLabelText(model).getText(), editor, FieldSet.M_IS_MANDATORY.create());
        context.addProblem(problem);
      }
    }
  }

  public Set<ResolvedField> getFields(Long project, Long type) {
    return getOnlyInScope(project, type).getFields();
  }
}
