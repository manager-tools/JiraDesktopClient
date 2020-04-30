package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Custom field {@link com.almworks.jira.provider3.custom.FieldKind kind} {@link com.almworks.jira.provider3.custom.FieldKind#getExtension(org.almworks.util.TypedKey) extension}.
 * Allows to load enum field options from REST API createMeta and editMeta resources.
 * @param <T> internal data class
 */
public interface OptionsLoader<T> {
  /**
   * Load options from REST /createmeta
   */
  TypedKey<OptionsLoader<?>> CREATE_META = TypedKey.create("loadOptions.createMeta");

  /**
   * Loads options for specified field
   * @param prevResult result of previous call to this method for this field
   */
  T loadOptions(@Nullable T prevResult, List<JSONObject> options);

  /**
   * Invoked with last  result of previous calls to {@link #loadOptions(T, java.util.List)}
   * @param field field - option owner
   * @param loadResult result of last call to {@link #loadOptions(Object, List)}
   * @param fullSet true means that caller is sure that {@link #loadOptions(Object, List)} has collected all existing options.<br>
   *                If this parameter is false - the implementation should not remove other options as they may exist but not loaded
   */
  void postProcess(EntityHolder field, @Nullable T loadResult, boolean fullSet);

  class Delegating<T> implements OptionsLoader<T> {
    @Nullable
    private final OptionsLoader<T> myDelegate;

    public Delegating(@Nullable OptionsLoader<T> delegate) {
      myDelegate = delegate;
    }

    @Override
    public T loadOptions(@Nullable T prevResult, List<JSONObject> options) {
      if (myDelegate == null) return null;
      return myDelegate.loadOptions(prevResult, options);
    }

    @Override
    public void postProcess(EntityHolder field, @Nullable T loadResult, boolean fullSet) {
      if (myDelegate != null) myDelegate.postProcess(field, loadResult, fullSet);
    }
  }
}
