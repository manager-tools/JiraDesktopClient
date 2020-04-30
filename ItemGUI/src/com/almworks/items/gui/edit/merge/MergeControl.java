package com.almworks.items.gui.edit.merge;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.DataRole;

import java.util.List;

public interface MergeControl extends Modifiable {
  DataRole<MergeControl> ROLE = DataRole.createRole(MergeControl.class);

  boolean isFiltered();

  void setFiltered(boolean filtered);

  Modifiable getSelectionModifiable();

  List<MergeValue> getAllValues();

  Configuration getConfig(String subset);
}
