package com.almworks.items.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBWriter;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelegatingWriter extends DelegatingReader implements DBWriter {
  private final DBWriter myWriter;

  protected DelegatingWriter(@NotNull DBWriter writer) {
    super(writer);
    myWriter = writer;
  }

  @Override
  public void clearItem(long item) {
    myWriter.clearItem(item);
  }

  @Override
  public void clearAttribute(DBAttribute<?> attribute) {
    myWriter.clearAttribute(attribute);
  }

  @Override
  public void finallyDo(ThreadGate gate, Procedure<Boolean> procedure) {
    myWriter.finallyDo(gate, procedure);
  }

  @Override
  public long materialize(DBIdentifiedObject object) {
    return myWriter.materialize(object);
  }

  @Override
  public long nextItem() {
    return myWriter.nextItem();
  }

  @Override
  public <T> void setValue(long item, DBAttribute<T> attribute, @Nullable T value) {
    myWriter.setValue(item, attribute, value);
  }
}
