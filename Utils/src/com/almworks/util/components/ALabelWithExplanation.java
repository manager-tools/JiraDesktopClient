package com.almworks.util.components;

import javax.swing.*;

/**
 * A component consisting of a label showing some message and a
 * "question mark" icon next to it that opens some explanation
 * in a pop-up.
 */
public class ALabelWithExplanation extends AComponentWithExplanation<JLabel> {
  public ALabelWithExplanation() {
    super(new JLabel());
  }

  public void setText(String text) {
    getMain().setText(text);
  }
}
