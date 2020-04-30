package com.almworks.util.ui.actions.globals;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public final class RoleSubscribtion implements ChangeListener, Lifespan {
  private final TypedKey<?> myRole;
  private final List<ActionCounterpart> myActions = Collections15.arrayList();
  private boolean myEnded = false;
  private final List<Detach> myDetaches = Collections15.arrayList();
  private boolean myActive = false;

  public RoleSubscribtion(TypedKey<?> role) {
    myRole = role;
  }

  public void onChange() {
    if (myActions.isEmpty()) return;
    ActionCounterpart[] copy = myActions.toArray(new ActionCounterpart[myActions.size()]);
    for (ActionCounterpart action : copy) action.fireChanged();
  }

  public void subscribe(ActionCounterpart action) {
    if (myActions.contains(action)) return;
    myActions.add(action);
    action.onSubscribedTo(this);
  }

  public void listnenTo(DataProvider provider) {
    provider.addRoleListener(this, myRole, this);
    myActive = true;
  }

  public void remove(ActionCounterpart action) {
    myActions.remove(action);
    if (myActions.isEmpty()) detach();
  }

  public Lifespan add(@Nullable Detach detach) {
    if (detach == null) return this;
    if (myEnded) detach.detach();
    else myDetaches.add(detach);
    return this;
  }

  public boolean isEnded() {
    return myEnded;
  }

  public void endLife() {
    myEnded = true;
    onChange();
    myActions.clear();
    detach();
  }

  private void detach() {
    myActive = false;
    for (int i = 0; i < myDetaches.size(); i++) {
      Detach detach = myDetaches.get(i);
      detach.detach();
    }
    myDetaches.clear();
  }

  public boolean isActive() {
    return myActive && !myEnded;
  }

  @NotNull
  public static RoleSubscribtion getOrCreate(TypedKey<?> role, HashMap<TypedKey<?>, RoleSubscribtion> subscriptions,
    Function<TypedKey<?>, DataProvider> getProvider)
  {
    RoleSubscribtion subscribtion = subscriptions.get(role);
    if (subscribtion != null && subscribtion.isActive())
      return subscribtion;
    if (subscribtion == null || subscribtion.isEnded()) subscribtion = new RoleSubscribtion(role);
    DataProvider provider = getProvider.invoke(role);
    if (provider != null) subscribtion.listnenTo(provider);
    subscriptions.put(role, subscribtion);
    return subscribtion;
  }
}
