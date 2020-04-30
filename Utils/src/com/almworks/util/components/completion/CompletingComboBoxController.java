package com.almworks.util.components.completion;

import com.almworks.util.Env;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory1;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.ComboboxModelAdapter;
import com.almworks.util.components.plaf.patches.MacComboBoxPatch;
import com.almworks.util.components.recent.AddRecentFromComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.recent.UnwrapCombo;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UndoUtil;
import org.almworks.util.Failure;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author dyoma
 */
public class CompletingComboBoxController<T> {
  private static final String DELETE_PREVIOUS = "delete-previous";
  private final AComboboxModel<T> myModel;
  private final Lifecycle myModelLife = new Lifecycle();
  private final RecentController<T> myRecents;
  private final CompletingComboBox<T> myComboBox;
  private final BoldingCanvasRenderer<T> myBoldingRenderer = new BoldingCanvasRenderer<T>();
  private Convertor<String, T> myFromString;
  private FilteringListDecorator<? extends T> myFiltered = FilteringListDecorator.create(AListModel.EMPTY);
  private boolean myEverFiltered = false;
  private Equality<T> myEquality = Equality.GENERAL;
  private CompletingDocument<T> myCurrentDocument = new CompletingDocument(new PlainDocument(), this, null);
  private final Procedure<T> mySetSelected = new Procedure<T>() {
    public void invoke(T arg) {
      setSelectedItem(arg);
    }
  };

  public CompletingComboBoxController() {
    this(new CompletingComboBox<T>(null));
  }

  CompletingComboBoxController(CompletingComboBox<T> comboBox) {
    MacComboBoxPatch.patch(comboBox);
    myRecents = new RecentController<T>(myFiltered, true);
    SelectionInListModel wrappersModel =
      SelectionInListModel.create(Lifespan.FOREVER, myRecents.getDecoratedModel(), null);
    myModel = new UnwrapCombo(wrappersModel);
    myBoldingRenderer.setRenderer(myRecents.getDecoratedRenderer());
    myComboBox = comboBox;
    comboBox.setController(this);
    myComboBox.setModel(new ComboboxModelAdapter<T>(wrappersModel));
    myComboBox.setCanvasRenderer(myBoldingRenderer);
//    myComboBox.setRenderer(new AComboBox.RendererAdapter<T>(Renderers.createRenderer(myBoldingRenderer), myComboBox));
    AddRecentFromComboBox.install(Lifespan.FOREVER, myRecents, myComboBox, myModel);
  }

  public CompletingComboBox<T> getComponent() {
    return myComboBox;
  }

  public void setConvertors(Convertor<? super T, String> toString, Convertor<String, T> fromString, @Nullable Equality<T> equality) {
    if (equality == null)
      equality = Equality.GENERAL;
    myEquality = equality;
    myFromString = fromString;
    myComboBox.setEditable(true);
    Component editorComponent = myComboBox.getEditor().getEditorComponent();
    JTextField editorField;
    if ((Env.isMac() || Env.isLinux()) && (editorComponent instanceof JTextField))
      editorField = (JTextField) editorComponent;
    else
      editorField = createEditorComponent();
    NotifingComboBoxEditor<T> editor = new NotifingComboBoxEditor<T>(toString, this, editorField);
    FieldListener.setup(editor, this);
    final JTextComponent textComponent = editor.getTextComponent();
    CompletingDocument newDocument = new CompletingDocument(textComponent.getDocument(), this, editor);
    myCurrentDocument.copyTo(newDocument);
    myCurrentDocument = newDocument;
    textComponent.setDocument(newDocument);
    Action deletePrev = textComponent.getActionMap().get(DELETE_PREVIOUS);
    if (deletePrev != null)
      textComponent.getActionMap().put(DELETE_PREVIOUS, new BackspaceAction(deletePrev));
    else
      assert false;
    myComboBox.setEditor(editor);
    hideAutoDropDown();
  }

  @Nullable
  public NotifingComboBoxEditor<T> getEditor() {
    //noinspection unchecked
    return Util.castNullable(NotifingComboBoxEditor.class, myComboBox.getEditor());
  }

  @Nullable
  public T getSelectedItem() {
    return myModel.getSelectedItem();
  }

  public void setSelectedItem(T item) {
    myRecents.setInitial(item);
    myModel.setSelectedItem(item);
  }

  public AComboboxModel<T> getModel() {
    return myModel;
  }

  public void stopEdit() {
    NotifingComboBoxEditor<T> editor = getEditor();
    if (editor != null) {
      T item = editor.getItem();
      T selected = myModel.getSelectedItem();
      if (!Util.equals(selected, item)) {
        myModel.setSelectedItem(item);
      }
    }
  }

  public T getItem(String text) {
    if (myFromString == null) {
      assert false;
      return null;
    }
    T newItem = myFromString.convert(text);
    int index = myModel.indexOf(newItem, myEquality);
    return index >= 0 ? myModel.getAt(index) : newItem;
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> renderer) {
    myRecents.setRenderer(renderer);
  }

  public void setCasesensitive(boolean casesensitive) {
    myCurrentDocument.setCasesensitive(casesensitive);
  }

  public boolean isCasesensitive() {
    return myCurrentDocument.isCasesensitive();
  }

  public void setMaxMatchesToShow(int maxMatchesToShow) {
    myCurrentDocument.setMaxMatchesToShow(maxMatchesToShow);
  }

  public void setMinCharsToShow(int minCharsToShow) {
    myCurrentDocument.setMinCharsToShow(minCharsToShow);
  }

  public void setFilterFactory(@Nullable Factory1<? extends Condition<? super T>, String> filterFactory) {
    myCurrentDocument.setFilterFactory(filterFactory);
    hideAutoDropDown();
  }

  public void setVariantsModel(AListModel<T> variants) {
    setVariantsModel(null, variants);
  }

  public void setIdentityConvertor(Convertor<? super T, String> identityConvertor) {
    myRecents.setIdentityConvertor(identityConvertor);
  }

  public void setVariantsModel(@Nullable Configuration recentConfig, AListModel<? extends T> variants) {
    myModelLife.cycle();
    myFiltered = FilteringListDecorator.create(myModelLife.lifespan(), variants);
    myFiltered.resynch();
    myRecents.setup(myFiltered, recentConfig);
    myEverFiltered = false;
    hideAutoDropDown();
  }

  public void setModel(AComboboxModel<T> model) {
    setModel(null, model);
  }

  public void setModel(@Nullable Configuration recentConfig, AComboboxModel<T> model) {
    setVariantsModel(recentConfig, model);
    SelectionInListModel.synchronizeSelection(myModelLife.lifespan(), model, myModel);
  }

  void setFilter(Condition<? super T> filter, String highlightText) {
    NotifingComboBoxEditor editor = getEditor();
    if (editor != null) myBoldingRenderer.setBoldText(highlightText);
    if (myEverFiltered && myFiltered.getFilter() == filter)
      return;
    myEverFiltered = true;
    if (editor != null)
      editor.startFilterUpdate();
    try {
      myFiltered.setFilter(filter);
      myFiltered.updateAll();
    } finally {
      if (editor != null)
        editor.stopFilterUpdate();
    }
  }

  void hideAutoDropDown() {
    hidePopup();
    setFilter(Condition.always(), null);
  }

  void hidePopup() {
    myComboBox.setPopupVisible(false);
  }

  void showPopup() {
    myComboBox.setPopupVisible(true);
  }

  boolean isPopupVisible() {
    return myComboBox.isPopupVisible();
  }

  private static final EmptyBorder EDITOR_BORDER = new EmptyBorder(0, 2, 0, 2);
  private static JTextField createEditorComponent() {
    return new JTextField() {
      {
        setBorder(EDITOR_BORDER);
      }
      public void setText(String s) {
        if (getText().equals(s)) {
          return;
        }
        super.setText(s);
      }
    };
  }

  public Procedure<T> getSetSelected() {
    return mySetSelected;
  }

  public RecentController<T> getRecents() {
    return myRecents;
  }

  private static class FieldListener implements FocusListener, DocumentListener, ChangeListener {
    private static final ComponentProperty<Boolean> SETUP_DONE = ComponentProperty.createProperty("setupDone");

    private final NotifingComboBoxEditor<?> myEditor;
    private final CompletingComboBoxController<?> myController;
    private boolean myDuringSelectAll = false;

    private FieldListener(NotifingComboBoxEditor<?> editor, CompletingComboBoxController<?> controller) {
      myEditor = editor;
      myController = controller;
    }

    public void focusGained(FocusEvent e) {
      selectAllText();
    }

    protected void selectAllText() {
      assert !myDuringSelectAll;
      myDuringSelectAll = true;
      try {
        myEditor.selectAll();
      } finally {
        assert myDuringSelectAll;
        myDuringSelectAll = false;
      }
    }

    public void focusLost(FocusEvent e) {
      myController.hidePopup();
    }

    public void insertUpdate(DocumentEvent e) {
      myEditor.textChanged(e);
    }

    public void removeUpdate(DocumentEvent e) {
      myEditor.textChanged(e);
    }

    public void changedUpdate(DocumentEvent e) {
      myEditor.textChanged(e);
    }

    public void stateChanged(ChangeEvent e) {
      if (myController.myCurrentDocument.isDocumentUpdate() || myDuringSelectAll)
        return;
      if (myEditor.isSetItemUpdate())
        return;
      myController.hideAutoDropDown();
    }

    public static void setup(NotifingComboBoxEditor<?> editor, CompletingComboBoxController<?> controller) {
      JTextField field = editor.getEditorComponent();
      if (SETUP_DONE.getClientValue(field) == Boolean.TRUE)
        return;
      FieldListener listener = new FieldListener(editor, controller);
      field.addFocusListener(listener);
      field.getDocument().addDocumentListener(listener);
      field.getCaret().addChangeListener(listener);
      UndoUtil.addUndoSupport(field);
      SETUP_DONE.putClientValue(field, Boolean.TRUE);
    }
  }

  @SuppressWarnings({"CloneableClassWithoutClone"})
  private static class BackspaceAction extends AbstractAction {
    private final Action myDefaultAction;

    public BackspaceAction(Action defaultAction) {
      myDefaultAction = defaultAction;
    }

    public void actionPerformed(ActionEvent event) {
      JTextComponent field = (JTextComponent) event.getSource();
      if (!isAutoCompleteState(field))
      {
        myDefaultAction.actionPerformed(event);
        return;
      }
      Document document = field.getDocument();
      int selectionStart = field.getSelectionStart();
      try {
        document.remove(selectionStart - 1, document.getLength() - selectionStart + 1);
      } catch (BadLocationException e) {
        throw new Failure(e);
      }
    }

    private static boolean isAutoCompleteState(JTextComponent field) {
      int documentLength = field.getDocument().getLength();
      if (documentLength == 0)
        return false;
      int selectionEnd = field.getSelectionEnd();
      int dot = field.getCaret().getDot();
      return (selectionEnd == 0 && dot == documentLength) ||
        (documentLength == selectionEnd && field.getSelectionStart() > 0 && dot == field.getSelectionStart());
    }
  }
}