package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.api.Database;
import com.almworks.util.collections.ChangeListener;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of {@link BigTime}.
 */
public abstract class BigTimeImpl implements BigTime {
  private final String myName;
  private final String myDescription;
  private final String myId;

  public BigTimeImpl(String name, String description, String id) {
    myName = name;
    myDescription = description;
    myId = id;
  }

  public void attach(Database db, @Nullable ChangeListener listener) {}

  public void detach() {}

  public String getName() {
    return myName;
  }

  public String getDescription() {
    return myDescription;
  }

  public String getId() {
    return myId;
  }
}
