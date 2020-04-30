package com.almworks.jira.provider3.custom.fieldtypes.scalars;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.items.gui.meta.util.ScalarFieldInfo;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.CommonFieldInfo;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.scalar.ScalarFieldDescriptor;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TextFieldType extends FieldType {
  /**
   * Defines text viewer and editor kind. Values: shortText, longText, url
   */
  private static final TypedKey<String> KIND = TypedKey.create("kind", String.class);

  public TextFieldType() {
    super("text", unite(CommonFieldInfo.KEYS, KIND, ConfigKeys.NO_REMOTE_SEARCH));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    CommonFieldInfo fieldInfo = CommonFieldInfo.create(map);
    Info kind = CreateProblem.getFromMap(Util.NN(KIND.getFrom(map), "shortText"), KINDS, "Unknown text kind");
    ConvertorFactory remoteSearch = ConvertorFactory.SEARCH_TEXT;
    if (Util.NN(ConfigKeys.NO_REMOTE_SEARCH.getFrom(map), false)) remoteSearch = null;

    ScalarEditorType<?> editorType;
    Map<TypedKey<?>, ?> editableMap = ConfigKeys.EDITABLE.getFrom(map);
    if (editableMap == null) {
      editorType = null;
    } else {
      editorType = kind.myEditorType;
    }
    @SuppressWarnings("ConstantConditions")
    boolean editable = editorType != null && editableMap != null;
    if (!editable) {
      editorType = null;
      fieldInfo = fieldInfo.noReorder();
    }
    String prefix = ConfigKeys.PREFIX.getFrom(map);
    if (prefix == null) prefix = editable ? "editableText" : "readonlyText";
    return ScalarKind.create(kind.myConvertor, prefix, fieldInfo, remoteSearch, editorType,
      editable ? ScalarFieldDescriptor.EDITABLE_TEXT : ScalarFieldDescriptor.READONLY_TEXT);
  }


  public static final ScalarKind.ScalarConvertor<String> SHORT_TEXT = new ScalarKind.ScalarConvertor<String>() {
    @Override
    public ScalarFieldInfo<String> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.shortText(ItemDownloadStage.QUICK, true)
        .setMultiline(false)
        .setHideEmptyLeftField(true)
        .setColumnId(connectionIdPrefix + id);
    }
  };

  public static final ScalarKind.ScalarConvertor<String> LONG_TEXT = new ScalarKind.ScalarConvertor<String>() {
    @Override
    public ScalarFieldInfo<String> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.longText()
        .setMultiline(true);
    }
  };

  public static final ScalarKind.ScalarConvertor<String> URL_TEXT = new ScalarKind.ScalarConvertor<String>() {
    @Override
    public ScalarFieldInfo<String> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.url(ItemDownloadStage.QUICK).setMultiline(false).setColumnId(connectionIdPrefix + id).setHideEmptyLeftField(true);
    }
  };


  private static final Map<String, Info> KINDS;
  static {
    HashMap<String,Info> map = Collections15.hashMap();
    ScalarEditorType<?> shortEditor = new ScalarEditorType<String>(String.class) {
      @Override
      protected FieldEditor createEditor(NameMnemonic name, DBAttribute<String> attribute) {
        return ScalarFieldEditor.shortText(name, attribute, true);
      }
    };
    ScalarEditorType<?> longEditor = new ScalarEditorType<String>(String.class) {
      @Override
      protected FieldEditor createEditor(NameMnemonic name, DBAttribute<String> attribute) {
        return ScalarFieldEditor.textPane(name, attribute);
      }
    };
    map.put("shortText", new Info(SHORT_TEXT, shortEditor));
    map.put("longText", new Info(LONG_TEXT, longEditor));
    map.put("url", new Info(URL_TEXT, shortEditor));
    KINDS = map;
  }

  private static class Info {
    private final ScalarKind.ScalarConvertor<String> myConvertor;
    private final ScalarEditorType<?> myEditorType;

    Info(ScalarKind.ScalarConvertor<String> convertor, ScalarEditorType<?> editorType) {
      myConvertor = convertor;
      myEditorType = editorType;
    }
  }
}
