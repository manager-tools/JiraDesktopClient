package com.almworks.util.text;

import com.almworks.util.Env;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author : Dyoma
 */
public class NameMnemonic {
  public static final NameMnemonic EMPTY = new NameMnemonic("", -1);

  private final int myIndex;
  private final String myText;

  private NameMnemonic(String text, int index) {
    myIndex = index;
    myText = text;
  }

  @NotNull
  public String getText() {
    return myText;
  }

  public int getMnemonicIndex() {
    return myIndex;
  }

  public int getMnemonicChar() {
    int index = getMnemonicIndex();
    if (index == -1)
      return 0;
    char c = getText().charAt(index);
    if (c >= 'a' && c <= 'z')
      c -= 'a' - 'A';
    return c;
  }

  public String getTextWithMnemonic() {
    if (myIndex < 0)
      return myText;
    return myText.substring(0, myIndex) + '&' + myText.substring(myIndex);
  }

  public void setToAction(Action action) {
    action.putValue(Action.NAME, getText());
    action.putValue(Action.MNEMONIC_KEY, getMnemonicCharInteger());
  }

  public Integer getMnemonicCharInteger() {
    int c = getMnemonicChar();
    return c != 0 ? c : -1;
  }

  @NotNull
  public static NameMnemonic parseString(@NotNull String textWithMnemonic) {
    StringBuilder visibleName = new StringBuilder();
    boolean ampFound = false;
    int actualIndex = 0;
    int mnemonicIndex = -1;
    for (int i = 0; i < textWithMnemonic.length(); i++, actualIndex++) {
      char c = textWithMnemonic.charAt(i);
      if (ampFound) {
        actualIndex--;
        if (c == '&')
          visibleName.append(c);
        else {
          if (mnemonicIndex != -1)
            Log.error(textWithMnemonic);
          mnemonicIndex = actualIndex;
          visibleName.append(c);
        }
        ampFound = false;
      } else {
        if (c != '&')
          visibleName.append(c);
        else
          ampFound = true;
      }
    }
    return new NameMnemonic(visibleName.toString(), Env.isMac() ? -1 : mnemonicIndex);
  }

  @NotNull
  public static NameMnemonic rawText(@NotNull String text) {
    return new NameMnemonic(text, -1);
  }

  public static NameMnemonic create(String text, int index) {
    if (index < 0 || index >= text.length()) index = -1;
    return new NameMnemonic(text, index);
  }

  public void setToButton(AbstractButton menuItem) {
    menuItem.setText(getText());
    if (getMnemonicIndex() == -1) {
      menuItem.setMnemonic(0);
      menuItem.setDisplayedMnemonicIndex(-1);
      return;
    }
    menuItem.setMnemonic(getMnemonicChar());
    menuItem.setDisplayedMnemonicIndex(getMnemonicIndex());
  }

  public void setToLabel(JLabel label) {
    label.setText(getText());
    int index = getMnemonicIndex();
    if (index != -1) {
      label.setDisplayedMnemonicIndex(index);
      label.setDisplayedMnemonic(getMnemonicChar());
    }
  }

  public void setToHtmlButton(AbstractButton button) {
    String text = getText();
    int index = getMnemonicIndex();
    if (index < 0 || index >= text.length()) {
      button.setText(text);
    } else {
      text = text.substring(0, index) + "<u>" + text.charAt(index) + "</u>" + text.substring(index + 1);
      button.setText(text);
      button.setMnemonic(getMnemonicChar());
    }
  }

  @Override
  public String toString() {
    return myText + "@" + myIndex;
  }
}
