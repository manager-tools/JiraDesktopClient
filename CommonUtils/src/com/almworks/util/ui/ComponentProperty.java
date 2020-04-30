package com.almworks.util.ui;

import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * @author : Dyoma
 */
public abstract class ComponentProperty <T> extends TypedKey<T> {
  public static final ComponentProperty<JComponent> JUMP = ComponentProperty.createProperty("JUMP");

  protected ComponentProperty(String name) {
    super(name, null, null);
  }

  public abstract void putClientValue(JComponent component, T value);

  public abstract T getClientValue(JComponent component);

  @Nullable
  public final T searchAncestors(@Nullable Component start) {
    while (start != null) {
      if (start instanceof JComponent) {
        T value = getClientValue((JComponent) start);
        if (value != null)
          return value;
      }
      start = start.getParent();
    }
    return null;
  }

  public static <T> ComponentProperty<T> createProperty(String name) {
    return new Simple<T>(name);
  }

  public static <T> ComponentProperty<T> createWeak(String name) {
    return new Weak<T>(name);
  }

  public static <T> ComponentProperty<T> delegate(final ComponentProperty<? super T> property, final Class<T> expectedClass) {
    return new ComponentProperty<T>(property.getName()) {
      public void putClientValue(JComponent component, T value) {
        assert expectedClass.isInstance(value);
        property.putClientValue(component, value);
      }

      public T getClientValue(JComponent component) {
        Object value = property.getClientValue(component);
        assert expectedClass.isInstance(value) : value + " " + expectedClass;
        return (T) value;
      }
    };
  }

  public void remove(Lifespan life, final JComponent component) {
    life.add(new Detach() {
      protected void doDetach() {
        putClientValue(component, null);
      }
    });
  }

  public void putClientValue(Lifespan life, JComponent component, T value) {
    putClientValue(component, value);
    remove(life, component);
  }

  public static class Simple<T> extends ComponentProperty<T> {
    protected Simple(String name) {
      super(name);
    }

    public void putClientValue(JComponent component, T value) {
      component.putClientProperty(this, value);
    }

    public T getClientValue(JComponent component) {
      return component != null ? (T) component.getClientProperty(this) : null;
    }
  }

  public static class Weak<T> extends ComponentProperty<T> {
    protected Weak(String name) {
      super(name);
    }

    public void putClientValue(JComponent component, T value) {
      if (value == null)
        component.putClientProperty(this, null);
      else
        component.putClientProperty(this, new WeakReference<T>(value));
    }

    public T getClientValue(JComponent component) {
      Object o = component.getClientProperty(this);
      if (o == null) return null;
      if (!(o instanceof WeakReference)) {
        assert false;
        component.putClientProperty(this, null);
        return null;
      }
      WeakReference<T> ref = (WeakReference<T>) o;
      T t = ref.get();
      if (t == null)
        component.putClientProperty(this, null);
      return t;
    }
  }
}
