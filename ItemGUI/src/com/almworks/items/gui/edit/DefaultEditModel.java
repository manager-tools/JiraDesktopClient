package com.almworks.items.gui.edit;

import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.DataRole;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DefaultEditModel extends EditItemModel implements EditItemModel.AttributeValueSource {
  public static final DataRole<Root> ROLE = DataRole.createRole(Root.class);

  private final ConcurrentHashMap<DBAttribute<Long>, Convertor<EditModelState, LongList>> mySingleEnumGetters = new ConcurrentHashMap<DBAttribute<Long>, Convertor<EditModelState, LongList>>();
  private final ConcurrentHashMap<DBAttribute<? extends Collection<Long>>, Convertor<EditModelState, LongList>> myMultiEnumGetters = new ConcurrentHashMap<DBAttribute<? extends Collection<Long>>, Convertor<EditModelState, LongList>>();
  private final EditItemModel myParent;
  private final List<AttributeValueSource> myChildAttributeSources = new CopyOnWriteArrayList<AttributeValueSource>();
  private final List<FieldEditor> myEditors = Collections15.arrayList();
  private final Map<TypedKey<?>, TLongObjectHashMap<Object>> myItemHints = Collections15.hashMap();
  private final Map<TypedKey<?>, Object> myValues = Collections15.hashMap();
  private final Set<FieldEditor> myDisabledEditors = Collections15.hashSet();
  private final LongList myItems;
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final AtomicReference<Boolean> myFiringEvent = new AtomicReference<Boolean>(null);
  private final Map<DBAttribute<?>, Pair<LongList, LongList>> myCubeAxises = Collections15.hashMap();
  private Map<TypedKey<?>, Object> myInitial = null;

  private DefaultEditModel(LongList items, EditItemModel parent) {
    myItems = items;
    myParent = parent;
  }

  protected final EditItemModel getParent() {
    return myParent;
  }

  @Override
  public void registerEditor(FieldEditor editor) {
    synchronized (myEditors) {
      if (myEditors.contains(editor)) {
        LogHelper.error("Already registered", editor);
        return;
      }
      myEditors.add(editor);
    }
  }

  @Override
  public void registerMultiEnum(DBAttribute<? extends Collection<Long>> attribute, Convertor<EditModelState, LongList> getter) {
    Convertor<EditModelState, LongList> prev = myMultiEnumGetters.putIfAbsent(attribute, getter);
    LogHelper.assertError(prev == null, "Already registered", attribute, prev, getter);
  }

  @Override
  public void registerSingleEnum(DBAttribute<Long> attribute, Convertor<EditModelState, LongList> getter) {
    Convertor<EditModelState, LongList> prev = mySingleEnumGetters.putIfAbsent(attribute, getter);
    LogHelper.assertError(prev == null, "Already registered", attribute, prev, getter);
  }

  /**
   * Changes value for a key and fire changed event. The value can be obtained via {@link #getValue(org.almworks.util.TypedKey)}
   * @param key value key
   * @param value the value
   * @see #getValue(org.almworks.util.TypedKey)
   */
  @Override
  public <T> void putValue(TypedKey<T> key, @Nullable T value) {
    T prev = priPutValue(key, value);
    if (!Util.equals(prev, value)) fireChanged();
  }

  private <T> T priPutValue(TypedKey<T> key, @Nullable T value) {
    synchronized (myEditors) {
      T prev = key.getFrom(myValues);
      key.putTo(myValues, value);
      return prev;
    }
  }

  @Override
  public <A, B> void putValues(TypedKey<A> key1, A value1, TypedKey<B> key2, B value2) {
    A prev1;
    B prev2;
    synchronized (myEditors) {
      prev1 = key1.getFrom(myValues);
      prev2 = key2.getFrom(myValues);
      key1.putTo(myValues, value1);
      key2.putTo(myValues, value2);
    }
    if (!Util.equals(prev1, value1) || !Util.equals(prev2, value2)) fireChanged();
  }

  @Override
  public <A, B, C> void putValues(TypedKey<A> key1, A value1, TypedKey<B> key2, B value2, TypedKey<C> key3, C value3) {
    A prev1;
    B prev2;
    C prev3;
    synchronized (myEditors) {
      prev1 = key1.getFrom(myValues);
      prev2 = key2.getFrom(myValues);
      prev3 = key3.getFrom(myValues);
      key1.putTo(myValues, value1);
      key2.putTo(myValues, value2);
      key3.putTo(myValues, value3);
    }
    if (!Util.equals(prev1, value1) || !Util.equals(prev2, value2) || !Util.equals(prev3, value3)) fireChanged();
  }

  @Override
  public <T> void putHint(TypedKey<T> key, T hint) {
    priPutValue(key, hint);
  }

  @Override
  public <T> T getValue(TypedKey<T> key) {
    synchronized (myEditors) {
      return key.getFrom(myValues);
    }
  }

  public <T> T copyHint(EditModelState source, TypedKey<T> key) {
    T value = source.getValue(key);
    return priPutValue(key, value);
  }

  public final void copyHints(EditModelState source, Collection<? extends TypedKey<?>> keys) {
    for (TypedKey<?> key : keys) copyHint(source, key);
  }

  public final void copyHints(EditModelState source, TypedKey<?> ... keys) {
    copyHints(source, Arrays.asList(keys));
  }

  @Override
  public void setEditorEnabled(FieldEditor editor, boolean enable) {
    boolean changed = false;
    synchronized (myEditors) {
      if (myEditors.contains(editor)) {
        if (enable) changed = myDisabledEditors.remove(editor);
        else changed = myDisabledEditors.add(editor);
      }
    }
    if (changed)
      fireChanged();
  }

  public void saveInitialValues() {
    FieldEditor[] editors;
    synchronized (myEditors) {
      LogHelper.assertError(myInitial == null, "Initial not empty", myInitial);
      myInitial = Collections15.hashMap(myValues);
      editors = myEditors.toArray(new FieldEditor[myEditors.size()]);
    }
    for (FieldEditor editor : editors) editor.afterModelFixed(this);
  }

  @Override
  public <T> T getInitialValue(TypedKey<T> key) {
    synchronized (myEditors) {
      if (myInitial == null) {
        LogHelper.error("Missing initial", myValues);
        return null;
      }
      else return key.getFrom(myInitial);
    }
  }

  @Override
  public List<FieldEditor> getCommitEditors() {
    List<FieldEditor> result;
    synchronized (myEditors) {
      result = Collections15.arrayList(myEditors);
      result.removeAll(myDisabledEditors);
    }
    for (Iterator<FieldEditor> it = result.iterator(); it.hasNext();) {
      FieldEditor editor = it.next();
      if (!editor.hasDataToCommit(this)) it.remove();
    }
    return result;
  }

  public boolean isChanged() {
    for (FieldEditor editor : getEnabledEditors())
      if (editor.isChanged(this))
        return true;
    return false;
  }

  @Override
  public List<FieldEditor> getAllEditors() {
    synchronized (myEditors) {
      return Collections15.arrayList(myEditors);
    }
  }

  @Override
  public List<FieldEditor> getEnabledEditors() {
    synchronized (myEditors) {
      List<FieldEditor> editors = Collections15.arrayList(myEditors);
      editors.removeAll(myDisabledEditors);
      return editors;
    }
  }

  @Override
  public boolean isEnabled(FieldEditor editor) {
    synchronized (myEditors) {
      if (!myEditors.contains(editor)) return false;
      if (myDisabledEditors.contains(editor)) return false;
    }
    return true;
  }

  @Override
  public Long getSingleEnumValue(DBAttribute<Long> attribute) {
    LongList list = getItemAttributeValue(attribute);
    if (list != null && list.size() != 1) return null; // No value or not unique
    Long value = list != null ? list.get(0) : null;
    if (value == null && myParent != null) value = myParent.getSingleEnumValue(attribute);
    if (value == null) {
      Pair<LongList, LongList> axisValue = getCubeAxis(attribute);
      if (axisValue != null) {
        LongList included = axisValue.getFirst();
        if (included.size() == 1) value = included.get(0);
      }
    }
    return value != null && value > 0 ? value : null;
  }

  @Override
  public Pair<LongList, LongList> getCubeAxis(DBAttribute<?> axis) {
    Pair<LongList, LongList> result = getItemCubeAxis(axis);
    if (result == null) result = myCubeAxises.get(axis);
    if (result == null && myParent != null) result = myParent.getCubeAxis(axis);
    return result;
  }

  @Override
  public Pair<LongList, LongList> getItemCubeAxis(DBAttribute<?> axis) {
    LongList list = getOwnEnumValue(axis);
    if (list != null) return Pair.create(list, LongList.EMPTY);
    for (AttributeValueSource source : myChildAttributeSources) {
      Pair<LongList, LongList> axisValue = source.getItemCubeAxis(axis);
      if (axisValue != null) return axisValue;
    }
    return null;
  }

  public void setHypercube(ItemHypercube cube) {
    if (cube == null) myCubeAxises.clear();
    else {
      for (DBAttribute<?> axis : cube.getAxes()) {
        SortedSet<Long> includedSet = cube.getIncludedValues(axis);
        SortedSet<Long> excludedSet = cube.getExcludedValues(axis);
        LongList included = toLongList(includedSet);
        LongList excluded = toLongList(excludedSet);
        myCubeAxises.put(axis, Pair.create(included, excluded));
      }
    }
  }

  @NotNull
  private LongList toLongList(SortedSet<Long> includedSet) {
    if (includedSet == null || includedSet.isEmpty())
      return LongList.EMPTY;
    else {
      LongArray array = LongArray.create(includedSet);
      array.sortUnique();
      return array;
    }
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private LongList getOwnEnumValue(DBAttribute<?> attribute) {
    Convertor<EditModelState, LongList> getter = mySingleEnumGetters.get(attribute);
    if (getter != null) return Util.NN(getter.convert(this), LongList.EMPTY);
    getter = myMultiEnumGetters.get(attribute);
    return getter != null ? Util.NN(getter.convert(this), LongList.EMPTY) : null;
  }

  public LongList getItemAttributeValue(DBAttribute<Long> attribute) {
    LongList value = getOwnEnumValue(attribute);
    if (value != null) return value;
    for (AttributeValueSource source : myChildAttributeSources) {
      value = source.getItemAttributeValue(attribute);
      if (value != null) return value;
    }
    return null;
  }

  @Override
  public void registerAttributeSource(Lifespan life, final AttributeValueSource source) {
    if (source == null || life.isEnded()) return;
    myChildAttributeSources.add(source);
    source.addAWTChangeListener(life, this);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        myChildAttributeSources.remove(source);
      }
    });
  }

  @Override
  public void addChildModel(EditItemModel child) {
    myModifiable.addStraightListener(Lifespan.FOREVER, child);
  }

  @Override
  public boolean isNewItem() {
    return myItems.isEmpty();
  }

  @Override
  public LongList getEditingItems() {
    return myItems;
  }

  public void fireChanged() {
    if (!myFiringEvent.compareAndSet(null, false)) {
      myFiringEvent.compareAndSet(false, true);
      return;
    }
    try {
      int iteration = 0;
      while (true) {
        myModifiable.fireChanged();
        if (!myFiringEvent.compareAndSet(true, false)) break;
        iteration++;
        if (iteration > 10) {
          LogHelper.error("Looped modification", iteration);
          break;
        }
      }
    } finally {
      myFiringEvent.set(null);
    }
  }

  @Override
  public void onChange() {
    if (myFiringEvent.get() != null) return;
    fireChanged();
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    myModifiable.removeChangeListener(listener);
  }

  @Override
  public Detach addAWTChangeListener(ChangeListener listener) {
    return myModifiable.addAWTChangeListener(listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myModifiable.addChangeListener(life, listener);
  }

  @Override
  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    myModifiable.addChangeListener(life, gate, listener);
  }

  @Override
  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    myModifiable.addAWTChangeListener(life, listener);
  }

  private void priCopyState(DefaultEditModel target) {
    synchronized (myEditors) {
      target.mySingleEnumGetters.putAll(mySingleEnumGetters);
      target.myMultiEnumGetters.putAll(myMultiEnumGetters);
      target.myEditors.addAll(myEditors);
      target.myDisabledEditors.addAll(myDisabledEditors);
      if (myInitial != null) target.myInitial = Collections15.hashMap(myInitial);
      target.myValues.putAll(myValues);
      target.myItemHints.putAll(myItemHints);
    }
    for (FieldEditor editor : target.myEditors) editor.afterModelCopied(target);
  }

  public static class Root extends DefaultEditModel {
    private Root(LongList items) {
      super(items, null);
    }

    @NotNull
    @Override
    public EditItemModel getRootModel() {
      return this;
    }

    public static Root newItem(ItemCreator creator) {
      Root model = new Root(LongList.EMPTY);
      model.putHint(ItemCreator.KEY, creator);
      return model;
    }

    public static Root editItems(LongList items) {
      LongArray copy = LongArray.copy(items);
      copy.sortUnique();
      return new Root(copy);
    }

    public Root copyState() {
      Root copy = new Root(getEditingItems());
      super.priCopyState(copy);
      return copy;
    }
  }

  public static class Child extends DefaultEditModel {
    private final boolean myProvidesAttributes;

    public Child(EditItemModel parent, LongList items, boolean providesAttributes) {
      super(items, parent);
      myProvidesAttributes = providesAttributes;
    }

    public static Child newItem(@NotNull EditItemModel parent, ItemCreator creator) {
      Child child = createChild(parent, LongList.EMPTY, false);
      child.putHint(ItemCreator.KEY, creator);
      return child;
    }

    @NotNull
    @Override
    public EditItemModel getRootModel() {
      EditItemModel parent = getParent();
      if (parent == null) {
        LogHelper.error("Missing parent", this);
        return this;
      }
      return parent.getRootModel();
    }

    /**
     * @param providesAttributes true means the child model (and all its future copies) registers as attribute provider to parent.<br>
     * Set true if the model is editing the same items as parent model - provide part of the state of the same items.<br>
     * Set false if the model is editing other items
     */
    @NotNull
    public static Child editItems(@NotNull EditItemModel parent, LongList items, boolean providesAttributes) {
      LongArray copy = LongArray.copy(items);
      copy.sortUnique();
      return createChild(parent, copy, providesAttributes);
    }

    /**
     * @see #editItems(EditItemModel, com.almworks.integers.LongList, boolean)
     */
    public static Child editItem(@NotNull EditItemModel parent, long item, boolean provideAttributes) {
      return editItems(parent, LongArray.create(item), provideAttributes);
    }

    @NotNull
    private static Child createChild(EditItemModel parent, LongList items, boolean providesAttributes) {
      if (parent == null) throw new NullPointerException("Null parent " + items +" " + providesAttributes);
      Child child = new Child(parent, items, providesAttributes);
      if (providesAttributes) parent.registerAttributeSource(Lifespan.FOREVER, child);
      else child.addAWTChangeListener(Lifespan.FOREVER, parent);
      parent.addChildModel(child);
      return child;
    }

    public Child copyState(EditItemModel parent) {
      Child copy = createChild(parent, getEditingItems(), myProvidesAttributes);
      super.priCopyState(copy);
      return copy;
    }
  }
}
