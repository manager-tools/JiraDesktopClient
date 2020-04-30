package com.almworks.gui;

import com.almworks.api.gui.StatusBar;
import com.almworks.api.gui.StatusBarComponent;
import com.almworks.util.components.FixedRowLayout;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

public class StatusBarImpl implements StatusBar {
  /**
   * Order of sections, counting from window's edge.
   */
  private static final Section[] SECTION_ORDER = {
    Section.MEMORY_BAR,
    Section.SYNCHRONIZATION_STATUS,
    Section.ARTIFACT_STATS,
    Section.MESSAGES
  };
  private static final ComponentProperty<StatusBarComponent> WRAPPER = ComponentProperty.createProperty("ui.wrapper");


  private final Detach myShowContextComponent = new Detach() {
    protected void doDetach() {
      showContextComponent(null);
    }
  };

  private final Map<Section, SortedMap<Double, StatusBarComponent>> mySectionsContent = Collections15.hashMap();
  private final Map<Section, JPanel> mySections = Collections15.hashMap();

  private PlaceHolder myLeftPanel;
  private JPanel myWholePanel;
  private Border mySectionBorder;
  private final EmptyBorder EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);


  public StatusBarImpl() {
    initComponents();
  }

  private void initComponents() {
    JPanel rightPanel = new JPanel(new FixedRowLayout(false));
    UIUtil.setupTransparentPanel(rightPanel);
    myLeftPanel = new PlaceHolder();

    myWholePanel = new JPanel(new BorderLayout(9, 0));
    myWholePanel.add(rightPanel, BorderLayout.EAST);
    myWholePanel.add(myLeftPanel, BorderLayout.CENTER);

    if(!Aqua.isAqua()) {
      myWholePanel.setBorder(new EmptyBorder(2, 2, 0, 2));
    } else {
      myWholePanel.setBorder(UIUtil.getCompoundBorder(
        Aqua.MAC_BORDER_NORTH,
        new EmptyBorder(2, 2, 2, 2)));
    }

    myWholePanel.setPreferredSize(new Dimension(0, getHeight() + 4));

    mySectionBorder = UIManager.getBorder("Ext.StatusBar.sectionBorder");
    if (mySectionBorder == null) {
      Log.warn("no statusbar border available");
      mySectionBorder = new EmptyBorder(2, 2, 2, 2);
    }

    for (Section aSECTION_ORDER : SECTION_ORDER) {
      JPanel panel = createSectionPanel();
      mySections.put(aSECTION_ORDER, panel);
      rightPanel.add(panel, FixedRowLayout.FULL_HEIGHT);
      rebuildSection(aSECTION_ORDER);
    }
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  private JPanel createSectionPanel() {
    JPanel panel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 5));
    panel.setBorder(mySectionBorder);
    return panel;
  }

  public Detach addComponent(Section section, StatusBarComponent component) {
    SortedMap<Double, StatusBarComponent> map = getSectionMap(section);
    Double key = map.size() == 0 ? (double) 1 : map.lastKey() + 10;
    return putComponent(section, map, key, component);
  }

  public Detach addComponent(Section section, StatusBarComponent component, double weight) {
    SortedMap<Double, StatusBarComponent> map = getSectionMap(section);
    Double key = weight;
    if (map.containsKey(key)) {
      SortedMap<Double, StatusBarComponent> submap = map.tailMap(key);
      Iterator<Double> kk = submap.keySet().iterator();
      Double key1 = kk.hasNext() ? kk.next() : null;
      Double key2 = kk.hasNext() ? kk.next() : null;
      if (key1 != null && key2 != null)
        key = (key1 + key2) / 2;
      else
        key = key + 10;
    }
    return putComponent(section, map, key, component);
  }

  private Detach putComponent(Section section, SortedMap<Double, StatusBarComponent> map,
    Double key, StatusBarComponent component) {
    map.put(key, component);
    rebuildSection(section);
    return getDetach(section, map, key);
  }

  private Detach getDetach(final Section section, final SortedMap<Double, StatusBarComponent> map, final Double key) {
    return new Detach() {
      protected void doDetach() {
        StatusBarComponent component = map.remove(key);
        if (component != null) {
          rebuildSection(section);
          component.dispose();
        }
      }
    };
  }

  private void rebuildSection(Section section) {
    JPanel panel = mySections.get(section);
    if (panel == null) {
      Log.warn("no component for " + section);
      return;
    }

    removeComponents(panel);
    panel.setVisible(false);

    SortedMap<Double, StatusBarComponent> content = mySectionsContent.get(section);
    if (content == null || content.size() == 0) {
      // no content
      // set border so that insets don't affect layout of other components
      panel.setBorder(EMPTY_BORDER);
      return;
    }

    StatusBarComponent[] wrappers = content.values().toArray(new StatusBarComponent[content.size()]);
    int fixedWidth = 0;
    for (int i = wrappers.length - 1; i >= 0; i--) {
      StatusBarComponent wrapper = wrappers[i];
      JComponent component = wrapper.getComponent();
      if (component == null) {
        assert false;
        continue;
      }
      int reservedWidth = wrapper.getReservedWidth();
      if (reservedWidth <= 0)
        fixedWidth = -1;
      else if (fixedWidth >= 0)
        fixedWidth += reservedWidth;
      WRAPPER.putClientValue(component, wrapper);
      panel.add(component);
      wrapper.attach();
    }

    int height = getHeight();
    Dimension size = fixedWidth > 0 ? new Dimension(fixedWidth, height) : null;
    panel.setMinimumSize(size);
    panel.setMaximumSize(size);
    panel.setPreferredSize(size);

    panel.setBorder(mySectionBorder);
    panel.setVisible(true);
    panel.invalidate();
  }

  private int getHeight() {
    return UIUtil.getLineHeight(myWholePanel) + 2;
  }

  private void removeComponents(JComponent panel) {
    Component[] components = panel.getComponents();
    panel.removeAll();
    for (Component component : components) {
      if (component instanceof JComponent) {
        StatusBarComponent wrapper = WRAPPER.getClientValue((JComponent) component);
        if (wrapper != null) {
          WRAPPER.putClientValue((JComponent) component, null);
          wrapper.dispose();
        }
      }
    }
  }

  private SortedMap<Double, StatusBarComponent> getSectionMap(Section section) {
    SortedMap<Double, StatusBarComponent> map = mySectionsContent.get(section);
    if (map == null) {
      map = Collections15.treeMap();
      mySectionsContent.put(section, map);
    }
    return map;
  }

  public Detach showContextComponent(UIComponentWrapper component) {
    myLeftPanel.show(component);
    return myShowContextComponent;
  }
}
