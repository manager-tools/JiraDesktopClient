package com.almworks.util.ui;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Lifecycle: <br>({@link #reset()}, <br>({@link #reset()} | {@link #apply()} | {@link #isModified()} | {@link #getComponent()})*,<br> {@link #dispose()})
 *
 * @author : Dyoma
 */
public interface DialogEditor extends UIComponentWrapper {
  /**
   * @return true if user has made significant modifications. false means apply won't change anything
   */
  boolean isModified();

  /**
   * Apply user changes.
   *
   * @throws CantPerformExceptionExplained if something went wrong when applying changes. In that case, caller of apply should
   *                                       behave as that "apply" action was not called.
   */
  void apply() throws CantPerformExceptionExplained;

  /**
   * Reset values to original ones.
   */
  void reset();

  Modifiable getModifiable();

  class SimpleEditor extends Simple implements DialogEditor {
    public SimpleEditor(@NotNull JComponent component) {
      super(component);
    }

    public boolean isModified() {
      return false;
    }

    public void apply() throws CantPerformExceptionExplained {
    }

    public void reset() {
    }

    @Override
    public Modifiable getModifiable() {
      return Modifiable.NEVER;
    }

    @Deprecated
    public Detach addAWTChangeListener(ChangeListener listener) {
      return Detach.NOTHING;
    }

    public void addChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    }

    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    }
  }
}
