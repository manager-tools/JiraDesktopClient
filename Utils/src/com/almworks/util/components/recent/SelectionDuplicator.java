package com.almworks.util.components.recent;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.IntArray;
import com.almworks.util.components.SelectionAccessor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for duplicating the selection in recent-enabled lists, so that all copies of an item are
 * selected (deselected) when you select (deselect) any of them.
 * This seemingly simple task is tricky to get right because of these concerns:
 * 1) You have to track selection history and compare the "new" selection against the "old" one, otherwise you won't
 * be able to detect and propagate removals.
 * 2) The back-end model can change at any time, so when comparing selections you have to work in "item space",
 * not "index space". Otherwise, when you find out that "item at index 15 was removed from selection", you
 * might not be able to tell reliably what the item was (it might have been removed from the model, hence the change),
 * so you won't be able to find its copies.
 * 3) The back-end model and selection will contain some items twice. This means that you have to construct the adjusted
 * selection in "index space", not "item space", because if you tell the selection accessor to "add A to the selection"
 * you can't be sure that it will add *both* A's. So you have to find the copies yourself.
 * 4) The duplication also makes comparing selections trickier -- you have to consider item counts, not just their
 * presence. If the old selection is "ABA" and the new selection is "AB", it means that A was removed.
 * Mind the above during maintenance.
 */
class SelectionDuplicator implements ChangeListener {
  private final SelectionAccessor myAccessor;
  private final AListModel myModel;
  private final Lifespan myExternalLife;

  private Map<Object, Integer> myCurrentSelection = Collections.emptyMap();
  private boolean myChangingSelection = false;

  public SelectionDuplicator(Lifespan lifespan, SelectionAccessor accessor, AListModel model) {
    myAccessor = accessor;
    myModel = model;
    myExternalLife = lifespan;
  }

  public static void install(Lifespan lifespan, SelectionAccessor accessor, AListModel model) {
    new SelectionDuplicator(lifespan, accessor, model).attach();
  }

  public void attach() {
    if(!myExternalLife.isEnded()) {
      myAccessor.addAWTChangeListener(myExternalLife, this);
      getInitialSelection();
    }
  }

  private void getInitialSelection() {
    myCurrentSelection = getSelection();
  }

  private Map<Object, Integer> getSelection() {
    return groupItems(RecentController.UNWRAPPER.collectList(myAccessor.getSelectedItems()));
  }

  private Map<Object, Integer> groupItems(List items) {
    final Map<Object, Integer> map = Collections15.hashMap();
    for(final Object o : items) {
      final int count = Util.NN(map.get(o), 0);
      map.put(o, count + 1);
    }
    return map;
  }

  @Override
  public void onChange() {
    if(myExternalLife.isEnded()) {
      return;
    }

    final Map<Object, Integer> newSelection = getSelection();
    if(myChangingSelection) {
      myCurrentSelection = newSelection;
    } else {
      findAndProcessSelectionChange(newSelection);
    }
  }

  private void findAndProcessSelectionChange(Map<Object, Integer> newSelection) {
    final Set added = findAddedItems(myCurrentSelection, newSelection);
    final Set removed = findAddedItems(newSelection, myCurrentSelection);

    myCurrentSelection = newSelection;

    if(!added.isEmpty() || !removed.isEmpty()) {
      calculateAndApplyAdjustments(added, removed);
    }
  }

  private Set findAddedItems(Map<Object, Integer> prev, Map<Object, Integer> curr) {
    final Set result = Collections15.hashSet();
    for(final Map.Entry<Object, Integer> e : curr.entrySet()) {
      final Object item = e.getKey();
      final int count = e.getValue();
      if(Util.NN(prev.get(item), 0) < count) {
        result.add(item);
      }
    }
    return result;
  }

  private void calculateAndApplyAdjustments(Set added, Set removed) {
    final IntArray toAdd = findModelIndices(added);
    final IntArray toRemove = findModelIndices(removed);
    if(toAdd.size() > 0 || toRemove.size() > 0) {
      final IntArray newSelection = makeAdjustedSelection(toAdd, toRemove);
      replaceSelectionBlindly(newSelection);
    }
  }

  private IntArray findModelIndices(Set source) {
    final IntArray result = new IntArray();
    for(final Object target : source) {
      for(int j = 0; j < myModel.getSize(); j++) {
        final Object candidate = RecentController.unwrap(myModel.getAt(j));
        if(Util.equals(target, candidate)) {
          result.add(j);
        }
      }
    }
    return result;
  }

  private IntArray makeAdjustedSelection(IntArray toAdd, IntArray toRemove) {
    final IntArray newSelection = new IntArray();
    newSelection.addAll(myAccessor.getSelectedIndexes());
    removeIndices(newSelection, toRemove);
    addIndices(newSelection, toAdd);
    newSelection.removeSubsequentDuplicates();
    return newSelection;
  }

  private void removeIndices(IntArray newSelection, IntArray toRemove) {
    for(int i = 0; i < toRemove.size(); i++) {
      newSelection.removeValue(toRemove.get(i));
    }
  }

  private void addIndices(IntArray newSelection, IntArray toAdd) {
    newSelection.addAll(toAdd.toNativeArray());
  }

  private void replaceSelectionBlindly(IntArray newSelection) {
    myChangingSelection = true;
    try {
      myAccessor.setSelectedIndexes(newSelection.toNativeArray());
    } finally {
      myChangingSelection = false;
    }
  }
}
