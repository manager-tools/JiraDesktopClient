package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class AGridDemo {
  private final AGrid<String, String, String> myGrid = new AGrid<String, String, String>();
  private final OrderListModel<String> myColumnModel = OrderListModel.create("a", "b", "c");
  private final OrderListModel<String> myRowModel = OrderListModel.create("1", "2", "3");

  public AGridDemo() {
    myGrid.setColumnModel(Lifespan.FOREVER, myColumnModel);
    myGrid.setRowModel(Lifespan.FOREVER, myRowModel);
    myGrid.setCellModel(Lifespan.FOREVER, new AGridCellFunction<String, String, String>() {
      public String getValue(String row, String column, int rowIndex, int columnIndex) {
        return row + "+" + column;
      }
    });
    LabelRenderer<String> renderer = new LabelRenderer<String>() {
      {
        myLabel.setOpaque(false);
      }
      protected void setElement(String element, CellState state) {
        myLabel.setText(element);
      }
    };
    LabelRenderer<String> renderer2 = new LabelRenderer<String>() {
      {
        myLabel.setOpaque(false);
        myLabel.setHorizontalAlignment(SwingConstants.CENTER);
      }
      protected void setElement(String element, CellState state) {
        UIUtil.adjustFont(myLabel, -1, Font.BOLD, false);
        myLabel.setText("haba: " + element);
      }
    };
    myGrid.setCellRenderer(renderer);
    myGrid.setColumnHeaderRenderer(renderer2);
    myGrid.setRowHeaderRenderer(renderer2);
  }

  public static void main(String[] args) {
    LAFUtil.initializeLookAndFeel();
    new AGridDemo().run();
  }

  private void run() {
    Component grid = myGrid.getComponent();
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(grid, BorderLayout.CENTER);
    JButton bah = new JButton("BAH");
    panel.add(bah, BorderLayout.NORTH);
    panel.add(new JTextField(), BorderLayout.SOUTH);
    bah.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final Random r = new Random();
        new Timer(1000, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            OrderListModel<String> m = r.nextBoolean() ? myRowModel : myColumnModel;
            int idx = r.nextInt(m.getSize() + 1);
            m.insert(idx, String.valueOf(r.nextInt(100)));
          }
        }).start();
      }
    });
    DebugFrame.show(panel, 400, 400);
  }
}
