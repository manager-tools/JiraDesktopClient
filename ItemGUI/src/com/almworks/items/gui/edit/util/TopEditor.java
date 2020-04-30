package com.almworks.items.gui.edit.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * Base class to implement {@link EditFeature}s which requires better control over editor. It combines capabilities of feature
 * and its editor in single object to avoid need to implement special top-level editor for such features
 */
public abstract class TopEditor extends NestedModelEditor implements EditFeature {
  public TopEditor(NameMnemonic labelText) {
    super(labelText);
  }

  @Override
  public final void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
    prepareModel(createSource(reader), model, editPrepare);
  }

  protected VersionSource createSource(DBReader reader) {
    return BranchSource.trunk(reader);
  }

  protected final Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createDefaultNestedModel(EditItemModel parent, FieldEditor ... editors) {
    return createDefaultNestedModel(parent, Arrays.asList(editors));
  }

  protected final Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createDefaultNestedModel(EditItemModel parent, List<FieldEditor> editors) {
    LongList issues = parent.getEditingItems();
    DefaultEditModel.Child child = DefaultEditModel.Child.editItems(parent, issues, false);
    return Pair.create(child, editors);
  }

  @Nullable
  @Override
  public final JComponent editModel(Lifespan life, EditItemModel model, Configuration config) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested == null) return null;
    return doEditModel(life, nested, config);
  }

  /**
   * @return see {@link EditFeature#editModel(org.almworks.util.detach.Lifespan, com.almworks.items.gui.edit.EditItemModel, com.almworks.util.config.Configuration)}
   */
  @Nullable
  protected abstract JComponent doEditModel(Lifespan life, EditItemModel model, Configuration config);

  @Override
  public final void commit(CommitContext context) throws CancelCommitException {
    DefaultEditModel.Child nested = getNestedModel(context.getModel());
    if (nested != null) doCommit(context.subContext(nested));
  }

  protected abstract void doCommit(CommitContext childContext) throws CancelCommitException;
}
