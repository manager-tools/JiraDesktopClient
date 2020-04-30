package com.almworks.util.components;

import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SelectionRole<T> extends DataRole<T> {
  @NotNull
  private final DataRole<T> myDataRole;

  public static <T> SelectionRole<T> first(DataRole<T> dataRole) {
    return new FirstSelected<T>(dataRole);
  }

  public static <T> SelectionRole<T> last(DataRole<T> dataRole) {
    return new LastSelected<T>(dataRole);
  }


  private SelectionRole(DataRole<T> dataRole) {
    super("SELECTION(" + dataRole.getName() + ")", dataRole.getDataClass());
    myDataRole = dataRole;
  }

  @Nullable
  abstract Object extractData(SelectionAccessor<?> accessor);

  DataRole<T> getDataRole() {
    return myDataRole;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    SelectionRole that = (SelectionRole) o;

    if (!myDataRole.equals(that.myDataRole))
      return false;

    return true;
  }

  public int hashCode() {
    return myDataRole.hashCode();
  }

  private static class FirstSelected<T> extends SelectionRole<T> {
    public FirstSelected(DataRole<T> dataRole) {
      super(dataRole);
    }

    Object extractData(SelectionAccessor<?> selectionAccessor) {
      return selectionAccessor.getFirstSelectedItem();
    }
  }


  private static class LastSelected<T> extends SelectionRole<T> {
    public LastSelected(DataRole<T> dataRole) {
      super(dataRole);
    }

    Object extractData(SelectionAccessor<?> selectionAccessor) {
      return selectionAccessor.getLastSelectedItem();
    }
  }
}
