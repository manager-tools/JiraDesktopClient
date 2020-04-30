package com.almworks.engine.gui;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Functional;
import com.almworks.util.components.CommunalFocusListener;
import com.almworks.util.components.ReadOnlyTextFields;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.SetHolderUtils;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ErrorFieldController implements UIController<JTextField> {
  private ErrorFieldController() {
  }

  public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull final JTextField field) {
    final LoadedItemServices itemServices = LoadedItemServices.VALUE_KEY.getValue(model);
    if (itemServices == null) {
      assert false : model;
      return;
    }
    final Engine engine = itemServices.getEngine();
    if (engine == null) {
      assert false : model;
      return;
    }
    final long item = itemServices.getItem();
    engine.getSynchronizer().getProblems().addInitListener(lifespan, ThreadGate.AWT, SetHolderUtils.fromChangeListener(new ChangeListener() {
      @Override
      public void onChange() {
        onSyncProblem(engine, item, field);
      }
    }));
    showProblemMessage(field, Functional.first(engine.getSynchronizer().getItemProblems(item)));
  }

  private void onSyncProblem(Engine engine, long item, JTextField field) {
    ItemSyncProblem itemProblem = Functional.first(engine.getSynchronizer().getItemProblems(item));
    showProblemMessage(field, itemProblem);
  }

  /**
   * Shows the specified problem with date and used credentials appended.
   */
  private static void showProblemMessage(@NotNull JTextField errorField, @Nullable ItemSyncProblem problem) {
    String text;
    if (problem != null) {
      StringBuilder builder = new StringBuilder();
      String description = Util.NN(problem.getLongDescription()).trim();
      builder.append(description);
      if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '.')
        builder.append('.');
      builder.append(builder.length() > 0 ? " " : "")
        .append("The problem has occurred ")
        .append(DateUtil.toFriendlyDateTime(problem.getTimeHappened()))
        .append(", ")
        .append(SyncProblems.getCredentialsDescription(problem))
        .append('.');
      text = builder.toString();
    } else {
      text = "";
    }
    showErrorMessage(errorField, text, problem != null);
  }

  private static void showErrorMessage(JTextField field, @NotNull String text, boolean visible) {
    ReadOnlyTextFields.setText(field, text);
    field.setVisible(visible);
  }

  @Nullable
  public static String getMultilinePresentation(String plainText, int columns, boolean isHTML) {
    assert plainText != null;
    assert columns > 0 : columns;
    String separator = isHTML ? "<br>" : "\n";
    plainText = plainText.replaceAll("\\s", " ").replaceAll("  ", " ").trim();
    if (plainText == null || columns < 1 || plainText.length() <= columns)
      return plainText;
    StringBuffer current = new StringBuffer();
    int lastPos = 0;
    int pos = 0;
    int nextPos = 0;
    while (current.length() < columns && pos > -1 && pos < columns - 1 && pos < plainText.length() - 1 &&
      nextPos > -1 && nextPos <= columns)
    {
      pos = getNextSpacePos(plainText, pos);
      nextPos = getNextSpacePos(plainText, pos);
      if (pos > -1) {
        current.append(plainText.substring(lastPos, pos));
        lastPos = pos;
      }
    }
    if (current.length() == 0)
      return plainText;
    if (pos > -1)
      return current.toString() + separator +
        getMultilinePresentation(plainText.substring(pos).trim(), columns, isHTML);
    if (nextPos < 0)
      return current.toString() + plainText.substring(lastPos);
    return current.toString() + separator + plainText.substring(lastPos).trim();
  }

  private static int getNextSpacePos(String text, int sPos) {
    int nPos = text.indexOf("\n", sPos + 1);
    sPos = text.indexOf(" ", sPos + 1);
    if (nPos > -1 && nPos < sPos)
      sPos = nPos;
    return sPos;
  }
                             
  public static JTextField createErrorField() {
    JTextField field = new JTextField();
    installTo(field);
    return field;
  }

  public static void installTo(JTextField field) {
    field.setForeground(GlobalColors.ERROR_COLOR);
    UIUtil.adjustFont(field, -1, Font.BOLD, false);
    CommunalFocusListener.setupJTextField(field);
//    ReadOnlyTextFields.setupReadOnlyTextField(field);
    UIController.CONTROLLER.putClientValue(field, new ErrorFieldController());
  }
}
