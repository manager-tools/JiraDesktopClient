package com.almworks.jira.provider3.custom.fieldtypes.enums.single;

import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumDescriptor;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumKind;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.remotedata.issue.fields.EntityType;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Function2;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SingleEnumFieldType extends FieldType {
  private static final SingleEnumEditorType RADIO_BUTTONS = new SingleEnumEditorType(true, false) {
    @Override
    protected FieldEditor createEditorFace(DropdownEditorBuilder builder) {
      return builder.createRadioButtonList();
    }
  };

  private static final Map<String, SingleEnumEditorType> EDITORS;
  static {
    HashMap<String, SingleEnumEditorType> map = Collections15.hashMap();
    map.put("combo", new SingleComboType(true, false));
    map.put("comboLegal", new SingleComboType(true, true));
    map.put("radio", RADIO_BUTTONS);
    EDITORS = map;
  }

  private static final Map<String, Function2<Pair<?,String>,String,Object>> TO_JSON;
  static {
    HashMap<String, Function2<Pair<?,String>,String,Object>> map = Collections15.hashMap();
    map.put("rawTextId", new Function2<Pair<?, String>, String, Object>() {
      @Override
      public Object invoke(Pair<?, String> pair, String s) {
        if (pair == null) return null;
        Object id = pair.getFirst();
        if (id == null) LogHelper.error("Missing id", pair, s);
        else {
          String strId = Util.castNullable(String.class, id);
          if (strId == null) LogHelper.error("Wrong id class", pair, s);
          return strId;
        }
        return null;
      }
    });
    map.put("intId", new Function2<Pair<?,String>, String, Object>() {
      @Override
      public Object invoke(Pair<?, String> pair, String s) {
        if (pair == null) return null;
        Object id = pair.getFirst();
        if (id == null) LogHelper.error("Missing id", pair, s);
        else {
          String strId = Util.castNullable(String.class, id);
          if (strId == null) LogHelper.error("Wrong id class", pair, s);
          else
            try {
              return Integer.parseInt(strId);
            } catch (NumberFormatException e) {
              LogHelper.warning("Wrong ID, expected int", strId, s);
            }
        }
        return null;
      }
    });
    TO_JSON = map;
  }

  public SingleEnumFieldType() {
    super("enum", FieldType.unite(EnumDescriptor.KEYS, ConfigKeys.NONE_NAME));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem{
    EnumDescriptor descriptor = EnumDescriptor.getEnumDescriptor(map);
    SingleEnumEditorType editorType;
    Function2<Pair<?,String>,String,?> toJson;
    Map<TypedKey<?>, ?> editableMap = ConfigKeys.EDITABLE.getFrom(map);
    if (editableMap == null) {
      editorType = null;
      toJson = null;
    } else {
      String editorId = Util.NN(ConfigKeys.EDITOR.getFrom(editableMap), descriptor.getSingleEditorId());
      editorType = CreateProblem.getFromMap(editorId, EDITORS, "Unknown single enum editor");
      toJson = Util.NN(CreateProblem.getOptional(editableMap, ConfigKeys.UPLOAD_JSON, TO_JSON, "Unknown JSON upload"), EntityType.GENERIC_JSON);
    }
    EnumKind enumInfo = descriptor.getEnumKind();
    String noneId = descriptor.getNoneId();
    String noneName = Util.NN(ConfigKeys.NONE_NAME.getFrom(map), descriptor.getNoneName());
    ConvertorFactory constraintFactory = descriptor.getConstraintFactory();
    String prefix = Util.NN(ConfigKeys.PREFIX.getFrom(map), descriptor.getSinglePrefix(editableMap != null));
    return new SingleSelectKind(enumInfo, noneId, noneName, prefix, constraintFactory, editorType, descriptor.getCreator(), toJson);
  }

  private static class SingleComboType extends SingleEnumEditorType {
    public SingleComboType(boolean verify, boolean forbidIllegalCommit) {
      super(verify, forbidIllegalCommit);
    }

    @Override
    protected FieldEditor createEditorFace(DropdownEditorBuilder builder) {
      return builder.create();
    }
  }
}
