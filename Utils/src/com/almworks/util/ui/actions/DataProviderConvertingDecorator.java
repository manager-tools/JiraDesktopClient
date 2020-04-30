package com.almworks.util.ui.actions;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class DataProviderConvertingDecorator<S, T> implements DataProvider {
  private final DataProvider myDelegate;
  private final DataRole<S> mySourceRole;
  private final DataRole<T> myTargetRole;
  private final Convertor<S,Collection<T>> myConvertor;

  public DataProviderConvertingDecorator(DataProvider delegate,
    DataRole<S> sourceRole, DataRole<T> targetRole, Convertor<S,Collection<T>> convertor) {

    assert delegate != null;
    assert convertor != null;
    assert sourceRole != null;
    assert targetRole != null;

    mySourceRole = sourceRole;
    myTargetRole = targetRole;
    myConvertor = convertor;
    myDelegate = delegate;
  }

  public void addRoleListener(Lifespan life, TypedKey role, ChangeListener listener) {
    if (isMine(role))
      role = mySourceRole;
    myDelegate.addRoleListener(life, role, listener);
  }

  @Nullable
  public JComponent getSourceComponent(@NotNull TypedKey<?> role, @NotNull JComponent originalComponent) {
    if (isMine(role))
      role = mySourceRole;
    return myDelegate.getSourceComponent(role, originalComponent);
  }

  public <E> List<E> getObjectsByRole(TypedKey<? extends E> role) {
    if (!isMine(role))
      return myDelegate.getObjectsByRole(role);
    DataRole<? extends E> dataRole = (DataRole<? extends E>) role;
    List<S> source = myDelegate.getObjectsByRole(mySourceRole);
    if (source == null)
      return null;
    if (source.isEmpty())
      return Collections15.emptyList();
    List<E> result = Collections15.arrayList();
    for (int i = 0; i < source.size(); i++) {
      Collection<T> targets = myConvertor.convert(source.get(i));
      for (Iterator<T> ii = targets.iterator(); ii.hasNext();) {
        Object target = ii.next();
        if (target != null && dataRole.matches(target))
          result.add((E)target);
      }
    }
    return result;
  }

  private boolean isMine(TypedKey<?> role) {
    return myTargetRole.equals(role);
  }

  public boolean hasRole(TypedKey<?> role) {
    if (isMine(role))
      role = mySourceRole;
    return myDelegate.hasRole(role);
  }

  public Collection<? extends TypedKey> getCurrentlyAvailableRoles() {
    return Collections.singleton(myTargetRole);
  }

  public static <S, T> DataProviderConvertingDecorator<S, T> create(DataProvider delegate,
    DataRole<S> sourceRole, DataRole<T> targetRole, Convertor<S,Collection<T>> convertor) {
    return new DataProviderConvertingDecorator<S, T>(delegate, sourceRole, targetRole, convertor);
  }

  public static <S, T> void decorate(JComponent component, DataRole<S> source, DataRole<T> target,
    Convertor<S, Collection<T>> convertor) {
    // todo remake - dyoma review
    DataProvider provider = DataProvider.DATA_PROVIDER.getClientValue(component);
    assert provider != null : component;
    provider = DataProviderConvertingDecorator.create(provider, source, target, convertor);
    component.putClientProperty(DataProvider.DATA_PROVIDER, null);  // remove - or there will be composite
    DataProvider.DATA_PROVIDER.putClientValue(component, provider);
  }

}
