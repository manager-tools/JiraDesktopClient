package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.components.ASeparator;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public class HtmlMessage extends ComponentHolder {
  private final String myHtml;
  private final ComponentControl.Dimensions myDimension;
  private final boolean mySeparator;

  public HtmlMessage(ComponentControl.Dimensions dimension, String html, boolean separator) {
    myHtml = html;
    myDimension = dimension;
    mySeparator = separator;
  }

  public static FieldEditor wideLine(String html, boolean separator) {
    return new HtmlMessage(ComponentControl.Dimensions.WIDE_LINE, html, separator);
  }

  @Override
  protected ComponentControl.Dimensions getDimensions(EditItemModel model) {
    return myDimension;
  }

  @Override
  protected JComponent createComponent(Lifespan life, EditItemModel model) {
    return mySeparator ? new ASeparator(myHtml) : new JLabel(myHtml);
  }
}
