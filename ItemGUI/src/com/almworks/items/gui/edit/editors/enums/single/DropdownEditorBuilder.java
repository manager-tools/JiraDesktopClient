package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Mandatory:<br>
 * {@link #setAttribute(com.almworks.items.api.DBAttribute)}<br>
 * {@link #setVariants(com.almworks.items.gui.edit.editors.enums.EnumVariantsSource)}<br>
 * {@link #setLabelText(com.almworks.util.text.NameMnemonic)}<br><br>
 * Defaults:
 * 1. Leaves empty selection if edited item has no value (to change {@link #setDefaultItem(com.almworks.items.gui.edit.editors.enums.DefaultItemSelector)})<br>
 * 2. Null (empty) selection is rendered with empty text (to change {@link #setNullPresentation(String)} or
 * {@link #setNullRenderable(com.almworks.util.components.CanvasRenderable)})<br>
 * 3. Does not populate variants list with null until null renderable is specified (to change {@link #setAppendNull(boolean)})<br>
 * 4. Does not verify for not empty value. Not empty value is valid if {@link #myAppendNull} is set. To force check for not null
 * forebid {@link #setAppendNull(boolean)} and set {@link #setVerify(boolean)}.<br><br>
 * May allow new enum item creation if {@link EnumItemCreator} is specified via
 * {@link #setAttribute(com.almworks.items.api.DBAttribute, com.almworks.items.gui.edit.editors.enums.EnumItemCreator)}<br>
 */
public class DropdownEditorBuilder {
  private EnumVariantsSource myVariants;
  private DefaultItemSelector myDefaultItem = DefaultItemSelector.ALLOW_EMPTY;
  private CanvasRenderable myNullRenderable = Renderers.text("");
  private DBAttribute<Long> myAttribute;
  private boolean myAppendNull = false;
  private EnumItemCreator myCreator;
  private NameMnemonic myLabelText = null;
  private boolean myVerify = false;
  private boolean myForbidIllegal = false;
  @Nullable
  private CanvasRenderer<ItemKey> myNNRenderer;

  public DropdownEditorBuilder setVariants(EnumVariantsSource variants) {
    myVariants = variants;
    return this;
  }

  public DropdownEditorBuilder setStaticVariants(ItemReference enumType, String recentConfigId) {
    return setVariants(ConnectionVariants.createStatic(enumType, recentConfigId));
  }

  public DropdownEditorBuilder setAttribute(DBAttribute<Long> attribute) {
    myAttribute = attribute;
    return this;
  }

  public DropdownEditorBuilder setAttribute(DBAttribute<Long> attribute, EnumItemCreator creator) {
    myAttribute = attribute;
    myCreator = creator;
    return this;
  }

  public DropdownEditorBuilder setDefaultItem(DefaultItemSelector defaultItem) {
    myDefaultItem = defaultItem;
    return this;
  }

  /**
   * If true the editor commit null is selected value rejected by {@link EnumVariantsSource#isValidValueFor(com.almworks.items.gui.edit.CommitContext, long) variants}
   */
  public DropdownEditorBuilder setForbidIllegalCommit(boolean forbidIllegal) {
    myForbidIllegal = forbidIllegal;
    return this;
  }

  public DropdownEditorBuilder setNullRenderable(CanvasRenderable nullRenderable) {
    myNullRenderable = nullRenderable;
    myAppendNull = true;
    return this;
  }

  /**
   * Optional setter. Allows to override default item key renderer (provided by enum type) with editor-specific renderer. This renderer is for rendering not null existing items.
   * @param renderer overriding renderer
   * @return the builder
   * @see #setNullRenderable(com.almworks.util.components.CanvasRenderable)
   */
  public DropdownEditorBuilder overrideRenderer(@Nullable CanvasRenderer<ItemKey> renderer) {
    myNNRenderer = renderer;
    return this;
  }

  public DropdownEditorBuilder setAppendNull(boolean appendNull) {
    myAppendNull = appendNull;
    return this;
  }

  public DropdownEditorBuilder setVerify(boolean verify) {
    myVerify = verify;
    return this;
  }

  public DropdownEditorBuilder setLabelText(NameMnemonic labelText) {
    myLabelText = labelText;
    return this;
  }

  public DropdownEditorBuilder setNullPresentation(String text) {
    return setNullRenderable(new CanvasRenderable.TextRenderable(Font.ITALIC, text));
  }

  public SingleEditableEnumEditor createEditable() {
    return new SingleEditableEnumEditor(myLabelText, myVariants, myDefaultItem, myNullRenderable, myNNRenderer, myAppendNull,
      createValueKey(), myVerify);
  }

  public DropdownEnumEditor createFixed() {
    return new DropdownEnumEditor(myLabelText, myVariants, myDefaultItem, myNullRenderable, myNNRenderer, myAppendNull,
      createValueKey(), myVerify);
  }

  public BaseSingleEnumEditor create() {
    return myCreator != null ? createEditable() : createFixed();
  }

  public BaseSingleEnumEditor createRadioButtonList() {
    return new RadioButtonListEditor(myLabelText, myVariants, myDefaultItem, myNullRenderable, myNNRenderer, myAppendNull,
      createValueKey(), myVerify);
  }

  public BaseSingleEnumEditor createCascade(TypedKey<LoadedItemKey> parentKey, DBAttribute<String> nameAttr) {
    return new CascadeEditor(myLabelText, myVariants, myDefaultItem, myNullRenderable, myAppendNull,
      createValueKey(), myVerify, parentKey, nameAttr);
  }

  private EnumValueKey createValueKey() {
    return new EnumValueKey(myAttribute, myCreator, myForbidIllegal);
  }
}
