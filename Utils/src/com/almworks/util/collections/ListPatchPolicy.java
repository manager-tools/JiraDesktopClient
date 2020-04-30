package com.almworks.util.collections;

public enum ListPatchPolicy {
  /**
   * ListPatch will produce steps that affect only one node at a time (hard-coded now),
   * and based on the contract that all items in a list are different. 
   */
  SINGLE_UNREPEATING_ITEMS
}
