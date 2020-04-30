package com.almworks.api.application.viewer;

import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * @author dyoma
 */
class CommentTextColumnAccessor<T extends Comment> extends BaseTableColumnAccessor<CommentState<T>, String> {
  private final CollectionEditor<CommentState<T>> myEditor;
  private final ColumnTooltipProvider<CommentState<T>> myTooltipProvider;
  private final CommentStateRenderer<T> myRenderer;

  public CommentTextColumnAccessor(ATable<CommentState<T>> table, TextAreaWrapper wrapper, boolean htmlContent,
    CommentRenderingHelper<T> helper)
  {
    super("");
    myTooltipProvider = new ColumnTooltipProvider<CommentState<T>>() {
      public String getTooltip(CellState cellState, CommentState<T> element, Point cellPoint, Rectangle cellRect) {
        return myRenderer.getTooltip(cellState, element, cellPoint, cellRect);
      }
    };
    myRenderer = new CommentStateRenderer<T>(wrapper, htmlContent, helper);
    myEditor = new CommentEditor(wrapper, table, myRenderer);
  }

  public String getValue(CommentState<T> state) {
    return state.getText();
  }

  public CollectionEditor<CommentState<T>> getDataEditor() {
    return myEditor;
  }

  public CollectionRenderer<CommentState<T>> getDataRenderer() {
    return myRenderer;
  }

  public ColumnTooltipProvider<CommentState<T>> getTooltipProvider() {
    return myTooltipProvider;
  }

  public CommentStateRenderer<T> getRenderer() {
    return myRenderer;
  }

  private class CommentEditor implements CollectionEditor<CommentState<T>> {
    private final ATable<CommentState<T>> myTable;
    private final MyJComponent<T> myComponent;

    public CommentEditor(TextAreaWrapper wrapper, ATable<CommentState<T>> table, CommentStateRenderer<T> renderer)
    {
      myTable = table;
      myComponent = new MyJComponent(wrapper.createEditorWrapper(), renderer);
    }

    public JComponent getEditorComponent(CellState state, CommentState<T> item) {
      myComponent.setData(state, item);
      myComponent.setBackground(state.getDefaultBackground());
      return myComponent;
    }

    public boolean startEditing(CommentState<T> item) {
      item.ensureExpanded(myTable);
      return true;
    }

    public boolean stopEditing(CommentState<T> item, boolean commitEditedData) {
      return true;
    }

    public boolean shouldEdit(EventObject event) {
      CommentState<T> selection;
      if (event instanceof MouseEvent) {
        MouseEvent e = (MouseEvent) event;
        if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1 )
          return false;
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), myTable);
        selection = myTable.getElementAt(p);
        if (selection == null || !myTable.isSelectedAtPoint(p))
          return false;
        return !CommentsController.processCommentMouseEvent(e, myTable, myRenderer) && !e.isConsumed();
      } else {
        if (myTable.getSelectionAccessor().getSelectedCount() != 1)
          return false;
        selection = myTable.getSelectionAccessor().getSelection();
        assert selection != null;
      }
      return !selection.isCollapsed();
    }

    public boolean shouldSelect(JTable table, int row, int column, CommentState<T> item) {
      return true;
    }
  }
  
  private static class MyJComponent<T extends Comment> extends JComponent {
    private final JLabel myPrefix = new JLabel();
    private final JTextField myAuthor = new JTextField();
    private final JTextField myDate = new JTextField();
    private final JLabel mySuffix = new JLabel();
    private final TextAreaWrapper myText;
    private final CommentStateRenderer<T> myRenderer;
    private CellState myState;
    private CommentState<T> myItem;

    public MyJComponent(TextAreaWrapper textAreaWrapper, CommentStateRenderer<T> renderer) {
      myText = textAreaWrapper;
      myRenderer = renderer;
      setOpaque(true);

      if(Aqua.isAqua()) {
        myAuthor.setBorder(null);
        myDate.setBorder(null);
      }

      CommunalFocusListener.setupJTextField(myAuthor);
      CommunalFocusListener.setupJTextField(myDate);

      add(myPrefix);
      add(myAuthor);
      add(myDate);
      add(mySuffix);
      add(myText.getComponent());

      UIUtil.adjustFont(myPrefix, -1, 0, false);
      UIUtil.adjustFont(mySuffix, -1, 0, false);
    }

    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      Color savedColor = g.getColor();
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setColor(savedColor);
    }

    public void doLayout() {
      myRenderer.layoutEditor(myState, myItem, this, myPrefix, myAuthor, myDate, mySuffix, myText.getComponent());
    }

    public void setData(CellState state, CommentState<T> item) {
      myState = state;
      myItem = item;

      final Color fg = myRenderer.getForegroundFor(item);
      
      myText.setTextForeground(fg);
      myText.setText(item.getText());
      myText.selectAll();

      myPrefix.setForeground(fg);
      myPrefix.setText(Util.NN(myRenderer.getHeaderPrefix(item)));

      myAuthor.setForeground(fg);
      myAuthor.setText(item.getWho());
      myAuthor.selectAll();

      myDate.setForeground(fg);
      myDate.setText(item.getWhen());
      myDate.selectAll();

      mySuffix.setForeground(fg);
      mySuffix.setText(Util.NN(myRenderer.getHeaderSuffix(item)));
    }
  }
}
