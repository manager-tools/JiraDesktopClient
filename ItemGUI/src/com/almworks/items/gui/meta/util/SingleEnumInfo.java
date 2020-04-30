package com.almworks.items.gui.meta.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.schema.columns.ColumnRenderer;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;

import java.awt.*;

public class SingleEnumInfo extends BaseEnumInfo<SingleEnumInfo> {
  private DBAttribute<Long> myAttribute;
  private int myNullFontStyle = Font.PLAIN;
  private String myNullText = "";

  public SingleEnumInfo() {
  }

  public SingleEnumInfo setAttribute(DBAttribute<Long> attribute) {
    myAttribute = attribute;
    return this;
  }

  public SingleEnumInfo setNullColumnPresentation(int fontStyle, String nullText) {
    myNullFontStyle = fontStyle;
    myNullText = nullText;
    return this;
  }

  @Override
  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  protected ScalarSequence createFieldBehaviour(DBStaticObject modelKey, String displayName, boolean hideEmptyLeftField, boolean isMultiline) {
    return ViewerField.singleEnum(modelKey, displayName, hideEmptyLeftField, isMultiline);
  }

  @Override
  protected ScalarSequence createRenderer() {
    return ColumnRenderer.valueCanvasDefault(getRendererMinStage(), myNullFontStyle, myNullText);
  }

  protected ScalarSequence createExportRenderer() {
    return getExportElementRenderer();
  }
}
