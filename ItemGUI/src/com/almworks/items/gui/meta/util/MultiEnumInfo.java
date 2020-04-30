package com.almworks.items.gui.meta.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;

import java.util.Set;

/**
 * Builder for meta data of enum field. Can be reused - used to describe several similar fields.
 */
public class MultiEnumInfo extends BaseEnumInfo<MultiEnumInfo> {
  private DBAttribute<Set<Long>> myAttribute;
  private boolean myInlineLeftField;
  private String myRendererSeparator = ", ";
  private ScalarSequence myCellConvertor = null;

  public MultiEnumInfo() {}

  public MultiEnumInfo setAttribute(DBAttribute<Set<Long>> attribute) {
    myAttribute = attribute;
    return this;
  }

  public MultiEnumInfo setInlineLeftField() {
    myInlineLeftField = true;
    return this;
  }

  public MultiEnumInfo setMultiLineTextField() {
    myInlineLeftField = false;
    myCellConvertor = ViewerField.CELL_LABEL;
    return this;
  }

  public MultiEnumInfo setMultiIconTextField() {
    myInlineLeftField = false;
    myCellConvertor = ViewerField.CELL_ICON_TEXT;
    return this;
  }

  public MultiEnumInfo setRendererSeparator(String rendererSeparator) {
    myRendererSeparator = rendererSeparator;
    return this;
  }

  @Override
  public DBAttribute<?> getAttribute() {
    return myAttribute;
  }

  protected ScalarSequence createFieldBehaviour(DBStaticObject modelKey, String displayName, boolean hideEmptyLeftField, boolean isMultiline) {
    return myInlineLeftField
      ? ViewerField.inlineMultiEnum(modelKey, displayName, hideEmptyLeftField, isMultiline)
      : ViewerField.multiEnum(modelKey, displayName, hideEmptyLeftField, isMultiline, myCellConvertor);
  }

  @Override
  protected ScalarSequence createRenderer() {
    return ColumnRenderer.valueListCanvasDefault(getRendererMinStage(), myRendererSeparator);
  }

  protected ScalarSequence createExportRenderer() {
    return ItemRenderers.listRenderer(myRendererSeparator, getExportElementRenderer());
  }

}
