package com.almworks.jira.provider3.issue.editor;

import com.almworks.items.gui.edit.EditModelState;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class IssueScreen {

  public abstract List<Tab> getTabs(EditModelState model);

  /**
   * Checks if the edit model state change affects the screen and the screen should be recreated.<br>
   * <b>Control flow:</b> when screen is activated for first time the method is called with null value for <br>prevState</br>. If the screen can be affected by edit the screen fixes
   * significant part of current model state and returns it.<br>
   * Later - when model changes - this method is called with previous state (returned value). If the model change affects (or may affect) the screen this method returns new state,
   * it the change does not affect the screen - returns null.
   * @param prevState significant part of edit state, returned by previous call. Null for first call
   * @return updated significant part of edit state or null if the screen can not or is not affected by model change
   */
  @Nullable("Screen does not track state, or state is not changed")
  public abstract Object checkModelState(EditModelState model, @Nullable Object prevState);

  public static class Tab {
    private final String myName;
    private final List<String> myFieldIds;

    public Tab(String name, List<String> fieldIds) {
      myName = name;
      myFieldIds = fieldIds != null ? Collections.unmodifiableList(fieldIds) : Collections.<String>emptyList();
    }

    public String getName() {
      return myName;
    }

    public List<String> getFieldIds() {
      return myFieldIds;
    }

    @Override
    public String toString() {
      return "Tab[" + myName +"](" + myFieldIds.size() + ")";
    }

    public static Collection<String> extractFields(Collection<Tab> tabs) {
      ArrayList<String> fields = Collections15.arrayList();
      for (Tab tab : tabs) fields.addAll(tab.getFieldIds());
      return fields;
    }
  }
}
