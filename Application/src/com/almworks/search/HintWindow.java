package com.almworks.search;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.TreeUtil;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

class HintWindow {
  private final JComponent myOffsetComponent;

  private Popup myPopup;
  private JPanel myLastPanel;
  private Set<GenericNode> myLastNodes;

  private final JLabel mySearchTypeLabel = new JLabel();
  private final JPanel myHeaderPanel = new JPanel(new BorderLayout());
  private final JPanel myWholePanel = new JPanel(new BorderLayout());

  public HintWindow(JComponent offsetComponent) {
    myOffsetComponent = offsetComponent;

    mySearchTypeLabel.setPreferredSize(UIUtil.getRelativeDimension(mySearchTypeLabel, 8, 1));
    myHeaderPanel.add(UIUtil.adjustFont(new JLabel("Will search:"), -1, Font.BOLD, false), BorderLayout.WEST);
    myHeaderPanel.add(mySearchTypeLabel, BorderLayout.EAST);
    mySearchTypeLabel.setBorder(new EmptyBorder(0, 9, 0, 3));
    mySearchTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    myHeaderPanel.setOpaque(false);
    myWholePanel.setOpaque(false);
    myWholePanel.add(myHeaderPanel, BorderLayout.NORTH);
  }

  public void update(boolean visible, Collection<GenericNode> nodes) {
    if (!isVisible()) {
      if (visible) {
        myLastNodes = set(nodes);
        myPopup = createPopup(myLastNodes);
        myPopup.show();
      }
    } else {
      if (visible) {
        Set<GenericNode> nodeset = set(nodes);
        if (myLastNodes == null || !myLastNodes.equals(nodeset)) {
          myLastNodes = nodeset;
          myPopup.hide();
          myPopup = createPopup(myLastNodes);
          myPopup.show();
        }
      } else {
        myPopup.hide();
        myPopup = null;
        myLastPanel = null;
      }
    }
  }

  private Popup createPopup(Set<GenericNode> nodes) {
    Threads.assertAWTThread();
    Box content = buildContent(nodes);
    buildWholePanel(content);
    myLastPanel = createTopmostPanel();
    Point p = getDisplayPoint();
    return UIUtil.getPopup(myOffsetComponent, myLastPanel, p.x, p.y);
  }

  private void buildWholePanel(Box content) {
    myWholePanel.removeAll();
    myWholePanel.add(myHeaderPanel, BorderLayout.NORTH);
    myWholePanel.add(content, BorderLayout.CENTER);
  }

  private JPanel createTopmostPanel() {
    JPanel panel = SingleChildLayout.envelop(myWholePanel, SingleChildLayout.CONTAINER);
    panel.setOpaque(true);
    Color bg = ColorUtil.between(UIUtil.getEditorBackground(), Color.YELLOW, 0.15F);
    LineBorder line = new LineBorder(bg.darker().darker().darker(), 1);
    panel.setBackground(bg);
    panel.setBorder(new CompoundBorder(line, new EmptyBorder(2, 5, 2, 5)));
    return panel;
  }

  private Point getDisplayPoint() {
    Point p = new Point(myOffsetComponent.getWidth(), myOffsetComponent.getHeight() /*+ 1*/);
    Dimension preferredSize = myLastPanel.getPreferredSize();
    if (preferredSize != null)
      p.x -= preferredSize.width;
    SwingUtilities.convertPointToScreen(p, myOffsetComponent);
    return p;
  }

  private Box buildContent(Set<GenericNode> nodes) {
    nodes = TreeUtil.excludeDescendants(nodes, GenericNode.GET_PARENT_NODE);
    Box content = Box.createVerticalBox();
    final int MAX = 10;
    int count = 0;
    for (GenericNode node : nodes) {
      if (++count > MAX) {
        addLabel(content, "\u2026 " + (nodes.size() - MAX) + " more", true);
        break;
      }
      addLabel(content, "   " + SearchComponentUtils.getStringPath(node), false);
    }
    return content;
  }

  private void addLabel(Box content, String text, boolean bold) {
    JLabel label = new JLabel(text);
    if (bold)
      UIUtil.adjustFont(label, -1, Font.BOLD, false);
    label.setAlignmentX(0F);
    label.setAlignmentY(0.5F);
    label.setHorizontalAlignment(SwingConstants.LEFT);
//    label.setBorder(new LineBorder(Color.green, 1));
    content.add(label);
  }

  private Set<GenericNode> set(Collection<GenericNode> nodes) {
    return Collections15.linkedHashSet(nodes);
  }

  private boolean isVisible() {
    return myPopup != null;
  }

  public Component getMainPanel() {
    return myLastPanel;
  }

  public void setSearchType(String type) {
    mySearchTypeLabel.setText(type == null ? null : "(" + type + ")");
  }
}
