package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.widgets.GraphContext;
import com.almworks.util.ui.widgets.impl.IWidgetHostComponent;
import com.almworks.util.ui.widgets.impl.WidgetHostUtil;
import com.almworks.util.ui.widgets.util.CanvasWidget;
import com.almworks.util.ui.widgets.util.TextLeafCell;
import com.almworks.util.ui.widgets.util.list.ColumnListWidget;
import com.almworks.util.ui.widgets.util.list.ConstHeight;
import com.almworks.util.ui.widgets.util.list.ListSelectionProcessor;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class ColumnListWidgetDemo {
  public static final ConstHeight TABLE_POLICY = new ConstHeight(0, 0, null);
  public static final ColumnListWidget<String> LIST = new ColumnListWidget<String>(TABLE_POLICY, TABLE_POLICY);
  public static final Procedure2<GraphContext,Object> PREPAINT_SELECTION = new ListSelectionProcessor.PrePaintSelection(LIST);
  public static final TextLeafCell<String> TEXT_COLUMN = new TextLeafCell<String>("abc", true, Font.PLAIN, PREPAINT_SELECTION, Convertor.<String>identity());

  public static final CanvasWidget<String> RENDERED_COLUMN = new CanvasWidget<String>(new CanvasRenderer<String>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, String item) {
      canvas.appendInt(item.length());
    }
  }, new ListSelectionProcessor.StateFactory(LIST));

  static {
    LIST.resetColumnLayout(1, 0, 100);
    LIST.addWidget(TEXT_COLUMN);
    LIST.addWidget(RENDERED_COLUMN);
    LIST.setColumnPolicy(1, 0, 0, 25);
    ListSelectionProcessor<String> selection = new ListSelectionProcessor<String>();
    selection.install(LIST);
    selection.setSelectable(new Condition<String>() {
      @Override
      public boolean isAccepted(String value) {
        return !value.startsWith("-");
      }
    });
  }

  public static void main(String[] args) throws InvocationTargetException, InterruptedException {
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        WidgetHostUtil<AListModel<String>> host = WidgetHostUtil.create(LIST, FixedListModel.create("-AAAAAAAAAAAAA", "1", "2", "-BBBBBBBBBBBBBBBBB", "q", "w"));
        host.putWidgetData(IWidgetHostComponent.SELECTION_FOREGROUND, Color.WHITE);
        showInFrame(host.wrapWithScrollPane());
      }
    });
  }

  private static void showInFrame(JScrollPane scrollPane) {
    JFrame frame = new JFrame();
    frame.getContentPane().add(scrollPane);
    frame.pack();
    frame.setVisible(true);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  }
}
