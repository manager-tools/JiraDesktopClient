package com.almworks.util.ui.actions.globals;

import com.almworks.util.commons.Procedure;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ContainerDescendantsWatcher;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author dyoma
 */
public class GlobalDataWatcher extends ContainerDescendantsWatcher {
  @Nullable
  private Container myComponentRoot;
  private final HashMap<TypedKey<?>, JComponent> myProviders = Collections15.hashMap();
  private final WatcherCallback myCallback;

  public GlobalDataWatcher(WatcherCallback callback) {
    myCallback = callback;
  }

  public void watch(Container root) {
    Threads.assertAWTThread();
    stopWatching();
    myComponentRoot = root;
    if (DataGlobalizationUtil.isDataRoot(root)) {
      addGlobalRoles(root);
      return;
    }
    watchStrictDescendants(root);
    root.addContainerListener(this);
  }

  protected void onStartWatchDescendant(Container descendant) {
    addGlobalRoles(descendant);
  }

  protected boolean shouldWatchDescendants(Container container) {
    return !DataGlobalizationUtil.isDataRoot(container);
  }

  private void addGlobalRoles(Component component) {
    Collection<? extends TypedKey<?>> datas = DataGlobalizationUtil.appendDataHost(component, myProviders);
    if (datas != null) {
      myCallback.onDataAppears(datas);
    }
  }

  protected void onStopWatchDescendant(Container descentant) {
    if (descentant instanceof JComponent) {
      Collection<? extends TypedKey<?>> globals = GlobalData.KEY.getClientValue((JComponent) descentant);
      if (globals == null)
        return;
      for (TypedKey<?> role : globals) {
        JComponent prev = myProviders.remove(role);
        assert prev == descentant : prev + " " + descentant;
      }
      //noinspection SuspiciousMethodCalls
      assert !myProviders.containsValue(descentant) : descentant;
      myCallback.onDataDisappears(globals);
    }
  }

  public void stopWatching() {
    Threads.assertAWTThread();
    if (myComponentRoot == null)
      return;
    myComponentRoot.removeContainerListener(this);
    for (Component component : DataGlobalizationUtil.descendants(myComponentRoot)) {
      if (!(component instanceof Container))
        continue;
      Container descentant = (Container) component;
      if (!shouldWatchDescendants(descentant))
        continue;
      descentant.removeContainerListener(this);
    }
    myProviders.clear();
    myComponentRoot = null;
  }

  public void reviewDataAt(JComponent component) {
    for (Iterator<Map.Entry<TypedKey<?>,JComponent>> iterator = myProviders.entrySet().iterator();
      iterator.hasNext();) {
      Map.Entry<TypedKey<?>,JComponent> entry = iterator.next();
      if (entry.getValue() == component)
        iterator.remove();
    }
    Collection<? extends TypedKey<?>> globalRoles = DataGlobalizationUtil.appendDataHost(component, myProviders);
    if (globalRoles != null)
      myCallback.onDataAppears(globalRoles);
  }

  @Nullable
  public DataProvider getProvider(TypedKey<?> role) {
    JComponent component = myProviders.get(role);
    if (component == null)
      return null;
    return DataProvider.DATA_PROVIDER.getClientValue(component);
  }

  public JComponent getComponent(TypedKey<?> role) {
    return myProviders.get(role);
  }

  public Collection<? extends TypedKey<?>> getRoles() {
    return Collections.unmodifiableSet(myProviders.keySet());
  }

  protected void applyToStrictDescendants(Container ancestor, Procedure<Container> procedure) {
    List<Component> descendants = DataGlobalizationUtil.descendants(ancestor);
    for (Component descendant : descendants)
      if (descendant instanceof Container)
        procedure.invoke((Container) descendant);
  }

  public interface WatcherCallback {
    void onDataAppears(@NotNull Collection<? extends TypedKey<?>> roles);

    void onDataDisappears(@NotNull Collection<? extends TypedKey<?>> roles);
  }
}
