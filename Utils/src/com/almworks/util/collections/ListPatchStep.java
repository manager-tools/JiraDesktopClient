package com.almworks.util.collections;

public class ListPatchStep {
  private final Action myAction;
  private final int mySubjectIndex;
  private final int myGoalIndex;

  public ListPatchStep(Action action, int subjectIndex, int goalIndex) {
    myAction = action;
    mySubjectIndex = subjectIndex;
    myGoalIndex = goalIndex;
  }

  public static ListPatchStep remove(int subjectIndex) {
    return new ListPatchStep(Action.REMOVE, subjectIndex, -1);
  }

  public static ListPatchStep add(int subjectIndex, int goalIndex) {
    return new ListPatchStep(Action.ADD, subjectIndex, goalIndex);
  }

  public Action getAction() {
    return myAction;
  }

  public int getSubjectIndex() {
    return mySubjectIndex;
  }

  public int getGoalIndex() {
    return myGoalIndex;
  }

  public enum Action {
    REMOVE,
    ADD
  }
}
