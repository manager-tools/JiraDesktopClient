package com.almworks.items.gui.edit.merge;

import com.almworks.util.L;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

class ApplyVersionAction extends SimpleAction {
  public static final AnAction LOCAL = new ApplyVersionAction(L.actionName("Take &Local Value"), Icons.MERGE_ACTION_APPLY_LOCAL, MergeValue.LOCAL);
  public static final AnAction BASE = new ApplyVersionAction(L.actionName("Take &Original Value Without Changes"), Icons.MERGE_ACTION_APPLY_ORIGINAL, MergeValue.BASE);
  public static final AnAction REMOTE = new ApplyVersionAction(L.actionName("Take &Remote Value"), Icons.MERGE_ACTION_APPLY_REMOTE, MergeValue.REMOTE);

  private final int myVersion;

  ApplyVersionAction(@Nullable String name, @Nullable Icon icon, int version) {
    super(name, icon);
    myVersion = version;
    watchRole(MergeValue.ROLE);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    getChanged(context);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    List<MergeValue> values = getChanged(context);
    for (MergeValue value : values) value.setResolution(myVersion);
  }

  public static List<MergeValue> getChanged(ActionContext context) throws CantPerformException {
    List<MergeValue> values = context.getSourceCollection(MergeValue.ROLE);
    ArrayList<MergeValue> result = Collections15.arrayList();
    for (MergeValue value : values) {
      if (value.isChangeOrConflict()) result.add(value);
    }
    return result;
  }
}
