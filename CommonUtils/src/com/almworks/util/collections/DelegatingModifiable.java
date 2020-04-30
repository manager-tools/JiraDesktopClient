package com.almworks.util.collections;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

public class DelegatingModifiable implements Modifiable {
  @NotNull
  private final Modifiable myDelegate;

  public DelegatingModifiable(@NotNull Modifiable delegate) {
    myDelegate = delegate;
  }

  @Override
  public Detach addAWTChangeListener(ChangeListener listener) {
    return myDelegate.addAWTChangeListener(listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myDelegate.addChangeListener(life, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    myDelegate.addChangeListener(life, gate, listener);
  }

  @Override
  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    myDelegate.addAWTChangeListener(life, listener);
  }
}
