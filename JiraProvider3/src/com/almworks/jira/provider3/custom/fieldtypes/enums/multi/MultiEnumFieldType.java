package com.almworks.jira.provider3.custom.fieldtypes.enums.multi;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.multi.CheckBoxListEditor;
import com.almworks.items.gui.edit.editors.enums.multi.CompactEnumSubsetEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumDescriptor;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumEditorInfo;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumKind;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.util.LogHelper;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MultiEnumFieldType extends FieldType {
  private static final MultiEnumEditorType MULTI_SUBSET = new MultiEnumEditorType() {
    @Override
    public FieldEditor createEditor(ItemVersion field) {
      EnumEditorInfo<Set<Long>> info = EnumEditorInfo.SET.load(field);
      if (info == null) return null;
      return new CompactEnumSubsetEditor(NameMnemonic.rawText(info.getName()), info.getAttribute(), info.getVariants(), info.getOverrideRenderer());
    }
  };
  private static final MultiEnumEditorType MULTI_CHECKBOXES = new MultiEnumEditorType() {
    @Override
    public FieldEditor createEditor(ItemVersion field) {
      EnumEditorInfo<Set<Long>> info = EnumEditorInfo.SET.load(field);
      if (info == null) return null;
      return new CheckBoxListEditor(NameMnemonic.rawText(info.getName()), info.getAttribute(), info.getVariants(), info.getOverrideRenderer());
    }
  };

  private static final Map<String, MultiEnumEditorType> EDITORS;
  static {
    HashMap<String, MultiEnumEditorType> map = Collections15.hashMap();
    map.put("subset", MULTI_SUBSET);
    map.put("checkboxes", MULTI_CHECKBOXES);
    EDITORS = map;
  }

  public MultiEnumFieldType() {
    super("multiEnum", FieldType.unite(EnumDescriptor.KEYS, MultiEnumProperties.KEYS, ConfigKeys.NONE_NAME));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    EnumDescriptor descriptor = EnumDescriptor.getEnumDescriptor(map);
    Map<TypedKey<?>, ?> editableMap = ConfigKeys.EDITABLE.getFrom(map);
    MultiEnumEditorType editorType;
    if (editableMap == null) {
      editorType = null;
    } else {
      String editorId = Util.NN(ConfigKeys.EDITOR.getFrom(editableMap), descriptor.getMultiEditorId());
      editorType = CreateProblem.getFromMap(editorId, EDITORS, "Unknown " + "multi" + " enum editor");
    }
    //noinspection ConstantConditions
    LogHelper.assertError((editorType == null) == (editableMap == null), "Expected both editor and upload", editorType);
    EnumKind enumInfo = descriptor.getEnumKind();
    MultiEnumProperties properties = MultiEnumProperties.create(map, descriptor.getNoneId(), descriptor.getNoneName());
    ConvertorFactory constraintFactory = descriptor.getConstraintFactory();
    String prefix = Util.NN(ConfigKeys.PREFIX.getFrom(map), descriptor.getMultiPrefix(editableMap != null));
    return new MultiSelectKind(enumInfo, prefix, properties, constraintFactory, editorType);
  }
}
