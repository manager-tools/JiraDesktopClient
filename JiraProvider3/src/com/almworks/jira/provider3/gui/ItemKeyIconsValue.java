package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.util.ModelKeyUtils;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.ColumnTooltipProvider;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.EmptyIcon;
import com.almworks.util.ui.RowIcon;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

import static com.almworks.util.collections.Containers.constF;
import static com.almworks.util.collections.Functional.*;
import static org.almworks.util.Collections15.arrayList;

/**
 * Value class for columns that contain multiple {@link ItemKey#getIcon() ItemKey icons}.
 * */
public class ItemKeyIconsValue<S> implements CanvasRenderable, ColumnTooltipProvider<S>, Comparable<ItemKeyIconsValue>, Comparator<S> {
  private final S mySource;
  private final List<Pair<String, Function<S, ItemKey>>> myValueGetters;
  private final Function<List<Icon>, RowIcon> myRowIconCreator;
  private final Condition<CellState> myClearSelectionCondition;

  private static final int DEFAULT_WIDTH = 16;
  private static final EmptyIcon DEFAULT_EMPTY_ICON = new EmptyIcon(DEFAULT_WIDTH, 1);

  private ItemKeyIconsValue(S source, List<Pair<String, Function<S, ItemKey>>> valueGetters, Function<List<Icon>, RowIcon> rowIconCreator, Condition<CellState> clearSelectionCondition) {
    mySource = source;
    myValueGetters = valueGetters;
    myRowIconCreator = rowIconCreator;
    myClearSelectionCondition = clearSelectionCondition;
  }

  /** Renders icon that occupies the left part of the cell without gaps. The renderer does not attempt to clear selection. Use if you are going to append other information in that cell as well. */
  public static <S> ItemKeyIconsValue<S> createPartialCell(S source, List<Pair<String, Function<S, ItemKey>>> valueGetters) {
    return new ItemKeyIconsValue<S>(source, valueGetters, RowIconCreator.NO_GAPS, Condition.<CellState>never());
  }

  public static <S> ColumnTooltipProvider<S> iconTooltip(List<Pair<String, Function<S, ItemKey>>> valueGetters, Function<List<Icon>, RowIcon> rowIconCreator) {
    return new ItemKeyIconsValue<S>(null, valueGetters, rowIconCreator, Condition.<CellState>never());
  }

  public static <S> Comparator<S> comparator(List<Pair<String, Function<S, ItemKey>>> valueGetters) {
    return new ItemKeyIconsValue<S>(null, valueGetters, constF((RowIcon)null).<List<Icon>>f(), Condition.<CellState>never());
  }

  @Override
  public void renderOn(Canvas canvas, CellState state) {
    if (myClearSelectionCondition.isAccepted(state)) {
      canvas.setCanvasBackground(state.getDefaultBackground());
    }
    canvas.setIcon(getIcon());
  }

  @Nullable
  private RowIcon getIcon() {
    return getIcon(mySource);
  }

  private RowIcon getIcon(S element) {
    List<Icon> icons = arrayList();
    for (Pair<String, Function<S, ItemKey>> valueGetter : myValueGetters) {
      ItemKey key = valueGetter.getSecond().invoke(element);
      Icon icon = key != null ? key.getIcon() : null;
      icons.add(icon != null ? icon : DEFAULT_EMPTY_ICON);
    }
    return icons.isEmpty() ? null : myRowIconCreator.invoke(icons);
  }

  @Override
  public String getTooltip(CellState cellState, S element, Point cellPoint, Rectangle cellRect) {
    RowIcon icon = getIcon(element);
    if (icon == null) return null;
    int keyIdx = icon.getIconIndex(cellPoint.x);
    return keyIdx >= 0 && keyIdx < myValueGetters.size() ? getTooltip(myValueGetters.get(keyIdx), element) : null;
  }

  private static <S> String getTooltip(Pair<String, Function<S, ItemKey>> valueGetter, S element) {
    String kind = valueGetter.getFirst();
    ItemKey value = valueGetter.getSecond().invoke(element);
    return Util.NN(kind) + (value != null ? (kind != null ? ": " : "") + value.getDisplayName() : "");
  }

  @Override
  public int compareTo(ItemKeyIconsValue o) {
    if (o == null || o.myValueGetters == null) return -1;
    int mySz = myValueGetters.size();
    int yrSz = o.myValueGetters.size();
    int i;
    int iend = Math.min(mySz, yrSz);
    for (i = 0; i < iend; ++i) {
      int comp = ItemKey.COMPARATOR.compare(getValue(i), o.getValue(i));
      if (comp != 0)
        return comp;
    }
    return i < mySz ? 1 : i < yrSz ? -1 : 0;
  }

  private ItemKey getValue(int idx) {
    return idx >= 0 && idx < myValueGetters.size() ? myValueGetters.get(idx).getSecond().invoke(mySource) : null;
  }

  @Override
  public int compare(S o1, S o2) {
    for (Pair<String, Function<S, ItemKey>> pair : myValueGetters) {
      Function<S, ItemKey> getter = pair.getSecond();
      int comp = ItemKey.COMPARATOR.compare(getter.invoke(o1), getter.invoke(o2));
      if (comp != 0) return comp;
    }
    return 0;
  }

  public static int getTablePxWidth(int nIcons) {
    List<Icon> icons = arrayList(repeat((Icon) null, nIcons));
    // The cast is necessary to circumvent javac bug http://bugs.sun.com/view_bug.do?bug_id=6522780
    return ((Function<List<Icon>, RowIcon>)RowIconCreator.ITEM_TABLE).invoke(icons).getIconWidth();
  }

  public static Convertor<LoadedItem, ItemKeyIconsValue<LoadedItem>> convertor(final List<Pair<DBStaticObject, DBStaticObject>> key_cols) {
    return new MyConvertor(key_cols);
  }

  public static enum RowIconCreator implements Function<List<Icon>, RowIcon> {
    ITEM_TABLE {
      @Override
      public RowIcon invoke(List<Icon> icons) {
        return RowIcon.create(DEFAULT_WIDTH, 0, 2, 5, 5, icons);
      }
    },
    NO_GAPS {
      @Override
      public RowIcon invoke(List<Icon> icons) {
        return RowIcon.create(0, 0, icons);
      }
    }
  }


  private static class MyConvertor extends Convertor<LoadedItem, ItemKeyIconsValue<LoadedItem>> {
    private final List<Pair<DBStaticObject, DBStaticObject>> myKey_cols;

    public MyConvertor(List<Pair<DBStaticObject, DBStaticObject>> key_cols) {
      myKey_cols = key_cols;
    }

    @Override
    public ItemKeyIconsValue convert(LoadedItem value) {
      List<Pair<String, Function<LoadedItem, ItemKey>>> mkGetters = arrayList();
      Function<LoadedItem, PropertyMap> getValues = cov(ItemWrapper.GET_LAST_DB_VALUES.fun());
      GuiFeaturesManager features = LoadedItemUtils.getFeatures(value);
      if (features != null) {
        for (Pair<DBStaticObject, DBStaticObject> key_col : myKey_cols) {
          LoadedModelKey<ItemKey> mk = features.findScalarKey(key_col.getFirst(), ItemKey.class);
          TableColumnAccessor<LoadedItem,?> column = features.getColumn(key_col.getSecond());
          if (mk != null && column != null) mkGetters.add(Pair.create(column.getName(), compose(ModelKeyUtils.getModelKeyValue(mk), getValues)));
          else LogHelper.error(key_col);
        }
      }
      Condition<CellState> leftmost = new Condition<CellState>() {
        @Override
        public boolean isAccepted(CellState state) {
       // todo kludge: knowledge that there's only 1 pinned column on the left which does not have selection
          return state.getCellColumn() < 2;
        }
      };
      return new ItemKeyIconsValue(value, mkGetters, RowIconCreator.ITEM_TABLE, leftmost);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      MyConvertor other = Util.castNullable(MyConvertor.class, obj);
      return other != null && Util.equals(myKey_cols, other.myKey_cols);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myKey_cols) ^ MyConvertor.class.hashCode();
    }
  }
}
