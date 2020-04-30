package com.almworks.util.ui;

import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The {@link #dispose()} should be called at least once atfer calling {@link #getComponent()} <br>
 * Lifecycle: <br> ({@link #getComponent()}+,  {@link #dispose()}+)*,  {@link #dispose()}*
 */
public interface UIComponentWrapper {
  /**
   * Should return the same componment untill {@link #dispose()} called.
   *
   * @return settings editor widget
   */
  JComponent getComponent();

  /**
   * Called when the component returned by {@link #getComponent()} becomes useless.
   */
  void dispose();

  public class Simple implements UIComponentWrapper2 {
    private final JComponent myComponent;

    public Simple(@NotNull JComponent component) {
      this(component, false);
    }

    public Simple(JComponent component, boolean scrollpane) {
      myComponent = scrollpane ? new JScrollPane(component) : component;
    }

    public JComponent getComponent() {
      return myComponent;
    }

    public void dispose() {
    }

    public Detach getDetach() {
      return Detach.NOTHING;
    }

    public static UIComponentWrapper2 empty() {
      return new Simple(new JPanel());
    }

    public static UIComponentWrapper2 message(String message) {
      return new Simple(UIUtil.createMessage(message));
    }

    public static UIComponentWrapper2 messageInScrollPane(String message) {
      return new Simple(UIUtil.createMessage(message), true);
    }
  }

  class Disposer extends Detach {
    private final UIComponentWrapper myWrapper;

    public Disposer(UIComponentWrapper wrapper) {
      myWrapper = wrapper;
    }

    protected void doDetach() {
      myWrapper.dispose();
    }
  }

  abstract class LazyWrapper<W extends UIComponentWrapper> implements UIComponentWrapper2 {
    private W myWrapper;

    protected abstract W initialize();

    public void dispose() {
      if (myWrapper != null) {
        myWrapper.dispose();
        myWrapper = null;
      }
    }

    public Detach getDetach() {
      return new Disposer(this);
    }

    public JComponent getComponent() {
      W wrapper = getWrapper();
      return wrapper.getComponent();
    }

    protected W getWrapper() {
      if (myWrapper == null)
        myWrapper = initialize();
      return myWrapper;
    }
  }

  interface DisplayableListener {
    void onComponentDisplayble();
  }
}
