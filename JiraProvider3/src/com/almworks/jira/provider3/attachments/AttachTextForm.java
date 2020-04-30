package com.almworks.jira.provider3.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.engine.gui.attachments.AttachmentUtils;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.speedsearch.TextSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.LineTokenizer;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.UpdateRequest;
import com.almworks.util.ui.actions.dnd.DndUtil;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;

class AttachTextForm implements UIComponentWrapper {
  public static final String ATTACHMENT_NAME_SETTING = "attachmentName";

  private JTextArea myText;
  private JPanel myWholePanel;
  private JTextField myAttachmentNameField;
  private AComboBox<Charset> myEncoding;
  private JLabel myContentLabel;
  private AComboBox<String> myMimeTypes;
  private final JPanel myTopPanel = new JPanel(new BorderLayout()) {
    private boolean myShown = false;

    @Override
    public void addNotify() {
      super.addNotify();
      if (myShown) return;
      myShown = true;
      onFormDisplayable();
    }

    @Override
    public void removeNotify() {
      super.removeNotify();
      myShown = false;
    }
  };

  private AttachTextForm() {
    myTopPanel.add(myWholePanel, BorderLayout.CENTER);
    TextSpeedSearch.installCtrlF(myText);
    myContentLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
  }

  public static AttachTextForm create(Lifespan life, final Configuration config, MiscConfig miscConfig) {
    final AttachTextForm form = new AttachTextForm();
    AttachmentUtils.configureEncodingCombo(life, form.myEncoding, config, miscConfig, null);
    AttachmentUtils.configureMimeTypesCombo(life, form.myMimeTypes, config, miscConfig);
    form.myAttachmentNameField.setText(config.getSetting(ATTACHMENT_NAME_SETTING, ""));
    UIUtil.addTextListener(life, form.myAttachmentNameField, new ChangeListener() {
      @Override
      public void onChange() {
        config.setSetting(ATTACHMENT_NAME_SETTING, form.myAttachmentNameField.getText());
      }
    });
//    new DocumentFormAugmentor().augmentForm(life, form.myTopPanel, false);
    UIUtil.setDefaultLabelAlignment(form.myTopPanel);
    return form;
  }

  private void onFormDisplayable() {
    pasteText();
    myAttachmentNameField.requestFocusInWindow();
    myAttachmentNameField.selectAll();
  }

  @Override
  public void dispose() {
  }

  public JComponent getComponent() {
    return myTopPanel;
  }

  public JTextField getFileNameField() {
    return myAttachmentNameField;
  }

  public AComboBox<String> getMimeTypes() {
    return myMimeTypes;
  }

  public String getText() {
    return LineTokenizer.replaceLineSeparators(myText.getText(), TextUtil.LINE_SEPARATOR);
  }

  public boolean isTextNotEmpty() {
    return !myText.getText().isEmpty();
  }

  public Charset getCharset() {
    return RecentController.unwrap(myEncoding.getModel().getSelectedItem());
  }

  public void subscribeUpdates(UpdateRequest request) {
    request.updateOnChange(myEncoding.getModel());
    request.updateOnChange(myText.getDocument());
  }

  public void pasteText() {
    String data = DndUtil.getClipboardTextSafe(false);
    if (data != null) setText(data);
  }

  private void setText(String data) {
    myText.setText(LineTokenizer.replaceLineSeparators(data, "\n"));
  }
}
