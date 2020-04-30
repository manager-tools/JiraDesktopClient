package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.DelegatingFieldEditor;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;

class DefaultSingleDnDChange extends DefaultDnDChange<ItemKey> {
  private final DBAttribute<Long> myAttribute;

  public DefaultSingleDnDChange(DBAttribute<Long> attribute, DnDVariants variants, long modelKey, long constraint, Applicability applicability) {
    super(variants, modelKey, constraint, applicability);
    myAttribute = attribute;
  }

  @Override
  protected void prepare(DnDApplication application, TargetValues target, ModelKey<ItemKey> modelKey, BaseEnumConstraintDescriptor constraint) {
    NameMnemonic labelText = NameMnemonic.rawText(constraint.getDisplayName());
    boolean allMatches = true;
    for (ItemWrapper wrapper : application.getItems()) {
      ItemKey value = wrapper.getModelKeyValue(modelKey);
      if (target.matches(value)) continue;
      allMatches = false;
      break;
    }
    if (allMatches) return;
    Long single = target.getSinglePositive();
    ItemKey empty = getMissingKey(constraint);
    if (single == null) application.addChangeEditor(new Editor(labelText, myAttribute, getVariants(), target, empty), true);
    else if (checkSinglePositive(application, labelText.getText(), single, empty))
      application.addChangeEditor(new BaseNotification.Single(labelText, getVariants(), myAttribute, single, empty), false);
  }

  protected LoadedModelKey<ItemKey> getModelKey(DnDApplication application, long modelKey) {
    return application.singleModelKey(modelKey);
  }

  @Override
  protected DBAttribute<?> getAttribute() {
    return myAttribute;
  }

  private static class Editor extends DelegatingFieldEditor<DropdownEnumEditor> implements DnDFieldEditor {
    private final DBAttribute<Long> myAttribute;
    private final NameMnemonic myLabelText;
    private final DnDVariants myVariants;
    private final TargetValues myTarget;
    private final CanvasRenderable myEmpty;

    private Editor(NameMnemonic labelText, DBAttribute<Long> attribute, DnDVariants variants, TargetValues target, CanvasRenderable empty) {
      myLabelText = labelText;
      myAttribute = attribute;
      myVariants = variants;
      myTarget = target;
      myEmpty = empty;
    }

    @Override
    public String getDescription(ActionContext context, boolean full) throws CantPerformException {
      return myLabelText.getText();
    }

    @Override
    protected DropdownEnumEditor getDelegate(VersionSource source, EditModelState model) {
      myVariants.prepare(source, model);
      EnumVariantsSource variants = myVariants.filterVariants(model, myTarget.getVariantsFilter(false));
      if (variants == null) return null;
      DropdownEditorBuilder builder = new DropdownEditorBuilder()
        .overrideRenderer(myVariants.getRenderer(model))
        .setAttribute(myAttribute)
        .setLabelText(myLabelText)
        .setVariants(variants)
        .setVerify(true);
      if (myEmpty != null && myTarget.isAllowsEmpty()) builder.setNullRenderable(myEmpty);
      return builder.createFixed();
    }
  }
}
