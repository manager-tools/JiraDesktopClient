package com.almworks.screenshot.editor.tools.annotate;

import com.almworks.util.ErrorHunt;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TextEditorDialog {

  private final JTextPane myTextArea = new JTextPane();

  private String myNewText = "";

  private JScrollPane myScrollPane;
  private JDialog myDialog;
  private JOptionPane myPane;
  private final AbstractAction myOkAction = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      closeAndSave();
    }
  };
  private final AbstractAction myCancelAction = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      closeAndCancel();
    }
  };

  private void initTextArea() {
    myScrollPane =
      new JScrollPane(myTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    myScrollPane.setPreferredSize(UIUtil.getRelativeDimension(myScrollPane, 40, 4));
    myTextArea.setOpaque(true);
  }

  public TextEditorDialog(Window parent) {
    initTextArea();
    myPane = new JOptionPane(myScrollPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    InputMap inputMap = myPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = myPane.getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "ok");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    actionMap.put("ok", myOkAction);
    actionMap.put("cancel", myCancelAction);
    myDialog = myPane.createDialog(parent, "Comment");

    myDialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        closeAndCancel();
      }
    });

    myDialog.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent ce) {
        UIUtil.requestFocusInWindowLater(myTextArea);
      }
    });
  }

  public String getNewText() {
    return myNewText;
  }

  public void closeAndSave() {
    // Save font & color information
    myNewText = myTextArea.getText();
    // Close the window
  }

  public void closeAndCancel() {
    myNewText = null;
    myDialog.setVisible(false);
  }

  public void setText(String text, Dimension areaSize) {
    myTextArea.setPreferredSize(areaSize);
    ErrorHunt.setEditorPaneText(myTextArea, text);
  }

  public void show(Point location) {
    myDialog.setLocation(location);
    myDialog.setVisible(true);
    Object o = myPane.getValue();
    if (Integer.valueOf(JOptionPane.OK_OPTION).equals(o)) {
      closeAndSave();
    } else {
      closeAndCancel();
    }
  }
}

