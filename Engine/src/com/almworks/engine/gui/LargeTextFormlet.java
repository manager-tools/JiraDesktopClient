package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ATextArea;
import com.almworks.util.components.CommunalFocusListener;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.ScrollPaneBorder;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.regex.Pattern;

public class LargeTextFormlet extends AbstractFormlet implements WidthDrivenComponent, Highlightable {
  private final ATextArea myField;
  private final Highlighter myHighlighter = new DefaultHighlighter();

  private final int myDx;
  private final int myDy;
  private final int myMinWidth;
  private final ScrollPaneBorder fieldBorder;

  private boolean myVisible;
  private Pattern myPattern;

  public LargeTextFormlet(ATextArea component, BaseTextController<?> controller, Configuration config) {
    super(config);
    myField = component;
    myField.setHighlighter(myHighlighter);
    myField.setLineWrap(true);
    myField.setWrapStyleWord(true);
    fieldBorder = new ScrollPaneBorder(myField);
    CommunalFocusListener.setupJTextArea(myField);
    myField.setMargin(new Insets(0, 5, 0, 0));
    Insets b = fieldBorder.getInsets();
    myDx = b.left + b.right;
    myDy = b.top + b.bottom;
    myMinWidth = 20;
    controller.invalidateParentOnChange();
    controller.update(new BaseTextController.Updater() {
      String myLastTextPresentation;
      public void onNewData(JTextComponent component, ModelKey modelKey, String textPresentation) {
        textPresentation = textPresentation.trim();
        myVisible = !textPresentation.isEmpty();
        setHighlightPattern(myPattern);
        if (!Util.equals(myLastTextPresentation, textPresentation)) {
          fireFormletChanged();
        }
        myLastTextPresentation = textPresentation;
      }
    });
  }

  public ATextArea getField() {
    return myField;
  }

  public void setHighlightPattern(Pattern pattern) {
    myPattern = pattern;
    Highlightable.HighlightUtil.changeHighlighterPattern(myHighlighter, UIUtil.getDocumentText(myField), pattern);
  }

  public static LargeTextFormlet withString(final ModelKey<String> modelKey, Configuration config) {
    ATextArea component = new ATextArea();
    BaseTextController<String> controller = TextController.installTextViewer(component, modelKey, false);
    return new LargeTextFormlet(component, controller, config);
  }

  public static <T> LargeTextFormlet headerWithInt(final ModelKey<T> modelKey, Convertor<T, String> convertorToStr,
    Convertor< String, T> convertorFromStr)
  {
    ATextArea component = new ATextArea();
    TextController<T> controller = new TextController<T>(modelKey, convertorToStr, convertorFromStr, null, false);
    UIController.CONTROLLER.putClientValue(component, controller);
    return new LargeTextFormlet(component, controller, null);
  }

  public int getPreferredWidth() {
    return myMinWidth;
  }

  public int getPreferredHeight(int width) {
    return UIUtil.getTextComponentPreferredHeight(myField, width - myDx) + myDy;
  }

  @NotNull
  public JComponent getComponent() {
    return fieldBorder;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return this;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public String getCaption() {
    return isCollapsed() ? myField.getText() : null;
  }

  public boolean isVisible() {
    return myVisible;
  }

  public void adjustFont(float factor, int style, boolean antialias) {
    UIUtil.adjustFont(myField, factor, style, antialias);
  }

  public void trackViewportDimensions() {
    myField.trackViewportDimensions();
  }

  public void setLineWrap(boolean wrap) {
    myField.setLineWrap(wrap);
  }
}
