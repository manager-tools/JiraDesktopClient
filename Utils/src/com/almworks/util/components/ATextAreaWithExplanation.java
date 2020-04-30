package com.almworks.util.components;

import javax.swing.*;

public class ATextAreaWithExplanation extends AComponentWithExplanation<JTextArea> {
  public ATextAreaWithExplanation() {
    super(new JTextArea());
  }

  @Override
  public void setText(String text) {
    getMain().setText(text);
  }
}
