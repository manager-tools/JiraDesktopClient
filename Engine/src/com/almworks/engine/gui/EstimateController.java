package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;

public class EstimateController implements UIController<JTextField> {
  private final ModelKey<Integer> myOriginalEstimate;
  private final ModelKey<Integer> myRemainingEstimate;
  private final ModelKey<Integer> myTimeSpent;

  public EstimateController(ModelKey<Integer> originalEstimate, ModelKey<Integer> remainingEstimate,
    ModelKey<Integer> timeSpent)
  {
    myOriginalEstimate = originalEstimate;
    myRemainingEstimate = remainingEstimate;
    myTimeSpent = timeSpent;
  }

  public static EstimateController install(JTextField field, ModelKey<Integer> originalEstimate,
    ModelKey<Integer> remainingEstimate, ModelKey<Integer> timeSpent)
  {
    EstimateController controller = new EstimateController(originalEstimate, remainingEstimate, timeSpent);
    CONTROLLER.putClientValue(field, controller);
    return controller;
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap model, @NotNull final JTextField component) {
    final Color normalFg = component.getForeground();
    final Color errorFg = GlobalColors.ERROR_COLOR;
    final PlainDocument doc = new PlainDocument();
    component.setDocument(doc);
    JointChangeListener fromModelUpdater = new JointChangeListener() {
      @Override
      protected void processChange() {
        ModelKey<Integer> key;
        if (!shouldChangeOriginal(model)) {
          key = myRemainingEstimate;
        } else {
          key = myOriginalEstimate;
        }
        Integer value = key.getValue(model);
        if (value != null && value >= 0) {
          DocumentUtil.setDocumentText(doc, DateUtil.getFriendlyDuration(value, false));
        }
      }
    };
    model.addAWTChangeListener(lifespan, fromModelUpdater);
    fromModelUpdater.onChange();
    DocumentUtil.addChangeListener(lifespan, doc, new JointChangeListener(fromModelUpdater.getUpdateFlag()) {
      protected void processChange() {
        String text = Util.NN(DocumentUtil.getDocumentText(doc)).trim();
        Integer value = null;
        boolean errors = false;
        if (text.length() > 0) {
          try {
            value = DateUtil.parseDuration(text, true);
          } catch (ParseException e) {
            errors = true;
          }
        }
        Color fg = errors ? errorFg : normalFg;
        if (!Util.equals(component.getForeground(), fg)) {
          component.setForeground(fg);
        }
        boolean original = shouldChangeOriginal(model);
        PropertyMap props = new PropertyMap();
        myRemainingEstimate.takeSnapshot(props, model);
        if (original)
          myOriginalEstimate.takeSnapshot(props, model);
        myRemainingEstimate.setValue(props, value);
        if (original)
          myOriginalEstimate.setValue(props, value);
        myRemainingEstimate.copyValue(model, props);
        if (original)
          myOriginalEstimate.copyValue(model, props);
        model.valueChanged(myRemainingEstimate);
        if (original)
          model.valueChanged(myOriginalEstimate);
      }
    });

  }

  private boolean shouldChangeOriginal(ModelMap model) {
    Integer timeSpent = myTimeSpent.getValue(model);
    return timeSpent == null || timeSpent <= 0;
  }
}
