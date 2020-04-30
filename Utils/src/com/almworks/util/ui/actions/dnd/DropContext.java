package com.almworks.util.ui.actions.dnd;

import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class DropContext extends DragContext {
  private final DragContext myDrag;

  public DropContext(DragContext drag, JComponent dropTarget) {
    super(dropTarget);
    myDrag = drag;
  }

  public <T> T getValue(TypedKey<T> key) {
    T thisValue = super.getValue(key);
    return thisValue != null ? thisValue : myDrag.getValue(key);
  }

  @NotNull
  public <T> T getSourceObject(final TypedKey<? extends T> role) throws CantPerformException {
    T result;
    try {
      result = super.getSourceObject(role);
    } catch (CantPerformException e) {
      result = myDrag.getSourceObject(role);
    }
    return result;
  }

  @NotNull
  public <T> List<T> getSourceCollection(@NotNull TypedKey<? extends T> role) throws CantPerformException {
    try {
      return super.getSourceCollection(role);
    } catch (CantPerformException e) {
      return myDrag.getSourceCollection(role);
    }
  }

  public DragContext getDrag() {
    return myDrag;
  }
}
