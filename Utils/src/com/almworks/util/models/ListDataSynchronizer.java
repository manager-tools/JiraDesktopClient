package com.almworks.util.models;

import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ListDataSynchronizer <E> implements ListDataListener {
  private final ArrayList<E> myImage;
  private final ListModel myListModel;
  private final Detach myDetach;

  public ListDataSynchronizer(ListModel listModel) {
    myImage = new ArrayList<E>();
    myListModel = listModel;
    myImage.addAll(getElementsInRange(0, listModel.getSize() - 1));
    myDetach = UIUtil.addListDataListener(myListModel, this);
  }

  public Detach getDetach() {
    return myDetach;
  }

  public void intervalAdded(ListDataEvent e) {
    List<E> added = getNewElementsInRange(e);
    myImage.addAll(e.getIndex0(), added);
  }

  public void intervalRemoved(ListDataEvent e) {
    getImageSublist(e).clear();
  }

  private List<E> getImageSublist(ListDataEvent e) {
    return myImage.subList(e.getIndex0(), e.getIndex1() + 1);
  }

  public void contentsChanged(ListDataEvent event) {
    if (event.getIndex0() == -1) {
      assert event.getIndex1() == -1;
      return;
    }
    List<E> changed = getImageSublist(event);
    changed.clear();
    changed.addAll(getNewElementsInRange(event));
  }

  protected List<E> getNewElementsInRange(ListDataEvent e) {
    return getElementsInRange(e.getIndex0(), e.getIndex1());
  }

  public List<E> getImageList() {
    return Collections.unmodifiableList(myImage);
  }

  private List<E> getElementsInRange(int index0, int index1) {
    final ListModel listModel = myListModel;
    return getElementsInRange(listModel, index0, index1);
  }

  public static List getElementsInRange(final ListModel listModel, int index0, int index1) {
    ArrayList result = new ArrayList();
    for (int i = index0; i <= index1; i++) {
      result.add(listModel.getElementAt(i));
    }
    return result;
  }

  protected List<E> getOldElementsInRange(ListDataEvent e) {
    ArrayList<E> result = new ArrayList<E>();
    for (int i = e.getIndex0(); i <= e.getIndex1(); i++)
      result.add(myImage.get(i));
    return result;
  }
}
