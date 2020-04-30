package com.almworks.util.components;

import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Log;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.BitSet;

/**
 * This class is a hack to overcome a controversial behavior of DLSM that
 * automatically extends selection when items are added to the position
 * that was selected. This model will only extend selection if adding
 * unselected items would "break" an existing multiple selection.
 * Uses nasty reflection to access private stuff.
 */
class PatchedListSelectionModel extends VetoableListSelectionModel {
  private Method mySetState;
  private Method myUpdateLeadAnchor;
  private Method myFireValueChanged;
  private BitSet myValue;

  /**
   * @throws CantPerformException if unable to access DLSM's private stuff that this class needs.
   */
  PatchedListSelectionModel(ScalarModel<Boolean> vetoModel) throws CantPerformException {
    super(vetoModel);
    try {
      initReflection();
    } catch(NoSuchMethodException e) {
      throw new CantPerformException(e);
    } catch(NoSuchFieldException e){
      throw new CantPerformException(e);
    } catch(SecurityException e) {
      throw new CantPerformException(e);
    } catch(InvocationTargetException e) {
      throw new CantPerformException(e);
    } catch(IllegalAccessException e) {
      throw new CantPerformException(e);
    } catch(ClassCastException e) {
      throw new CantPerformException(e);
    }
  }

  private void initReflection()
    throws NoSuchMethodException, NoSuchFieldException, SecurityException,
    InvocationTargetException, IllegalAccessException, ClassCastException
  {
    final Class<DefaultListSelectionModel> dlsm = DefaultListSelectionModel.class;

    final Field valueField = dlsm.getDeclaredField("value");
    valueField.setAccessible(true);
    myValue = (BitSet)valueField.get(this);

    mySetState = dlsm.getDeclaredMethod("setState", int.class, boolean.class);
    mySetState.setAccessible(true);
    mySetState.invoke(this, 0, false); // warm-up

    myUpdateLeadAnchor = dlsm.getDeclaredMethod("updateLeadAnchorIndices", int.class, int.class);
    myUpdateLeadAnchor.setAccessible(true);
    myUpdateLeadAnchor.invoke(this, -1, -1); // warm-up

    myFireValueChanged = dlsm.getDeclaredMethod("fireValueChanged");
    myFireValueChanged.setAccessible(true);
    myFireValueChanged.invoke(this); // warm-up
  }

  /**
   * Adapted from DefaultListSelectionModel.
   */
  public void insertIndexInterval(int index, int length, boolean before) {
    /* The first new index will appear at insMinIndex and the last
     * one will appear at insMaxIndex
     */
    final int insMinIndex = (before) ? index : index + 1;
    final int insMaxIndex = (insMinIndex + length) - 1;

    /* Right shift the entire bitset by length, beginning with
     * index-1 if before is true, index+1 if it's false (i.e. with
     * insMinIndex).
     */
    for(int i = getMaxSelectionIndex(); i >= insMinIndex; i--) {
      setState(i + length, myValue.get(i));
    }

    /* Will add the new items to the selection if an existing selection
     * would otherwise be broken.
     */
    final int above = insMinIndex - 1;
    final int below = insMaxIndex + 1;
    final boolean setInsertedValues =
      getSelectionMode() != SINGLE_SELECTION &&
        above >= 0 && myValue.get(above) && myValue.get(below);

    for(int i = insMinIndex; i <= insMaxIndex; i++) {
      setState(i, setInsertedValues);
    }

    int leadIndex = getLeadSelectionIndex();
    if(leadIndex > index || (before && leadIndex == index)) {
      leadIndex += length;
    }

    int anchorIndex = getAnchorSelectionIndex();
    if(anchorIndex > index || (before && anchorIndex == index)) {
      anchorIndex += length;
    }

    if(leadIndex != getLeadSelectionIndex() || anchorIndex != getAnchorSelectionIndex()) {
      updateLeadAnchorIndices(anchorIndex, leadIndex);
    }

    fireValueChanged();
  }

  private void setState(int index, boolean selected) {
    invoke(mySetState, index, selected);
  }

  private void updateLeadAnchorIndices(int anchor, int lead) {
    invoke(myUpdateLeadAnchor, anchor, lead);
  }

  private void fireValueChanged() {
    invoke(myFireValueChanged);
  }

  private void invoke(Method m, Object... args) {
    try {
      m.invoke(this, args);
    } catch(IllegalAccessException e) {
      // we've invoked it for warm-up in initReflection(), haven't we?
      assert false : e;
    } catch(InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if(cause instanceof RuntimeException) {
        throw (RuntimeException)cause;
      }
      if(cause instanceof Error) {
        throw (Error)cause;
      }
      Log.warn(cause);
      assert false : cause;
    }
  }
}
