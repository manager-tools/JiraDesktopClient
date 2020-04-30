package com.almworks.util.config;

import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ConfigAttach {
  public static void attachTextField(@NotNull Lifespan span, @NotNull final JTextComponent component,
    @NotNull final Configuration configuration, @NotNull final String settingName, @NotNull String defaultValue)
  {
    component.setText(configuration.getSetting(settingName, defaultValue));
    DocumentUtil.addListener(span, component.getDocument(), new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        configuration.setSetting(settingName, component.getText());
      }
    });
    configuration.setSetting(settingName, component.getText());
  }

  public static void attachDocument(@NotNull Lifespan span, @NotNull final Document document,
    @NotNull final Configuration configuration, @NotNull final String settingName, @NotNull String defaultValue)
  {
    DocumentUtil.setDocumentText(document, Util.NN(configuration.getSetting(settingName, defaultValue)));
    DocumentUtil.addListener(span, document, new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        configuration.setSetting(settingName, DocumentUtil.getDocumentText(document));
      }
    });
    configuration.setSetting(settingName, DocumentUtil.getDocumentText(document));
  }

  public static void attachTwoRadioButtons(@NotNull Lifespan span, @NotNull JRadioButton trueButton,
    @NotNull JRadioButton falseButton, @NotNull Configuration config, @NotNull String settingName, boolean defaultValue)
  {
    attachButton(span, trueButton, new ButtonTransfer(config, settingName, defaultValue));
    attachButton(span, falseButton, new InverseButtonTransfer(config, settingName, defaultValue));
  }

  public static void attachCheckbox(@NotNull Lifespan span, @NotNull JCheckBox checkBox, @NotNull Configuration config,
    @NotNull final String settingName, boolean defaultValue)
  {
    attachButton(span, checkBox, new ButtonTransfer(config, settingName, defaultValue));
  }

  public static void attachCheckboxInverse(@NotNull Lifespan span, @NotNull JCheckBox checkBox,
    @NotNull Configuration config, @NotNull final String settingName, boolean defaultValue)
  {
    attachButton(span, checkBox, new InverseButtonTransfer(config, settingName, defaultValue));
  }

  private static void attachButton(@NotNull Lifespan span, @NotNull AbstractButton button,
    @NotNull final Transfer<ButtonModel> transfer)
  {
    final ButtonModel model = button.getModel();
    transfer.toModel(model);
    final ItemListener listener = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getID() != ItemEvent.ITEM_STATE_CHANGED)
          return;
        transfer.fromModel(model);
      }
    };
    model.addItemListener(listener);
    span.add(new Detach() {
      protected void doDetach() {
        model.removeItemListener(listener);
      }
    });
    transfer.fromModel(model);
  }

  private static interface Transfer<M> {
    void toModel(M model);

    void fromModel(M model);
  }


  private static class ButtonTransfer implements Transfer<ButtonModel> {
    private final String mySetting;
    private final Configuration myConfiguration;
    private final boolean myDefaultValue;

    public ButtonTransfer(Configuration configuration, String setting, boolean defaultValue) {
      myConfiguration = configuration;
      mySetting = setting;
      myDefaultValue = defaultValue;
    }

    public void toModel(ButtonModel buttonModel) {
      buttonModel.setSelected(getConfigurationValue());
    }

    protected boolean getConfigurationValue() {
      String setting = myConfiguration.getSetting(mySetting, null);
      return setting == null ? myDefaultValue : Boolean.valueOf(setting);
    }

    public void fromModel(ButtonModel buttonModel) {
      setConfigurationValue(buttonModel.isSelected());
    }

    protected void setConfigurationValue(boolean value) {
      myConfiguration.setSetting(mySetting, String.valueOf(value));
    }
  }


  private static class InverseButtonTransfer extends ButtonTransfer {
    public InverseButtonTransfer(Configuration configuration, String setting, boolean defaultValue) {
      super(configuration, setting, defaultValue);
    }

    protected boolean getConfigurationValue() {
      return !super.getConfigurationValue();
    }

    protected void setConfigurationValue(boolean value) {
      super.setConfigurationValue(!value);
    }
  }
}
