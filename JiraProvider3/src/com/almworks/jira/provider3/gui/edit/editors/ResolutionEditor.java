package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.composition.EnableWrapEditor;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.FilteringEnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.single.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Resolution;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResolutionEditor implements SingleEnumDefaultValue {
  private static final EnumVariantsSource NO_CHECK_VARIANTS = ConnectionVariants.createStatic(Resolution.ENUM_TYPE, "resolution");
  private static final EnumVariantsSource CHECKED_VARIANTS = new FilteringEnumVariantsSource(NO_CHECK_VARIANTS, Resolution.IS_UNRESOLVED.not());

  public static final DropdownEnumEditor NO_CHECK_EDITOR = buildEnumEditor(false).createFixed();

  /** Basic editor: no default value, not mandatory */
  public static final DropdownEnumEditor BASIC_EDITOR = buildEnumEditor(true)
    // When current value is UNRESOLVED - it is shown as current selection (in spite of it is absent in variants). Do not show ChooseResolution as variant (so next line is comment out)
//    .setNullRenderable(new CanvasRenderable.TextRenderable(Font.ITALIC, "<Choose Resolution>"))
    .setVerify(true)
    .createFixed();

  private static DropdownEditorBuilder buildEnumEditor(boolean checkUnresolved) {
    return new DropdownEditorBuilder()
      .setVariants(checkUnresolved ? CHECKED_VARIANTS : NO_CHECK_VARIANTS)
      .setAttribute(Issue.RESOLUTION)
      .setAppendNull(false)
      .setLabelText(NameMnemonic.parseString("Res&olution"));
  }

  public static final FieldEditor MANDATORY_EDITOR;
  static {
    SingleEnumWithDefaultValueEditor<DropdownEnumEditor> editor = SingleEnumWithDefaultValueEditor.create(BASIC_EDITOR, new ResolutionEditor());
    MANDATORY_EDITOR = EnableWrapEditor.alwaysEnabled(editor);
  }

  private final TypedKey<Long> myFixedKey = TypedKey.create("resolution/fixed");

  private ResolutionEditor() {
  }

  @Override
  public void prepare(VersionSource source, EditItemModel model) {
    long connection = EngineConsts.getConnectionItem(model);
    if (connection <= 0) return;
    long candidate = findDefaultResolution(source, connection);
    if (candidate <= 0) return;
    EngineConsts.ensureGuiFeatureManager(source, model);
    model.putHint(myFixedKey, candidate);
  }

  private static final List<String> DEFAULT_RESOLUTION_NAMES = Arrays.asList("fixed", "behoben", "corrigé", "修正済み", "solucionada");
  private static final String SETTING_DEFAULT_RESOLUTION = "workflow.resolve.defaultResolution";

  private static long findDefaultResolution(VersionSource source, long connection) {
    String defaultName = Env.getString(SETTING_DEFAULT_RESOLUTION, "").trim();
    List<String> names;
    if (defaultName.isEmpty()) names = DEFAULT_RESOLUTION_NAMES;
    else names = Collections.singletonList(defaultName);
    BoolExpr<DP> query = DPEquals.create(SyncAttributes.CONNECTION, connection).and(DPEqualsIdentified.create(DBAttribute.TYPE, Resolution.DB_TYPE));
    long candidate = 0;
    for (ItemVersion resolution : source.readItems(source.getReader().query(query).copyItemsSorted())) {
      for (String name : names) {
        if (name.equalsIgnoreCase(resolution.getValue(Resolution.NAME))) {
          if (candidate == 0) candidate = resolution.getItem();
          else {
            LogHelper.debug("Two candidates for default resolution", source.forItem(candidate).getValue(Resolution.NAME), resolution.getValue(Resolution.NAME));
            return 0;
          }
        }
      }
    }
    return candidate;
  }

  @Override
  public boolean isEnabled(EditItemModel model) {
    return getValue(model) != null;
  }

  @Override
  public Pair<? extends ItemKey, Long> getValue(EditItemModel model) {
    return SingleEnumWithInlineButtonEditor.getItemValue(model, myFixedKey, Resolution.ENUM_TYPE);
  }
}
