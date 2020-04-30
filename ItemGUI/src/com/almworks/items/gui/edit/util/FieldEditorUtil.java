package com.almworks.items.gui.edit.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class FieldEditorUtil {

  /**
   * Collects pair of common and not common value items. Supported cases: <br>
   * 1  there is not empty common subset, any item's value contains the common subset <br>
   * 1a Some items has additional uncommon values <br>
   * 2  the common value subset is empty<br>
   * 2a all items has empty value<br>
   * @return null means the common subset is empty and at least one item has not empty value<br>
   * Not null result contains two not null sets [commonValues, notCommonValues].
   */
  @Nullable
  public static Pair<LongList, LongList> getCommonSet(VersionSource source, DBAttribute<Set<Long>> attribute, LongList items) {
    if (items == null || items.isEmpty()) return Pair.create(LongList.EMPTY, LongList.EMPTY);
    LongArray common = new LongArray();
    LongSetBuilder notCommon = new LongSetBuilder();
    for (int i = 0; i < source.readItems(items).size(); i++) {
      ItemVersion item = source.readItems(items).get(i);
      Set<Long> valueItems = Util.NN(item.getValue(attribute), Collections15.<Long>emptySet());
      if (i == 0) {
        for (Long valueItem : valueItems) if (valueItem != null && valueItem > 0) common.add(valueItem);
        continue;
      }
      if (common.isEmpty() && valueItems.isEmpty()) continue;
      for (int j = 0; j < common.size(); j++) {
        long commonItem = common.get(j);
        if (!valueItems.contains(commonItem)) {
          common.removeAt(j);
          notCommon.add(commonItem);
          j--;
        }
      }
      if (common.isEmpty()) return null;
      for (Long valueItem : valueItems)
        if (valueItem != null && valueItem > 0 && !common.contains(valueItem)) notCommon.add(valueItem);
    }
    common.sortUnique();
    return Pair.<LongList, LongList>create(common, notCommon.commitToArray());
  }

  public static void assertNotChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues, DBAttribute<?> attribute, FieldEditor editor) {
    LongList items = model.getEditingItems();
    for (LongIterator it : items) {
      ItemValues values = newValues.get(it.value());
      if (values != null && values.indexOf(attribute) >= 0) {
        LogHelper.error("Not supported", editor, newValues);
        return;
      }
    }
  }

  /**
   * @return true iff the model edit only new items (not uploaded to server), false if at least one editing item has server version.
   */
  public static boolean isNewItem(VersionSource source, EditModelState model) {
    if (model.isNewItem()) return true;
    for (ItemVersion issue : source.readItems(model.getEditingItems())) if (issue.getSyncState() != SyncState.NEW) return false;
    return true;
  }

  private static final TypedKey<List<Pair<FieldEditor, JComponent>>> REGISTERED_COMPONENTS = TypedKey.create("registeredComponents");
  public static void registerComponent(EditModelState model, FieldEditor editor, JComponent component) {
    List<Pair<FieldEditor, JComponent>> list = model.getValue(REGISTERED_COMPONENTS);
    if (list == null) {
      list = Collections15.arrayList();
      model.putHint(REGISTERED_COMPONENTS, list);
    }
    list.add(Pair.create(editor, component));
  }
  
  public static List<JComponent> getRegisteredComponents(EditModelState model, FieldEditor editor) {
    List<Pair<FieldEditor, JComponent>> list = model.getValue(REGISTERED_COMPONENTS);
    if (list == null) return Collections.emptyList();
    ArrayList<JComponent> result = Collections15.arrayList();
    for (Pair<FieldEditor, JComponent> pair : list) {
      if (editor == pair.getFirst()) result.add(pair.getSecond());
    }
    return result;
  }

  /**
   * Creates components for all editors and returns them via out parameters (named targetXXX)
   * @param targetComponents list of all component in creation order (components for first editor are first)
   * @param targetByEditor map components by original editor
   */
  public static void createComponents(Lifespan life, EditItemModel model, List<? extends FieldEditor> editors, @Nullable List<ComponentControl> targetComponents, @Nullable Map<FieldEditor, List<ComponentControl>> targetByEditor) {
    if (editors == null) editors = Collections15.emptyList();
    for (FieldEditor editor : editors) {
      List<? extends ComponentControl> components = editor.createComponents(life, model);
      if (targetComponents != null) targetComponents.addAll(components);
      if (targetByEditor != null) targetByEditor.put(editor, targetComponents);
    }
  }

  public static void setupTopWhitePanel(Lifespan life, JComponent topComponent) {
    EditFeature.ERROR_BORDER.putClientValue(topComponent, true);
    topComponent.setBorder(UIUtil.EDITOR_PANEL_BORDER);
    new DocumentFormAugmentor().augmentForm(life, topComponent, false);
  }

  public static void setupTopScrollPane(Lifespan life, JComponent topComponent) {
    boolean needErrorBorder;
    if (Aero.isAero()) {
      Aero.cleanScrollPaneBorder(topComponent);
      needErrorBorder = true;
    } else if (Aqua.isAqua()) {
      Aqua.cleanScrollPaneBorder(topComponent);
      Aqua.cleanScrollPaneResizeCorner(topComponent);
      Aqua.disableMnemonics(topComponent);
      needErrorBorder = true;
    } else needErrorBorder = false;
    EditFeature.ERROR_BORDER.putClientValue(topComponent, needErrorBorder);
    new DocumentFormAugmentor().augmentForm(life, topComponent, true);
  }

  /**
   * Filters out enum values not accepted by {@link EnumVariantsSource#isValidValueFor(com.almworks.items.gui.edit.CommitContext, long) enum variants}.
   * @param context committing item
   * @param value enum values to filter
   * @return filtered enum variants
   */
  public static LongList filterEnumValues(CommitContext context, @Nullable EnumVariantsSource variants, LongList value) {
    if (variants == null) return value;
    if (value == null || value.isEmpty()) return value;
    LongArray result = new LongArray();
    for (LongIterator cursor : value) {
      long itemValue = cursor.value();
      if (variants.isValidValueFor(context, itemValue)) result.add(itemValue);
    }
    return filterOutRemoved(context.getReader(), result);
  }

  public static LongList filterOutRemoved(DBReader reader, LongList values) {
    if (values == null || values.isEmpty()) return values;
    LongArray copy = null;
    for (int i = 0; i < values.size(); i++) {
      long item = values.get(i);
      if (SyncUtils.isRemoved(SyncUtils.readTrunk(reader, item))) {
        if (copy == null) {
          copy = new LongArray();
          copy.addAll(values.subList(0, i));
        }
      } else if (copy != null) copy.add(item);
    }
    return copy != null ? copy : values;
  }

  public static boolean hasDataToCommit(EditItemModel model, List<FieldEditor> editors) {
    if (model == null) return false;
    for (FieldEditor editor : editors) if (editor.hasDataToCommit(model)) return true;
    return false;
  }
}
