package com.almworks.util.model;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleScalarSource<T> extends SimpleModifiable implements ScalarSource<T> {
  private final DetachComposite myLife = new DetachComposite();

  public void depend(@Nullable Lifespan life, @NotNull Modifiable modifiable) {
    modifiable.addChangeListener(life, ThreadGate.STRAIGHT, this);
  }
}
