package com.almworks.util.ui.actions.globals;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.collections.ElementVisitor2;
import com.almworks.util.exec.Context;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.almworks.util.ui.swing.TreeElementVisitor;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class DataGlobalizationUtil {
  public static boolean isDataRoot(@Nullable Component descendant) {
    return descendant instanceof JComponent && DataRoot.KEY.getClientValue((JComponent) descendant) != null;
  }

  public static void collectDataHosts(Container container, @NotNull final Map<TypedKey<?>, JComponent> hosts) {
    iterateDataHosts(container, new ElementVisitor2<TypedKey<?>, JComponent>() {
      public boolean visit(TypedKey<?> role, JComponent component) {
        assert !hosts.containsKey(role) : component + " " + role;
        hosts.put(role, component);
        return true;
      }
    });
  }

  public static boolean isAncestorOfDataHost(Container container) {
    class DataHostSearch implements ElementVisitor2<TypedKey<?>, JComponent> {
      private boolean myFound = false;

      public boolean visit(TypedKey<?> typedKey, JComponent component) {
        myFound = true;
        return false;
      }
    }
    DataHostSearch search = new DataHostSearch();
    if (visitComponent(container, search)) iterateDataHosts(container, search);
    return search.myFound;
  }

  private static final ThreadLocal<DataHostsIterator> DATA_HOSTS = new ThreadLocal<DataHostsIterator>();
  public static void iterateDataHosts(Container container, ElementVisitor2<TypedKey<?>, JComponent> visitor) {
    assert Context.isAWT();
    DataHostsIterator wrapper = DATA_HOSTS.get();
    if (wrapper == null) wrapper = new DataHostsIterator();
    else DATA_HOSTS.set(null);
    wrapper.setIterator(visitor);
    iterateDataComponents(container, wrapper);
    wrapper.setIterator(null);
    DATA_HOSTS.set(wrapper);
  }

  public static boolean visitComponent(@Nullable Component component, ElementVisitor2<TypedKey<?>, JComponent> visitor) {
    if (!(component instanceof JComponent))
      return true;
    JComponent jComponent = (JComponent) component;
    List<? extends TypedKey<?>> globalRoles = GlobalData.KEY.getClientValue(jComponent);
    if (globalRoles != null) {
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < globalRoles.size(); i++) {
        TypedKey<?> role = globalRoles.get(i);
        if (!visitor.visit(role, jComponent))
          return false;
      }
    }
    return true;
  }

  @Nullable
  public static Collection<? extends TypedKey<?>> appendDataHost(@Nullable Component descendant, @NotNull Map<TypedKey<?>, JComponent> hosts) {
    if (!(descendant instanceof JComponent))
      return null;
    JComponent jComponent = (JComponent) descendant;
    Collection<? extends TypedKey<?>> globalRoles = GlobalData.KEY.getClientValue(jComponent);
    if (globalRoles != null) {
      for (TypedKey<?> role : globalRoles) {
        assert !hosts.containsKey(role) : descendant + " " + role + " " + hosts.get(role);
        hosts.put(role, jComponent);
      }
    }
    return globalRoles;
  }

  private static final ThreadLocal<ElementVisitor.Collector<Component>> DATA_COMPONENT_COLLECTOR = new ThreadLocal<ElementVisitor.Collector<Component>>();
  public static List<Component> descendants(Container ancestor) {
    ElementVisitor.Collector<Component> collector = DATA_COMPONENT_COLLECTOR.get();
    if (collector == null) collector = new ElementVisitor.Collector<Component>();
    else DATA_COMPONENT_COLLECTOR.set(null);
    collector.clear();
    iterateDataComponents(ancestor, collector);
    List<Component> result = collector.copyCollectedAndClear();
    DATA_COMPONENT_COLLECTOR.set(collector);
    return result;
  }

  private static final ThreadLocal<DataComponentsIterator> DATA_COMPONENTS = new ThreadLocal<DataComponentsIterator>();
  private static void iterateDataComponents(Container ancestor, ElementVisitor<Component> iterator) {
    assert Context.isAWT();
    DataComponentsIterator wrapper = DATA_COMPONENTS.get();
    if (wrapper == null) wrapper = new DataComponentsIterator();
    else DATA_COMPONENTS.set(null);
    wrapper.setIterator(iterator);
    SwingTreeUtil.iterateDescendants(ancestor, wrapper);
    wrapper.setIterator(null);
    DATA_COMPONENTS.set(wrapper);
  }

  private static class DataComponentsIterator implements TreeElementVisitor<Component> {
    private ElementVisitor<Component> myIterator = null;

    public Result visit(Component item) {
      if (!myIterator.visit(item)) return Result.STOP;
      if (isDataRoot(item)) return Result.SKIP_SUBTREE;
      return Result.GO_ON;
    }

    public void setIterator(ElementVisitor<Component> iterator) {
      myIterator = iterator;
    }
  }

  private static class DataHostsIterator implements ElementVisitor<Component> {
    private ElementVisitor2<TypedKey<?>, JComponent> myIterator = null;

    public DataHostsIterator() {
    }

    public boolean visit(Component element) {
      return visitComponent(element, myIterator);
    }

    public void setIterator(ElementVisitor2<TypedKey<?>, JComponent> iterator) {
      myIterator = iterator;
    }
  }
}
