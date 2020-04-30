package com.almworks.util.sfs;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Factory;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.ContextTransfer;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.actions.dnd.DragContext;
import com.almworks.util.ui.actions.dnd.StringListTransferable;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.almworks.util.sfs.StringFilterSet.Kind;

public class StringFilterSetEditor {
  private static final StringFilterRenderer STRING_FILTER_RENDERER = new StringFilterRenderer();
  private static final DataRole<StringFilter> STRING_FILTER_ROLE =
    DataRole.createRole(StringFilter.class, "STRING_FILTER");

  private final AComboBox<Kind> myKindCombo = new AComboBox<Kind>();
  private final ListWithAddRemoveButtons<StringFilter> myFilterListPanel = new ListWithAddRemoveButtons<StringFilter>();
  private final OrderListModel<StringFilter> myFiltersModel = OrderListModel.create();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final Map<Kind, String> myKindPresentation = Collections15.hashMap();

  private final StringFilterEditor myFilterEditor = new StringFilterEditor();
  private final Function<JPanel, Boolean> myDialogShower;

  private Configuration myConfig;
  private String myConfigSubset;

  private static final StringFilter FILTER_PROTOTYPE =
    new StringFilter(StringFilter.MatchType.REGEXP, "^this.*[0-z]+is_a_regexp", true);

  private StringFilterSet myCachedValue;


  public StringFilterSetEditor(Function<JPanel, Boolean> dialogShower, int rowCount) {
    myDialogShower = dialogShower;
    initActions();
    initCombo();
    initList(rowCount);
    initDnd();

    myModifiable.addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        myCachedValue = null;
      }
    });
  }

  private void initDnd() {
    myFilterListPanel.setDataRoles(STRING_FILTER_ROLE);
    myFilterListPanel.setTransfer(new MyContextTransfer());
  }

  private void initCombo() {
    SelectionInListModel<Kind> model = SelectionInListModel.create(Arrays.asList(Kind.ALL, Kind.INCLUSIVE, Kind.EXCLUSIVE), Kind.ALL);
    myKindCombo.setModel(model);
    myKindCombo.setCanvasRenderer(new CanvasRenderer<Kind>() {
      public void renderStateOn(CellState state, Canvas canvas, Kind item) {
        canvas.appendText(myKindPresentation.get(item));
      }
    });

    myKindPresentation.put(Kind.ALL, "All values");
    myKindPresentation.put(Kind.INCLUSIVE, "Only values from the following list:");
    myKindPresentation.put(Kind.EXCLUSIVE, "Only values that do not match the following list:");

    myKindCombo.setColumns(0);
    updatePrototype();
  }

  public void setKindPresentation(Kind kind, String presentation) {
    if (kind == null || presentation == null)
      return;
    myKindPresentation.put(kind, presentation);
    if (myKindCombo.isShowing()) {
      myKindCombo.repaint();
    }
    updatePrototype();
  }

  private void updatePrototype() {
    String max = "";
    Kind maxKind = Kind.ALL;
    for (Map.Entry<Kind, String> entry : myKindPresentation.entrySet()) {
      String string = entry.getValue();
      if (string != null && string.length() > max.length()) {
        max = string;
        maxKind = entry.getKey();
      }
    }
    myKindCombo.getCombobox().setPrototypeDisplayValue(maxKind);
  }

  private void initList(int rowCount) {
    myFilterListPanel.setVisibleRowCount(rowCount);
    ChangeListener listener = new ChangeListener() {
      public void onChange() {
        boolean all = myKindCombo.getModel().getSelectedItem() == Kind.ALL;
        myFilterListPanel.setEnabled(!all);
        myFilterListPanel.setCollectionModel(all ? AListModel.EMPTY : myFiltersModel);
        myModifiable.fireChanged();
      }
    };
    myKindCombo.getModel().addSelectionChangeListener(Lifespan.FOREVER, listener);
    listener.onChange();

    myFiltersModel.addChangeListener(Lifespan.FOREVER, myModifiable);
    myFilterListPanel.setCanvasRenderer(STRING_FILTER_RENDERER);
    myFilterListPanel.setPrototypeValue(FILTER_PROTOTYPE);
  }

  private void initActions() {
    myFilterListPanel.setAddAction(new SimpleAction("", Icons.ACTION_GENERIC_ADD) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(myKindCombo.getModifiable());
        context.setEnabled(myKindCombo.getModel().getSelectedItem() != Kind.ALL);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        Configuration config = myConfig;
        if (config == null) {
          assert false;
          return;
        }
        String subset = myConfigSubset;
        if (subset != null)
          config = config.getOrCreateSubset(subset);
        StringFilter filter = myFilterEditor.showNewFilter(myDialogShower, config, myFilterListPanel);
        if (filter != null) {
          myFiltersModel.addElement(filter);
          myFilterListPanel.getSelectionAccessor().setSelected(filter);
        }
        myFilterListPanel.getJList().requestFocus();
      }
    });
    myFilterListPanel.setRemoveAction(new SimpleAction("", Icons.ACTION_GENERIC_REMOVE) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        SelectionAccessor accessor = myFilterListPanel.getSelectionAccessor();
        context.updateOnChange(accessor);
        context.updateOnChange(myKindCombo.getModifiable());
        context.setEnabled(accessor.hasSelection() && myKindCombo.getModel().getSelectedItem() != Kind.ALL);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        SelectionAccessor<StringFilter> accessor = myFilterListPanel.getSelectionAccessor();
        StringFilter last = accessor.getLastSelectedItem();

        int lastIndex = -1;
        if (last != null) {
          lastIndex = myFiltersModel.indexOf(last);
          int[] indices = accessor.getSelectedIndexes();
          int diff = 0;
          for (int index : indices) {
            if (index < lastIndex)
              diff++;
          }
          lastIndex -= diff;
        }

        myFiltersModel.removeAll(accessor.getSelectedItems());

        if (lastIndex >= myFiltersModel.getSize())
          lastIndex = myFiltersModel.getSize() - 1;
        if (lastIndex < 0) {
          accessor.ensureSelectionExists();
        } else {
          accessor.setSelectedIndex(lastIndex);
        }
      }
    });
  }

  public JComponent getFilterTypeCombobox() {
    return myKindCombo;
  }

  public JComponent getFilterList() {
    return myFilterListPanel;
  }

  public void setEnabled(boolean enabled) {
    myKindCombo.setEnabled(enabled);
    myFilterListPanel.setEnabled(enabled);
  }

  public void setValue(StringFilterSet set) {
    myKindCombo.getModel().setSelectedItem(set.getKind());
    myFiltersModel.clear();
    myFiltersModel.addAll(set.getFilters());
  }

  /**
   * This method can be used for rapid operations while editor is running. It avoids production of much garbage.
   * @return filter set that is updated while user changes filter
   * @see #createValue()
   */
  public StringFilterSet getCachedValue() {
    if (myCachedValue == null) {
      Kind kind = myKindCombo.getModel().getSelectedItem();
      myCachedValue = new StringFilterSet(Util.NN(kind, Kind.ALL), myFiltersModel.toList());
    }
    return myCachedValue;
  }

  /**
   * This method should be used to get final value when edit is done
   * @return filter set not connected to editing model. It does not hold reference to this editor and not updated according to user changes
   * @see #getCachedValue()
   */
  public StringFilterSet createValue() {
    Kind kind = myKindCombo.getModel().getSelectedItem();
    return new StringFilterSet(Util.NN(kind, Kind.ALL), Collections15.arrayList(myFiltersModel.toList()));
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public void clear() {
    setValue(StringFilterSet.ALL);
  }

  public void setConfiguration(Lifespan lifespan, Configuration configuration, String subsetName) {
    myConfig = configuration;
    myConfigSubset = subsetName;
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        myConfig = null;
        myConfigSubset = null;
      }
    });
  }

  private static class StringFilterRenderer implements CanvasRenderer<StringFilter> {
    private Color myGrey;
    private Border myEmptyBorder;

    public void renderStateOn(CellState state, Canvas canvas, StringFilter item) {
      String filterString = Util.NN(item.getFilterString());
      StringFilter.MatchType matchType = Util.NN(item.getMatchType(), StringFilter.MatchType.INVALID);
      boolean ignoreCase = item.isIgnoreCase();

      if (matchType == StringFilter.MatchType.EXACT) {
        canvas.appendText(filterString);
      } else if (matchType == null || matchType == StringFilter.MatchType.INVALID) {
        if (!state.isSelected())
          canvas.setForeground(getGrey(state));
        canvas.appendText("invalid filter");
        return;
      } else {
        if (!state.isSelected())
          canvas.setForeground(getGrey(state));
        canvas.getCurrentSection().setBorder(getEmptyBorder(state));
        canvas.appendText(matchType.getLangPrefix());
        canvas.appendText(" ");
        CanvasSection section = canvas.newSection();
        section.setForeground(state.getForeground());
        section.setBorder(state.getBorder());
        section.appendText(filterString);
      }

      if (!ignoreCase) {
        CanvasSection section = canvas.newSection();
        if (!state.isSelected())
          section.setForeground(getGrey(state));
        section.setBorder(getEmptyBorder(state));
        section.appendText(", case-sensitive");
      }
    }

    private Border getEmptyBorder(CellState state) {
      if (myEmptyBorder == null) {
        Border border = state.getBorder();
        if (border == null)
          myEmptyBorder = AwtUtil.EMPTY_BORDER;
        else
          myEmptyBorder = new EmptyBorder(border.getBorderInsets(new JLabel()));
      }
      return myEmptyBorder;
    }

    private Color getGrey(CellState state) {
      if (myGrey == null)
        myGrey = ColorUtil.between(state.getForeground(), state.getDefaultBackground(), 0.7F);
      return myGrey;
    }
  }
  


  private class MyContextTransfer implements ContextTransfer {
    @NotNull
    public Transferable transfer(DragContext context) throws CantPerformException {
      return new StringListTransferable(getStringsForTransfer(context));
    }

    private List<String> getStringsForTransfer(ActionContext context) throws CantPerformException {
      List<StringFilter> filters = context.getSourceCollection(STRING_FILTER_ROLE);
      if (filters.size() == 0)
        throw new CantPerformException();
      List<String> converted = StringFilter.TO_STRING.collectListWithoutNulls(filters);
      if (converted.size() == 0)
        throw new CantPerformException();
      return converted;
    }

    public void acceptTransfer(DragContext context, Transferable tranferred)
      throws CantPerformException, UnsupportedFlavorException, IOException
    {
      int row = getInsertRow(context);
      List<StringFilter> filters = getTransferredFilters(tranferred);
      myFiltersModel.insertAll(row, filters);
      if (myKindCombo.getModel().getSelectedItem() == Kind.ALL) {
        myKindCombo.getModel().setSelectedItem(Kind.INCLUSIVE);
      }
    }

    private List<StringFilter> getTransferredFilters(Transferable tranferred) throws CantPerformException {
      List<String> strings = getStrings(tranferred);
      List<StringFilter> filters = StringFilter.FROM_STRING.collectListWithoutNulls(strings);
      if (filters.size() == 0)
        throw new CantPerformException();
      return filters;
    }

    private int getInsertRow(DragContext context) throws CantPerformException {
      ListDropPoint dropPoint = context.getValue(DndUtil.LIST_DROP_POINT);
      if (dropPoint == null || !dropPoint.isValid())
        throw new CantPerformException();
      int row = dropPoint.getTargetRow();
      if (row < 0 || row > myFiltersModel.getSize())
        throw new CantPerformException();
      return row;
    }

    private List<String> getStrings(Transferable tranferred) throws CantPerformException {
      List<String> strings = DndUtil.LIST_OF_STRINGS.getDataOrNull(tranferred);
      if (strings == null) {
        try {
          Object o = tranferred.getTransferData(DataFlavor.stringFlavor);
          if (o instanceof String) {
            String[] lines = ((String) o).split("[\\r\\n]+");
            strings = Arrays.asList(lines);
          }
        } catch (UnsupportedFlavorException e) {
          // ignore
        } catch (IOException e) {
          // ignore
        }
      }
      if (strings == null || strings.size() == 0)
        throw new CantPerformException();
      return strings;
    }

    public void cleanup(DragContext context) throws CantPerformException {
    }

    public void remove(ActionContext context) throws CantPerformException {
    }

    public boolean canRemove(ActionContext context) throws CantPerformException {
      return false;
    }

    public boolean canMove(ActionContext context) throws CantPerformException {
      return canRemove(context);
    }

    public boolean canCopy(ActionContext context) throws CantPerformException {
      return getStringsForTransfer(context).size() > 0;
    }

    public boolean canLink(ActionContext context) throws CantPerformException {
      return false;
    }

    public boolean canImportData(DragContext context) throws CantPerformException {
      return getTransferredFilters(context.getTransferable()).size() > 0;
    }

    public boolean canImportDataNow(DragContext context, Component dropTarget) throws CantPerformException {
      int row = getInsertRow(context);
      assert row >= 0;
      return getTransferredFilters(context.getTransferable()).size() > 0;
    }

    public void startDrag(DragContext dragContext, InputEvent event) throws CantPerformException {
    }

    public boolean canImportFlavor(DataFlavor flavor) {
      return DndUtil.LIST_OF_STRINGS.getFlavor().equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
    }

    @Nullable
    public Factory<Image> getTransferImageFactory(DragContext dragContext) throws CantPerformException {
      return null;
    }
  }
}
