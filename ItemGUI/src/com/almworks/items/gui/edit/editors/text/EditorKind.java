package com.almworks.items.gui.edit.editors.text;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.util.NestedComponent;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.spellcheck.SpellCheckManager;
import com.almworks.util.LogHelper;
import com.almworks.util.components.AScrollPane;
import com.almworks.util.components.SizeConstraints;
import com.almworks.util.components.SizeDelegate;
import com.almworks.util.components.speedsearch.TextSpeedSearch;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public abstract class EditorKind {
  public abstract ComponentControl createComponent(Lifespan life, FieldEditor editor, EditItemModel model,
    ComponentControl.Enabled componentEnabledState);

  public abstract ComponentControl setupComponent(Lifespan life, FieldEditor editor, EditItemModel model,
    JComponent component, ComponentControl.Enabled componentEnabledState);

  @Nullable
  public final JTextComponent getTextComponent(@Nullable ComponentControl control) {
    if (control == null) return null;
    JTextComponent textComponent = Util.castNullable(JTextComponent.class, control.getComponent());
    if (textComponent != null) return textComponent;
    NestedComponent nested = Util.castNullable(NestedComponent.class, control);
    return nested != null ? nested.getInner(JTextComponent.class) : null;
  }

  public static final EditorKind TEXT_FIELD = new EditorKind() {
    @Override
    public ComponentControl createComponent(Lifespan life, FieldEditor editor, EditItemModel model,
      ComponentControl.Enabled enabled) {
      JTextField field = new JTextField();
      field.setColumns(15);
      SpellCheckManager.attach(life, field);
      return SimpleComponentControl.singleLine(field, editor, model, enabled);
    }

    @Override
    public ComponentControl setupComponent(Lifespan life, FieldEditor editor, EditItemModel model, JComponent component,
      ComponentControl.Enabled enabled) {
      JTextField field = Util.castNullable(JTextField.class, component);
      if (field == null) {
        LogHelper.error("Wrong component", component);
        return null;
      }
      SpellCheckManager.attach(life, field);
      return SimpleComponentControl.singleLine(field, editor, model, enabled);
    }
  };

  public static final EditorKind TEXT_PANE = new TextPane(60);

  public static class TextPane extends EditorKind {
    private final SizeDelegate mySize;

    public TextPane(int prefHeight) {
      mySize = new SizeConstraints.PreferredSizeBoundedByConstant(new Dimension(100, prefHeight), new Dimension(100, prefHeight));
    }

    @Override
    public ComponentControl createComponent(Lifespan life, FieldEditor editor, EditItemModel model, ComponentControl.Enabled enabled) {
      JTextPane textPane = new JTextPane();
      TextSpeedSearch.installCtrlF(textPane);
      SpellCheckManager.attach(life, textPane);
      AScrollPane scrollPane = new AScrollPane(textPane);
      scrollPane.setSizeDelegate(mySize);
      scrollPane.setAdaptiveVerticalScroll(true);
      UIUtil.configureScrollpaneVerticalOnly(scrollPane);
      return new NestedComponent(scrollPane, textPane, ComponentControl.Dimensions.WIDE, editor, model, enabled);
    }

    @Override
    public ComponentControl setupComponent(Lifespan life, FieldEditor editor, EditItemModel model, JComponent component,
      ComponentControl.Enabled enabled) {
      JScrollPane scrollPane = Util.castNullable(JScrollPane.class, component);
      JTextComponent textComponent;
      if (scrollPane != null) textComponent = Util.castNullable(JTextComponent.class, scrollPane.getViewport().getView());
      else {
        textComponent = Util.castNullable(JTextComponent.class, component);
        if (textComponent instanceof JTextField) {
          SpellCheckManager.attach(life, textComponent);
          return SimpleComponentControl.singleLine(textComponent, editor, model, enabled);
        }
      }
      if (textComponent == null) {
        LogHelper.error("Wrong component", component, scrollPane, scrollPane != null ? scrollPane.getViewport().getView() : null);
        return null;
      }
      SpellCheckManager.attach(life, textComponent);
      if (scrollPane == null) return SimpleComponentControl.create(textComponent, ComponentControl.Dimensions.WIDE, editor, model,
        enabled);
      return new NestedComponent(scrollPane, textComponent, ComponentControl.Dimensions.WIDE, editor, model, enabled);
    }
  }
}
