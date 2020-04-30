package com.almworks.items.gui.edit;

import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DataVerification {
  private final EditItemModel myModel;
  private final List<Problem> myErrors = Collections15.arrayList();
  private final List<DataVerification> myChildren = Collections15.arrayList();
  private final Purpose myPurpose;

  public DataVerification(EditItemModel model, Purpose purpose) {
    myModel = model;
    myPurpose = purpose;
  }

  public Purpose getPurpose() {
    return myPurpose;
  }

  public boolean hasErrors() {
    if (!myErrors.isEmpty()) return true;
    for (DataVerification child : myChildren) if (child.hasErrors()) return true;
    return false;
  }

  public String getErrorMessage(String separator) {
    StringBuilder builder = new StringBuilder();
    getErrorMessage(builder, separator, getErrors());
    return builder.toString();
  }

  @NotNull
  public List<Problem> getErrors() {
    ArrayList<Problem> list = Collections15.arrayList();
    collectErrors(list);
    return list;
  }

  private void collectErrors(ArrayList<Problem> target) {
    target.addAll(myErrors);
    for (DataVerification child : myChildren) child.collectErrors(target);
  }

  private static void getErrorMessage(StringBuilder target, String separator, List<Problem> problems) {
    for (Problem problem : problems) {
      String message = problem.getMessage();
      String label = Util.NN(problem.getEditorLabel());
      if (target.length() > 0) target.append(separator);
      if (label.isEmpty()) target.append(message);
      else target.append(label).append(": ").append(message);
      target.append(".");
    }
  }

  public EditItemModel getModel() {
    return myModel;
  }

  @NotNull
  public DataVerification subContext(EditItemModel subModel) {
    for (DataVerification child : myChildren) {
      if (child.getModel() == subModel) return child;
    }
    DataVerification child = new DataVerification(subModel, myPurpose);
    myChildren.add(child);
    return child;
  }

  @NotNull
  public DataVerification subContext() {
    DataVerification child = new DataVerification(myModel, myPurpose);
    myChildren.add(child);
    return child;
  }

  public void addError(FieldEditor source, String message) {
    addProblem(Problem.create(myModel, source, message));
  }

  public boolean addProblem(Problem problem) {
    return myErrors.add(problem);
  }

  public static class Problem {
    private final EditModelState myModel;
    private final FieldEditor mySource;
    private final String myMessage;
    private final String myEditorLabel;

    public Problem(EditModelState model, String editorLabel, FieldEditor source, String message) {
      myModel = model;
      mySource = source;
      myEditorLabel = editorLabel;
      myMessage = message;
    }

    public static Problem create(EditModelState model, FieldEditor source, String message) {
      return new Problem(model, source.getLabelText(model).getText(), source, message);
    }

    public FieldEditor getEditor() {
      return mySource;
    }

    public String getMessage() {
      return myMessage;
    }

    public String getEditorLabel() {
      return myEditorLabel;
    }

    public EditModelState getModel() {
      return myModel;
    }
  }

  public enum Purpose {
    /**
     * The model is going to commit, collect current problems.
     */
    PRE_COMMIT_NOTIFICATION,
    /**
     * Collect most significant problems to show them to user immediately during edit.
     */
    EDIT_WARNING,

    ANY_ERROR
    }
}
