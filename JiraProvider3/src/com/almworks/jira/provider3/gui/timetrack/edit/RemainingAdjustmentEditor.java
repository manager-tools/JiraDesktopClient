package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

class RemainingAdjustmentEditor extends BaseFieldEditor {
  static final int ADJUST_AUTO = 0;
  static final int ADJUST_DONT_CHANGE = 1;
  static final int ADJUST_SET = 2;

  private final TypedKey<Integer> ADJUST_KIND;

  public RemainingAdjustmentEditor() {
    super(NameMnemonic.rawText("Remaining Time"));
    ADJUST_KIND = TypedKey.create("estimate/adjustKind");
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    model.registerEditor(this);
    model.putHint(ADJUST_KIND, ADJUST_AUTO);
    // todo support edit work log
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    LogHelper.error("Not implemented");
    return Collections15.emptyList();
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return false;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return true;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return false;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  public int getAdjust(EditItemModel model) {
    return Util.NN(model.getValue(ADJUST_KIND), -1);
  }

  public boolean isAdjustSetTo(EditItemModel model) {
    return getAdjust(model) == ADJUST_SET;
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {}

  @Override
  public void commit(CommitContext context) {
    LogHelper.error("Should not happen");
  }

  public void attach(Lifespan life, final EditItemModel issue, final JComponent remainingField, ButtonGroup group,
    JRadioButton adjustAutomatically, JRadioButton doNotChange, JRadioButton setTo)
  {
    int adjust = getAdjust(issue);
    ButtonModel buttonModel;
    final ButtonModel autoModel = adjustAutomatically.getModel();
    final ButtonModel noChangeModel = doNotChange.getModel();
    final ButtonModel setModel = setTo.getModel();
    switch (adjust) {
    case ADJUST_AUTO: buttonModel = autoModel; break;
    case ADJUST_DONT_CHANGE: buttonModel = noChangeModel; break;
    case ADJUST_SET: buttonModel = setModel; break;
    default: LogHelper.error("Missing adjust value", adjust); return;
    }
    group.setSelected(buttonModel, true);
    ChangeListener listener = new ChangeListener() {
      @Override
      public void onChange() {
        int value;
        if (autoModel.isSelected()) value = ADJUST_AUTO;
        else if (noChangeModel.isSelected()) value = ADJUST_DONT_CHANGE;
        else if (setModel.isSelected()) value = ADJUST_SET;
        else return;
        issue.putValue(ADJUST_KIND, value);
        remainingField.setEnabled(value == ADJUST_SET);
      }
    };
    UIUtil.addChangeListener(life, listener, autoModel);
    UIUtil.addChangeListener(life, listener, noChangeModel);
    UIUtil.addChangeListener(life, listener, setModel);
    remainingField.setEnabled(adjust == ADJUST_SET);
  }
}
