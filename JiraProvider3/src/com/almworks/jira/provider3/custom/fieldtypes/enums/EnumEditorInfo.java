package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.util.LogHelper;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.text.NameMnemonic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class EnumEditorInfo<T> {
  public static final Type<Long> SINGLE = new Type<Long>() {
    @Override
    protected DBAttribute<Long> readAttribute(ItemVersion field) {
      return BadUtil.readScalarAttribute(Long.class, field.getReader(), field.getItem(), CustomField.ATTRIBUTE);
    }
  };
  public static final Type<Set<Long>> SET = new Type<Set<Long>>() {
    @Override
    protected DBAttribute<Set<Long>> readAttribute(ItemVersion field) {
      final DBAttribute<?> attribute = BadUtil.getAttribute(field.getReader(), field.getValue(CustomField.ATTRIBUTE));
      if (attribute == null)
        return null;
      DBAttribute<Set<Long>> setAttribute = BadUtil.castSetAttribute(Long.class, attribute);
      LogHelper.assertError(setAttribute != null, "Wrong attribute", attribute);
      return setAttribute;
    }
  };

  private final String myName;
  private final DBAttribute<T> myAttribute;
  private final EnumVariantsSource myVariants;
  private final CanvasRenderer<ItemKey> myOverrideRenderer;
  private final DBItemType myType;

  public EnumEditorInfo(String name, DBAttribute<T> attribute, EnumVariantsSource variants, CanvasRenderer<ItemKey> overrideRenderer, DBItemType type) {
    myName = name;
    myAttribute = attribute;
    myVariants = variants;
    myOverrideRenderer = overrideRenderer;
    myType = type;
  }

  public DBItemType getType() {
    return myType;
  }

  public CanvasRenderer<ItemKey> getOverrideRenderer() {
    return myOverrideRenderer;
  }

  public String getName() {
    return myName;
  }

  public DBAttribute<T> getAttribute() {
    return myAttribute;
  }

  public EnumVariantsSource getVariants() {
    return myVariants;
  }

  @NotNull
  public static DropdownEditorBuilder buildDropDown(EnumEditorInfo<Long> info, boolean myForbidIllegalCommit, boolean myVerify, @Nullable EnumItemCreator creator) {
    return new DropdownEditorBuilder()
      .setLabelText(NameMnemonic.rawText(info.myName))
      .setAttribute(info.myAttribute, creator)
      .setVariants(info.myVariants)
      .setDefaultItem(DefaultItemSelector.ALLOW_EMPTY)
      .setForbidIllegalCommit(myForbidIllegalCommit)
      .setNullPresentation("<None>")
      .setVerify(myVerify)
      .overrideRenderer(info.myOverrideRenderer);
  }

  public static abstract class Type<T> {
    @Nullable
    public EnumEditorInfo<T> load(ItemVersion field) {
      DBAttribute<T> attribute = readAttribute(field);
      DBItemType type = BadUtil.getItemType(field.getReader(), field.getValue(CustomField.ENUM_TYPE));
      String id = field.getValue(CustomField.ID);
      String name = field.getValue(CustomField.NAME);
      if (attribute == null || type == null || id == null) return null;
      if (name == null) name = id;
      long enumTypeItem = EnumType.findByDBType(field.getReader(), type);
      if(enumTypeItem <= 0) return null;
      ItemVersion enumType = field.forItem(enumTypeItem);
      CanvasRenderer<ItemKey> overrideRenderer = EnumType.EDITOR_RENDERER.getValue(enumType);
      EnumVariantsSource variants = ConnectionVariants.createDynamic(enumTypeItem, id);
      return new EnumEditorInfo<T>(name, attribute, variants, overrideRenderer, type);
    }

    protected abstract DBAttribute<T> readAttribute(ItemVersion field);
  }
}
