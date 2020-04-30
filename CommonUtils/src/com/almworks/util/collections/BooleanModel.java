package com.almworks.util.collections;

import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author : Dyoma
 */
public class BooleanModel extends SimpleModifiable {
  private boolean myValue;

  public BooleanModel(boolean value) {
    myValue = value;
  }

  public BooleanModel() {
  }

  public Detach attachWidget(final JToggleButton button, boolean negate) {
    return new ToggleButtonController(this, button, negate).myDetach;
  }

  public Boolean getBooleanValue() {
    return Boolean.valueOf(myValue);
  }

  public void setBooleanValue(Boolean value) {
    boolean b = value.booleanValue();
    setValue(b);
  }

  public void setValue(boolean b) {
    if (myValue == b)
      return;
    myValue = b;
    fireChanged();
  }

  private static class ToggleButtonController {
    private final DetachComposite myDetach = new DetachComposite();

    public ToggleButtonController(final BooleanModel model, final JToggleButton button, final boolean negate) {
      ChangeListener modelListener = new ChangeListener() {
        public void onChange() {
          button.setSelected(model.myValue != negate);
        }
      };
      model.addAWTChangeListener(myDetach, modelListener);
      modelListener.onChange();

      final PropertyChangeListener listener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          if (!AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(evt.getPropertyName()))
            return;
          Object newValue = evt.getNewValue();
          Object oldValue = evt.getOldValue();
          if (AccessibleState.SELECTED.equals(newValue))
            model.setValue(!negate);
          else if (AccessibleState.SELECTED.equals(oldValue))
            model.setValue(negate);
        }
      };
      final AccessibleContext context = button.getAccessibleContext();
      context.addPropertyChangeListener(listener);
      myDetach.add(new Detach() {
        protected void doDetach() {
          context.removePropertyChangeListener(listener);
        }
      });
    }
  }
}
