package com.almworks.util.components.layout;

import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author dyoma
 */
public class CollapsablePanel extends JComponent implements WidthDrivenComponent {
  private static final int BUTTON_GAP = 5;
  private final WidthDrivenComponent myFull;
  private final WidthDrivenComponent myBrief;
  private final String myToBriefActionName;
  private final String myToFullActionName;
  private final JButton myCollapseButton = new JButton();
  private final Configuration myConfiguration;
  private boolean myShowFull;
  private final ClientArea myArea = new ClientArea(this);

  public CollapsablePanel(WidthDrivenComponent full, WidthDrivenComponent brief, String toBriefAction,
    String toFullAction, Configuration config, String configParam)
  {
    myFull = full;
    myBrief = brief;
    myToBriefActionName = toBriefAction;
    myToFullActionName = toFullAction;
    myConfiguration = config;
    myShowFull = myConfiguration.getBooleanSetting(configParam, true);
    myCollapseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateShowFields(!myShowFull);
      }
    });
    updateShowFields(myShowFull);
    myCollapseButton.setMargin(new Insets(2, 4, 2, 4));
    myCollapseButton.setIconTextGap(2);
    myCollapseButton.setOpaque(false);
    add(myCollapseButton);
    add(myBrief.getComponent());
    add(myFull.getComponent());
  }

  private void updateShowFields(boolean full) {
    myShowFull = full;
    myConfiguration.setSetting("showFields", myShowFull);
    myFull.getComponent().setVisible(myShowFull);
    myBrief.getComponent().setVisible(!myShowFull);

    myCollapseButton.setIcon(myShowFull ? Icons.COLLAPSE_UP : Icons.EXPAND_DOWN);
    myCollapseButton.setToolTipText(getCollapseText());
    myCollapseButton.setText("");
  }

  private String getCollapseText() {
    return myShowFull ? myToBriefActionName : myToFullActionName;
  }

  public JComponent getButton() {
    return myCollapseButton;
  }

  public int getPreferredWidth() {
    Dimension button = myCollapseButton.getPreferredSize();
    int width = Math.max(WidthDrivenColumn.getVisiblePrefferredWidth(myBrief),
      WidthDrivenColumn.getVisiblePrefferredWidth(myFull));
    return myArea.widthToWhole(button.width + BUTTON_GAP + width);
  }

  public int getPreferredHeight(int width) {
    int panelWidth = width - myCollapseButton.getPreferredSize().width - BUTTON_GAP;
    panelWidth = Math.max(0, panelWidth);
    int height =
      WidthDrivenColumn.getVisibleHeight(myBrief, panelWidth) + WidthDrivenColumn.getVisibleHeight(myFull, panelWidth);
    return myArea.heightToWhole(Math.max(myCollapseButton.getPreferredSize().height, height));
  }


  public Dimension getPreferredSize() {
    int width = getPreferredWidth();
    int height = getPreferredHeight(width);
    return new Dimension(width, height);
  }

  @NotNull
  public JComponent getComponent() {
    return this;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public void layout() {
    Dimension buttonSize = myArea.placeLeftTopChild(myCollapseButton);
    int height = myArea.getHeight();
    int x = buttonSize.width + BUTTON_GAP;
    myArea.fillUptoRightVCentering(myBrief, x, 0, height);
    myArea.fillUptoRightVCentering(myFull, x, 0, height);
  }
}
