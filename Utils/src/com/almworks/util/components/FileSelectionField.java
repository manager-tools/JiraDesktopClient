package com.almworks.util.components;

import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class FileSelectionField extends BaseFileSelectionField<JTextField> {
  public FileSelectionField() {
    super(new JTextField());
    getField().addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
      }

      @Override
      public void focusLost(FocusEvent e) {
        fireChanged();
      }
    });
  }

  @NotNull
  public String getFilename() {
    return getField().getText().trim();
  }

  protected void setFilename(@Nullable String filename) {
    getField().setText(Util.NN(filename));
  }
}
