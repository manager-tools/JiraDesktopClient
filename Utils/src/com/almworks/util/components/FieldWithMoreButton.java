package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author : Dyoma
 */
public class FieldWithMoreButton <C extends JComponent> extends JPanel {
  private static final String PRESS_MORE_BUTTON = "pressMoreButton";
  private static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
//  private static final KeyStroke KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_MASK, false);
  private static final int GAP = 0;
  private static final int ADDITIONAL_FIELD_WIDTH = 5;

  private final SimpleAction myPressButtonAction = new SimpleAction("\u2026") {
    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      if (myActionName != null) {
        context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, myActionName.trim() + (getKeystrokeEnabled() ? " (" + UIUtil.getStringPresentation(KEYSTROKE) + ")" : ""));
      }
      if (myIcon != null) {
        context.putPresentationProperty(PresentationKey.SMALL_ICON, myIcon);
        context.putPresentationProperty(PresentationKey.NAME, "");
      }
      if (myAction instanceof AnAction) {
        ((AnAction) myAction).update(context);
      }
      if (!context.isDisabled() && !isEnabled()) {
        UpdateRequest request = context.getUpdateRequest();
        final ChangeListener listener = request.getChangeListener();
        UIUtil.addSwingPropertyChangeListener(request.getLifespan(), FieldWithMoreButton.this, "enabled", new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            listener.onChange();
          }
        });
        context.setEnabled(false);
      }
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      CantPerformException.ensureNotNull(myAction).perform(context);
    }
  };

  private AActionButton myButton;

  private final MouseListener myDblClickListener = new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
      if (!myDoubleClickEnabled)
        return;
      if (!isEnabled())
        return;
      if (e.getClickCount() != 2)
        return;
      if (e.getButton() != MouseEvent.BUTTON1)
        return;
      getActionMap().get(PRESS_MORE_BUTTON).actionPerformed(
        new ActionEvent(getField(), ActionEvent.ACTION_PERFORMED, ""));
    }
  };
  private AnActionListener myAction;
  private String myActionName;
  private Icon myIcon;
  private C myField;
  private boolean myDoubleClickEnabled = true;

  public FieldWithMoreButton() {
    setButton(new AActionButton());
    setKeystrokeEnabled(true);
    getActionMap().put(PRESS_MORE_BUTTON, ActionListenerBridge.action(myPressButtonAction));
    setActionName(null);
    setIcon(null);
  }

  public final void setButton(AActionButton button) {
    if (button == null) return;
    if (myButton != null) {
      ToolTipManager.sharedInstance().unregisterComponent(myButton);
      remove(myButton);
      myButton = null;
    }
    myButton = button;
    myButton.setAnAction(myPressButtonAction);
    myButton.setFocusable(false);
    Aqua.makeSquareButton(myButton);
    ToolTipManager.sharedInstance().registerComponent(myButton);
    add(myButton);
    invalidate();
    revalidate();
    repaint();
  }

  public boolean getKeystrokeEnabled() {
    return getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KEYSTROKE) != null;
  }

  public void setKeystrokeEnabled(boolean enable) {
    InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    if (enable) {
      inputMap.put(KEYSTROKE, PRESS_MORE_BUTTON);
    } else {
      inputMap.remove(KEYSTROKE);
    }
  }

  public void setField(C field) {
    assert myField == null || field == null;
    if (myField == field)
      return;
    if (myField != null) {
      myField.removeMouseListener(myDblClickListener);
      remove(myField);
    }
    myField = field;
    if (myField != null) {
      add(myField);
      myField.setEnabled(isEnabled());
      myField.addMouseListener(myDblClickListener);
    }
    revalidate();
    repaint();
  }

  public C getField() {
    return myField;
  }
  
  public void setEnabled(boolean enabled) {
    myButton.setEnabled(enabled);
    if (myField != null)
      myField.setEnabled(enabled);
    super.setEnabled(enabled);
  }

  public void setAction(AnActionListener action) {
    myAction = action;
  }

  public String getActionName() {
    return myActionName;
  }

  public void setActionName(String actionName) {
    if (actionName == null)
      actionName = "More\u2026";
    myActionName = actionName;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }

  public String getFieldClassName() {
    return myField != null ? myField.getClass().getName() : "";
  }

  public void setFieldClassName(String className) {
    Class<C> aClass;
    try {
      //noinspection unchecked
      aClass = (Class<C>) getClass().getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Class not found: " + className, e);
    }
    if (!JComponent.class.isAssignableFrom(aClass))
      throw new RuntimeException("Expected subclass of " + JComponent.class.getName() + " but was " + className);
    try {
      setField(aClass.newInstance());
    } catch (Exception e) {
      throw new RuntimeException("Can't instantiate " + className + ". Reason: " + e.getLocalizedMessage(), e);
    }
  }

  public void doLayout() {
    int fixButtonHeight = getFixButtonHeight();
    Dimension size = getSize();
    Insets insets = getInsets();
    int x = insets.left;
    int y = insets.top;
    size.width -= insets.left + insets.right;
    size.height -= insets.top + insets.bottom;
    int fieldSpace = size.width - size.height;
    if (size.width <= 0 || size.height <= 0)
      myButton.setBounds(0, 0, 0, 0);
    else
      //noinspection SuspiciousNameCombination
      myButton.setBounds(x + fieldSpace, y, size.height, size.height - fixButtonHeight);
    if (myField == null)
      return;
    int fieldWidth = fieldSpace - GAP;
    if (fieldWidth <= 0) {
      myField.setBounds(0, 0, 0, 0);
      return;
    }
    myField.setBounds(x, y, fieldWidth, size.height);
  }

  private int getFixButtonHeight() {
    if (!myButton.getUI().getClass().getName().endsWith("MetalButtonUI")) return 0;
    JTextComponent textField = Util.castNullable(JTextComponent.class, getField());
    if (textField == null) return 0;
    if (!textField.getUI().getClass().getName().endsWith("MetalTextFieldUI")) return 0;
    return 1;
  }

  public Dimension getMinimumSize() {
    return calcSize(InlineLayout.MIN_SIZE);
  }

  private Dimension calcSize(Convertor<Component, Dimension> sizeType) {
    if (myField == null) {
      Dimension size = sizeType.convert(myButton);
      int buttonDimension = size.height;
      return new Dimension(buttonDimension, buttonDimension);
    }
    Dimension fieldSize = sizeType.convert(myField);
    return new Dimension(fieldSize.width + GAP + fieldSize.height + ADDITIONAL_FIELD_WIDTH, fieldSize.height);

  }

  public Dimension getPreferredSize() {
    return calcSize(InlineLayout.PREF_SIZE);
  }

  public Dimension getMaximumSize() {
    return calcSize(InlineLayout.MAX_SIZE);
  }

  public void setDoubleClickEnabled(boolean doubleClickEnabled) {
    myDoubleClickEnabled = doubleClickEnabled;
  }

  public boolean isDoubleClickEnabled() {
    return myDoubleClickEnabled;
  }
}
