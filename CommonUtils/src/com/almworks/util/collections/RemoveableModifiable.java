package com.almworks.util.collections;

public interface RemoveableModifiable extends Modifiable {
  void removeChangeListener(ChangeListener listener);
}
