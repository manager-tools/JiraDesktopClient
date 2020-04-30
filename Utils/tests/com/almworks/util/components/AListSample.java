package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;

/**
 * @author : Dyoma
 */
public class AListSample {
  private JComponent myWholePanel;
  private JTextField myNewElement;
  private JButton myUp;
  private JButton myDown;
  private JButton myAdd;
  private AList<String> myList;
  private final OrderListModel<String> myModel = new OrderListModel<String>();

  public AListSample() {
    myList.setCollectionModel(myModel);
    myList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel selection = (ListSelectionModel) e.getSource();
        int min = selection.getMinSelectionIndex();
        if (min == -1) {
          myUp.setEnabled(false);
          myDown.setEnabled(false);
          return;
        }
        myUp.setEnabled(min > 0);
        myDown.setEnabled(selection.getMaxSelectionIndex() < myModel.getSize() - 1);
      }
    });
    myUp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int[] selection = myList.getSelectionAccessor().getSelectedIndexes();
        for (int i = 0; i < selection.length; i++) {
          int index = selection[i];
          myModel.swap(index - 1, index);
        }
      }
    });
    myDown.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int[] selection = myList.getSelectionAccessor().getSelectedIndexes();
        for (int i = selection.length - 1; i >= 0; i--) {
          int index = selection[i];
          myModel.swap(index, index + 1);
        }
      }
    });
    myAdd.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int index = myModel.addElement(myNewElement.getText());
        myList.setSelectionIndex(index);
      }
    });
  }

  public JComponent getWholePanel() {
    return myWholePanel;
  }

  public AList<String> getList() {
    return myList;
  }

  public static void main(String[] args) {
    LAFUtil.initializeLookAndFeel();
    AListSample sample = new AListSample();
    JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(sample.getWholePanel());
    AList<String> sortedView = new AList<String>();
    SortedListDecorator<String> sorted = SortedListDecorator.createWithoutComparator(sample.myModel);
    sortedView.setCollectionModel(sorted);
    JPanel sortPanel = createSortedPanel(sortedView, sorted);
    panel.add(sortPanel);
    JFrame frame = LinkSample.showFrame(panel, "AList Sample");
    frame.getRootPane().setDefaultButton(sample.myAdd);
  }

  private static JPanel createSortedPanel(AList<String> sortedView, final SortedListDecorator<String> model) {
    JPanel sortPanel = new JPanel(UIUtil.createBorderLayout());
    sortPanel.add(new JScrollPane(sortedView), BorderLayout.CENTER);
    final JCheckBox reverseSort = new JCheckBox("Reverse");
    sortPanel.add(reverseSort, BorderLayout.NORTH);
    reverseSort.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Comparator<String> comparator = String.CASE_INSENSITIVE_ORDER;
        if (reverseSort.isSelected())
          comparator = Containers.reverse(comparator);
        model.setComparator(comparator);
      }
    });
    model.setComparator(String.CASE_INSENSITIVE_ORDER);
    sortPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Sorted"));
    return sortPanel;
  }
}
