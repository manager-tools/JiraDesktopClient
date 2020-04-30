package com.almworks.items.gui.edit.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

public class VerticalLinePlacement {
  /**
   * Editor component may override default anchor via this property. Should be provided for top field editor component
   */
  public static final ComponentProperty<Integer> ANCHOR = ComponentProperty.createProperty("GridBag.anchor");

  private final JPanel myPanel = new JPanel(new GridBagLayout());
  private final boolean myAllowDisable;
  private final JScrollPane myScrollpane = new JScrollPane(ScrollablePanel.create(myPanel));
  private final GridBagConstraints myConstraints = new GridBagConstraints(
    0, 0, 1, 1, 0d, 0d,
    GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
    AwtUtil.EMPTY_INSETS, 0, 0);
  private int myRow = 0;
  private boolean myStretchesY = false;

  public VerticalLinePlacement(boolean allowDisable) {
    myAllowDisable = allowDisable;
  }

  /**
   * Creates top editor component to a return value of {@link com.almworks.items.gui.edit.helper.EditFeature#editModel(org.almworks.util.detach.Lifespan, com.almworks.items.gui.edit.EditItemModel, com.almworks.util.config.Configuration)}
   */
  public static JComponent buildTopComponent(Lifespan life, EditItemModel model, FieldEditor... editors) {
    return buildTopComponent(life, model, Arrays.asList(editors));
  }

  /**
   * Creates top editor component to a return value of {@link com.almworks.items.gui.edit.helper.EditFeature#editModel(org.almworks.util.detach.Lifespan, com.almworks.items.gui.edit.EditItemModel, com.almworks.util.config.Configuration)}
   */
  public static JComponent buildTopComponent(Lifespan life, EditItemModel model, List<? extends FieldEditor> editors) {
    List<ComponentControl> components = Collections15.arrayList();
    FieldEditorUtil.createComponents(life, model, editors, components, null);
    return buildComponent(life, model, components);
  }

  public static JComponent buildComponent(Lifespan life, EditItemModel model, List<ComponentControl> components) {
    VerticalLinePlacement builder = new VerticalLinePlacement(model.getEditingItems().size() > 1);
    for (ComponentControl component : components) builder.addComponent(component, false);
    return builder.finishPanel(life);
  }


  public void addComponent(ComponentControl component, boolean mandatoryMark) {
    NameMnemonic fieldLabel = component.getLabel();
    addComponent(component, fieldLabel, mandatoryMark);
  }

  public void addComponent(ComponentControl component, NameMnemonic fieldLabel, boolean mandatoryMark) {
    ComponentControl.Dimensions dimensions = component.getDimension();
    if (fieldLabel != null && !fieldLabel.getText().isEmpty()) {
      JComponent fieldTitle = createTitleComponent(component, fieldLabel, dimensions, !mandatoryMark);
      if (mandatoryMark) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel mark = new JLabel("<html><font color='red'>*</font>:");
        mark.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, Boolean.FALSE);
        mark.setHorizontalAlignment(SwingConstants.LEADING);
        if (Env.isMac()) { // Align text to right
          panel.add(fieldTitle, BorderLayout.CENTER);
          panel.add(mark, BorderLayout.EAST);
        } else {
          panel.add(fieldTitle, BorderLayout.WEST);
          panel.add(mark, BorderLayout.CENTER);
        }
        fieldTitle = panel;
      }
      prepareTitleConstraints(myConstraints, dimensions);
      myPanel.add(fieldTitle, myConstraints);
    }

    JComponent fieldComponent = component.getComponent();
    prepareFieldConstraints(myConstraints, dimensions, fieldComponent);
    myStretchesY |= !dimensions.isFixedHeight();
    myPanel.add(fieldComponent, myConstraints);
    myRow++;
  }

  public JComponent finishPanel(Lifespan life) {
    if (!myStretchesY) {
      final GridBagConstraints gbc =
        new GridBagConstraints(0, myRow, 2, 1, 0d, 1d, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL,
          AwtUtil.EMPTY_INSETS, 0, 0);
      myPanel.add(Box.createVerticalStrut(0), gbc);
    }
    FieldEditorUtil.setupTopScrollPane(life, myScrollpane);
    myPanel.setBorder(UIUtil.EDITOR_PANEL_BORDER);
    myPanel.setAlignmentY(0F);
    UIUtil.setDefaultLabelAlignment(myPanel);
    return myScrollpane;
  }


  private void prepareTitleConstraints(GridBagConstraints gbc, ComponentControl.Dimensions dimensions) {
    gbc.gridx = 0;
    gbc.gridy = myRow;
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = EMPTY_INSETS;
    gbc.anchor = GridBagConstraints.NORTHWEST;

    if (isWide(dimensions)) {
      gbc.gridwidth = 2;
      gbc.insets = getComponentInsets(myRow == 0);
      myRow++;
    } else {
      gbc.gridwidth = 1;
      gbc.insets = getLabelInsets(myRow == 0);
      if (dimensions == ComponentControl.Dimensions.SINGLE_LINE) gbc.anchor = GridBagConstraints.CENTER;
    }
  }

  private JComponent createTitleComponent(ComponentControl component, NameMnemonic fieldName, ComponentControl.Dimensions dimensions, boolean trailingColon) {
    if (!myAllowDisable) return createLabelTitle(component.getComponent(), fieldName, dimensions, trailingColon);
    ComponentControl.Enabled enabled = component.getEnabled();
    switch (enabled) {
    case NOT_APPLICABLE:
      return createLabelTitle(component.getComponent(), fieldName, dimensions, trailingColon);
    case ALWAYS_ENABLED:
      return createPseudoCheckBoxTitle(component.getComponent(), fieldName, dimensions, trailingColon);
    case ENABLED:
    case DISABLED:
      return createCheckboxTitle(component, fieldName, enabled == ComponentControl.Enabled.ENABLED, dimensions, trailingColon);
    default:
      LogHelper.error("Unknown controlling value", enabled);
      return createLabelTitle(component.getComponent(), fieldName, dimensions, trailingColon);
    }
  }

  private JComponent createPseudoCheckBoxTitle(JComponent component, NameMnemonic fieldName, ComponentControl.Dimensions dimensions, boolean trailingColon) {
    final JLabel label = createLabelTitle(component, fieldName, dimensions, trailingColon);

    final JCheckBox check = new JCheckBox("");
    check.setSelected(true);
    check.setEnabled(false);

    return combine(check, label);
  }

  private JComponent createCheckboxTitle(final ComponentControl component, NameMnemonic fieldName, boolean initiallySelected, ComponentControl.Dimensions dimensions, boolean trailingColon) {
    final JLabel label = createLabelTitle(component.getComponent(), fieldName, dimensions, trailingColon);

    final JCheckBox check = new JCheckBox("");
    check.setSelected(initiallySelected);

    final class MyListener extends MouseAdapter implements ItemListener {
      @Override
      public void mouseClicked(MouseEvent e) {
        check.requestFocusInWindow();
        check.setSelected(!check.isSelected());
      }

      @Override
      public void itemStateChanged(ItemEvent e) {
        boolean enabled = check.isSelected();
        component.setEnabled(enabled);
      }
    }

    final MyListener myListener = new MyListener();
    label.addMouseListener(myListener);
    check.addItemListener(myListener);
    component.setEnabled(initiallySelected);
    return combine(check, label);
  }

  private JComponent combine(JComponent west, JComponent center) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(west, BorderLayout.WEST);
    panel.add(center, BorderLayout.CENTER);
    return panel;
  }

  private JLabel createLabelTitle(JComponent component, NameMnemonic fieldName, ComponentControl.Dimensions dimensions, boolean trailingColon) {
    JLabel c = new JLabel();
    if (fieldName != null) {
      String text = fieldName.getText().trim();
      if (trailingColon && !text.trim().endsWith(":")) text = text + ":";
      else if (!trailingColon && text.trim().endsWith(":")) text = text.substring(0, text.length() - 1);
      c.setText(text);
      int index = fieldName.getMnemonicIndex();
      if (index != -1) {
        c.setDisplayedMnemonicIndex(index);
        c.setDisplayedMnemonic(fieldName.getMnemonicChar());
      }
    }
    c.setLabelFor(component);
    if (isWide(dimensions)) {
      c.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, Boolean.FALSE);
      c.setHorizontalAlignment(SwingConstants.LEADING);
    }
    return c;
  }

  private boolean isWide(ComponentControl.Dimensions dimensions) {
    return !Env.isMac() && dimensions.isWide();
  }

  private void prepareFieldConstraints(GridBagConstraints gbc, ComponentControl.Dimensions dimensions, JComponent fieldComponent) {
    gbc.weightx = 1;
    gbc.insets = getComponentInsets(myRow == 0);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = myRow;
    Integer anchor = ANCHOR.getClientValue(fieldComponent);
    gbc.anchor = anchor != null ? anchor : GridBagConstraints.NORTHWEST;

    if (isWide(dimensions)) {
      gbc.gridx = 0;
      gbc.gridwidth = 2;
    } else {
      gbc.gridx = 1;
      gbc.gridwidth = 1;
    }
    gbc.weighty = dimensions.isFixedHeight() ? 0 : 1;
  }

  private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
  private static final Insets TOP_GAP = new Insets(5, 0, 0, 0);
  private Insets getComponentInsets(boolean firstLine) {
    return firstLine ? EMPTY_INSETS : TOP_GAP;
  }

  private static final Insets TOP_LABEL_INSETS = new Insets(2, 0, 0, 5);
  private static final Insets NOT_TOP_LABEL_INSETS = new Insets(7, 0, 0, 5);
  private Insets getLabelInsets(boolean firstLine) {
    return firstLine ? TOP_LABEL_INSETS : NOT_TOP_LABEL_INSETS;
  }

  public static abstract class EditImpl implements EditFeature {
    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
      List<? extends FieldEditor> editors = getEditors(model);
      if (editors == null) LogHelper.error("Missing editors", this);
      else for (FieldEditor editor : editors) editor.prepareModel(BranchSource.trunk(reader), model, editPrepare);
    }

    @Override
    public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
      List<? extends FieldEditor> editors = getEditors(model);
      if (editors == null) return null;
      return VerticalLinePlacement.buildTopComponent(life, model, editors);
    }

    @Nullable
    protected abstract List<? extends FieldEditor> getEditors(EditItemModel model);
  }
}
