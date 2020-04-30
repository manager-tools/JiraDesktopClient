package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.NestedComponent;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.JointChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class CascadeEditor extends BaseSingleEnumEditor {
  @NotNull private final TypedKey<? extends ItemKey> myParentKey;
  @Nullable private final DBAttribute<String> myOptionNameAttribute;
  @Nullable private final CanvasRenderable myNullRenderable;

  public CascadeEditor(NameMnemonic labelText, EnumVariantsSource variants, @Nullable DefaultItemSelector defaultItem,
    CanvasRenderable nullRenderable, boolean appendNull, EnumValueKey valueKey, boolean verify,
    TypedKey<? extends ItemKey> parentKey, DBAttribute<String> optionNameAttribute)
  {
    super(labelText, variants, defaultItem, appendNull, valueKey, verify);
    myParentKey = parentKey;
    myOptionNameAttribute = optionNameAttribute;
    myNullRenderable = nullRenderable;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    final ATree<TreeModelBridge<ItemKey>> tree = new ATree<TreeModelBridge<ItemKey>>();
    tree.setCanvasRenderer(getRenderer());
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    AScrollPane scroll = new AScrollPane(tree);
    scroll.setSizeDelegate(new SizeConstraints.PreferredSizeBoundedByConstant(null, new Dimension(200, Short.MAX_VALUE)));
    return Collections.singletonList(attachComponent(life, model, scroll, tree));
  }

  private CanvasRenderer<TreeModelBridge<ItemKey>> getRenderer() {
    return new CanvasRenderer<TreeModelBridge<ItemKey>>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, TreeModelBridge<ItemKey> item) {
        if (item.getUserObject() == BaseSingleEnumEditor.NULL_ITEM) {
          if (myNullRenderable != null) myNullRenderable.renderOn(canvas, state);
          return;
        }
        canvas.appendText(getOptionName(item.getUserObject()));
      }
    };
  }

  public ComponentControl attachComponent(final Lifespan life, final EditItemModel model, JScrollPane scrollPane, final ATree<TreeModelBridge<ItemKey>> tree) {
    LogHelper.assertError(scrollPane.getViewport().getView() == tree, "Wrong tree", this);
    getVariants().configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        tree.getScrollable().setVisibleRowCount(Util.bounded(4, variants.getSize(), 7));
        final ListModelTreeAdapter<ItemKey, TreeModelBridge<ItemKey>> adapter =
          ListModelTreeAdapter.create(variants, new CascadeTreeStructure(), null);
        adapter.attach(life);
        tree.setRoot(adapter.getRootNode());
        tree.expandAll();

        ((JTree)tree.getSwingComponent()).getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        ItemKey initialItem = getInitialItem(model, variants);
        setValue(model, initialItem);

        final SelectionAccessor<TreeModelBridge<ItemKey>> selection = tree.getSelectionAccessor();
        if (initialItem == null) selection.clearSelection();
        else {
          List<TreeModelBridge<ItemKey>> nodes = adapter.findNodes(initialItem);
          if (nodes == null || nodes.isEmpty()) selection.clearSelection();
          else {
            LogHelper.assertError(nodes.size() == 1, "Single selection expected", nodes);
            selection.setSelected(nodes.get(0));
          }
        }
        final boolean[] duringUpdate = new boolean[]{false};
        selection.addListener(new SelectionAccessor.Listener<TreeModelBridge<ItemKey>>() {
          public void onSelectionChanged(TreeModelBridge<ItemKey> node) {
            if (duringUpdate[0]) return;
            duringUpdate[0] = true;
            try {
              setValue(model, node != null ? node.getUserObject() : null);
            } finally {
              duringUpdate[0] = false;
            }
          }
        });
        model.addAWTChangeListener(life, new JointChangeListener(duringUpdate) {
          @Override
          protected void processChange() {
            selection.setSelected(findUserObject((TreeModelBridge<ItemKey>) tree.getRoot(), getValue(model)));
          }
        });
      }
    });
    FieldEditorUtil.registerComponent(model, this, tree);
    return new NestedComponent(scrollPane, tree, ComponentControl.Dimensions.TALL, this, model, getComponentEnableState(model));
  }

  private TreeModelBridge<ItemKey> findUserObject(TreeModelBridge<ItemKey> node, ItemKey value) {
    if (Util.equals(node.getUserObject(), value)) return node;
    for (int i = 0; i < node.getChildCount(); i++) {
      TreeModelBridge<ItemKey> result = findUserObject(node.getChildAt(i), value);
      if (result != null) return result;
    }
    return null;
  }

  private String getOptionName(ItemKey key) {
    if(key instanceof LoadedItemKey && myOptionNameAttribute != null) {
      final String name = ((LoadedItemKey)key).getValue(myOptionNameAttribute);
      if(name != null) {
        return name;
      }
    }
    return key == null ? "" : key.getDisplayName();
  }

  private class CascadeTreeStructure implements TreeStructure<ItemKey, ItemKey, TreeModelBridge<ItemKey>> {
    @Override
    public ItemKey getNodeKey(ItemKey key) {
      return key;
    }

    @Override
    public ItemKey getNodeParentKey(ItemKey key) {
      if(key instanceof LoadedItemKey) {
        return ((LoadedItemKey)key).getValue(myParentKey);
      }
      return null;
    }

    @Override
    public TreeModelBridge<ItemKey> createTreeNode(ItemKey key) {
      return TreeModelBridge.create(key);
    }
  }
}
