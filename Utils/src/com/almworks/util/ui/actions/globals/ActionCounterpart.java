package com.almworks.util.ui.actions.globals;

import com.almworks.util.collections.ChangeListener;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ActionCounterpart {
  private Lifespan myListenerLife;
  private ChangeListener myListener;
  private final List<RoleSubscribtion> mySubscribtions = Collections15.arrayList();
  private final List<ActionCounterpart> myPool;
  private boolean myPooled = true;

  ActionCounterpart(Lifespan life, ChangeListener listener, List<ActionCounterpart> pool) {
    myListenerLife = life;
    myListener = listener;
    myPool = pool;
  }

  @Nullable
  public static ActionCounterpart createIfNotSame(@Nullable ActionCounterpart existing, Lifespan life, ChangeListener listener,
    List<ActionCounterpart> pool) {
    if (existing != null && existing.isSame(life, listener)) return existing;
    if (life.isEnded()) return null;
    ActionCounterpart result;
    if (pool.isEmpty()) result = new ActionCounterpart(life, listener, pool);
    else {
      result = pool.remove(pool.size() - 1);
      assert result.myPool == pool;
      result.myListenerLife = life;
      result.myListener = listener;
    }
    result.attachLife();
    return result;
  }

  private void attachLife() {
    assert myPooled;
    myPooled = false;
    myListenerLife.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        onDetach();
      }
    });
  }

  private void onDetach() throws Exception {
    assert !myPooled;
    for (int i = 0; i < mySubscribtions.size(); i++) {
      RoleSubscribtion subscribtion = mySubscribtions.get(i);
      subscribtion.remove(this);
    }
    myListenerLife = null;
    myListener = null;
    mySubscribtions.clear();
    myPool.add(this);
    myPooled = true;
  }

  public boolean isSame(Lifespan life, ChangeListener listener) {
    return myListenerLife == life && myListener == listener;
  }

  public void fireChanged() {
    if (myPooled) return;
    myListener.onChange();
  }

  public void onSubscribedTo(RoleSubscribtion subscribtion) {
    if (myPooled) return;
    mySubscribtions.add(subscribtion);
  }
}
