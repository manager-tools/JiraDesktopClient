package com.almworks.timetrack.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.engine.gui.LargeTextFormlet;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.DesignPlaceHolder;
import com.almworks.util.ui.DocumentFormAugmentor;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;

public class ArtifactBoxViewer {
  private JPanel myWholePanel;
  private JLabel myIconsLabel;
  private DesignPlaceHolder myKeyPlace;
  private DesignPlaceHolder mySummaryPlace;

  public <V> ArtifactBoxViewer(
    Lifespan life, ModelKey<V> keyKey, Convertor<V, String> toStr, Convertor<String, V> fromStr, 
    ModelKey<String> summaryKey)
  {
    LargeTextFormlet key = LargeTextFormlet.headerWithInt(keyKey, toStr, fromStr);
    key.adjustFont(-1, Font.BOLD, true);
    key.setLineWrap(false);
    key.getField().setMargin(new Insets(0, 0, 0, 0));
    myKeyPlace.setComponent(key.getComponent());

    LargeTextFormlet summary = LargeTextFormlet.withString(summaryKey, null);
    summary.adjustFont(0.9F, -1, true);
    summary.getField().setMargin(new Insets(0, 0, 0, 0));
    summary.getField().setColumns(0);
    summary.getField().setRows(0);
    mySummaryPlace.setComponent(summary.getComponent());

    myIconsLabel.setText(null);

    new DocumentFormAugmentor().augmentForm(life, myWholePanel, true);
    myWholePanel.setOpaque(false);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }
}
