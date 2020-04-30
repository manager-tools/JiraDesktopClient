package com.almworks.util.components;

import com.almworks.integers.IntArray;
import com.almworks.util.Env;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.OverridenCellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static com.almworks.util.ui.swing.Shortcuts.ksPlain;

public class CompactSubsetEditor<T> extends ListWithAddRemoveButtons<T> {
  private final OrderListModel<T> myNothingSelectedModel = OrderListModel.create();
  private final OrderListModel<T> mySubsetModel = OrderListModel.create();

  private final ListModelHolder<T> myFullModel;
  private final ListDropDown myDropDown;
  private final SubsetAccessorAccessor mySubsetAccessor = new SubsetAccessorAccessor();
  private final RecentController myRecents = new RecentController();
  private final Lifecycle myDispayableLife = new Lifecycle(false);

  private Color myUnknownSelectionItemColor;
  private CanvasRenderer<? super T> myExtRenderer;
  @Nullable
  private Condition<T> myAddFilter;
  private final SegmentedListModel myAddModel = new SegmentedListModel();
  private final AListModel<T> myComplemetary;

  private T myNothingSelectedItem;

  public CompactSubsetEditor() {
    this(AListModel.EMPTY, null);
  }

  public CompactSubsetEditor(AListModel<? extends T> model, T nothingSelectedItem) {
    super();
    myNothingSelectedItem = nothingSelectedItem;
    SortedListDecorator<T> sortedSubset =
      SortedListDecorator.create(Lifespan.FOREVER, mySubsetModel, new Comparator<T>() {
        public int compare(T o1, T o2) {
          int i1 = myFullModel.indexOf(o1);
          int i2 = myFullModel.indexOf(o2);
          return i1 - i2;
        }
      });
    super.setCollectionModel(SegmentedListModel.create(myNothingSelectedModel, sortedSubset));

    myFullModel = ListModelHolder.create(model);
    myComplemetary = new ImageBasedDecorator.ComplemetaryListModel<T>(myFullModel, mySubsetModel);
    myRecents.setModel(myAddModel);
    setAddAction(new MyAddAction());
    setRemoveAction(new MyRemoveAction());
    myDropDown = new ListDropDown(getAddButton(), getJList());

    checkNothingSelected();
  }

  /**
   * Specify additional filter for add-elements popup.
   * When the filter is null the popup shows all element from complementary element set. When the filter is not-null the popup filters out elements which do not pass filter
   * (even if they appear in {@link #getFullModel() the full model} and are missing in {@link #getSubsetModel() subset}.
   * @param addFilter filter for add elements popup
   */
  public void setAddFilter(@Nullable Condition<T> addFilter) {
    myAddFilter = addFilter;
    updateAddModel();
  }

  private void updateAddModel() {
    if (myDispayableLife.isCycleStarted()) {
      AListModel addModel = createAddModel(myDispayableLife.lifespan());
      if (myAddModel.getSegment(0) == null) myAddModel.addSegment(addModel);
      else myAddModel.setSegment(0, addModel);
    }
  }

  private AListModel createAddModel(Lifespan life) {
    AListModel model = myComplemetary;
    final Condition<T> addFilter = myAddFilter;
    if (addFilter == null) return model;
    return FilteringListDecorator.create(life, model, new Condition() {
      @Override
      public boolean isAccepted(Object value) {
        Object obj = RecentController.unwrap(value);
        return addFilter.isAccepted((T) obj);
      }
    });
  }

  @Override
  public Dimension getPreferredSize() {
    return super.getPreferredSize();
  }

  @Override
  public void addNotify() {
    if (myDispayableLife.cycleStart()) {
      // Repaint visible items because of they may become valid or invalid (contained in full model)
      myDispayableLife.lifespan().add(myFullModel.addListener(new AListModel.Adapter<T>() {
        @Override
        public void onRemove(int index, int length, AListModel.RemovedEvent<T> event) {
          getJList().repaint();
        }

        @Override
        public void onInsert(int index, int length) {
          getJList().repaint();
        }
      }));
      updateAddModel();
    }
    super.addNotify();
  }

  @Override
  public void removeNotify() {
    if (myDispayableLife.cycleEnd()) myAddModel.removeAll();
    super.removeNotify();
  }

  private void checkNothingSelected() {
    if (myNothingSelectedItem != null) {
      int size = mySubsetModel.getSize();
      if (myNothingSelectedModel.getSize() > 0) {
        if (size > 0) {
          myNothingSelectedModel.clear();
        }
      } else {
        if (size == 0) {
          myNothingSelectedModel.insert(0, myNothingSelectedItem);
        }
      }
    }
  }

  public void setIdentityConvertor(Convertor convertor) {
    myRecents.setIdentityConvertor(convertor);
  }

  public void setNothingSelectedItem(T nothingSelectedItem) {
    if (nothingSelectedItem != myNothingSelectedItem) {
      if (myNothingSelectedModel.getSize() > 0) {
        assert myNothingSelectedItem != null;
        if (nothingSelectedItem == null) {
          myNothingSelectedModel.clear();
        } else {
          T removed = myNothingSelectedModel.replaceAt(0, nothingSelectedItem);
          assert removed == myNothingSelectedItem;
        }
      } else {
        if (myNothingSelectedItem == null) {
          assert nothingSelectedItem != null;
          if (mySubsetModel.getSize() == 0) {
            myNothingSelectedModel.insert(0, nothingSelectedItem);
          }
        }
      }
      myNothingSelectedItem = nothingSelectedItem;
    }
  }

  public boolean isEveryItemSelected() {
    if (mySubsetModel.getSize() < myFullModel.getSize())
      return false;
    Set<T> chosen = getSubset();
    for (int i = 0; i < myFullModel.getSize(); i++)
      if (!chosen.contains(myFullModel.getAt(i)))
        return false;
    return true;
  }

  private Set<T> getSubset() {
    return Collections15.hashSet(mySubsetModel.toList());
  }

  public void showAddWindow() {
    myDropDown.showDropDown(this);
  }

  public boolean canRemove() {
    return getSelectionAccessor().hasSelection() && mySubsetModel.getSize() > 0;
  }

  public void doRemove() {
    SelectionAccessor<T> accessor = getSelectionAccessor();
    List<T> selected = accessor.getSelectedItems();
    int[] indices = accessor.getSelectedIndexes();
    int minIndex = ArrayUtil.min(indices);
    removeAllItems(selected);
    if(!selected.isEmpty()) {
      fireUserModification();
    }
    checkNothingSelected();
    if (!accessor.hasSelection()) {
      int size = mySubsetModel.getSize();
      if (minIndex == size) {
        minIndex--;
      }
      if (minIndex >= 0 && minIndex < size) {
        accessor.setSelectedIndex(minIndex);
      } else {
        accessor.ensureSelectionExists();
      }
    }
  }

  private void removeAllItems(List<T> selected) {
    IntArray indexes = new IntArray();
    List<T> subsetList = mySubsetModel.toList();
    for (T element : selected) {
      int index = -1;
      for (int i = 0; i < subsetList.size(); i++) {
        T t = subsetList.get(i);
        if (t == element && !indexes.contains(i)) {
          index = i;
          break;
        }
      }
      if (index < 0) {
        for (int i = 0; i < subsetList.size(); i++) {
          T t = subsetList.get(i);
          if (!indexes.contains(i) && Util.equals(t, element)) {
            index = i;
            break;
          }
        }
      }
      if (index >= 0) indexes.add(index);
    }
    mySubsetModel.removeAll(indexes.toNativeArray());
  }

  public Detach setFullModel(AListModel<? extends T> model) {
    return setFullModel(model, null);
  }

  public Detach setFullModel(AListModel<? extends T> model, @Nullable Configuration recentConfig) {
    myRecents.setConfig(null);
    Detach detach = myFullModel.setModel(model);
    myRecents.setConfig(recentConfig);
    return detach;
  }

  public void setCollectionModel(AListModel<T> m) {
    throw new UnsupportedOperationException();
  }

  public void addSelected(Collection<? extends T> items) {
    if (items.size() > 0) {
      mySubsetModel.addAll(items);
      checkNothingSelected();
    }
  }

  public void setSelected(@Nullable Collection<? extends T> items) {
    if (items == null)
      items = Collections15.emptyCollection();
    mySubsetModel.setElements(items);
    checkNothingSelected();
  }

  public AListModel<T> getSubsetModel() {
    return mySubsetModel;
  }

  public void setCanvasRenderer(final CanvasRenderer<? super T> renderer) {
    CanvasRenderer<? super T> collectionRenderer = renderer;
    final Color unknownSelectionColor = myUnknownSelectionItemColor;
    if (renderer != null && unknownSelectionColor != null) {
      collectionRenderer = new CanvasRenderer<T>() {
        public void renderStateOn(CellState state, Canvas canvas, T item) {
          if (item != myNothingSelectedItem && !getFullModel().contains(item)) {
            state = OverridenCellState.overrideForeground(state, unknownSelectionColor);
            canvas.setForeground(unknownSelectionColor);
          }
          renderer.renderStateOn(state, canvas, item);
        }
      };
    }
    super.setCanvasRenderer(collectionRenderer);
    myRecents.setRenderer(renderer);
    myExtRenderer = renderer;
  }

  protected boolean isMyComponent(Component component) {
    if (component == null)
      return false;
    if (super.isMyComponent(component))
      return true;
    return component == myDropDown.myForm.myWholePanel || component == myDropDown.myForm.myList
      || component == myDropDown.myForm.myList.getSwingComponent() || component == myDropDown.getDropDownWindow();
  }

  public void setUnknownSelectionItemColor(Color color) {
    myUnknownSelectionItemColor = color;
    setCanvasRenderer(myExtRenderer);
  }

  public static <T> CompactSubsetEditor<T> create(AListModel<? extends T> model) {
    return new CompactSubsetEditor<T>(model, null);
  }

  public AListModel<T> getFullModel() {
    return myFullModel;
  }

  public SelectionAccessor<T> getSubsetAccessor() {
    return mySubsetAccessor;
  }

  public List<T> getSelectedItems() {
    return Collections15.arrayList(getSubsetModel().toList());
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myDropDown.hideDropDown();
  }


  private static class DropDownForm<T> {
    private final JPanel myWholePanel;
    private final ACheckboxList<T> myList = new ACheckboxList<T>();

    private DropDownForm() {
      myWholePanel = new JPanel(new BorderLayout(0, 0)) {
        public Dimension getPreferredSize() {
          Dimension preferredSize = super.getPreferredSize();
          GraphicsConfiguration gc = getGraphicsConfiguration();
          int maxWidth = Math.max(200, gc == null ? 0 : gc.getBounds().width / 3);
          if (preferredSize.width > maxWidth)
            preferredSize.width = maxWidth;
          return preferredSize;
        }
      };
      JScrollPane scrollPane = new JScrollPane(myList);
      scrollPane.setMinimumSize(new Dimension(50, 100));
      Aqua.cleanScrollPaneBorder(scrollPane);
      myWholePanel.add(scrollPane, BorderLayout.CENTER);
    }
  }


  private class ListDropDown extends DropDownListener {
    private final JComponent myLocation;
    private final DropDownForm<T> myForm = new DropDownForm<T>();
    private final JComponent myReturnFocus;
    private final Lifecycle myShowingCycle = new Lifecycle();

    private final AddAction myAdd = new AddAction();
    private boolean myAdding = false;

    public ListDropDown(JComponent location, JComponent returnFocus) {
      myLocation = location;
      myReturnFocus = returnFocus;
      myForm.myList.setCollectionModel(myRecents.getDecoratedModel());
      myForm.myList.setCanvasRenderer(myRecents.getDecoratedRenderer());

      final JComponent list = myForm.myList.getSwingComponent();
      list.getActionMap().put("Add", myAdd);
      list.getInputMap().put(ksPlain(KeyEvent.VK_ENTER), "Add");
      list.getInputMap().put(ksPlain(KeyEvent.VK_INSERT), "Add");
      myForm.myList.addDoubleClickListener(Lifespan.FOREVER, new CollectionCommandListener<T>() {
        public void onCollectionCommand(ACollectionComponent<T> aCollectionComponent, int index, T element) {
          myForm.myList.getCheckedAccessor().addSelection(element);
          myAdd.doPerform(false);
        }
      });
      ListSpeedSearch.install(myForm.myList);
    }

    protected JComponent createPopupComponent() {
      return myForm.myWholePanel;
    }

    protected void onDropDownShown() {
      final ACheckboxList<T> list = myForm.myList;
      list.getCheckedAccessor().clearSelection();
      list.getSelectionAccessor().setSelectedIndex(0);
      list.scrollListSelectionToView();
      list.getSwingComponent().requestFocus();
      myShowingCycle.cycle();
      myRecents.duplicateSelection(myShowingCycle.lifespan(), list.getCheckedAccessor());
      myAdding = false;
    }

    protected void onDropDownCancelled() {
      myShowingCycle.cycle();
      myReturnFocus.requestFocus();
    }

    protected void onDropDownHidden() {
      if(!myAdding) {
        myAdding = true;
        myAdd.doPerform(false);
      }
      onDropDownCancelled();
    }

    protected Rectangle getScreenLocation() {
      return new Rectangle(myLocation.getLocationOnScreen(), myLocation.getSize());
    }

    public Collection<? extends T> getAllItems() {
      return Collections15.arrayList(myForm.myList.getCollectionModel().toList());
    }

    private class AddAction extends AbstractAction {
      private final SelectionAccessor<T> myFirst = myForm.myList.getCheckedAccessor();
      private final SelectionAccessor<T> mySecond = myForm.myList.getSelectionAccessor();

      @Override
      public void actionPerformed(ActionEvent e) {
        doPerform(true);
      }

      protected void doPerform(boolean useSecond) {
        List<T> toAdd = myFirst.getSelectedItems();
        if(toAdd.isEmpty() && useSecond) {
          toAdd = mySecond.getSelectedItems();
        }

        if(toAdd.isEmpty()) {
          hideDropDown();
          return;
        }

        final Set<T> newElements = Collections15.linkedHashSet(toAdd);
        for(final Iterator<T> it = newElements.iterator(); it.hasNext();) {
          if(mySubsetModel.contains(it.next())) {
            it.remove();
          }
        }
        mySubsetModel.addAll(newElements);
        if(!newElements.isEmpty()) {
          fireUserModification();
        }
        myRecents.addToRecent(newElements);
        checkNothingSelected();
        if(!myAdding) {
          myAdding = true;
          hideDropDown();
        }
        getSelectionAccessor().setSelected(newElements);
      }
    }
  }


  private class SubsetAccessorAccessor extends SelectionAccessor<T> {
    public SubsetAccessorAccessor() {
      mySubsetModel.addListener(new AListModel.Adapter() {
        public void onChange() {
          fireSelectionChanged();
        }

        public void onItemsUpdated(AListModel.UpdateEvent event) {
          for (int i = event.getLowAffectedIndex(); i <= event.getHighAffectedIndex(); i++)
            if (isSelectedAt(i)) {
              fireSelectedItemsChanged();
              return;
            }
        }
      });
    }

    public T getSelection() {
      return hasSelection() ? mySubsetModel.getAt(0) : null;
    }

    public T getFirstSelectedItem() {
      return hasSelection() ? mySubsetModel.getAt(0) : null;
    }

    public T getLastSelectedItem() {
      return hasSelection() ? mySubsetModel.getAt(mySubsetModel.getSize() - 1) : null;
    }

    public boolean hasSelection() {
      return mySubsetModel.getSize() > 0;
    }

    @NotNull
    public List<T> getSelectedItems() {
      return Collections15.arrayList(mySubsetModel.toList());
    }

    @NotNull
    public int[] getSelectedIndexes() {
      IntArray result = new IntArray();
      for (int i = 0; i < mySubsetModel.getSize(); i++)
        result.add(myFullModel.indexOf(mySubsetModel.getAt(i)));
      return result.toNativeArray();
    }

    protected int getElementCount() {
      return myFullModel.getSize();
    }

    protected T getElement(int index) {
      return myFullModel.getAt(index);
    }

    public boolean setSelected(T item) {
      mySubsetModel.setElements(Collections.singleton(item));
      checkNothingSelected();
      return true;
    }

    public void setSelectedIndex(int index) {
      setSelected(myFullModel.getAt(index));
    }

    public boolean isSelected(T item) {
      return mySubsetModel.indexOf(item) != -1;
    }

    public void selectAll() {
      mySubsetModel.addAll(myDropDown.getAllItems());
      checkNothingSelected();
    }

    public void clearSelection() {
      mySubsetModel.clear();
      checkNothingSelected();
    }

    public void invertSelection() {
      Collection<? extends T> notSelected = myDropDown.getAllItems();
      mySubsetModel.clear();
      mySubsetModel.addAll(notSelected);
      checkNothingSelected();
    }

    public void addSelectedRange(int first, int last) {
      setSelectedRange(first, last, true);
    }

    @Override
    public void removeSelectedRange(int first, int last) {
      setSelectedRange(first, last, false);
    }

    public void setSelectedRange(int first, int last, boolean makeSelected) {
      for (int i = first; i <= last; i++) {
        T item = myFullModel.getAt(i);
        if (isSelected(item) != makeSelected) {
          if (makeSelected) mySubsetModel.addElement(item);
          else mySubsetModel.remove(item);
        }
      }
      checkNothingSelected();
    }

    public int getSelectedIndex() {
      return hasSelection() ? myFullModel.indexOf(mySubsetModel.getAt(0)) : -1;
    }

    public boolean ensureSelectionExists() {
      if (hasSelection())
        return true;
      if (myFullModel.getSize() == 0)
        return false;
      setSelectedIndex(0);
      return true;
    }

    public void addSelection(T item) {
      if (!isSelected(item))
        mySubsetModel.addElement(item);
      checkNothingSelected();
    }

    public boolean isAllSelected() {
      return myDropDown.getAllItems().isEmpty();
    }

    public boolean isSelectedAt(int index) {
      return mySubsetModel.indexOf(myFullModel.getAt(index)) != -1;
    }

    public void addSelectionIndex(int index) {
      addSelection(myFullModel.getAt(index));
    }

    public void removeSelectionAt(int index) {
      removeSelection(myFullModel.getAt(index));
    }

    public void removeSelection(T element) {
      mySubsetModel.remove(element);
      checkNothingSelected();
    }
  }


  private class MyAddAction extends SimpleAction {
    public MyAddAction() {
      super("", Icons.ACTION_GENERIC_ADD);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, getTooltip());
      updateOnChange(mySubsetModel);
      updateOnChange(myFullModel);
      updateOnChange(getInternalModifiable());
    }

    private String getTooltip() {
      return Env.isMac() ? "Add (+)" : "Add (+, Ins)";
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(CompactSubsetEditor.this.isEnabled() && !isEveryItemSelected());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      if (!myDropDown.isDropDownShown()) {
        showAddWindow();
      } else {
        myDropDown.hideDropDown();
      }
    }
  }


  private class MyRemoveAction extends SimpleAction {
    public MyRemoveAction() {
      super("", Icons.ACTION_GENERIC_REMOVE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, getTooltip());
      updateOnChange(getSelectionAccessor());
      updateOnChange(getInternalModifiable());
    }

    private String getTooltip() {
      return String.format("Remove (-, %s)", Shortcuts.DEL);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(CompactSubsetEditor.this.isEnabled() && canRemove());
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      doRemove();
    }
  }
}
