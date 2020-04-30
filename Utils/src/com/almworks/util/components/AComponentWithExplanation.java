package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A wrapper for some component showing some message and a
 * "question mark" icon next to it that opens some explanation
 * in a pop-up.
 */
public abstract class AComponentWithExplanation<C extends JComponent> extends JComponent {
  private final C myMain;
  private final Link myLink = new Link();

  @Nullable
  private String myExplanation;
  private UIUtil.Positioner myPositioner = UIUtil.NOTICE_POPUP;

  protected AComponentWithExplanation(C main) {
    myMain = main;
    setLayout(InlineLayout.horizontal(5));

    add(myMain);

    myLink.setIcon(Icons.QUESTION);
    myLink.setVisible(false);
    add(myLink);

    myLink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(myExplanation != null) {
          UIUtil.showNoticePopup(myExplanation, AComponentWithExplanation.this, null, myPositioner);
        }
      }
    });
  }

  public C getMain() {
    return myMain;
  }

  public Link getLink() {
    return myLink;
  }

  public abstract void setText(String text);

  public void setExplanation(@Nullable String explanation) {
    myLink.setVisible(explanation != null);
    myExplanation = explanation;
  }

  public void setTextAndExplanation(@Nullable String text, @Nullable String expl) {
    setText(text);
    setExplanation(expl);
  }

  public void setTextAndExplanation(@Nullable Pair<String, String> pair) {
    if(pair == null) {
      setTextAndExplanation(null, null);
    } else {
      setTextAndExplanation(pair.getFirst(), pair.getSecond());
    }
  }

  public void setTextForeground(Color color) {
    myMain.setForeground(color);
  }

  public void setPositioner(@NotNull UIUtil.Positioner positioner) {
    myPositioner = positioner;
  }

  public void setEnabled(boolean enabled) {
    myMain.setEnabled(enabled);
    myLink.setEnabled(enabled);
  }
}
