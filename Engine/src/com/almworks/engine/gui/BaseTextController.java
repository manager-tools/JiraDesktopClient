package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.TextComponentWrapper;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.ReadOnlyTextFields;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.UndoUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public abstract class BaseTextController<T> implements UIController<JTextComponent>, TextValueController {
  protected final ModelKey<T> myKey;

  private final boolean myDirectSet;

  private final java.util.List<Updater<T>> myUpdaters = Collections15.arrayList();

  public BaseTextController(ModelKey<T> key, boolean directSet) {
    LogHelper.assertError(key != null, "Null key", this);
    myKey = key;
    myDirectSet = directSet;
  }

  protected abstract T toValue(String text);

  protected abstract boolean isEditable();

  protected abstract String toText(T value);

  protected abstract T getEmptyStringValue();

  @Override
  public void onModelChanged(ModelMap model, JTextComponent component) {
    updateComponent(model, component);
  }

  @Override
  public void onTextChanged(JTextComponent component, ModelMap model) {
    updateModel(component, model);
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap model,
    @NotNull final JTextComponent component)
  {
    connectTextValue(lifespan, model, component, this, isEditable());
  }

  public static void connectTextValue(@NotNull Lifespan lifespan, @NotNull final ModelMap model,
    @NotNull final JTextComponent component, final TextValueController controller, boolean editable) {
    JointChangeListener listener = new JointChangeListener() {
      protected void processChange() {
        controller.onModelChanged(model, component);
      }
    };
    model.addAWTChangeListener(lifespan, listener);
    listener.onChange();
    if (editable) {
      UIUtil.addTextListener(lifespan, component, new JointChangeListener(listener.getUpdateFlag()) {
        protected void processChange() {
          controller.onTextChanged(component, model);
        }
      });
    }
  }

  protected void updateModel(JTextComponent component, ModelMap model) {
    assert isEditable();
    String text = component.getText();
    T newValue;
    T emptyValue = getEmptyStringValue();
    if (emptyValue != null && (text == null || text.trim().length() == 0))
      newValue = emptyValue;
    else
      newValue = toValue(text);
    PropertyMap map = new PropertyMap();
    myKey.setValue(map, newValue);
    myKey.copyValue(model, map);
    notifyUpdated(text, component);
  }

  protected void updateComponent(ModelMap model, JTextComponent component) {
    String text;
    if (!myKey.hasValue(model)) {
      text = "";
    } else {
      T value = myKey.getValue(model);
      T emptyValue = getEmptyStringValue();
      if (Util.equals(emptyValue, value))
        text = "";
      else
        text = Util.NN(toText(value));
    }
    setComponentText(component, text);
    notifyUpdated(text, component);
  }

  protected void notifyUpdated(String text, JTextComponent component) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myUpdaters.size(); i++)
      myUpdaters.get(i).onNewData(component, myKey, text);
  }

  protected void setComponentText(JTextComponent component, String text) {
    if (isEditable() || myDirectSet) {
      TextComponentWrapper.setComponentText(component, text);
      if(!component.getText().equals(text)) {
        UndoUtil.discardUndo(component);
      }
    } else {
      ReadOnlyTextFields.setText(component, text);
    }
  }

  public void invalidateParentOnChange() {
    myUpdaters.add(new Updater<T>() {
      public void onNewData(JTextComponent component, ModelKey<T> key, String textPresentation) {
        Container parent = component.getParent();
        if (parent != null)
          parent = parent.getParent();
        if (parent instanceof JScrollPane) {
          parent = parent.getParent();
          if (parent instanceof JComponent) {
            parent.invalidate();
            ((JComponent) parent).revalidate();
            parent.repaint();
          }
        }
      }
    });
  }

  public void update(Updater<T> updater) {
    myUpdaters.add(updater);
  }

  protected void setComponentColor(JTextComponent component, Color color) {
    component.setForeground(color != null ? color : UIUtil.getEditorForeground());
  }

  public interface Updater<T> {
    void onNewData(JTextComponent component, ModelKey<T> key, String textPresentation);
  }
}
