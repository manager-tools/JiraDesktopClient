package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.ComboBoxModelHolder;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.macosx.combobox.MacPrettyComboBox;
import com.almworks.util.components.renderer.ListCanvasWrapper;
import com.almworks.util.components.renderer.ListCellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionListenerBridge;
import com.almworks.util.ui.actions.AnActionListener;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author : Dyoma
 */
public class AComboBox<T> extends JComponent {
  private final JComboBox myCombobox;
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final Lifecycle mySwingLife = new Lifecycle(false);
  private int myColumns = 10;
  private int myMinColumns = 10;
  private boolean myMaxToPref = false;
  private final ComboBoxModelHolder<T> myModelHolder = new ComboBoxModelHolder<T>();

  public AComboBox() {
    this(10);
  }

  public AComboBox(int columns) {
    this(new MyJComboBox(), columns);
  }

  public AComboBox(JComboBox hostComponent, int columns) {
    myCombobox = hostComponent;
    myColumns = columns;
    myCombobox.setModel(new ComboboxModelAdapter<T>(myModelHolder));
    myCombobox.setOpaque(false);
    setOpaque(false);
    add(myCombobox);
    KeyStroke altDown = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
    getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(altDown, "altDown");
    getActionMap().put("altDown", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
//        if (!myCombobox.isPopupVisible()) {
        myCombobox.requestFocusInWindow();
        myCombobox.setPopupVisible(true);
//        }
      }
    });
  }

  public void updateUI() {
  }

  /**
   * @deprecated
   */
  public void layout() {
    myCombobox.setBounds(0, 0, getWidth(), getHeight());
  }

  public Dimension getMaximumSize() {
    return myMaxToPref ? getPreferredSize() : myCombobox.getMaximumSize();
  }

  public void requestFocus() {
    myCombobox.requestFocus();
  }

  public boolean requestFocusInWindow() {
    return myCombobox.requestFocusInWindow();
  }

  public void setFocusable(boolean focusable) {
    super.setFocusable(focusable);
    myCombobox.setFocusable(focusable);
    ComboBoxEditor editor = getEditor();
    if (editor != null) {
      Component editorComponent = editor.getEditorComponent();
      if (editorComponent != null) {
        editorComponent.setFocusable(focusable);
      }
    }
  }

  public Dimension getMinimumSize() {
    Dimension minimumSize = myCombobox.getMinimumSize();
    if (myMinColumns > 0) {
      int preferredWidth = UIUtil.getColumnWidth(myCombobox) * myMinColumns;
      if (minimumSize.width > preferredWidth)
        minimumSize = new Dimension(preferredWidth, minimumSize.height);
    }
    return minimumSize;
  }

  public Dimension getPreferredSize() {
    Dimension preferredSize = myCombobox.getPreferredSize();
    if (myColumns > 0)
      preferredSize = new Dimension(UIUtil.getColumnWidth(myCombobox) * myColumns, preferredSize.height);
    return preferredSize;
  }

  public int getColumns() {
    return myColumns;
  }

  public void setColumns(int columns) {
    if (columns != myColumns) {
      myColumns = columns;
      myMinColumns = columns;
      invalidate();
    }
  }

  public void setMinColumns(int columns) {
    if (columns != myMinColumns) {
      myMinColumns = columns;
      invalidate();
    }
  }

  public void setMaxToPref(boolean maxToPref) {
    myMaxToPref = maxToPref;
  }

  public void setMaximumSize(Dimension maximumSize) {
    myCombobox.setMaximumSize(maximumSize);
  }

  public void setMinimumSize(Dimension minimumSize) {
    myCombobox.setMinimumSize(minimumSize);
  }

  public void setPreferredSize(Dimension preferredSize) {
    myCombobox.setPreferredSize(preferredSize);
  }

  public void setRenderer(CollectionRenderer<? super T> renderer) {
    myCombobox.setRenderer(new RendererAdapter<T>(renderer, myCombobox));
  }

  public void setToolTipText(String text) {
    myCombobox.setToolTipText(text);
  }

  public synchronized void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    myCombobox.setEnabled(enabled);
  }

  public synchronized boolean isEnabled() {
    return myCombobox.isEnabled();
  }

  public void setModel(AComboboxModel<T> model) {
    myModelHolder.setModel(model);
  }

  public AComboboxModel<T> getModel() {
    return myModelHolder;
  }

  public Detach setEditor(ComboBoxEditor editor) {
    final ComboBoxEditor lastEditor = getEditor();
    if (editor == null || lastEditor == null)
      return Detach.NOTHING;
    myCombobox.setEditor(editor);
    return new Detach() {
      protected void doDetach() {
        setEditor(lastEditor);
      }
    };
  }

  public void setEditable(boolean isEditable) {
    myCombobox.setEditable(isEditable);
  }

  public boolean isEditable() {
    return myCombobox.isEditable();
  }

  public ComboBoxEditor getEditor() {
    return myCombobox.getEditor();
  }

  public Detach addActionListener(AnActionListener listener) {
    final ActionListener bridge = ActionListenerBridge.listener(listener);
    myCombobox.addActionListener(bridge);
    return new Detach() {
      protected void doDetach() {
        myCombobox.removeActionListener(bridge);
      }
    };
  }

  public void setBorder(Border border) {
    myCombobox.setBorder(border);
  }

  public void setMaximumRowCount(int count) {
    myCombobox.setMaximumRowCount(count);
  }

  public void addNotify() {
    super.addNotify();
    if (mySwingLife.cycleStart())
      listenChanges();
  }

  private void listenChanges() {
    UIUtil.addActionListener(getDisplayableLifespan(), myCombobox, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myModifiable.fireChanged();
      }
    });
  }

  public Lifespan getDisplayableLifespan() {
    return mySwingLife.lifespan();
  }

  public void removeNotify() {
    mySwingLife.cycleEnd();
    super.removeNotify();
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public JComboBox getCombobox() {
    return myCombobox;
  }

  public void setCanvasRenderer(CanvasRenderer<? super T> canvasRenderer) {
    if (myCombobox instanceof MyJComboBox)
      ((MyJComboBox) myCombobox).setCanvasRenderer(canvasRenderer);
    else
      setRenderer(Renderers.createRenderer(canvasRenderer));
  }

  public static class RendererAdapter<T> extends ListCellRendererAdapter<T> {
    private final JComboBox myComboBox;

    public RendererAdapter(CollectionRenderer<? super T> collectionRenderer, JComboBox comboBox) {
      super(collectionRenderer);
      myComboBox = comboBox;
    }

    public ListCellState createCellState(JList list, boolean isSelected, boolean cellHasFocus, int index) {
      return new ListCellState(list, isSelected, cellHasFocus, index) {
        public boolean isEnabled() {
          return myComboBox.isEnabled();
        }

        @Override
        public Color getBackground() {
          return getBackground(myComboBox.isOpaque());
        }

        public Color getBackground(boolean opaque) {
          boolean transparent = !Env.isWindows() && !Env.isMacLeopardOrNewer() && !opaque && isExtracted();
          return transparent ? null : super.getBackground(opaque);
        }
      };
    }
  }

  private static class MyJComboBox<T> extends MacPrettyComboBox {
    private static final boolean IS_MAC = Aqua.isAqua();
    private final MyWrapper<T> myCanvasWrapper = new MyWrapper<T>();

    private MyJComboBox() {
      myCanvasWrapper.setLafRenderer(getRenderer());
      setRenderer(myCanvasWrapper);
    }

    public void updateUI() {
      if (myCanvasWrapper == null) {
        super.updateUI();
        return;
      }
      ListCellRenderer cellRenderer = getRenderer();
      boolean temporaryRemove = cellRenderer == myCanvasWrapper;
      if (temporaryRemove)
        setRenderer(null);
      super.updateUI();
      if (temporaryRemove) {
        myCanvasWrapper.setLafRenderer(getRenderer());
        setRenderer(myCanvasWrapper);
      }
    }

    public void setCanvasRenderer(CanvasRenderer<? super T> canvasRenderer) {
      myCanvasWrapper.setCanvasRenderer(canvasRenderer);
      setRenderer(myCanvasWrapper);
    }

    private class MyWrapper<T> extends ListCanvasWrapper<T> {
      private MyWrapper() {
        super(false);
      }

      @Override
      protected Component prepareRendererComponent(JList list, Object value, int index, boolean isSelected,
        boolean cellHasFocus, boolean useBgRenderer)
      {
        final Component c = super.prepareRendererComponent(list, value, index, isSelected, cellHasFocus, useBgRenderer);
        if(IS_MAC) {
          myCanvasWrapper.getRenderer().transferCanvasDecorationsToBackground();
        }
        return c;
      }
    }
  }
}