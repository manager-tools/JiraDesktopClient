package com.almworks.util.components.completion;

import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory1;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.*;

class CompletingDocument<T> implements Document {
  private final Document myDelegate;
  private final CompletingComboBoxController<T> myController;
  @Nullable
  private final NotifingComboBoxEditor<T> myEditor;
  @Nullable
  private Factory1<? extends Condition<? super T>, String> myFilterFactory = null;
  private boolean myCasesensitive = true;
  private boolean myDocumentUpdate = false;
  @Nullable
  private String myLastFilterText = null;
  private int myMaxMatchesToShow = 0;
  private int myMinCharsToShow = 0;

  public CompletingDocument(Document delegate, CompletingComboBoxController controller, @Nullable NotifingComboBoxEditor<T> editor) {
    myDelegate = delegate;
    myController = controller;
    myEditor = editor;
  }

  public void copyTo(CompletingDocument<T> other) {
    other.myMaxMatchesToShow = myMaxMatchesToShow;
    other.myMinCharsToShow = myMinCharsToShow;
    other.myCasesensitive = myCasesensitive;
    other.myFilterFactory = myFilterFactory;
  }

  public void setFilterFactory(@Nullable Factory1<? extends Condition<? super T>, String> filterFactory) {
    myFilterFactory = filterFactory;
  }

  public boolean isCasesensitive() {
    return myCasesensitive;
  }

  public void setCasesensitive(boolean casesensitive) {
    myCasesensitive = casesensitive;
  }

  public boolean isDocumentUpdate() {
    return myDocumentUpdate;
  }

  public void setMaxMatchesToShow(int maxMatchesToShow) {
    myMaxMatchesToShow = maxMatchesToShow;
  }

  public void setMinCharsToShow(int minCharsToShow) {
    myMinCharsToShow = minCharsToShow;
  }

  public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
    if (myEditor == null || myEditor.isSetItemUpdate()) {
      insertToDelegate(offset, str, a);
      return;
    }
    Convertor<? super T, String> toString = myEditor.getToString();
    if (toString == null) {
      insertToDelegate(offset, str, a);
      return;
    }
    int typedEnd = getLength();
    if (offset == typedEnd) {
      final String typedText = getText(0, typedEnd) + str;
      int matches = updateFilteredPopup(typedText);
      if (matches == 1 && myFilterFactory == null) {
        AComboboxModel<T> model = myController.getModel();
        int count = model.getSize();
        if (count != 1) {
          assert false : model;
        } else {
          String fullText = toString.convert(model.getAt(0));
          assert (myCasesensitive && fullText.startsWith(typedText)) ||
            (!myCasesensitive && fullText.toLowerCase().startsWith(typedText.toLowerCase())) : fullText + "  " + typedText;
          String completion = fullText.substring(offset);
          insertToDelegate(offset, completion, a);
          JTextComponent textComponent = myEditor.getTextComponent();
          int completedTo = offset + str.length();
          assert !myDocumentUpdate;
          myDocumentUpdate = true;
          try {
            textComponent.setCaretPosition(getLength());
            textComponent.moveCaretPosition(completedTo);
          } finally {
            assert myDocumentUpdate;
            myDocumentUpdate = false;
          }
          return;
        }
      }
    }
    insertToDelegate(offset, str, a);
  }

  /**
   * returns the number of matched items
   */
  private int updateFilteredPopup(final String typedText) {
    if (typedText.length() == 0) {
      myController.hideAutoDropDown();
      myLastFilterText = null;
      return 0;
    }
    myLastFilterText = typedText;
    startAutoComplete();
    AComboboxModel<T> model = myController.getModel();
    int matches = model.getSize();
    if (matches > 0
      && (myMaxMatchesToShow <= 0 || matches <= myMaxMatchesToShow)
      && (myMinCharsToShow <= 0 || typedText.length() >= myMinCharsToShow))
    {
      if (!myController.isPopupVisible()) myController.showPopup();
    } else {
      myController.hideAutoDropDown();
    }
    return matches;
  }

  private void startAutoComplete() {
    Condition<? super T> filter = createFilter();
    myController.setFilter(filter, myFilterFactory != null ? myLastFilterText : null);
  }

  private Condition<? super T> createFilter() {
    if (myFilterFactory != null)
      return myFilterFactory.create(myLastFilterText);
    if (myEditor == null) return Condition.always();
    final Convertor<? super T, String> toString = myEditor.getToString();
    assert toString != null;
    return new Condition<T>() {
      public boolean isAccepted(T value) {
        String str = toString.convert(value);
        String prefix;
        String lft = Util.NN(myLastFilterText);
        if (myCasesensitive) {
          prefix = lft;
        } else {
          str = Util.lower(str);
          prefix = Util.lower(lft);
        }
        return str != null && str.startsWith(prefix);
      }
    };
  }

  public void remove(int offs, int len) throws BadLocationException {
    if (myEditor == null || !myEditor.isSetItemUpdate()) {
      int typedEnd = getLength();
      if (offs == typedEnd - len) {
        updateFilteredPopup(getText(0, offs));
      }
    }
    removeFromDelegate(offs, len);
  }

  private void insertToDelegate(int offset, String str, AttributeSet a) throws BadLocationException {
    assert !myDocumentUpdate;
    myDocumentUpdate = true;
    try {
      myDelegate.insertString(offset, str, a);
    } finally {
      assert myDocumentUpdate;
      myDocumentUpdate = false;
    }
  }

  private void removeFromDelegate(int offs, int len) throws BadLocationException {
    assert !myDocumentUpdate;
    myDocumentUpdate = true;
    try {
      myDelegate.remove(offs, len);
    } finally {
      assert myDocumentUpdate;
      myDocumentUpdate = false;
    }
  }

  public int getLength() {
    return myDelegate.getLength();
  }

  public void addDocumentListener(DocumentListener listener) {
    myDelegate.addDocumentListener(listener);
  }

  public void removeDocumentListener(DocumentListener listener) {
    myDelegate.removeDocumentListener(listener);
  }

  public void addUndoableEditListener(UndoableEditListener listener) {
    myDelegate.addUndoableEditListener(listener);
  }

  public void removeUndoableEditListener(UndoableEditListener listener) {
    myDelegate.removeUndoableEditListener(listener);
  }

  public Object getProperty(Object key) {
    return myDelegate.getProperty(key);
  }

  public void putProperty(Object key, Object value) {
    myDelegate.putProperty(key, value);
  }

  public String getText(int offset, int length) throws BadLocationException {
    return myDelegate.getText(offset, length);
  }

  public void getText(int offset, int length, Segment txt) throws BadLocationException {
    myDelegate.getText(offset, length, txt);
  }

  public Position getStartPosition() {
    return myDelegate.getStartPosition();
  }

  public Position getEndPosition() {
    return myDelegate.getEndPosition();
  }

  public Position createPosition(int offs) throws BadLocationException {
    return myDelegate.createPosition(offs);
  }

  public Element[] getRootElements() {
    return myDelegate.getRootElements();
  }

  public Element getDefaultRootElement() {
    return myDelegate.getDefaultRootElement();
  }

  public void render(Runnable r) {
    myDelegate.render(r);
  }
}
