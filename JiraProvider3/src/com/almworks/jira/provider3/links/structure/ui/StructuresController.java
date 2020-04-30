package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.jira.provider3.schema.LinkType;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.TreeStructure;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.*;

class StructuresController {
  private final JPanel myPanel = new JPanel(InlineLayout.vertical(4));
  private final ScrollablePanel myScrollable = new ScrollablePanel(myPanel);
  private final List<HierarchyType> myHierarchyTypes = Collections15.arrayList();
  private HierarchyEditState myHierarchy = null;

  StructuresController(GuiFeaturesManager features, Connection connection) {
    myPanel.setOpaque(false);
    myScrollable.setBackground(DocumentFormAugmentor.backgroundColor());
    Map<Long, LoadedItemKey> linkTypes = getLinkTypes(features, connection);
    Map<LoadedItemKey, ItemsTreeLayout[]> anisotropicGroups = Collections15.hashMap();
    ItemsTreeLayout.OwnerFilter filter = new ItemsTreeLayout.OwnerFilter(connection);
    for (ItemsTreeLayout treeLayout : features.getTreeLayouts().toList()) {
      if (!filter.isAccepted(treeLayout)) continue;
      TreeStructure<LoadedItem, ?, TreeModelBridge<LoadedItem>> structure = treeLayout.getTreeStructure();
      AnisotropicLinkStructure anisotropic = Util.castNullable(AnisotropicLinkStructure.class, structure);
      if (anisotropic == null) {
        Isotropic isotropic = new Isotropic(treeLayout.getDisplayName(), treeLayout.getId());
        myHierarchyTypes.add(isotropic);
        JComponent component = isotropic.getComponent();
        component.setOpaque(false);
        myPanel.add(component);
      } else {
        long linkTypeItem = anisotropic.getLinkTypeItem();
        LoadedItemKey linkType = linkTypes.get(linkTypeItem);
        if (linkType == null) {
          LogHelper.warning("Missing link type", linkTypeItem, anisotropic);
          continue;
        }
        ItemsTreeLayout[] array = anisotropicGroups.get(linkType);
        if (array == null) {
          array = new ItemsTreeLayout[2];
          anisotropicGroups.put(linkType, array);
        }
        array[anisotropic.isOutward() ? 0 : 1] = treeLayout;
      }
    }
    for (Map.Entry<LoadedItemKey, ItemsTreeLayout[]> entry : anisotropicGroups.entrySet()) {
      ItemsTreeLayout[] array = entry.getValue();
      HierarchyType hierarchyType = AnisotropicPanel.create(entry.getKey(), array[0], array[1]);
      if (hierarchyType != null) {
        myHierarchyTypes.add(hierarchyType);
        JComponent component = hierarchyType.getComponent();
        component.setOpaque(false);
        myPanel.add(component);
      }
    }
  }

  public ScrollablePanel getScrollable() {
    return myScrollable;
  }

  private static Map<Long, LoadedItemKey> getLinkTypes(GuiFeaturesManager features, Connection connection) {
    EnumTypesCollector.Loaded enumType = features.getEnumTypes().getType(LinkType.ENUM_TYPE);
    if (enumType == null) return Collections.emptyMap();
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    ItemHypercubeUtils.adjustForConnection(cube, connection);
    List<LoadedItemKey> linkTypes = enumType.getEnumValues(cube);
    HashMap<Long, LoadedItemKey> map = Collections15.hashMap();
    for (LoadedItemKey linkType : linkTypes) map.put(linkType.getItem(), linkType);
    return map;
  }

  public void setHierarchy(Lifespan life, ChangeListener listener, @Nullable HierarchyEditState hierarchy) {
    myHierarchy = hierarchy;
    reset(life, listener);
  }

  public void reset() {
    reset(null, null);
  }

  private void reset(@Nullable Lifespan life, @Nullable ChangeListener listener) {
    if (myHierarchy == null) return;
    Set<String> ids = myHierarchy.getLayoutIds();
    for (HierarchyType type : myHierarchyTypes) {
      type.setSelectedIds(ids);
      if (life != null && listener != null) type.addChangeListener(life, listener);
    }
  }

  public void applyCurrent() {
    if (myHierarchy == null) return;
    List<String> ids = Collections15.arrayList();
    for (HierarchyType type : myHierarchyTypes) {
      String selectedId = type.getSelectedId();
      if (selectedId != null) ids.add(selectedId);
    }
    myHierarchy.setLayoutIds(ids);
  }

  private interface HierarchyType {
    @Nullable("When not selected")
    String getSelectedId();

    void setSelectedIds(Set<String> ids);

    JComponent getComponent();

    void addChangeListener(Lifespan life, ChangeListener listener);
  }

  private static class Isotropic implements HierarchyType {
    private final JCheckBox myCheckBox;
    private final String myId;

    private Isotropic(String displayName, String id) {
      myCheckBox = new JCheckBox(displayName);
      myId = id;
    }

    public static Isotropic create(ItemsTreeLayout treeLayout) {
      return new Isotropic(treeLayout.getDisplayName(), treeLayout.getId());
    }

    @Override
    public String getSelectedId() {
      return myCheckBox.isSelected() ? myId : null;
    }

    @Override
    public void setSelectedIds(Set<String> ids) {
      myCheckBox.setSelected(ids.contains(myId));
    }

    @Override
    public JComponent getComponent() {
      return myCheckBox;
    }

    @Override
    public void addChangeListener(Lifespan life, ChangeListener listener) {
      UIUtil.addChangeListener(life, listener, myCheckBox.getModel());
    }
  }

  private static class AnisotropicPanel implements HierarchyType {
    private JCheckBox myEnabled;
    private JPanel myWholePanel;
    private JRadioButton myOutward;
    private JRadioButton myInward;
    private final ButtonGroup myButtonGroup = new ButtonGroup();
    private final String myOutwardId;
    private final String myInwardId;

    public AnisotropicPanel(String outwardId, String inwardId) {
      myOutwardId = outwardId;
      myInwardId = inwardId;
      ComponentEnabler.create(myEnabled, myInward, myOutward);
      myOutward.getModel().setGroup(myButtonGroup);
      myInward.getModel().setGroup(myButtonGroup);
      myEnabled.addChangeListener(new javax.swing.event.ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          if (!myEnabled.isSelected()) myButtonGroup.clearSelection();
          else {
            if (!myInward.isSelected() && !myOutward.isSelected())
              myOutward.setSelected(true);
          }
        }
      });
    }

    @Nullable
    public static HierarchyType create(LoadedItemKey linkType, ItemsTreeLayout outward, ItemsTreeLayout inward) {
      String outName = linkType.getValue(LinkType.OUTWARD_DESCRIPTION);
      String inName = linkType.getValue(LinkType.INWARD_DESCRIPTION);
      if (outName == null) outward = null;
      if (inName == null) inward = null;
      if (inward != null && outward != null) {
        AnisotropicPanel panel = new AnisotropicPanel(outward.getId(), inward.getId());
        panel.myEnabled.setText(linkType.getDisplayName());
        panel.myOutward.setText(outName);
        panel.myInward.setText(inName);
        return panel;
      }
      if (inward != null) return Isotropic.create(inward);
      if (outward != null) return Isotropic.create(outward);
      return null;
    }

    @Nullable
    @Override
    public String getSelectedId() {
      if (!myEnabled.isSelected()) return null;
      if (myOutward.isSelected()) return myOutwardId;
      return myInward.isSelected() ? myInwardId : null;
    }

    @Override
    public void setSelectedIds(Set<String> ids) {
      boolean outward = ids.contains(myOutwardId);
      boolean inward = ids.contains(myInwardId);
      if (inward || outward) {
        myEnabled.setSelected(true);
        myInward.setSelected(inward);
        myOutward.setSelected(outward);
      } else {
        myEnabled.setSelected(false);
        myButtonGroup.clearSelection();
      }
    }

    @Override
    public JComponent getComponent() {
      return myWholePanel;
    }

    @Override
    public void addChangeListener(Lifespan life, ChangeListener listener) {
      UIUtil.addChangeListener(life, listener, myEnabled.getModel());
      UIUtil.addChangeListener(life, listener, myInward.getModel());
      UIUtil.addChangeListener(life, listener, myOutward.getModel());
    }
  }
}
