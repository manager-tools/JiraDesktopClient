package com.almworks.util.components;

import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.images.IconHandle;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationKey;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class AActionButton extends JButton implements AActionComponent, Antialiasable {
  private static final Map<String, Procedure2<AnActionHolder, Action>> ourUpdateProcedures;
  private final AnActionHolder myActionHolder = new AnActionHolder(this);
  private boolean myAntialiased;

  public AActionButton() {
  }

  public AActionButton(AnAction action) {
    setAnAction(action);
  }

  public void setAntialiased(boolean antialiased) {
    myAntialiased = antialiased;
    repaint();
  }

  public boolean isAntialiased() {
    return myAntialiased;
  }

  protected void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    super.paintComponent(g);
  }

  protected PropertyChangeListener createActionPropertyChangeListener(final Action a) {
    final PropertyChangeListener superListener = super.createActionPropertyChangeListener(a);
    return new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        superListener.propertyChange(evt);
        Procedure2<AnActionHolder, Action> procedure2 = ourUpdateProcedures.get(evt.getPropertyName());
        if (procedure2 != null)
          procedure2.invoke(myActionHolder, a);
      }
    };
  }

  protected void configurePropertiesFromAction(Action a) {
    super.configurePropertiesFromAction(a);
    for (Iterator<Procedure2<AnActionHolder, Action>> iterator = ourUpdateProcedures.values().iterator();
      iterator.hasNext();) {
      Procedure2<AnActionHolder, Action> procedure = iterator.next();
      procedure.invoke(myActionHolder, a);
    }
  }

  public Dimension getPreferredSize() {
    myActionHolder.ensureUpToDate();
    return super.getPreferredSize();
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

  public Detach setAnAction(AnAction action) {
    return myActionHolder.setAnAction(action);
  }

  public void setActionById(String actionId) {
    myActionHolder.setActionById(actionId);
  }

  public void setContextComponent(JComponent component) {
    myActionHolder.setContextComponent(component);
  }

  public JComponent toComponent() {
    return this;
  }

  public void updateNow() {
    myActionHolder.updateNow();
  }

  public void setPresentationMapping(String swingKey, PresentationMapping mapping) {
    myActionHolder.setPresentationMapping(swingKey, mapping);
  }

  public void overridePresentation(Map mapping) {
    myActionHolder.overridePresentation(mapping);
  }

  public void setIcon(Icon icon) {
    super.setIcon(icon);
    if (icon instanceof IconHandle)
      setDisabledIcon(((IconHandle) icon).getGrayed());
  }

  public void setAction(Action action) {
    myActionHolder.setSwingAction(action);
    super.setAction(action);
  }

  static {
    HashMap<String, Procedure2<AnActionHolder, Action>> procedures = Collections15.hashMap();

    procedures.put(PresentationKey.ACTION_KEY_VISIBLE, new Procedure2<AnActionHolder, Action>() {
      public void invoke(AnActionHolder holder, Action action) {
        AbstractButton button = holder.getActionComponent();
        updateVisibility(button, action);
      }
    });

    Procedure2<AnActionHolder, Action> tooltipProcedure = new Procedure2<AnActionHolder, Action>() {
      public void invoke(AnActionHolder holder, Action action) {
        KeyStroke stroke = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
        holder.registerKeyStroke(stroke);

        String shortDescription = (String) action.getValue(Action.SHORT_DESCRIPTION);
        AbstractButton button = holder.getActionComponent();

        if (shortDescription == null && stroke == null) {
          button.setToolTipText(null);
        }

        else {
          final StringBuilder tooltip = new StringBuilder();
          if (shortDescription != null) {
            tooltip.append(shortDescription);
          }

          if (stroke != null && !LAFUtil.isAcceleratorShown()) {
            if (tooltip.length() > 0) {
              tooltip.append(' ');
            }
            tooltip.append('(');
            tooltip.append(Shortcuts.getKeyStrokeText(stroke));
            tooltip.append(')');
          }

          button.setToolTipText(tooltip.toString());
        }
      }
    };
    procedures.put(Action.ACCELERATOR_KEY, tooltipProcedure);
    procedures.put(Action.SHORT_DESCRIPTION, tooltipProcedure);

    procedures.put(PresentationKey.ACTION_KEY_TOGGLED_ON, new Procedure2<AnActionHolder, Action>() {
      public void invoke(AnActionHolder holder, Action action) {
        Boolean isOnValue = (Boolean) action.getValue(PresentationKey.ACTION_KEY_TOGGLED_ON);
        boolean isOn = isOnValue != null && isOnValue.booleanValue();
        holder.getActionComponent().getModel().setSelected(isOn);
      }
    });

    ourUpdateProcedures = Collections.unmodifiableMap(procedures);
  }

  public static void updateVisibility(AbstractButton button, Action action) {
    Boolean visibleBoolean = (Boolean) action.getValue(PresentationKey.ACTION_KEY_VISIBLE);
    if (visibleBoolean == null) {
      assert !button.isDisplayable() : button;
      return;
    }
    button.setVisible(visibleBoolean);
  }
}
