package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

/**
 * @author dyoma
 */
public class CompactSubsetEditorDemo {
  private static CompactSubsetEditor<String> editor;

  public static void main(String[] args) {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        System.getProperties().setProperty("use.metal", "true");
        LAFUtil.initializeLookAndFeel();
        LAFUtil.installExtensions();

        JPanel panel = new JPanel(new FlowLayout());
        CompactSubsetEditor<String> first = createEditor();
        first.setNothingSelectedItem("nothing!");
        panel.add(first);
        panel.add(createEditor());
        panel.add(createEditor());
        JButton button = new JButton("JButton");
        panel.add(button);
        button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            System.out.println("editor = " + editor);
          }
        });

        DebugFrame.show(panel);
      }
    });
  }

  private static CompactSubsetEditor<String> createEditor() {
    editor = new CompactSubsetEditor<String>();
    editor.setBorder(UIUtil.BORDER_9);
    editor.setPrototypeValue("whatever");

    OrderListModel<String> model = OrderListModel.create();
    for (int i = 1; i < 100; i++)
      model.addElement(String.valueOf(i));
    editor.setFullModel(model);
    editor.setCanvasRenderer(Renderers.canvasToString());
    editor.addSelected(Arrays.asList("1", "A", "C"));
    editor.setUnknownSelectionItemColor(GlobalColors.ERROR_COLOR);
    return editor;
  }
}
