package com.almworks.util.components;

import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.DefaultActionContext;
import com.almworks.util.ui.actions.globals.GlobalData;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public class DataProviders {
  @Nullable
  public static <T> List<T> ensureMatchesAll(DataRole<? extends T> dataRole, List items) {
    for (int i = 0; i < items.size(); i++) {
      Object item = items.get(i);
      if (dataRole.matches(item))
        continue;
      if (item instanceof ObjectWrapper)
        item = ((ObjectWrapper<?>) item).getUserObject();
      if (!dataRole.matches(item))
        return null; // todo dyoma review: item == null? remove?
      items.set(i, item);
    }
    return items;
  }

  public static <T> void globalizeAs(final JComponent source, final TypedKey<T> original, final TypedKey<T> other) {
    GlobalData.KEY.addClientValue(source, other);
    if (original == other)
      return;
    DataProvider.DATA_PROVIDER.putClientValue(source, new DataProvider() {
      private boolean mySearching = false;
      @Nullable
      public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
        Threads.assertAWTThread();
        if (!Util.equals(role, other))
          return null;
        try {
          if (mySearching)
            return null;
          mySearching = true;
          DataProvider provider = DATA_PROVIDER.getClientValue(source);
          return (List<T>) (provider != null ? provider.getObjectsByRole(original) : null);
        } finally {
          mySearching = false;
        }
      }

      public boolean hasRole(@NotNull TypedKey<?> role) {
        Threads.assertAWTThread();
        if (!Util.equals(role, other))
          return false;
        try {
          if (mySearching)
            return false;
          mySearching = true;
          DataProvider provider = DATA_PROVIDER.getClientValue(source);
          return provider != null ? provider.hasRole(original) : false;
        } finally {
          mySearching = false;
        }
      }

      @NotNull
      public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
        Threads.assertAWTThread();
        if (!Util.equals(role, other))
          return;
        try {
          if (mySearching)
            return;
          mySearching = true;
          DataProvider provider = DATA_PROVIDER.getClientValue(source);
          if (provider != null)
            provider.addRoleListener(life, original, listener);
        } finally {
          mySearching = false;
        }
      }

      @Nullable
      public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
        Threads.assertAWTThread();
        if (!Util.equals(role, other))
          return null;
        try {
          if (mySearching)
            return null;
          mySearching = true;
          DataProvider provider = DATA_PROVIDER.getClientValue(source);
          return provider != null ? provider.getSourceComponent(original, source) : null;
        } finally {
          mySearching = false;
        }
      }

      @NotNull
      public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
        Threads.assertAWTThread();
        try {
          if (mySearching)
            return Collections15.emptyCollection();
          mySearching = true;
          DataProvider provider = DATA_PROVIDER.getClientValue(source);
          return provider != null ? provider.getCurrentlyAvailableRoles() : Collections15.<TypedKey>emptySet();
        } finally {
          mySearching = false;
        }
      }
    });
  }

  public static ProviderCollector collectDataRoles(JComponent component) {
    ProviderCollector result = new ProviderCollector();
    DefaultActionContext.iterateComponents(result, component);
    return result;
  }

  public static class ProviderCollector implements ElementVisitor<JComponent> {
    private final List<ComponentDataInfo> myInfos = Collections15.arrayList();

    public boolean visit(JComponent element) {
      myInfos.add(new ComponentDataInfo(element));
      return true;
    }

    public void printAll(PrintStream stream) {
      for (ComponentDataInfo info : myInfos)
        info.printTo(stream);
    }
  }

  public static class ComponentDataInfo {
    private final JComponent myComponent;
    private final List<Pair<DataProvider, Collection<? extends TypedKey>>> myRoles = Collections15.arrayList();

    public ComponentDataInfo(JComponent component) {
      myComponent = component;
      Collection<? extends DataProvider> providers = DataProvider.DATA_PROVIDER.getAllProviders(component);
      for (DataProvider provider : providers)
        myRoles.add(Pair.<DataProvider, Collection<? extends TypedKey>>create(provider, provider.getCurrentlyAvailableRoles()));
    }

    public void printTo(PrintStream stream) {
      stream.println(myComponent);
      for (Pair<DataProvider, Collection<? extends TypedKey>> pair : myRoles)
        stream.println("  Roles: " + pair.getSecond() + " From: " + pair.getFirst());
    }
  }
}
