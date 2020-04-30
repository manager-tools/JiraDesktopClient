package com.almworks.util.components;

import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ValueModel;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static javax.swing.JComponent.WHEN_FOCUSED;

// IS: this class is full of spaghetti, thanks to Alex.
// Redesign is needed on first serious occasion.
public final class SubsetEditor<T> extends UIComponentWrapper2Support {
  private static final Pattern EMPTY_PATTERN = Pattern.compile("");
  private final DetachComposite myLife = new DetachComposite();
  private final Form myForm;
  private final SubsetModel<T> myModel;
  private final ImageBasedDecorator<T, T> myFilteredComplementModel;
  private final Function<String, T> myCreator;
  private final FilteringCondition myCondition;

  private SubsetEditor(final SubsetModel<T> model, Comparator<? super T> comparator, String selectedLabelText,
    String availableLabelText, boolean showReordering, @Nullable Convertor<T, String> filteringConvertor,
    @Nullable Function<String, T> creator)
  {
    myForm = new Form(filteringConvertor != null);
    myModel = model;
    myCreator = creator;

    if (selectedLabelText != null) {
      NameMnemonic.parseString(selectedLabelText).setToLabel(myForm.mySelectedLabel);
    }
    if (availableLabelText != null) {
      NameMnemonic.parseString(availableLabelText).setToLabel(myForm.myAvailableLabel);
    }

    AListModel<T> complementSet = model.getComplementSet();
    if(comparator != null) {
      complementSet = SortedListDecorator.create(complementSet, comparator);
    }
    
    if (filteringConvertor != null) {
      myCondition = new FilteringCondition(filteringConvertor);
      myFilteredComplementModel = FilteringListDecorator.<T>create(myLife, complementSet, myCondition);
      complementSet = myFilteredComplementModel;
    } else {
      myCondition = null;
      myFilteredComplementModel = null;
    }

    myForm.setModels(myLife, complementSet, model);

    myForm.myAdd.setAnAction(createAddAction(model));
    myForm.myRemove.setAnAction(createRemoveAction(model));
    if (showReordering) {
      myForm.myMoveUp.setAnAction(createMoveAction(model, false));
      myForm.myMoveDown.setAnAction(createMoveAction(model, true));
    } else {
      myForm.myMoveDown.setVisible(false);
      myForm.myMoveUp.setVisible(false);
    }

    if (creator != null) {
      myForm.initAddNew(model);
    } else {
      myForm.myAddNew.setVisible(false);
    }
  }

  private SimpleAction createAddAction(final SubsetModel<T> model) {
    CollectionCommandListener<T> listener = new CollectionCommandListener<T>() {
      public void onCollectionCommand(ACollectionComponent<T> aCollectionComponent, int index, T element) {
        doAdd(model);
      }
    };
    myForm.mySourceList.addDoubleClickListener(myLife, listener);
    myForm.mySourceList.addKeyCommandListener(myLife, listener, KeyEvent.VK_ENTER);

    return new SimpleAction("&Add", Icons.ARROW_LEFT) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(myForm.mySourceList.getSelectionAccessor());
        context.setEnabled(myForm.mySourceList.getSelectionAccessor().getSelectedCount() > 0);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        doAdd(model);
      }
    };
  }

  private void doAdd(SubsetModel<T> model) {
    SelectionAccessor<T> selectionAccessor = myForm.mySourceList.getSelectionAccessor();
    int index = selectionAccessor.getSelectedIndex();
    java.util.List<T> addItems = selectionAccessor.getSelectedItems();
    model.addFromComplementSet(addItems);
    updateSelection(myForm.mySourceList, index);
    updateComplementSelection(myForm.myTargetList, addItems);
  }

  private void updateSelection(FlatCollectionComponent<T> list, int index) {
    SelectionAccessor<T> selectionAccessor = list.getSelectionAccessor();
    int modelSize = list.getCollectionModel().getSize();
    if (modelSize > 0) {
      if (index < 0)
        selectionAccessor.ensureSelectionExists();
      else if (index >= modelSize)
        selectionAccessor.addSelectionIndex(modelSize - 1);
      else
        selectionAccessor.addSelectionIndex(index);
      list.scrollSelectionToView();
    }
  }

  private SimpleAction createRemoveAction(final SubsetModel<T> model) {
    CollectionCommandListener<T> listener = new CollectionCommandListener<T>() {
      public void onCollectionCommand(ACollectionComponent<T> aCollectionComponent, int index, T element) {
        doRemove(model);
      }
    };
    myForm.myTargetList.addDoubleClickListener(myLife, listener);
    myForm.myTargetList.addKeyCommandListener(myLife, listener, KeyEvent.VK_ENTER);
    myForm.myTargetList.addKeyCommandListener(myLife, listener, KeyEvent.VK_DELETE);

    return new SimpleAction("&Remove", Icons.ARROW_RIGHT) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(myForm.myTargetList.getSelectionAccessor());
        context.setEnabled(myForm.myTargetList.getSelectionAccessor().getSelectedCount() > 0);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        doRemove(model);
      }
    };
  }

  private void doRemove(SubsetModel<T> model) {
    SelectionAccessor<T> selectionAccessor = myForm.myTargetList.getSelectionAccessor();
    int index = selectionAccessor.getSelectedIndex();
    List<T> removeItems = selectionAccessor.getSelectedItems();
    model.removeAllAt(selectionAccessor.getSelectedIndexes());
    updateSelection(myForm.myTargetList, index);
    AList<T> complementList = myForm.mySourceList;
    updateComplementSelection(complementList, removeItems);
  }

  private void updateComplementSelection(AList<T> complementList, List<T> selection) {
    complementList.getSelectionAccessor().setSelected(selection);
    JViewport viewPort = SwingTreeUtil.findAncestorOfType(complementList, JViewport.class);
    if (viewPort != null) viewPort.validate();
    complementList.scrollSelectionToView();
  }

  private AnAction createMoveAction(final SubsetModel<T> model, final boolean down) {
    return new SimpleAction(down ? "Move &Down" : "Move &Up", down ? Icons.ARROW_DOWN : Icons.ARROW_UP) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(myForm.myTargetList.getSelectionAccessor());
        int[] indices = myForm.myTargetList.getSelectionAccessor().getSelectedIndexes();
        context.setEnabled(indices.length > 0);
        int bad = down ? myForm.myTargetList.getCollectionModel().getSize() - 1 : 0;
        for (int i : indices) {
          if (i == bad) {
            context.setEnabled(false);
            break;
          }
        }
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        AList<T> list = myForm.myTargetList;
        int collSize = myForm.myTargetList.getCollectionModel().getSize();
        int[] indices = list.getSelectionAccessor().getSelectedIndexes();
        Arrays.sort(indices);
        int increment = down ? -1 : 1;
        for (int i = down ? indices.length - 1 : 0; i >= 0 && i < indices.length; i += increment) {
          int index = indices[i];
          int newInd = index - increment;
          if (newInd >= 0 && newInd < collSize) {
            model.swap(index, newInd);
          }
        }
      }
    };
  }

  public static <T> SubsetEditor<T> create(final SubsetModel<T> model, Comparator<T> comparator,
    String selectedLabelText, String availableLabelText, boolean showReordering)
  {
    return new SubsetEditor<T>(model, comparator, selectedLabelText, availableLabelText, showReordering, null, null);
  }

  public static <T> SubsetEditor<T> create(final SubsetModel<T> model, boolean showReordering, Comparator<T> comparator, Convertor<T, String> filteringConvertor, @Nullable Function<String, T> creator)
  {
    return new SubsetEditor<T>(model, comparator, null, null, showReordering, filteringConvertor, creator);
  }

  public JComponent getComponent() {
    return myForm.mySizingPanel;
  }

  public Detach getDetach() {
    return myLife;
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
    myForm.setCanvasRenderer(renderer);
  }

  private class Form {
    private AActionButton myAdd;
    private AActionButton myMoveDown;
    private AActionButton myMoveUp;
    private AActionButton myRemove;
    private AActionButton myAddNew;
    private AList<T> mySourceList;
    private AList<T> myTargetList;
    private JLabel myAvailableLabel;
    private JLabel mySelectedLabel;
    private JPanel myWholePanel;
    private JTextField mySearchText;
    private JPanel mySearchPanel;
    private JScrollPane mySourceListScrollpane;

    public final JPanel mySizingPanel = new JPanel(new SingleChildLayout(SingleChildLayout.CONTAINER)) {
      public Dimension getPreferredSize() {
        return UIUtil.getPreferredSizePropped(super.getPreferredSize(), 500, 0);
      }
    };

    public Form(boolean filtering) {
      mySelectedLabel.setLabelFor(myTargetList.getScrollable());
      myAvailableLabel.setLabelFor(filtering ? mySearchText : mySourceList.getScrollable());
      mySizingPanel.add(myWholePanel);
      myAvailableLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
      mySelectedLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
      Aqua.disableMnemonics(myWholePanel);

      if(filtering) {
        initExtendedSearch();
      } else {
        myWholePanel.remove(mySearchPanel);
        FormLayout layout = (FormLayout) myWholePanel.getLayout();
        CellConstraints cc = layout.getConstraints(mySourceListScrollpane);
        layout.setRowSpec(cc.gridY - 1, new RowSpec("0px"));
        layout.setRowSpec(cc.gridY - 2, new RowSpec("0px"));
      }
    }

    public void initAddNew(SubsetModel<T> model) {
      myAddNew.setMargin(new Insets(2, 4, 2, 4));
      myAddNew.setAnAction(new AddNewAction());
    }

    private void initExtendedSearch() {
      mySearchText.getDocument().addDocumentListener(new DocumentAdapter() {
        protected void documentChanged(DocumentEvent e) {
          doTextSearch();
        }
      });

      final KeyStroke enter = Shortcuts.ksPlain(KeyEvent.VK_ENTER);
      mySearchText.addKeyListener(UIUtil.pressButtonWithKeyStroke(myAddNew, enter));

      final Action browse = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          mySourceList.getSelectionAccessor().ensureSelectionExists();
          mySourceList.getScrollable().requestFocusInWindow();
        }
      };

      final KeyStroke up = Shortcuts.ksPlain(KeyEvent.VK_UP);
      final KeyStroke dn = Shortcuts.ksPlain(KeyEvent.VK_DOWN);
      action(WHEN_FOCUSED, mySearchText, browse, up);
      action(WHEN_FOCUSED, mySearchText, browse, dn);
      action(WHEN_FOCUSED, myAddNew, browse, up);
      action(WHEN_FOCUSED, myAddNew, browse, dn);
    }

    private void doTextSearch() {
      final Pattern pattern = getSearchPattern();
      if(pattern != null) {
        myCondition.setPattern(pattern);
      }
    }

    private Pattern getSearchPattern() {
      try {
        final String s = mySearchText.getText().trim();
        return Pattern.compile(s, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
      } catch(PatternSyntaxException e) {
        return null;
      }
    }

    private void action(int condition, JComponent component, Action action, KeyStroke keyStroke) {
      component.getInputMap(condition).put(keyStroke, action);
      component.getActionMap().put(action, action);
    }

    public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
      mySourceList.setCanvasRenderer(renderer);
      myTargetList.setCanvasRenderer(renderer);
      ListSpeedSearch.install(mySourceList);
      ListSpeedSearch.install(myTargetList);
    }

    public void setModels(Lifespan life, AListModel<T> source, SubsetModel<T> target) {
      myLife.add(mySourceList.setCollectionModel(source));
      myLife.add(AComponentUtil.selectNewElements(mySourceList));
      myLife.add(myTargetList.setCollectionModel(target));
      myLife.add(AComponentUtil.selectNewElements(myTargetList));
      mySourceList.getSelectionAccessor().ensureSelectionExists();
      myTargetList.getSelectionAccessor().ensureSelectionExists();
    }

    private class AddNewAction extends SimpleAction {
      public AddNewAction() {
        super("", Icons.ACTION_GENERIC_ADD);
      }

      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(mySearchText.getDocument());
        context.setEnabled(mySearchText.getText().trim().length() > 0);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        final SelectionAccessor<T> accessor = mySourceList.getSelectionAccessor();
        T item = accessor.getSelection();
        boolean clearText = false;
        if(item == null) {
          assert myCreator != null;
          item = myCreator.invoke(mySearchText.getText().trim());
          if(item != null) {
            clearText = true;
          }
        }
        if(item != null) {
          myModel.add(item);
          updateComplementSelection(myForm.myTargetList, Collections.singletonList(item));
          UIUtil.requestFocusInWindowLater(mySearchText);
        }
        if(clearText) {
          mySearchText.setText("");
        } else {
          doTextSearch();
        }
      }
    }
  }


  private class FilteringCondition extends Condition<T> {
    private final ValueModel<Pattern> myPattern;
    private Convertor<T, String> myConvertor;

    private FilteringCondition(Convertor<T, String> convertor) {
      myConvertor = convertor;
      assert convertor != null;
      myPattern = ValueModel.create();
      myPattern.setValue(EMPTY_PATTERN);
      myPattern.addChangeListener(myLife, new ChangeListener() {
        public void onChange() {
          myFilteredComplementModel.resynch();
          SelectionAccessor<T> accessor = myForm.mySourceList.getSelectionAccessor();
          accessor.clearSelection();
          accessor.ensureSelectionExists();
        }
      });
    }

    public boolean isAccepted(T value) {
      String stringValue = myConvertor.convert(value);
      if (stringValue == null)
        return true;
      Pattern pattern = myPattern.getValue();
      Matcher matcher = pattern.matcher(stringValue);
      return matcher.find();
    }

    public void setPattern(Pattern pattern) {
      myPattern.setValue(pattern);
    }
  }
}
