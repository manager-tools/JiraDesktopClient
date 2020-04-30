package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.export.ModelKeyValueExport;
import com.almworks.items.gui.meta.schema.export.RenderValueExport;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.gui.meta.util.*;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Jira;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Date;

public class JiraFields {
  public static final ScalarSequence COMPARATOR_NUMBER_AS_TEXT =
      new ScalarSequence.Builder().append(ColumnComparator.FEATURE_NULL_FIRST).append(ColumnComparator.FEATURE_NUMBERS_AS_STRINGS).create();
  public static final ScalarSequence COMPARATOR_DECIMAL =
    new ScalarSequence.Builder().append(ColumnComparator.FEATURE_NULL_FIRST).append(ColumnComparator.FEATURE_COMPARABLES).create();
  public static final ScalarSequence COMPARATOR_BOOL =
    new ScalarSequence.Builder().append(ColumnComparator.FEATURE_NULL_LAST).append(ColumnComparator.FEATURE_REVERSE_ORDER).append(ColumnComparator.FEATURE_COMPARABLES).create();
  public static final ScalarSequence COMPARATOR_DURATION =
    new ScalarSequence.Builder().append(ColumnComparator.FEATURE_NULL_LAST).append(ColumnComparator.FEATURE_REVERSE_ORDER).append(ColumnComparator.FEATURE_COMPARABLES).create();
  public static final ScalarSequence COMPARATOR_MULTI_ENUM_BY_SIZE =
    new ScalarSequence.Builder().append(ColumnComparator.FEATURE_LIST_SIZE_COMPARATOR).append(ColumnComparator.FEATURE_COMPARABLES).create();
  public static final ScalarSequence COMPARATOR_SINGLE_ENUM =
    new ScalarSequence.Builder().append(ColumnComparator.FEATURE_NULL_LAST).append(ColumnComparator.FEATURE_COMPARABLES).create();
  public static final ScalarSequence COMPARATOR_COUNT_LIST =
      ScalarSequence.create(ColumnComparator.FEATURE_NULL_LAST, ColumnComparator.FEATURE_REVERSE_ORDER, ColumnComparator.FEATURE_COMPARABLES);

  private static final DBIdentity FEATURE_EDIT_APPLICABILITY = Jira.feature("applicability.jiraEdit");
  public static final ScalarSequence SEQUENCE_EDIT_APPLICABILITY = ScalarSequence.create(FEATURE_EDIT_APPLICABILITY);

  public static ScalarFieldInfo<String> longText() {
    return text(ExportValueType.LARGE_STRING);
  }

  private static ScalarFieldInfo<String> text(ExportValueType exportType) {
    ScalarFieldInfo<String> info = new ScalarFieldInfo<String>(ModelKeys.FEATURE_LOADER_STRING, ViewerField.FEATURE_BEHAVIOUR_TEXT);
    info.setModelKeyExport(exportType, RenderValueExport.SEQUENCE_DEFAULT_RENDERER);
    return setupField(info);
  }

  public static ScalarFieldInfo<String> shortText(ItemDownloadStage minStage, boolean numberColumnOrder) {
    return text(ExportValueType.STRING).setEmptyNullRenderer(minStage).setColumnComparator(minStage, numberColumnOrder ? COMPARATOR_NUMBER_AS_TEXT : ColumnComparator.SEQUENCE_COMPARATOR_TEXT);
  }

  public static ScalarFieldInfo<BigDecimal> decimal(ItemDownloadStage minStage, boolean showZero) {
    ScalarFieldInfo<BigDecimal> field = new ScalarFieldInfo<BigDecimal>(ModelKeys.FEATURE_LOADER_DECIMAL, showZero ? ViewerField.FEATURE_BEHAVIOUR_DECIMAL : ViewerField.FEATURE_BEHAVIOUR_DECIMAL_NOT_ZERO);
    field
      .setModelKeyExport(ExportValueType.NUMBER, ModelKeyValueExport.SEQUENCE_DECIMAL)
      .setSimpleRenderer(minStage, Font.PLAIN, "")
      .setColumnComparator(minStage, COMPARATOR_DECIMAL)
      .setCharCount(10);
    return setupField(field);
  }

  public static ScalarFieldInfo<Integer> day(ItemDownloadStage minStage, int maxDaysRelativeDays) {
    ScalarFieldInfo<Integer> field = new ScalarFieldInfo<Integer>(ModelKeys.FEATURE_LOADER_INTEGER, ViewerField.FEATURE_BEHAVIOUR_DAY);
    field
      .setModelKeyExport(ExportValueType.DATE, ModelKeyValueExport.SEQUENCE_DAY)
      .setColumnComparator(minStage, ColumnComparator.SEQUENCE_COMPARATOR_DATE_NULL_EARLIER)
      .setRenderer(ColumnRenderer.relativeDays(minStage, maxDaysRelativeDays))
      .setConstraint(Descriptors.SEQUENCE_DAY_CONSTRAINT)
      .setCharCount(10);
    return setupField(field);
  }

  public static ScalarFieldInfo<Date> dateTime(ItemDownloadStage minStage, boolean nullEarlier) {
    ScalarFieldInfo<Date> field = new ScalarFieldInfo<Date>(ModelKeys.FEATURE_LOADER_DATE_TIME, ViewerField.FEATURE_BEHAVIOUR_DATE_TIME);
    field
      .setModelKeyExport(ExportValueType.DATE, ModelKeyValueExport.SEQUENCE_DATE_TIME)
      .setRenderer(ColumnRenderer.shortestDate(minStage))
      .setColumnComparator(minStage, nullEarlier ? ColumnComparator.SEQUENCE_COMPARATOR_DATE_NULL_EARLIER : ColumnComparator.SEQUENCE_COMPARATOR_DATE_NULL_LATER)
      .setCharCount(10);
    return setupField(field);
  }

  public static ScalarFieldInfo<String> url(ItemDownloadStage minStage) {
    ScalarFieldInfo<String> info = new ScalarFieldInfo<String>(ModelKeys.FEATURE_LOADER_STRING, ViewerField.FEATURE_BEHAVIOUR_URL);
    info.setModelKeyExport(ExportValueType.STRING, RenderValueExport.SEQUENCE_DEFAULT_RENDERER);
    return setupField(info).setEmptyNullRenderer(minStage).setColumnComparator(minStage, ColumnComparator.SEQUENCE_COMPARATOR_TEXT).setCharCount(35);
  }

  public static ScalarFieldInfo<Integer> integerReversed(ItemDownloadStage minStage) {
    ScalarFieldInfo<Integer> field = new ScalarFieldInfo<Integer>(ModelKeys.FEATURE_LOADER_INTEGER, ViewerField.FEATURE_BEHAVIOUR_INTEGER);
    field
      .setModelKeyExport(ExportValueType.NUMBER, ModelKeyValueExport.SEQUENCE_INTEGER)
      .setSimpleRenderer(minStage, Font.PLAIN, "0")
      .setColumnComparator(minStage, ColumnComparator.SEQUENCE_COMPARATOR_DECIMAL_REVERSED)
      .setCharCount(5);
    return setupField(field);
  }

  public static ScalarFieldInfo<Boolean> bool(ItemDownloadStage minStage) {
    // Add left field when needed
    ScalarFieldInfo<Boolean> field = new ScalarFieldInfo<Boolean>(ModelKeys.FEATURE_LOADER_BOOLEAN, null);
    field.setEmptyNullRenderer(minStage);
    field.setColumnComparator(minStage, COMPARATOR_BOOL);
    field.setModelKeyExport(ExportValueType.STRING, RenderValueExport.SEQUENCE_RENDERER_BOOLEAN);
    return setupField(field);
  }

  public static ScalarFieldInfo<Integer> secondsDuration(ItemDownloadStage minStage) {
    ScalarFieldInfo<Integer> field = new ScalarFieldInfo<Integer>(ModelKeys.FEATURE_LOADER_INTEGER, ViewerField.FEATURE_BEHAVIOUR_SECONDS_DURATION);
    field
      .setModelKeyExport(ExportValueType.NUMBER, ModelKeyValueExport.SEQUENCE_SECONDS)
      .setColumnComparator(minStage, COMPARATOR_DURATION)
      .setCharCount(10)
      .setRenderer(ColumnRenderer.valueSeconds(minStage));
    return setupField(field);
  }

  public static SingleEnumInfo staticSingleEnum(ItemDownloadStage minStage, DBStaticObject enumType, int charCount, boolean hideEmptyLeftField) {
    SingleEnumInfo fieldInfo = new SingleEnumInfo().setColumnComparator(minStage, COMPARATOR_SINGLE_ENUM);
    setupStaticEnumInfo(fieldInfo, enumType, charCount, hideEmptyLeftField);
    return setupField(fieldInfo);
  }

  public static MultiEnumInfo staticMultiEnum(ItemDownloadStage minStage, DBStaticObject enumType, int charCount, boolean hideEmptyLeftField, boolean lexicalColumnOrder) {
    MultiEnumInfo fieldInfo = new MultiEnumInfo();
    setupStaticEnumInfo(fieldInfo, enumType, charCount, hideEmptyLeftField);
    fieldInfo.setMultiLineTextField();
    fieldInfo.setColumnComparator(minStage, lexicalColumnOrder ? ColumnComparator.SEQUENCE_COMPARATOR_MULTI_ENUM_LEXICAL : COMPARATOR_MULTI_ENUM_BY_SIZE);
    fieldInfo.setRendererMinStage(minStage);
    return setupField(fieldInfo);
  }

  public static SingleEnumInfo singleEnum(ItemDownloadStage minStage) {
    return setupField(new SingleEnumInfo().setColumnComparator(minStage, COMPARATOR_SINGLE_ENUM));
  }

  public static MultiEnumInfo multiEnum(ItemDownloadStage minStage, boolean lexicalColumnOrder) {
    return setupField(new MultiEnumInfo().setColumnComparator(minStage, lexicalColumnOrder ? ColumnComparator.SEQUENCE_COMPARATOR_MULTI_ENUM_LEXICAL : COMPARATOR_MULTI_ENUM_BY_SIZE));
  }

  private static <T extends BaseEnumInfo<T>> T setupStaticEnumInfo(T info, DBStaticObject enumType, int charCount, boolean hideEmptyLeftField) {
    info
      .setOwner(Jira.JIRA_PROVIDER_ID)
      .setEnumType(enumType)
      .setCharCount(charCount)
      .setHideEmptyLeftField(hideEmptyLeftField);
    return info;
  }

  private static <F extends BaseFieldInfo<?>> F setupField(F fieldInfo) {
    fieldInfo.setSecondLevelComparator(MetaSchema.FEATURE_ISSUE_BY_KEY_COMPARATOR);
    return fieldInfo;
  }

  public static void registerFeatures(FeatureRegistry features) {
    features.register(FEATURE_EDIT_APPLICABILITY, JiraEditApplicability.FEATURE);
  }
}
