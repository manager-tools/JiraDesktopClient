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
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public class DecimalFieldType extends FieldType {
  /**
   * If false column renders nothing for zero value ("0" otherwise)
   */
  private static final TypedKey<Boolean> SHOW_ZERO = TypedKey.create("showZero", Boolean.class);

  public static final ScalarEditorType<?> DECIMAL_EDITOR = new ScalarEditorType<BigDecimal>(BigDecimal.class) {
    @Override
    protected FieldEditor createEditor(NameMnemonic name, DBAttribute<BigDecimal> attribute) {
      return ScalarFieldEditor.decimal(name, attribute);
    }
  };

  public DecimalFieldType() {
    super("decimal", unite(CommonFieldInfo.KEYS, SHOW_ZERO, ConfigKeys.NO_REMOTE_SEARCH));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    CommonFieldInfo fieldInfo = CommonFieldInfo.create(map);
    ScalarKind.ScalarConvertor<BigDecimal> convertor = new Decimal(Util.NN(SHOW_ZERO.getFrom(map), true));
    ConvertorFactory remoteSearch = Util.NN(ConfigKeys.NO_REMOTE_SEARCH.getFrom(map), false) ? null : ConvertorFactory.SEARCH_FLOAT;
    boolean editable = ConfigKeys.EDITABLE.getFrom(map) != null;
    String prefix = ConfigKeys.PREFIX.getFrom(map);
    if (prefix == null) prefix = editable ? "editableDecimal" : "readonlyDecimal";
    ScalarEditorType<?> editor = DECIMAL_EDITOR;
    if (!editable) {
      editor = null;
      fieldInfo = fieldInfo.noReorder();
    }
    return ScalarKind.create(convertor, prefix, fieldInfo, remoteSearch, editor, editable ? ScalarFieldDescriptor.EDITABLE_DECIMAL : ScalarFieldDescriptor.READONLY_DECIMAL);
  }

  private static class Decimal extends ScalarKind.ScalarConvertor<BigDecimal> {
    private final boolean myShowZero;

    protected Decimal(boolean showZero) {
      myShowZero = showZero;
    }

    @Override
    public ScalarFieldInfo<BigDecimal> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.decimal(ItemDownloadStage.QUICK, myShowZero)
        .setMultiline(false)
        .setHideEmptyLeftField(true)
        .setColumnId(connectionIdPrefix + id);
    }
  }
}
