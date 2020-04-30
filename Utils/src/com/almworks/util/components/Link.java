package com.almworks.util.components;

import com.almworks.util.components.plaf.LinkUI;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.UserHintListener;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class Link extends AbstractButton implements Antialiasable, AActionComponent {
  @SuppressWarnings({"FieldNameHidesFieldInSuperclass"})
  private static final String uiClassID = "LinkUI";

  private final AnActionHolder myActionHolder = new AnActionHolder(this);

  private Color myPressedColor;
  private Color myHoverColor;
  private Boolean myUnderlined;

  private LinkUI.PaintingPolicy myDisabledLook = null;
  private SizeDelegate mySizeDelegate;
  private String myTextActionKey = Action.NAME;

  // todo delegate hint processing somewhere
  private UserHintListener myHintListener = null;
  private boolean myHintAvailable = false;
  private boolean myHintShowing = false;

  private boolean myAntiAliasing = false;

  public Link() {
    setModel(new DefaultButtonModel());
    addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        updateHint(true, false);
      }

      public void mouseExited(MouseEvent e) {
        updateHint(false, false);
      }
    });
    updateUI();
    PresentationMapping.clearMnemonic(this);
  }

  public void addNotify() {
    super.addNotify();
    myActionHolder.startUpdate();
  }

  public void removeNotify() {
    myActionHolder.stopUpdate();
    super.removeNotify();
  }

  public KeyStroke[] getRegisteredKeyStrokes() {
    return myActionHolder.getRegisteredKeyStrokes(super.getRegisteredKeyStrokes());
  }

  public int getConditionForKeyStroke(KeyStroke aKeyStroke) {
    return myActionHolder.getConditionForKeyStroke(aKeyStroke, super.getConditionForKeyStroke(aKeyStroke));
  }

  protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
    final PropertyChangeListener superListener = super.createActionPropertyChangeListener(a);
    return new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        superListener.propertyChange(evt);
        if (PresentationKey.ACTION_KEY_VISIBLE.equals(evt.getPropertyName()))
          AActionButton.updateVisibility(Link.this, getAction());
      }
    };
  }

  public LinkUI.PaintingPolicy getDisabledLook() {
    return myDisabledLook;
  }

  public String getHint() {
    return UserHintListener.TEXT_HINT.getClientValue(this);
  }

  public void setHint(String hint) {
    UserHintListener.TEXT_HINT.putClientValue(this, hint);
    updateHint(myHintShowing, true);
  }

  public UserHintListener getHintListener() {
    return myHintListener;
  }

  public void setHintListener(UserHintListener hintListener) {
    myHintListener = hintListener;
  }

  public LinkUI getLinkUI() {
    return (LinkUI) super.getUI();
  }

  public Dimension getMaximumSize() {
    return SizeDelegate.maximum(this, mySizeDelegate, super.getMaximumSize());
  }

  public Dimension getMinimumSize() {
    return SizeDelegate.minimum(this, mySizeDelegate, super.getMinimumSize());
  }

  public Dimension getPreferredSize() {
    return SizeDelegate.preferred(this, mySizeDelegate, super.getPreferredSize());
  }

  public Color getPressedColor() {
    return myPressedColor;
  }

  public String getUIClassID() {
    return uiClassID;
  }

  public Detach setAnAction(AnAction action) {
    return myActionHolder.setAnAction(action);
  }

  public JComponent toComponent() {
    return this;
  }

  public void setActionById(String actionId) {
    myActionHolder.setActionById(actionId);
  }

  public void updateNow() {
    myActionHolder.updateNow();
  }

  public void setContextComponent(JComponent component) {
    myActionHolder.setContextComponent(component);
  }

  public void setPresentationMapping(String swingKey, PresentationMapping mapping) {
    myActionHolder.setPresentationMapping(swingKey, mapping);
  }

  public void overridePresentation(Map mapping) {
    myActionHolder.overridePresentation(mapping);
  }

  public void setDisabledLook(LinkUI.PaintingPolicy disabledLook) {
    myDisabledLook = disabledLook;
    repaint();
  }

  public void setPressedColor(Color pressedColor) {
    Color oldColor = myPressedColor;
    myPressedColor = pressedColor;
    repaint();
    firePropertyChange("pressedColor", oldColor, pressedColor);
  }

  public Color getHoverColor() {
    return myHoverColor;
  }

  public void setHoverColor(Color hoverColor) {
    Color old = myHoverColor;
    myHoverColor = hoverColor;
    repaint();
    firePropertyChange("hoverColor", old, hoverColor);
  }

  public boolean isUnderlined() {
    return myUnderlined == null || myUnderlined;
  }

  public boolean isUnderlinedSet() {
    return myUnderlined != null;
  }

  public void setUnderlined(boolean underlined) {
    Boolean old = myUnderlined;
    myUnderlined = Boolean.valueOf(underlined);
    repaint();
    firePropertyChange("underlined", old, myUnderlined);
  }

  public void setSizeDelegate(SizeDelegate delegate) {
    mySizeDelegate = delegate;
    revalidate();
  }

  public void setTextActionKey(String textActionKey) {
    myTextActionKey = textActionKey;
  }

  public void updateUI() {
    LinkUI ui = (LinkUI) UIManager.getUI(this);
    if (ui == null)
      ui = (LinkUI) LinkUI.createUI(this);
    setUI(ui);
  }

  protected void configurePropertiesFromAction(Action a) {
    super.configurePropertiesFromAction(a);
    if (myTextActionKey != null && !Action.NAME.equals(myTextActionKey)) {
      setText(a != null ? (String) a.getValue(myTextActionKey) : null);
    }
  }

  @Nullable
  private UserHintListener locateHintListener() {
    if (myHintListener != null)
      return myHintListener;
    for (Container component = this; component != null;) {
      if (component instanceof JComponent) {
        UserHintListener listener = UserHintListener.USER_HINT_LISTENER.getClientValue((JComponent) component);
        if (listener != null)
          return listener;
      }
      component = component.getParent();
    }
    return null;
  }

  private void updateHint(boolean show, boolean force) {
    if (myHintShowing == show && !force)
      return;
    myHintShowing = show;

    if (myHintAvailable) {
      if (!show) {
        myHintAvailable = false;
        UserHintListener hintListener = locateHintListener();
        if (hintListener != null)
          hintListener.onHintsUnavailable(this);
      }
    } else {
      if (show) {
        String hint = getHint();
        if (hint != null) {
          UserHintListener hintListener = locateHintListener();
          if (hintListener != null) {
            hintListener.onTextHintAvailable(this, hint);
            myHintAvailable = true;
          }
        }
      }
    }
  }

  public void setAntialiased(boolean antialiased) {
    myAntiAliasing = antialiased;
    repaint();
  }

  public boolean isAntialiased() {
    return myAntiAliasing;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
  }
}
