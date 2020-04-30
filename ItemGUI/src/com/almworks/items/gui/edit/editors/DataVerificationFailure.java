package com.almworks.items.gui.edit.editors;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ValueModel;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.GlobalColors;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataVerificationFailure implements ChangeListener, Runnable  {
  public static final FieldEditor EDITOR = new MockEditor() {
    @NotNull
    @Override
    public List<? extends ComponentControl> createComponents(Lifespan life, final EditItemModel model) {
      JComponent component = install(life, model);
      return Collections.singletonList(SimpleComponentControl.create(component, ComponentControl.Dimensions.WIDE_LINE, this, model, ComponentControl.Enabled.ALWAYS_ENABLED));
    }
  };
  private static final CanvasRenderer<List<DataVerification.Problem>> RENDERER = new CanvasRenderer<List<DataVerification.Problem>>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, List<DataVerification.Problem> problems) {
      if (problems == null || problems.isEmpty())
        return;
      canvas.setForeground(GlobalColors.ERROR_COLOR);
      if (problems.size() == 1) {
        canvas.setFullyOpaque(false);
        canvas.setBackground(null);
        DataVerification.Problem problem = problems.get(0);
        CanvasSection name = canvas.emptySection();
        name.setFontStyle(Font.BOLD);
        name.appendText(problem.getEditorLabel());
        name.appendText(": ");
        CanvasSection content = canvas.emptySection();
        content.setFontStyle(Font.PLAIN);
        content.appendText(problem.getMessage());
      } else {
        CanvasSection header = canvas.emptySection();
        header.setFontStyle(Font.PLAIN);
        header.appendInt(problems.size());
        header.appendText(" problems: ");
        CanvasSection editors = canvas.emptySection();
        editors.setFontStyle(Font.BOLD);
        boolean first = true;
        for (DataVerification.Problem error : problems) {
          if (!first)
            editors.appendText(", ");
          first = false;
          editors.appendText(error.getEditorLabel());
        }
      }
    }
  };

  private final Lifespan myLife;
  private final EditItemModel myModel;
  private final ASingleCell<List<DataVerification.Problem>> myMessage = new ASingleCell<List<DataVerification.Problem>>();
  private final AtomicBoolean myDirty = new AtomicBoolean(false);
  private final ValueModel<List<DataVerification.Problem>> myProblem = ValueModel.create(null);
  private final MessageUpdate myUpdate = new MessageUpdate();

  public DataVerificationFailure(Lifespan life, EditItemModel model) {
    myLife = life;
    myModel = model;
    myMessage.setModel(myProblem);
    myMessage.setRenderer(RENDERER);
  }

  @Override
  public void onChange() {
    if (myDirty.compareAndSet(false, true)) ThreadGate.AWT_QUEUED.execute(this);
  }

  @Override
  public void run() {
    if (!myDirty.compareAndSet(true, false)) return;
    updateMessage();
  }

  private void updateMessage() {
    if (myLife.isEnded()) return;
    List<DataVerification.Problem> errors = myModel.verifyData(DataVerification.Purpose.EDIT_WARNING).getErrors();
    myUpdate.updateMessage(errors);
  }

  public static JComponent install(Lifespan life, EditItemModel model) {
    DataVerificationFailure listener = new DataVerificationFailure(life, model);
    model.addAWTChangeListener(life, listener);
    listener.updateMessage();
    return listener.myMessage;
  }

  private class MessageUpdate implements Runnable {
    private final Bottleneck myBottleneck = new Bottleneck(1000, ThreadGate.AWT, this);
    private String myPendingMessage = null;
    private List<DataVerification.Problem> myPendingValue = Collections.emptyList();

    public void updateMessage(List<DataVerification.Problem> errors) {
      if (errors.isEmpty()) {
        clearMessage();
        return;
      }
      PlainTextCanvas canvas = new PlainTextCanvas();
      RENDERER.renderStateOn(CellState.LABEL, canvas, errors);
      String newMessage = canvas.getText();
      if (Util.equals(myPendingMessage, newMessage)) return;
      myPendingMessage = newMessage;
      myPendingValue = errors;
      myBottleneck.requestDelayed();
    }

    private void clearMessage() {
      myBottleneck.abort();
      myPendingMessage = null;
      myPendingValue = Collections.emptyList();
      myProblem.setValue(myPendingValue);
      myMessage.setVisible(false);
    }

    @Override
    public void run() {
      if (myPendingValue.isEmpty()) myPendingMessage = null;
      if (myLife.isEnded()) return;
      if (myPendingMessage == null) {
        clearMessage();
        return;
      }
      myProblem.setValue(myPendingValue);
      if (!myMessage.isVisible()) {
        myMessage.setVisible(true);
        myMessage.invalidate();
        myMessage.revalidate();
        myMessage.repaint();
      }
    }
  }
}
