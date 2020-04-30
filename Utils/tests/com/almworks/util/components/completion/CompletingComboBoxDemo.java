package com.almworks.util.components.completion;

import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory1;
import com.almworks.util.debug.DebugFrame;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class CompletingComboBoxDemo {
  public static void main(String[] args) {
    CompletingComboBoxController<String> completion = createController();
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.add(new JButton("button"), BorderLayout.CENTER);
    addCombobox(panel, completion, "Completion:", BorderLayout.SOUTH);
    CompletingComboBoxController<String> filter = createController();
    filter.setFilterFactory(new Factory1<Condition<? super String>, String>() {
      public Condition<? super String> create(final String argument) {
        return new Condition<String>() {
          public boolean isAccepted(String value) {
            return value.indexOf(argument) >= 0;
          }
        };
      }
    });
    addCombobox(panel, filter, "Filter:", BorderLayout.NORTH);
    panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, panel.getPreferredSize().height * 3));
    DebugFrame.show(panel);
  }

  private static CompletingComboBoxController<String> createController() {
    CompletingComboBoxController<String> completion = new CompletingComboBoxController<String>();
    //noinspection unchecked
    completion.setConvertors(Convertor.<String>identity(), Convertor.<String>identity(), Equality.GENERAL);
    completion.setVariantsModel(FixedListModel.create("abc", "aabc", "aaabc", "bbc", "bc"));
    return completion;
  }

  private static void addCombobox(JPanel panel, CompletingComboBoxController<?> controller, String label,
    String constraints) {
    JPanel p = new JPanel(new GridLayout(1, 2, 5, 0));
    p.add(new JLabel(label));
    p.add(controller.getComponent());
    panel.add(p, constraints);
  }
}
