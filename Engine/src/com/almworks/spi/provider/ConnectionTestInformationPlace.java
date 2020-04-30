package com.almworks.spi.provider;

import com.almworks.util.components.ALabelWithExplanation;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.swing.AwtUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ConnectionTestInformationPlace {
  private final JPanel myMessagesPlace;
  private final JScrollPane myScrollPane;

  public ConnectionTestInformationPlace() {
    myMessagesPlace = new JPanel(new FormLayout("left:max(16px;min),4dlu,fill:pref"));
    final JPanel marginPanel = new JPanel(new BorderLayout());
    marginPanel.setBorder(new EmptyBorder(0, 5, 0, 5));
    marginPanel.add(myMessagesPlace, BorderLayout.CENTER);
    myScrollPane = new JScrollPane(marginPanel);
  }

  public JComponent getComponent() {
    return myScrollPane;
  }

  public void clearMessages() {
    Threads.assertAWTThread();
    myMessagesPlace.removeAll();
    myMessagesPlace.setBorder(AwtUtil.EMPTY_BORDER);
    final FormLayout layout = ((FormLayout)myMessagesPlace.getLayout());
    for(int row = layout.getRowCount(); row > 0; row--) {
      layout.removeRow(row);
    }
  }

  public MessageHandle addMessage(@Nullable Icon icon, String shortMessage, @Nullable String longMessage) {
    Threads.assertAWTThread();
    final int row = addNewRowAndGetItsIndex();
    final MessageHandle mh =
      new MessageHandle(addIconComponent(icon, row), addMessageComponent(shortMessage, longMessage, row));
    revalidateScrollPane();
    return mh;
  }

  private int addNewRowAndGetItsIndex() {
    final FormLayout layout = (FormLayout) myMessagesPlace.getLayout();
    layout.appendRow(new RowSpec("3dlu"));
    layout.appendRow(new RowSpec("fill:pref"));
    return layout.getRowCount();
  }

  private JLabel addIconComponent(@Nullable Icon icon, int row) {
    final JLabel label = new JLabel();
    if(icon != null) {
      label.setIcon(icon);
    }
    myMessagesPlace.add(label, new CellConstraints(1, row));
    return label;
  }

  private ALabelWithExplanation addMessageComponent(String shortMessage, String longMessage, int row) {
    final ALabelWithExplanation label = createMessageComponent(shortMessage, longMessage);
    myMessagesPlace.add(label, new CellConstraints(3, row));
    return label;
  }

  private ALabelWithExplanation createMessageComponent(String shortMessage, String longMessage) {
    final ALabelWithExplanation label = new ALabelWithExplanation();
    label.setTextAndExplanation(shortMessage, longMessage);
    return label;
  }

  private void revalidateScrollPane() {
    myScrollPane.revalidate();
  }

  public static class MessageHandle {
    private final JLabel myIcon;
    private final ALabelWithExplanation myLabel;

    private MessageHandle(JLabel icon, ALabelWithExplanation label) {
      myIcon = icon;
      myLabel = label;
    }

    public void setIcon(@Nullable Icon icon) {
      myIcon.setIcon(icon);
    }

    public void setMessage(String message) {
      myLabel.setText(message);
    }

    public void setExplanation(String explanation) {
      myLabel.setExplanation(explanation);
    }
  }
}
