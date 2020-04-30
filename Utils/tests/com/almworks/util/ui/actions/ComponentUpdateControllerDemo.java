package com.almworks.util.ui.actions;

import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.AList;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * @author dyoma
 */
public class ComponentUpdateControllerDemo implements Runnable {
  private static final DataRole<String> SELECTION_ROLE = DataRole.createRole(String.class);

  public static void main(String[] args) {
    ThreadGate.AWT.execute(new ComponentUpdateControllerDemo());
  }

  public void run() {
    final JPanel panel = new JPanel(UIUtil.createBorderLayout());
    GlobalDataRoot.install(panel);
    final AList<String> list = new AList<String>();
    panel.add(list, BorderLayout.CENTER);
    final JLabel selected = new JLabel();
    panel.add(selected, BorderLayout.NORTH);
    final JLabel selectionCount = new JLabel();
    panel.add(selectionCount, BorderLayout.SOUTH);
    panel.add(new JButton(new AbstractAction("Show") {
      public void actionPerformed(ActionEvent e) {
        panel.add(list, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
      }
    }), BorderLayout.WEST);
    panel.add(new JButton(new AbstractAction("Hide"){
      public void actionPerformed(ActionEvent e) {
        panel.remove(list);
        panel.revalidate();
        panel.repaint();
      }
    }), BorderLayout.EAST);
    list.setCollectionModel(FixedListModel.create("1", "2", "3", "4", "5", "6"));
    list.addGlobalRoles(SELECTION_ROLE);
    ComponentUpdateController controller = new ComponentUpdateController(panel);
    controller.attachContext(Lifespan.FOREVER);
    controller.addUpdatable(Lifespan.FOREVER, showAllRoles(selected, SELECTION_ROLE, Convertor.<String>identity()));
    controller.addUpdatable(Lifespan.FOREVER, showRoleCount(selectionCount, SELECTION_ROLE));
    JFrame frame = DebugFrame.show(panel);
  }

  private static UpdateRequestable showRoleCount(final JLabel where, final TypedKey<?> role) {
    return new UpdateRequestable() {
      public void update(UpdateRequest request) {
        request.watchRole(role);
        Collection<Object> collection = request.getSourceCollectionOrNull(role);
        where.setText(collection != null ? String.valueOf(collection.size()) : "No source object");
      }
    };
  }

  private static <T> UpdateRequestable showAllRoles(final JLabel where, final TypedKey<T> role, final Convertor<T, String> toString) {
    return new UpdateRequestable() {
      public void update(UpdateRequest request) {
        request.watchRole(role);
        Collection<T> collection = request.getSourceCollectionOrNull(role);
        if (collection == null) {
          where.setText("No source objects");
          return;
        }
        where.setText(collection.isEmpty() ? "<Empty>" : TextUtil.separate(collection.iterator(), ", ", toString));
      }
    };
  }
}
