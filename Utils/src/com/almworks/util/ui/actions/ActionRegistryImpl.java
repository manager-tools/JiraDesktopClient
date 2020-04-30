package com.almworks.util.ui.actions;

import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : Dyoma
 */
public class ActionRegistryImpl implements ActionRegistry {
  private final Map<String, AnAction> myActions = Collections15.hashMap();
  private final Map<String, FireEventSupport<Listener>> myListeners = Collections15.hashMap();
  private final Map<ScopedKeyStroke, String> myActionByKeyMap = Collections15.hashMap();
  private final MultiMap<KeyStroke, ScopedKeyStroke> myKeySearchMap = new MultiMap<KeyStroke, ScopedKeyStroke>();

  public void registerAction(@NotNull String actionKey, @NotNull AnAction action) {
    FireEventSupport<Listener> listeners;
    synchronized (myActions) {
      myActions.put(actionKey, action);
      listeners = myListeners.remove(actionKey);
    }
    if (listeners != null) {
      listeners.getDispatcher().onActionRegister(actionKey, action);
      listeners.noMoreEvents();
    }
  }

  public void registerKeyStroke(@NotNull String actionKey, @NotNull ScopedKeyStroke stroke) {
    synchronized (myActionByKeyMap) {
      myActionByKeyMap.put(stroke, actionKey);
    }
    synchronized (myKeySearchMap) {
      myKeySearchMap.add(stroke.getKeyStroke(), stroke);
    }
  }

  @Nullable
  public AnAction getAction(@Nullable String actionKey) {
    if (actionKey == null)
      return null;
    synchronized (myActions) {
      return myActions.get(actionKey);
    }
  }

  @Override
  public void addListener(Lifespan life, @NotNull final String actionId, @NotNull final Listener listener) {
    @Nullable
    final AnAction action;
    synchronized (myActions) {
      action = myActions.get(actionId);
      if (action == null) {
        FireEventSupport<Listener> support;
        support = myListeners.get(actionId);
        if (support == null) {
          support = FireEventSupport.createSynchronized(Listener.class);
          myListeners.put(actionId, support);
        }
        support.addAWTListener(life, listener);
        return;
      }
    }
    @NotNull
    final AnAction notNullAction = action;
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        listener.onActionRegister(actionId, notNullAction);
      }
    });
  }

  public void removeListener(@Nullable String actionId, @Nullable Listener listener) {
    if (actionId == null || listener == null)
      return;
    synchronized (myActions) {
      FireEventSupport<Listener> support = myListeners.get(actionId);
      if (support == null)
        return;
      support.removeListener(listener);
      if (support.getListenersCount() == 0)
        myListeners.remove(actionId);
    }
  }

  public boolean isActionRegistered(@Nullable String actionId) {
    return getAction(actionId) != null;
  }

  @NotNull
  public List<ScopedKeyStroke> getScopedKeystrokes(@NotNull KeyStroke stroke) {
    if (stroke == null) {
      throw new NullPointerException();
    }

    final List<ScopedKeyStroke> scoped;
    synchronized (myKeySearchMap) {
      scoped = myKeySearchMap.getAll(stroke);
    }

    return scoped == null ? Collections15.<ScopedKeyStroke>emptyList() : scoped;
  }

  @Nullable
  public AnAction getAction(@Nullable ScopedKeyStroke stroke) {
    if (stroke == null) {
      return null;
    }

    String actionId;
    synchronized (myActionByKeyMap) {
      actionId = myActionByKeyMap.get(stroke);
    }
    return getAction(actionId);
  }

  @Nullable
  public ScopedKeyStroke getKeyStroke(@NotNull String id) {
    synchronized (myActionByKeyMap) {
      return CollectionUtil.getKeyByValue(myActionByKeyMap, id);
    }
  }

  protected boolean processKeyEvent(@NotNull KeyEvent event) {
    if (event.getID() != KeyEvent.KEY_PRESSED) {
      return false;
    }

    final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (!(focusOwner instanceof JComponent)) {
      return false;
    }

    final JComponent contextComponent = (JComponent) focusOwner;
    Window window = SwingTreeUtil.getOwningWindow(contextComponent);
    if (window == null)
      return false;
    final String winScope = ActionScope.get(window);

    final KeyStroke stroke = KeyStroke.getKeyStroke(event.getKeyCode(), event.getModifiers());
    final List<ScopedKeyStroke> scopedStrokes = getScopedKeystrokes(stroke);

    ScopedKeyStroke matchingStroke = null;
    ScopedKeyStroke catchAllStroke = null;

    for (final ScopedKeyStroke scoped : scopedStrokes) {
      final Set<String> scopes = scoped.getScopes();
      if (scopes.isEmpty()) {
        if (catchAllStroke == null) {
          catchAllStroke = scoped;
        }
      } else {
        for (final String scope : scopes) {
          if (winScope.indexOf(scope) >= 0) {
            matchingStroke = scoped;
            break;
          }
        }
      }
    }

    if (matchingStroke == null) {
      matchingStroke = catchAllStroke;
    }

    if (matchingStroke == null) {
      return false;
    }

    final AnAction action = getAction(matchingStroke);
    if (!ActionUtil.isActionEnabled(action, contextComponent)) {
      return false;
    }

    assert action != null;

    ActionUtil.performAction(action, contextComponent);
    return true;
  }

  protected void clearKeyMap() {
    synchronized (myActionByKeyMap) {
      myActionByKeyMap.clear();
    }
    synchronized (myKeySearchMap) {
      myKeySearchMap.clear();
    }
  }

  protected void clearActions() {
    synchronized (myActions) {
      myActions.clear();
      myListeners.clear();
    }
  }

  public void addActionToGroup(@NotNull String actionKey, @NotNull String group) {
    // todo
  }

  @NotNull
  public List<String> getActionKeysForGroup(@NotNull String group) {
    // todo
    return Collections15.emptyList();
  }
}
