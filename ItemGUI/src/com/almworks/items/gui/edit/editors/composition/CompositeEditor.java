package com.almworks.items.gui.edit.editors.composition;

import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;

import java.util.List;

public class CompositeEditor extends NestedModelEditor {
  private final List<FieldEditor> myEditors;

  private CompositeEditor(List<FieldEditor> editors) {
    super(NameMnemonic.EMPTY);
    myEditors = editors;
  }

  public static FieldEditor create(FieldEditor ... editors) {
    return new CompositeEditor(Collections15.unmodifiableListCopy(editors));
  }

  public static FieldEditor create(List<? extends FieldEditor> editors) {
    return new CompositeEditor(Collections15.unmodifiableListCopy(editors));
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare)
  {
    if (parent.isNewItem()) return null;
    LongList items = parent.getEditingItems();
    return Pair.create(DefaultEditModel.Child.editItems(parent, items, true), myEditors);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    commitNested(context);
  }
}
