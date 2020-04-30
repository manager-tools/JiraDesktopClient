package com.almworks.jira.provider3.custom.fieldtypes.scalars;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.scalar.DateEditor;
import com.almworks.items.gui.edit.editors.scalar.DateEditorKind;
import com.almworks.items.gui.meta.util.ScalarFieldInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.fieldtypes.CommonFieldInfo;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.fieldtypes.JqlSearchInfo;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.gui.JiraFields;
import com.almworks.jira.provider3.remotedata.issue.fields.scalar.ScalarFieldDescriptor;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlDate;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

public class DateFieldType extends FieldType {
  /**
   * If true describes date-time field. Otherwise day-only.
   */
  private static final TypedKey<Boolean> TIME = TypedKey.create("withTime", Boolean.class);

  private static final ScalarEditorType<?> DATE_EDITOR = dateType(DateEditorKind.DAY, Integer.class);
  private static final ScalarEditorType<?> DATE_TIME_EDITOR = dateType(DateEditorKind.DATE_TIME, Date.class);
  private static final ConvertorFactory FACTORY = new ConvertorFactory() {

    @Override
    public JQLConvertor createJql(ItemVersion field) {
      JqlSearchInfo<?> info = JqlSearchInfo.load(field);
      return info != null ? new JqlDate(info.getJqlName(), info.getAttribute(), info.getDisplayName()) : null;
    }
  };

  public DateFieldType() {
    super("date", unite(CommonFieldInfo.KEYS, TIME, ConfigKeys.NO_REMOTE_SEARCH));
  }

  @Override
  @NotNull
  public FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem {
    CommonFieldInfo fieldInfo = CommonFieldInfo.create(map);
    boolean withTime = Util.NN(TIME.getFrom(map), false);
    ConvertorFactory remoteSearch = Util.NN(ConfigKeys.NO_REMOTE_SEARCH.getFrom(map), false) ? null : FACTORY;
    boolean editable = ConfigKeys.EDITABLE.getFrom(map) != null;
    String prefix = ConfigKeys.PREFIX.getFrom(map);
    if (prefix == null) {
      if (withTime) prefix = editable ? "editableDateTime" : "readonlyDateTime";
      else prefix = editable ? "editableDay" : "readonlyDay";
    }
    ScalarEditorType<?> editor = withTime ? DATE_TIME_EDITOR : DATE_EDITOR;
    if (!editable) {
      editor = null;
    }
    fieldInfo = fieldInfo.noReorder();
    return withTime ?
      ScalarKind.create(new DateTime(), prefix, fieldInfo, remoteSearch, editor, editable ? ScalarFieldDescriptor.EDITABLE_DATE : ScalarFieldDescriptor.READ_ONLY_DATE) :
      ScalarKind.create(new Day(), prefix, fieldInfo, remoteSearch, editor, editable ? ScalarFieldDescriptor.EDITABLE_DAYS : ScalarFieldDescriptor.READONLY_DAYS);
  }

  private static <T> ScalarEditorType<T> dateType(final DateEditorKind<T> dateKind, Class<T> clazz) {
    return new ScalarEditorType<T>(clazz) {
      @Override
      protected FieldEditor createEditor(NameMnemonic name, DBAttribute<T> attribute) {
        return new DateEditor<T>(name, attribute, dateKind);
      }
    };
  }

  private static class DateTime extends ScalarKind.ScalarConvertor<Date> {
    public DateTime() {
    }

    @Override
    public ScalarFieldInfo<Date> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.dateTime(ItemDownloadStage.QUICK, true)
        .setMultiline(false)
        .setHideEmptyLeftField(true)
        .setColumnId(connectionIdPrefix + id);
    }
  }

  private static class Day extends ScalarKind.ScalarConvertor<Integer> {
    public Day() {
    }

    @Override
    public ScalarFieldInfo<Integer> createFieldInfo(String connectionIdPrefix, String id) {
      return JiraFields.day(ItemDownloadStage.QUICK, 0)
        .setMultiline(false)
        .setHideEmptyLeftField(true)
        .setColumnId(connectionIdPrefix + id);
    }
  }
}
