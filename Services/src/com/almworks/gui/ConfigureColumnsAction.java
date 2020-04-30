package com.almworks.gui;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.macosx.MacCornerButton;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;

import static com.almworks.util.collections.Containers.compareInts;
import static java.lang.Math.max;

public class ConfigureColumnsAction extends SimpleAction {
  @NotNull
  private final Factory<? extends ArtifactTableColumns<?>> myColumnsFactory;
  private final Factory<? extends SubsetModel<? extends TableColumnAccessor<?, ?>>> mySelectedColumns;
  private final BasicScalarModel<Boolean> myDialogActive = BasicScalarModel.createModifiable(false);

  public ConfigureColumnsAction(Factory<? extends ArtifactTableColumns<?>> columnsFactory, Factory<? extends SubsetModel<? extends TableColumnAccessor<?, ?>>> selectedColumns) {
    super(L.actionName("Select Columns\u2026"));
    myColumnsFactory = columnsFactory;
    mySelectedColumns = selectedColumns;

    setActionShortcuts();
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Select columns"));
  }

  private void setActionShortcuts() {
    setDefaultPresentation(
      PresentationKey.SHORTCUT,
      Env.isMac()
        ? KeyStroke.getKeyStroke("meta CLOSE_BRACKET")
        : KeyStroke.getKeyStroke("control CLOSE_BRACKET"));
  }

  protected void customUpdate(UpdateContext context) {
    context.setEnabled(myDialogActive.getValue() != Boolean.TRUE);
    context.updateOnChange(myDialogActive);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ColumnEditor editor = new ColumnEditor();
    editor.setup();
    DialogBuilder builder = createDialog(context, editor);
    setupDialogCloseShortcuts(editor, builder);
    myDialogActive.setValue(true);
    builder.showWindow(createCloseDetach(editor));
  }

  private DialogBuilder createDialog(ActionContext context, ColumnEditor editor) throws CantPerformException {
    DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    String configName = "columnsConfigurator" + (editor.hasAux() ? "WithAux" : "NoAux"); 
    DialogBuilder builder = dialogManager.createBuilder(configName);
    builder.setTitle(L.dialog("Select Table Columns"));
    builder.setCancelAction(L.actionName("Close Window"));
    builder.setContent(editor);
    builder.setInitialFocusOwner(editor.getInitialFocused());
    editor.getComponent().getPreferredSize();
    return builder;
  }

  private void setupDialogCloseShortcuts(ColumnEditor editor, final DialogBuilder builder) {
    JComponent editorComponent = editor.getComponent();
    editorComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Shortcuts.CTRL_ENTER, "closeDialog");
    editorComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Shortcuts.ENTER, "closeDialog");
    editorComponent.getActionMap().put("closeDialog", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          builder.closeWindow();
        } catch (CantPerformException ex) {
          Log.warn(ex);
        }
      }
    });
  }

  private Detach createCloseDetach(final ColumnEditor editor) {
    return new Detach() {
      protected void doDetach() throws Exception {
        editor.dispose();
        myDialogActive.setValue(false);
      }
    };
  }

  public void addCornerButton(JScrollPane scrollPane) {
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    final AbstractButton button;
    if(Aqua.isAqua()) {
      button = new MacCornerButton(this);
      button.setBorder(Aqua.MAC_LIGHT_BORDER_SOUTH);
    } else {
      button = new AActionButton(this);
    }
    button.setText("");
    button.setFocusable(false);
    scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, button);
  }

  private class ColumnEditor implements UIComponentWrapper {
    private final Lifecycle myLife = new Lifecycle();
    private final SubsetModel<TableColumnAccessor<?, ?>> mySelectedModel;
    /**
     * It is important that this model is *the* model that is passed to the corresponding ACheckboxList in its constructor.
     */
    private final AListModel<? extends TableColumnAccessor<?, ?>> myMainModelSorted;
    private final AListModel<? extends TableColumnAccessor<?, ?>> myAuxModelSorted;
    private final ACheckboxList<TableColumnAccessor<?, ?>> myMainList;
    private final ACheckboxList<TableColumnAccessor<?, ?>> myAuxList;
    private final AList<TableColumnAccessor<?, ?>> mySelectedList;

    private boolean myProcessingSelection;

    private JComponent myComponent;
    private final AActionButton myRemoveButton;

    public ColumnEditor() throws CantPerformException {
      ArtifactTableColumns<?> columns = myColumnsFactory.create();
      if (columns == null) throw new CantPerformException("CColA: no columns");

      myMainModelSorted = sort(columns.getMain());
      myAuxModelSorted = sort(columns.getAux());
      myMainList = new ACheckboxList<TableColumnAccessor<?, ?>>(myMainModelSorted);
      if (myAuxModelSorted != null && myAuxModelSorted.getSize() > 0) {
        myAuxList = new ACheckboxList<TableColumnAccessor<?, ?>>(myAuxModelSorted);
      } else {
        myAuxList = null;
      }

      mySelectedModel = (SubsetModel<TableColumnAccessor<?, ?>>)mySelectedColumns.create();
      mySelectedList = new AList<TableColumnAccessor<?,?>>(mySelectedModel);
      myRemoveButton = createRemoveButton();
    }

    @Nullable
    private AListModel<TableColumnAccessor<?, ?>> sort(@Nullable ArtifactTableColumns.ColumnsSet<?> columns) {
      if (columns == null || columns.model == null) return null;
      if (columns.model instanceof SortedListDecorator) return (AListModel)columns.model;
      return SortedListDecorator.create(myLife.lifespan(), columns.model, Util.NN(columns.order, (Comparator)TableColumnAccessor.NAME_ORDER));
    }

    public boolean hasAux() {
      return myAuxList != null;
    }

    public void setup() {
      setupListeners(myMainList, myMainModelSorted);
      if (myAuxList != null) setupListeners(myAuxList, myAuxModelSorted);
      myComponent = createComponent();
    }

    private void setupListeners(ACheckboxList<TableColumnAccessor<?, ?>> list, final AListModel<? extends TableColumnAccessor<?, ?>> model) {
      final SelectionAccessor<TableColumnAccessor<?,?>> checked = list.getCheckedAccessor();
      setupInitialCheckSelection(checked);
      propagateListChangesToSelectedModel(model, checked);
      propagateSelectedModelChangesToList(checked);
      fixSelectionInSelectedList();
    }

    private void setupInitialCheckSelection(SelectionAccessor<TableColumnAccessor<?, ?>> checked) {
      checked.selectAll(new Condition<TableColumnAccessor<?, ?>>() {
        @Override
        public boolean isAccepted(TableColumnAccessor<?, ?> value) {
          return mySelectedModel.contains(value);
        }
      });
    }

    private void propagateListChangesToSelectedModel(final AListModel<? extends TableColumnAccessor<?, ?>> model, final SelectionAccessor<TableColumnAccessor<?, ?>> checked) {
      checked.addChangeListener(myLife.lifespan(), new ChangeListener() {
        @Override
        public void onChange() {
          if (!myProcessingSelection) {
            removeUncheckedColumns(model, checked);
            addCheckedColumns(checked);
          }
        }
      });
    }

    private void removeUncheckedColumns(final AListModel<? extends TableColumnAccessor<?, ?>> model, final SelectionAccessor<TableColumnAccessor<?, ?>> checked) {
      mySelectedModel.removeAll(new Condition<TableColumnAccessor<?, ?>>() {
        @Override
        public boolean isAccepted(TableColumnAccessor<?, ?> value) {
          int index = model.indexOf(value);
          return index >= 0 && !checked.isSelectedAt(index);
        }
      });
    }

    private void addCheckedColumns(SelectionAccessor<TableColumnAccessor<?, ?>> checked) {
      int oldSize = mySelectedModel.getSize();
      for (TableColumnAccessor<?, ?> added : checked.getSelectedItems()) {
        if (!mySelectedModel.contains(added))
          mySelectedModel.add(added);
      }
      int newSize = mySelectedModel.getSize();
      if (newSize > oldSize) {
        int[] newSelection = new int[newSize - oldSize];
        for (int i = 0; i < newSelection.length; ++i) {
          newSelection[i] = i + oldSize;
        }
        mySelectedList.getSelectionAccessor().setSelectedIndexes(newSelection);
        mySelectedList.scrollToLastEntry();
      }
    }

    private void propagateSelectedModelChangesToList(final SelectionAccessor<TableColumnAccessor<?, ?>> checked) {
      myLife.lifespan().add(mySelectedModel.addListener(new AListModel.Listener<TableColumnAccessor<?, ?>>() {
        @Override
        public void onInsert(int index, int length) {
          myProcessingSelection = true;
          for (int i = 0; i < length; ++i) {
            checked.addSelection(mySelectedModel.getAt(index + i));
          }
          myProcessingSelection = false;
        }

        @Override
        public void onRemove(int index, int length, AListModel.RemovedEvent<TableColumnAccessor<?, ?>> event) {
          myProcessingSelection = true;
          checked.removeSelection(event.getAllRemoved());
          myProcessingSelection = false;
        }

        @Override
        public void onListRearranged(AListModel.AListEvent event) { }

        @Override
        public void onItemsUpdated(AListModel.UpdateEvent event) { }
      }));
    }

    private void fixSelectionInSelectedList() {
      SelectedListSelectionFixer fixer = new SelectedListSelectionFixer();
      myLife.lifespan().add(mySelectedModel.addRemovedElementListener(fixer));
      myLife.lifespan().add(mySelectedModel.addListener(fixer));
    }

    private class SelectedListSelectionFixer extends AListModel.Adapter<TableColumnAccessor<?, ?>> implements AListModel.RemovedElementsListener<TableColumnAccessor<?, ?>> {
      private int mySelectedIndexBeforeRemoval = -1;

      @Override
      public void onBeforeElementsRemoved(AListModel.RemoveNotice<TableColumnAccessor<?, ?>> elements) {
        mySelectedIndexBeforeRemoval = mySelectedList.getSelectedIndex();
      }

      @Override
      public void onRemove(int index, int length, AListModel.RemovedEvent<TableColumnAccessor<?, ?>> event) {
        if (mySelectedIndexBeforeRemoval >= 0) {
          int beforeDeleted = mySelectedIndexBeforeRemoval > 0 ? mySelectedIndexBeforeRemoval - 1 : 0;
          mySelectedList.setSelectionIndex(beforeDeleted);
        }
        mySelectedIndexBeforeRemoval = -1;
      }
    }

    private AActionButton createRemoveButton() {
      return new AToolbarButton(new SimpleAction("", Icons.ACTION_GENERIC_REMOVE) {
        @Override
        protected void customUpdate(UpdateContext context) {
          context.updateOnChange(mySelectedModel);
          context.updateOnChange(mySelectedList.getSelectionAccessor());
          context.setEnabled(mySelectedModel.getSize() > 0 && mySelectedList.getSelectionAccessor().getSelectedCount() > 0);
        }

        @Override
        protected void doPerform(ActionContext context) {
          myProcessingSelection = true;
          mySelectedModel.removeAll(mySelectedList.getSelectionAccessor().getSelectedItems());
          myProcessingSelection = false;
        }
      });
    }

    @Override
    @NotNull
    public JComponent getComponent() {
      return myComponent;
    }

    public JComponent getInitialFocused() {
      return myMainList;
    }

    private JComponent createComponent() {
      JComponent selected = createSelectedListPane();
      JComponent main = createCheckboxListPane(myMainList, myAuxList != null ? getMainColName() : "A&vailable:", false);
      JComponent aux = myAuxList != null ? createCheckboxListPane(myAuxList, getAuxColName(), true) : null;

      setPrefSizeNotLess(120, 200, selected);
      setPrefSizeNotLess(120, 200, main);
      setPrefSizeNotLess(120, 200, aux);

      DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("d:g", "d:g"));
      builder.add(selected, "1, 1, 1, 1, f, f");
      builder.appendUnrelatedComponentsGapColumn();
      builder.appendColumn("d:g");
      builder.add(main, "3, 1, 1, 1, f, f");
      if (aux != null) {
        builder.appendRelatedComponentsGapColumn();
        builder.appendColumn("d:g");
        builder.add(aux, "5, 1, 1, 1, f, f");
      }

      return builder.getPanel();
    }

    private String getMainColName() {
      return Local.parse(Terms.ref_Main_columns) + ':';
    }

    private String getAuxColName() {
      return Local.parse(Terms.ref_Auxiliary_columns) + ':';
    }

    private void setPrefSizeNotLess(int width, int height, @Nullable JComponent component) {
      if (component == null) return;
      Dimension curPsz = component.getPreferredSize();
      component.setPreferredSize(new Dimension(max(curPsz.width, width), max(curPsz.height, height)));
    }

    private JComponent createCheckboxListPane(ACheckboxList<TableColumnAccessor<?, ?>> list, @NotNull String labelTextWithMnemonic, boolean searchSubstring) {
      JComponent listComponent = createListComponent(list, searchSubstring);

      DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("d:g"));
      JLabel label = b.append(labelTextWithMnemonic);
      label.setLabelFor(listComponent);
      b.appendRow("3dlu:none");
      b.appendRow("d:g");
      b.add(listComponent, "1, 3, 1, 1, f, f");

      return b.getPanel();
    }

    private void setPrototypeCellValue(BaseAList<TableColumnAccessor<?, ?>> list) {
      AListModel<TableColumnAccessor<?, ?>> items = list.getCollectionModel();
      if (items.getSize() == 0) {
        list.setPreferredSize(new Dimension(50, 50));
      } else {
        TableColumnAccessor<?, ?> longest = Collections.max(items.toList(), new Comparator<TableColumnAccessor<?, ?>>() {
          @Override
          public int compare(TableColumnAccessor<?, ?> o1, TableColumnAccessor<?, ?> o2) {
            return compareInts(o1.getName().length(), o2.getName().length());
          }
        });
        list.setPrototypeCellValue(longest);
      }
    }

    private JComponent createListComponent(BaseAList<TableColumnAccessor<?, ?>> list, boolean searchSubstring) {
      list.setVisibleRowCount(12);
      list.setCanvasRenderer(TableColumnAccessor.NAME_RENDERER);
      setPrototypeCellValue(list);
      AComponentUtil.selectElementWhenAny(list);
      ListSpeedSearch.install(list).setSearchSubstring(searchSubstring);
      return new AScrollPane(list);
    }

    private JComponent createSelectedListPane() {
      JComponent listComponent = createListComponent(mySelectedList, false);
      setupRemoveButton();

      DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("d:grow, d:none", "d:none, 3dlu:none, d:grow"));
      JLabel label = builder.append("&Selected:");
      label.setLabelFor(listComponent);
      builder.add(listComponent, "1, 3, 1, 1, f, f");
      builder.add(myRemoveButton, "2, 3, 1, 1, l, t");

      return builder.getPanel();
    }

    private void setupRemoveButton() {
      mySelectedList.getSwingComponent().addKeyListener(UIUtil.pressButtonWithKey(myRemoveButton, KeyEvent.VK_DELETE, KeyEvent.VK_SUBTRACT));
      myRemoveButton.setMargin(new Insets(2, 2, 2, 2));
      Dimension psz = myRemoveButton.getPreferredSize();
      myRemoveButton.setPreferredSize(new Dimension(psz.width, psz.width));
      myRemoveButton.setToolTipText("Remove column (Del)");
    }

    @Override
    public void dispose() {
      myLife.dispose();
    }
  }
}
