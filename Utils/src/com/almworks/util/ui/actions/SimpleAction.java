package com.almworks.util.ui.actions;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Factories;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Features:<p>
 * 1. Default presentation - {@link #setDefaultPresentation(PresentationKey,Object)} subclass can set initial presentation values<p>
 * 2. Attach replaced with auto subscription on update {@link #watchRole(TypedKey)}, {@link #watchModifiableRole(org.almworks.util.TypedKey)}, {@link #updateOnChange(Modifiable)}<p>
 *
 * @author dyoma
 */
public abstract class SimpleAction implements AnAction {
  @Nullable
  private ContextWatcher myContextWatcher = null;
  private final Map<PresentationKey, Factory> myDefaultPresentation = Collections15.hashMap();

  protected SimpleAction() {
    this((String)null, null);
  }

  protected SimpleAction(@Nullable String name) {
    this(name, null);
  }

  protected SimpleAction(@Nullable String name, @Nullable Icon icon) {
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    setNameIcon(name, icon);
  }

  protected SimpleAction(Factory<String> nameFactory, @Nullable Icon icon) {
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    LogHelper.assertError(nameFactory != null, "Null name factory");
    if (nameFactory != null) myDefaultPresentation.put(PresentationKey.NAME, nameFactory);
    if (icon != null) setDefaultPresentation(PresentationKey.SMALL_ICON, icon);
  }

  protected void setNameIcon(@Nullable String name, @Nullable Icon icon) {
    if (name != null)
      setDefaultPresentation(PresentationKey.NAME, name);
    if (icon != null)
      setDefaultPresentation(PresentationKey.SMALL_ICON, icon);
  }

  /**
   * Should be called from subclass constructor to set up default presentation
   */
  public final <T> void setDefaultPresentation(PresentationKey<T> key, T value) {
    if (value instanceof String) {
      value = (T) Local.parse((String) value);
    }
    myDefaultPresentation.put(key, Factories.singleton(value));
  }

  public final <T> T getDefaultPresentation(PresentationKey<T> key) {
    Factory<T> factory = myDefaultPresentation.get(key);
    //noinspection ConstantConditions
    return factory != null ? factory.create() : null;
  }

  public void setDefaultText(PresentationKey<String> key, final String localized) {
    myDefaultPresentation.put(key, new Factory<String>() {
      public String create() {
        return Local.parse(localized);
      }
    });
  }

  public <T> void setDefaultFactory(PresentationKey<T> key, Factory<T> factory) {
    if (factory != null) myDefaultPresentation.put(key, factory);
  }

  /**
   * Should be called from constructor to declare subsription for role
   */
  protected final void watchRole(@NotNull TypedKey<?> role) {
    getWatcher().watchRole(role);
  }

  @NotNull
  public ContextWatcher getWatcher() {
    synchronized (myDefaultPresentation) {
      if (myContextWatcher == null)
        myContextWatcher = new ContextWatcher();
      return myContextWatcher;
    }
  }

  /**
   * Should be called from subclass constructor to declare subsription for modifiable role
   */
  protected final void watchModifiableRole(@NotNull TypedKey<? extends Modifiable> role) {
    getWatcher().watchModifiableRole(role);
  }

  /**
   * Should be called from subclass constructor to declare subsription for modifiable.
   */
  protected final void updateOnChange(@NotNull final Modifiable modifiable) {
    getWatcher().updateOnChange(modifiable);
  }

  protected final void updateOnChange(@NotNull final ScalarModel<?> model) {
    getWatcher().updateOnChange(model);
  }

  protected abstract void customUpdate(UpdateContext context) throws CantPerformException;

  protected abstract void doPerform(ActionContext context) throws CantPerformException;

  public final void update(UpdateContext context) throws CantPerformException {
    ContextWatcher watcher;
    synchronized (myDefaultPresentation) {
      watcher = myContextWatcher;
    }
    if (watcher != null)
      watcher.requestUpdates(context.getUpdateRequest());
    for (Map.Entry<PresentationKey, Factory> entry : myDefaultPresentation.entrySet())
      context.putPresentationProperty(entry.getKey(), entry.getValue().create());
    customUpdate(context);
  }

  public final void perform(ActionContext context) throws CantPerformException {
    doPerform(context);
  }

  public static SimpleAction cloneAction(final AnAction action) {
    return new SimpleAction() {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        action.update(context);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        action.perform(context);
      }
    };
  }

  public static SimpleAction createDoNothing(String name) {
    return new SimpleAction(name) {
      protected void customUpdate(UpdateContext context) {
      }

      protected void doPerform(ActionContext context) {
      }
    };
  }
}
