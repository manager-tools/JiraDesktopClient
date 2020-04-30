package com.almworks.jira.provider3.links;

import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ATable;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.globals.GlobalData;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LoadLinkProvider<L extends LoadedLink> implements DataProvider {
  private final ATable<? extends Pair<?, TreeModelBridge<?>>> myTable;
  private final DataRole<L> myRole;

  public LoadLinkProvider(ATable<? extends Pair<?, TreeModelBridge<?>>> table, DataRole<L> role) {
    myTable = table;
    myRole = role;
  }

  @Nullable
  public <T> List<T> getObjectsByRole(TypedKey<? extends T> role) {
    if (role != myRole)
      return null;
    List<? extends Pair<?, TreeModelBridge<?>>> selection = myTable.getSelectionAccessor().getSelectedItems();
    List<LoadedLink> result = Collections15.arrayList();
    for (Pair<?, TreeModelBridge<?>> pair : selection)
      addAllLinks(pair, result);
    return (List<T>) result;
  }

  private void addAllLinks(Pair<?, TreeModelBridge<?>> pair, List<LoadedLink> result) {
    if (pair == null)
      return;
    Object obj = pair.getFirst();
    if (obj == null)
      return;
    L link = Util.castNullable(myRole.getDataClass(), obj);
    if (link != null) {
      if (myRole.matches(link)) result.add(link);
    } else {
      TreeModelBridge<?> node = pair.getSecond();
      for (int i = 0; i < node.getChildCount(); i++) {
        Pair<Object, TreeModelBridge<?>> userObject =
          (Pair<Object, TreeModelBridge<?>>) node.getChildAt(i).getUserObject();
        if (userObject != null) {
          addAllLinks(userObject, result);
        }
      }
    }
  }

  public boolean hasRole(@NotNull TypedKey<?> role) {
    return role == myRole;
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    if (role == myRole)
      myTable.getSelectionAccessor().addChangeListener(life, listener);
  }

  @Nullable
  public JComponent getSourceComponent(TypedKey<?> role, JComponent originalComponent) {
    return role != myRole ? null : myTable;
  }

  @NotNull
  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    return Collections.singleton(myRole);
  }

  public static <L extends LoadedLink> LoadLinkProvider<L> install(ATable<? extends Pair<?, TreeModelBridge<?>>> table, DataRole<L> role) {
    LoadLinkProvider<L> provider = new LoadLinkProvider<L>(table, role);
    DataProvider.DATA_PROVIDER.putClientValue(table, provider);
    return provider;
  }

  public void provideGlobal() {
    GlobalData.KEY.addClientValue(myTable, myRole);
  }
}
