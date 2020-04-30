package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AList;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.FlatCollectionComponent;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.widgets.CellContext;
import com.almworks.util.ui.widgets.HostCell;
import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.genutil.Log;
import com.almworks.util.ui.widgets.impl.HostComponentState;
import com.almworks.util.ui.widgets.impl.WidgetHostComponent;
import com.almworks.util.ui.widgets.util.ComponentWidget;
import com.almworks.util.ui.widgets.util.GridWidget;
import com.almworks.util.ui.widgets.util.SegmentsLayout;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class GridDemo implements Runnable {
  private static final Log<GridDemo> log = Log.get(GridDemo.class);

  @Override
  public void run() {
    final AList<MyLocation> list = new AList<MyLocation>();
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.add(new JScrollPane(list), BorderLayout.CENTER);
    JPanel bottom = new JPanel(new GridLayout(1, 2, 5, 5));
    panel.add(bottom, BorderLayout.SOUTH);
    GridWidget<MyLocation> outter = createOutterGrid();
    bottom.add(createHost(list, outter));
    bottom.add(createHost(list, outter));
    final JFrame frame = DebugFrame.show(panel);
    list.setCanvasRenderer(new CanvasRenderer<MyLocation>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, MyLocation item) {
        canvas.appendText(item.myName);
        canvas.appendText(": ");
        canvas.appendText(item.myX);
        canvas.appendText("x");
        canvas.appendText(item.myY);
      }
    });
    final MyLocation counter = new MyLocation("Counter", "", "");
    setListData(list,
        new MyLocation("A", "1", "2"),
        new MyLocation("B", "10", "20"),
        new MyLocation("C", "100", "200"),
        counter);
//    SwingUtilities.invokeLater(new Runnable() {
//      private int myCounter = 0;
//      private long myStart = System.currentTimeMillis();
//      @Override
//      public void run() {
//        myCounter++;
//        int size = myCounter % 200 + 200;
//        frame.setSize(size, size);
//        int modelSize = list.getCollectionModel().size();
//        int index = myCounter % (modelSize + 1);
//        if (index < modelSize) list.getSelectionAccessor().setSelectedIndex(index);
//        else list.getSelectionAccessor().clearSelection();
//        if (myCounter % 11 == 0) counter.setX(String.valueOf(myCounter));
//        if (myCounter % 1000 == 0) {
//          long now = System.currentTimeMillis();
//          counter.setY(String.valueOf(now - myStart));
//          myStart = now;
//        }
//        SwingUtilities.invokeLater(this);
//      }
//    });
  }

  public static void main(String[] args) {
    ThreadGate.AWT.execute(new GridDemo());
  }

  private static void setListData(FlatCollectionComponent<MyLocation> list, MyLocation ... locations) {
    final OrderListModel<MyLocation> model = OrderListModel.create(locations);
    for (final MyLocation location : locations) {
      location.myModifiable.addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
        @Override
        public void onChange() {
          int index = model.indexOf(location);
          model.updateAt(index);
        }
      });
    }
    list.setCollectionModel(model);
  }

  private static Component createHost(final AList<MyLocation> list, Widget<MyLocation> widget) {
    WidgetHostComponent host = new WidgetHostComponent();
    final HostComponentState<MyLocation> state = host.createState();
    state.setWidget(widget);
    list.getSelectionAccessor().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      @Override
      public void onChange() {
        MyLocation location = list.getSelectionAccessor().getSelection();
        state.setValue(location);
      }
    });
    host.setState(state);
    return host;
  }

  private static GridWidget<MyLocation> createOutterGrid() {
    GridWidget<MyLocation> grid = new GridWidget();
    SegmentsLayout columns = new SegmentsLayout(0, 0);
    columns.setSegmentCount(2);
    columns.setSegmentPolicy(0, 0, 0);
    columns.setSegmentPolicy(1, 1, 1);
    SegmentsLayout rows = new SegmentsLayout(0, 0);
    rows.setSegmentCount(2);
    grid.setLayout(columns, rows);
    grid.setChild(0, 0, FocusBorderWidget.wrap(new ButtonWidget("Name")));
    grid.setChild(1, 0, FocusBorderWidget.wrap(new EditTextWidget(new MyTextAccessor() {
      @Override
      public String getText(MyLocation value) {
        return value.myName;
      }

      @Override
      public void setText(MyLocation value, String text) {
        value.setName(text);
      }
    })));
    grid.setChild(0, 1, FocusBorderWidget.wrap(new ButtonWidget("Location")));
    grid.setChild(1, 1, createInnerGrid());
    return grid;
  }

  private static GridWidget<MyLocation> createInnerGrid() {
    GridWidget<MyLocation> innerGrid = new GridWidget<MyLocation>();
    SegmentsLayout innerColumns = new SegmentsLayout(0, 0);
    innerColumns.setSegmentCount(4);
    innerColumns.setSegmentPolicy(0, 0, 0);
    innerColumns.setSegmentPolicy(1, 1, 1);
    innerColumns.setSegmentPolicy(2, 0, 0);
    innerColumns.setSegmentPolicy(3, 1, 1);
    SegmentsLayout innerRows = new SegmentsLayout(0, 0);
    innerRows.setSegmentCount(1);
    innerGrid.setLayout(innerColumns, innerRows);
    innerGrid.setChild(0, 0, FocusBorderWidget.wrap(new ButtonWidget("X", false)));
    innerGrid.setChild(1, 0, FocusBorderWidget.wrap(new EditTextWidget(new MyTextAccessor() {
      @Override
      public String getText(MyLocation value) {
        return value.myX;
      }

      @Override
      public void setText(MyLocation value, String text) {
        value.setX(text);
      }
    })));
    innerGrid.setChild(2, 0, FocusBorderWidget.wrap(new ButtonWidget("Y", false)));
    innerGrid.setChild(3, 0, FocusBorderWidget.wrap(new EditTextWidget(new MyTextAccessor() {
      @Override
      public String getText(MyLocation value) {
        return value.myY;
      }

      @Override
      public void setText(MyLocation value, String text) {
        value.setY(text);
      }
    })));
    return innerGrid;
  }

  private static class MyLocation {
    private final SimpleModifiable myModifiable = new SimpleModifiable();
    private String myName;
    private String myX;
    private String myY;

    private MyLocation(String name, String x, String y) {
      myName = name;
      myX = x;
      myY = y;
    }

    public void setName(String text) {
      if (myName.equals(text)) return;
      myName = text;
      myModifiable.fireChanged();
    }

    public void setX(String text) {
      if (myX.equals(text)) return;
      myX = text;
      myModifiable.fireChanged();
    }

    public void setY(String text) {
      if (myY.equals(text)) return;
      myY = text;
      myModifiable.fireChanged();
    }
  }

  private static class ButtonWidget extends ComponentWidget<MyLocation> {
    private final String myText;
    private final Boolean myFocusable;

    private ButtonWidget(String text, Boolean focusable) {
      super(FILL_BOTH);
      myText = text;
      myFocusable = focusable;
    }

    private ButtonWidget(String text) {
      this(text, null);
    }

    private ButtonWidget(String text, boolean focusable) {
      this(text, Boolean.valueOf(focusable));
    }

    @Override
    protected JComponent obtainComponent(HostCell cell) {
      JButton button = new JButton(myText);
      if (myFocusable != null) button.setFocusable(myFocusable);
      return button;
    }

    @Nullable
    @Override
    protected Dimension getPreferedSize(CellContext context, MyLocation value) {
      return null;
    }
  }

  private abstract static class MyTextAccessor implements EditTextWidget.TextAccessor<MyLocation> {
    @Override
    public <X> boolean addListener(Lifespan life, final Procedure<X> listener, final X callbackValue, MyLocation value) {
      value.myModifiable.addAWTChangeListener(life, new ChangeListener() {
        @Override
        public void onChange() {
          listener.invoke(callbackValue);
        }
      });
      return true;
    }
  }
}
