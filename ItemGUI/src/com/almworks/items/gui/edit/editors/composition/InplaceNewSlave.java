package com.almworks.items.gui.edit.editors.composition;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.PerItemTransactionCache;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Composite implementation. This allow to create new slave item when editing master item. The new slave can be edited with
 * several editors. This implementation creates nested model for slave item editors.
 */
public class InplaceNewSlave extends NestedModelEditor {
  private final ItemCreator myCreator;
  private final DBAttribute<Long> myMasterAttribute;
  private final List<FieldEditor> myEditors;
  private final PerItemTransactionCache<Long> myNewSlaveItem;

  protected InplaceNewSlave(NameMnemonic labelText, ItemCreator slaveCreator, DBAttribute<Long> masterAttribute, List<? extends FieldEditor> editors) {
    super(labelText);
    myCreator = slaveCreator;
    myMasterAttribute = masterAttribute;
    myEditors = Collections15.arrayList(editors);
    myNewSlaveItem = PerItemTransactionCache.create(labelText + "/newItem");
  }

  public static InplaceNewSlave addNewWhenEditMaster(NameMnemonic labelText, ItemCreator slaveCreator,
    DBAttribute<Long> masterAttribute, FieldEditor... editors) {
    return new InplaceNewSlave(labelText, slaveCreator, masterAttribute, Collections15.unmodifiableListCopy(editors));
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    return Pair.create(DefaultEditModel.Child.newItem(parent, myCreator), myEditors);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    commitNew(context);
  }

  /**
   * Creates new slave commits it and returns
   * @return null if the model missing nested (the editor is poorly prepared and nothing is actually edited).<br>
   * Not null - just created new slave item
   */
  @Nullable
  public ItemVersionCreator commitNew(CommitContext master) throws CancelCommitException {
    EditItemModel slaveModel = getNestedModel(master.getModel());
    if (slaveModel == null) return null;
    CommitContext slaveCommit = master.newSubContext(slaveModel);
    myNewSlaveItem.put(master.readTrunk(), slaveCommit.getItem());
    ItemVersionCreator slaveItem = slaveCommit.getCreator();
    slaveItem.setValue(myMasterAttribute, master.getItem());
    myCreator.setupNewItem(slaveModel, slaveItem);
    slaveCommit.commitEditors(null);
    return slaveItem;
  }

  /**
   * The item created during commit. This method expects that the editor creates only one slave for single master during commit.<br>
   * This assumption is obviously true if the editor is not reused in several models (in case one model
   * contains several nested models). Or if nested models (where the editor is reused) edits different master items.
   * @param master master of created slave.
   * @return created slave item or 0 if no item is created (or no info is stored in the transaction)
   */
  public long getCreatedItem(ItemVersion master) {
    Long newItem = myNewSlaveItem.get(master);
    return newItem != null && newItem > 0 ? newItem : 0;
  }
}
