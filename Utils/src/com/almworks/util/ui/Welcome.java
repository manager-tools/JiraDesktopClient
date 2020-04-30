package com.almworks.util.ui;

import com.almworks.util.components.ALabel;
import com.almworks.util.components.Link;
import com.almworks.util.components.SizeConstraints;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Welcome implements UIComponentWrapper2 {
  private final JPanel myWholePanel;
  private JComponent myWelcome;
  private static final String CONTENT = "content";
  private static final String WELCOME = "welcome";
  private ALabel myWelcomeTextLabel;
  private Box myActionsPanel;
  private Dimension myMessageBoxSize;
  private String myLinkTextActionKey;
  private final String myContentCard;
  private final int myColumns;
  private final int myRows;

  public Welcome(JComponent contentComponent) {
    this(contentComponent, null, null, 50, 20);
  }

  public Welcome(JComponent contentComponent, int columns, int rows) {
    this(contentComponent, null, null, columns, rows);
  }

  public Welcome(JComponent contentComponent, String htmlText, Action[] actions, int columns, int rows) {
    myColumns = columns;
    myRows = rows;
    myWholePanel = new JPanel(new CardLayout());
    createWelcomeComponent();
    if (contentComponent != null) {
      myContentCard = CONTENT;
      myWholePanel.add(contentComponent, CONTENT);
    } else {
      myContentCard = null;
    }
    myWholePanel.add(myWelcome, WELCOME);
    if (htmlText != null)
      setWelcomeText(htmlText);
    if (actions != null)
      setActions(actions);
  }

  public void setActions(Action[] actions) {
    clearActions();
    for (Action action : actions) {
      Link link = createActionLink();
      link.setAction(action);
    }
  }

  public void updateActions() {
    for (int i = 0; i < myActionsPanel.getComponentCount(); i++) {
      Component c = myActionsPanel.getComponent(i);
      if (c instanceof Link)
        ((Link) c).updateNow();
    }
  }

  private Link createActionLink() {
    Link link = new Link();
    myActionsPanel.add(link);
    link.setTextActionKey(myLinkTextActionKey);
    link.setSizeDelegate(SizeConstraints.boundedPreferredSize(myMessageBoxSize));
    link.setMinimumSize(new Dimension(0, 40));
    link.setAlignmentX(0);
    link.setBorder(new EmptyBorder(11, 5, 0, 5));
    UIUtil.adjustFont(link, 1.15F, Font.BOLD, false);
    return link;
  }

  private void clearActions() {
    myActionsPanel.removeAll();
  }

  public void setAnActions(AnAction[] actions) {
    clearActions();
    for (AnAction action : actions) {
      createActionLink().setAnAction(action);
    }
  }

  public void setLinkTextActionKey(String linkTextActionKey) {
    myLinkTextActionKey = linkTextActionKey;
  }

  public void setWelcomeText(String htmlText) {
    myWelcomeTextLabel.setText(htmlText);
    myWelcomeTextLabel.invalidate();
    myWelcome.invalidate();
    myWelcome.validate();
  }

  private void createWelcomeComponent() {
    myWelcomeTextLabel = new ALabel();
    myMessageBoxSize = UIUtil.getRelativeDimension(myWelcomeTextLabel, myColumns, myRows);

    UIUtil.adjustFont(myWelcomeTextLabel, 1.15F, -1, false);
    myWelcomeTextLabel.setAlignmentX(0);
    myWelcomeTextLabel.setMaximumSize(myMessageBoxSize);
    myWelcomeTextLabel.setSizeDelegate(SizeConstraints.boundedPreferredSize());

    myActionsPanel = new Box(BoxLayout.Y_AXIS);
    myActionsPanel.setAlignmentX(0);
    myActionsPanel.setMaximumSize(myMessageBoxSize);
    myActionsPanel.setMinimumSize(new Dimension(10, 10));
    myActionsPanel.setBorder(new EmptyBorder(21, 0, 0, 0));

    Box box = new Box(BoxLayout.Y_AXIS);
    box.add(myWelcomeTextLabel);
    box.add(myActionsPanel);
    box.setBorder(new EmptyBorder(29, 49, 0, 0));

    myWelcome = WelcomePanel.create(box);
  }

  public void setWelcomeVisible(boolean welcomeVisible) {
    String card = (welcomeVisible || myContentCard == null) ? WELCOME : myContentCard;
    ((CardLayout)myWholePanel.getLayout()).show(myWholePanel, card);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public JComponent getWelcomeComponent() {
    return myWelcome;
  }

  public void dispose() {
  }

  public Detach getDetach() {
    return Detach.NOTHING;
  }
}
