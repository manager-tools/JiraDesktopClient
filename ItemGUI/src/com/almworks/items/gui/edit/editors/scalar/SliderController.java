package com.almworks.items.gui.edit.editors.scalar;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class SliderController extends MouseAdapter implements ChangeListener,
  com.almworks.util.collections.ChangeListener
{
  private static final int[] SCALE = createScale();
  private static final int SCALE_MAX = SCALE.length - 1;

  private final TypedKey<Integer> myLastEditorValueKey;
  private final DefaultBoundedRangeModel mySliderModel = new DefaultBoundedRangeModel(0, 0, 0, SCALE_MAX);
  private final EditItemModel myModel;
  private final JSlider mySlider;
  private final ScalarFieldEditor<Integer> myFieldEditor;
  private final JTextField myField;
  private boolean myUpdating = false;
  private boolean mySliderDragging = false;

  public SliderController(EditItemModel model, JSlider slider, ScalarFieldEditor<Integer> editor, JTextField field) {
    myModel = model;
    mySlider = slider;
    myFieldEditor = editor;
    myField = field;
    myLastEditorValueKey = TypedKey.create(editor.getLabelText(model) + "/lastValue");
  }

  public void stateChanged(ChangeEvent e) {
    if (myUpdating) return;
    myUpdating = true;
    try {
      int index = mySliderModel.getValue();
      if (index >= 0 && index < SCALE.length) {
        myFieldEditor.setValue(myModel, SCALE[index]);
        saveCurrentValue();
      }
      if (index == SCALE_MAX && !mySliderDragging) {
        myField.requestFocus();
      }
    } finally {
      myUpdating = false;
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    mySliderDragging = true;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    mySliderDragging = false;
    if (mySliderModel.getValue() == SCALE_MAX && mySlider.isFocusOwner()) {
      myField.requestFocus();
    }
  }

  public void saveCurrentValue() {
    myModel.putHint(myLastEditorValueKey, myFieldEditor.getCurrentValue(myModel));
  }

  @Override
  public void onChange() {
    if (myUpdating) return;
    Integer lastValue = myModel.getValue(myLastEditorValueKey);
    Integer current = myFieldEditor.getCurrentValue(myModel);
    if (Util.equals(current, lastValue)) return;
    myModel.putHint(myLastEditorValueKey, current);
    myUpdating = true;
    try {
      int index = current != null ? findClosestIndex(current) : 0;
      if (index != mySliderModel.getValue()) mySliderModel.setValue(index);
    } finally {
      myUpdating = false;
    }
  }

  public static SliderController attach(Lifespan life, EditItemModel model, JSlider slider, ScalarFieldEditor<Integer> editor, JTextField field) {
    SliderController controller = new SliderController(model, slider, editor, field);
    slider.setModel(controller.mySliderModel);
    controller.mySliderModel.addChangeListener(controller);
    UIUtil.addMouseListener(life, slider, controller);
    UIUtil.addChangeListener(life, controller.mySliderModel, controller);
    model.addAWTChangeListener(life, controller);
    controller.onChange();
    return controller;
  }

  private static int findClosestIndex(int seconds) {
    int r = Arrays.binarySearch(SCALE, seconds);
    if (r >= 0)
      return r;
    r = -r - 1;
    if (r >= SCALE.length)
      r = SCALE_MAX;
    return r;
  }

  private static int[] createScale() {
    java.util.List<Integer> scale = Collections15.arrayList();
    scale.add(60);
    scale.add(600);
    scale.add(900);
    scale.add(1200);
    scale.add(1800);
    scale.add(2400);
    scale.add(2700);
    scale.add(3000);
    // from 1 hour to 8 hours
    addScale(scale, 3600, 8 * 3600, 1800);
    // from 8 hours to 16 hours
    addScale(scale, 8 * 3600, 16 * 3600, 3600);
    // from 16 hours to 40 hours
    addScale(scale, 16 * 3600, 48 * 3600, 2 * 3600);
    scale.add(48 * 3600);
    int[] r = new int[scale.size()];
    for (int i = 0; i < scale.size(); i++) {
      r[i] = scale.get(i);
    }
    return r;
  }

  private static void addScale(List<Integer> scale, int fromSeconds, int toSeconds, int step) {
    for (int i = fromSeconds; i < toSeconds; i += step)
      scale.add(i);
  }

  private static Dictionary createLabels() {
    Hashtable r = new Hashtable();
    addLabel(r, 60);
    addLabel(r, 3600);
    addLabel(r, 8 * 3600);
    addLabel(r, 16 * 3600);
    addLabel(r, 24 * 3600);
    addLabel(r, 32 * 3600);
    addLabel(r, 40 * 3600);
    JLabel inf = new JLabel("\u221E");
    r.put(SCALE_MAX, inf);
    return r;
  }

  private static void addLabel(Hashtable r, int value) {
    for (int i = 0; i < SCALE.length; i++) {
      if (SCALE[i] == value) {
        JLabel label = new JLabel(DateUtil.getFriendlyDuration(value, false));
//        label.setBorder(new LineBorder(Color.RED, 1));
        r.put(i, label);
      }
    }
  }

  public static void setupSlider(JSlider slider) {
    slider.setLabelTable(createLabels());
    slider.setModel(new DefaultBoundedRangeModel(0, 0, 0, SCALE_MAX));
  }
}
