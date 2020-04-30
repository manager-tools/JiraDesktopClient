package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.util.components.CommunalFocusListener;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.ScrollPaneBorder;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.regex.Pattern;

public class LinkTextFormlet extends AbstractFormlet implements WidthDrivenComponent, Highlightable {
  private static final Insets MARGIN_INSETS = Aqua.isAqua() ? new Insets(0, 6, 0, 0) : new Insets(0, 5, 0, 0);

  private final Highlighter myHighlighter = new DefaultHighlighter();
  private final JEditorPane myComponent;
  private final ScrollPaneBorder fieldBorder;
  private final int myDx;
  private final int myDy;
  private final int myMinWidth = 20;

  private boolean myVisible;
  private Pattern myPattern;

  public LinkTextFormlet(JEditorPane component, BaseTextController<?> controller, Configuration config) {
    super(config);
    myComponent = component;
    myComponent.setHighlighter(myHighlighter);
    fieldBorder = new ScrollPaneBorder(myComponent);
    CommunalFocusListener.setupJEditorPane(myComponent);
    myComponent.setMargin(MARGIN_INSETS);
    Insets b = fieldBorder.getInsets();
    myDx = b.left + b.right;
    myDy = b.top + b.bottom;
    controller.invalidateParentOnChange();
    controller.update(new BaseTextController.Updater() {
      public void onNewData(JTextComponent component, ModelKey modelKey, String textPresentation) {
        myVisible = textPresentation.trim().length() > 0;
        setHighlightPattern(myPattern);
        fireFormletChanged();
      }
    });
  }

  public boolean isVisible() {
    return myVisible;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return this;
  }

  public int getPreferredWidth() {
    return myMinWidth;
  }

  public int getPreferredHeight(int width) {
    return UIUtil.getTextComponentPreferredHeight(myComponent, width - myDx) + myDy;
  }

  @NotNull
  public JComponent getComponent() {
    return fieldBorder;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public void setHighlightPattern(Pattern pattern) {
    myPattern = pattern;
    Highlightable.HighlightUtil.changeHighlighterPattern(myHighlighter, UIUtil.getDocumentText(myComponent), pattern);
  }

  public String getCaption() {
    return isCollapsed() ? myComponent.getText() : null;
  }
}
