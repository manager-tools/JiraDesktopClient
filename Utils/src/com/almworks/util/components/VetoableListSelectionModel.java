package com.almworks.util.components;

import com.almworks.util.model.ScalarModel;

import javax.swing.*;

class VetoableListSelectionModel extends DefaultListSelectionModel {
  private final ScalarModel<Boolean> myVetoModel;

  public VetoableListSelectionModel(ScalarModel<Boolean> vetoModel) {
    myVetoModel = vetoModel;
  }

  public void addSelectionInterval(int index0, int index1) {
    if (!myVetoModel.getValue())
      super.addSelectionInterval(index0, index1);
  }

  public void clearSelection() {
    if (!myVetoModel.getValue())
      super.clearSelection();
  }


  public void insertIndexInterval(int index, int length, boolean before) {
    if (!myVetoModel.getValue())
      super.insertIndexInterval(index, length, before);
  }


  public void moveLeadSelectionIndex(int leadIndex) {
    if (!myVetoModel.getValue())
      super.moveLeadSelectionIndex(leadIndex);
  }


  public void removeIndexInterval(int index0, int index1) {
    if (!myVetoModel.getValue())
      super.removeIndexInterval(index0, index1);
  }


  // implements javax.swing.ListSelectionModel
  public void removeSelectionInterval(int index0, int index1) {
    if (!myVetoModel.getValue())
      super.removeSelectionInterval(index0, index1);
  }


  public void setAnchorSelectionIndex(int anchorIndex) {
    if (!myVetoModel.getValue())
      super.setAnchorSelectionIndex(anchorIndex);
  }


  public void setLeadSelectionIndex(int leadIndex) {
    if (!myVetoModel.getValue())
      super.setLeadSelectionIndex(leadIndex);
  }


  // implements javax.swing.ListSelectionModel
  public void setSelectionInterval(int index0, int index1) {
    if (!myVetoModel.getValue())
      super.setSelectionInterval(index0, index1);
  }
}
