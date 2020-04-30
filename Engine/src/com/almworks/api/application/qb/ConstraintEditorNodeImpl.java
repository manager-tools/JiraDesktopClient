package com.almworks.api.application.qb;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ModelMapBinding;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import java.awt.*;

/**
 * This class represents editor of a leaf node: the list with constraint types (attributes) and
 * editing panel. Some stuff like rendering is delegated further to ConstraintEditor.
 *
 * @author : Dyoma
 */
public class ConstraintEditorNodeImpl extends EditorNode implements ChangeListener, ModelAware {
  public static final PropertyKey<Document, String> TEXT = PropertyKey.createText("text");

  /**
   * model of all attributes (currently)
   * todo: model of all constraints
   * tree and tree model?
   */
  private final AList<ConstraintDescriptor> myTypes = new AList<ConstraintDescriptor>();

  private final ConstraintDescriptor myType;
  private final ModelMapBinding myModelMapBinding;
  private final DetachComposite myLife = new DetachComposite();

  private ConstraintEditor myCurrentEditor = nullEditor();
  @Nullable
  private ConstraintDescriptor myCurrentType = null;
  private boolean myCreatingEditorState = false;
  private static final UIComponentWrapper NO_CONSTRAINT_TYPE_SELECTED = UIComponentWrapper.Simple.message("No constraint type selected");

  public ConstraintEditorNodeImpl(EditorContext context, ConstraintDescriptor type, PropertyMap values) {
    super(context, null);
    myModelMapBinding = new ModelMapBinding(values);
    myType = type;
    myTypes.setCollectionModel(sort(myLife, getContext().getConditions()));
    myTypes.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    ListSpeedSearch.install(myTypes);
    myTypes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTypes.getSelectionAccessor().setSelected(myType);
    watchSelection(context.getLifespan(), myTypes.getSelectionAccessor());
    myModelMapBinding.addChangeListener(myLife, this);
  }

  private static AListModel<ConstraintDescriptor> sort(Lifespan life, AListModel<ConstraintDescriptor> conditions) {
    return SortedListDecorator.create(life, conditions, ConstraintDescriptor.ORDER_BY_DISPLAY_NAME);
  }

  public ConstraintEditorNodeImpl(EditorContext context) {
    this(context, null, new PropertyMap(null));
  }

  public void renderOn(Canvas canvas, CellState state) {
    getCurrentConstraintEditor().renderOn(canvas, state, myCurrentType);
  }

  public void onChange() {
    if (myCreatingEditorState) {
      // todo comment - why?
      return;
    }
    getTreeNode().fireChanged();
  }

  public void onInsertToModel() {
    ensureEditorUpToDate();
  }

  public void onRemoveFromModel() {
    disposeEditor();
  }

  public void onChildrenChanged() {
  }

  public UIComponentWrapper createEditor(Configuration configuration) {
    return new MyEditorWrapper(myTypes, configuration);
  }

  @NotNull
  public FilterNode createFilterNodeTree() {
    ConstraintEditor constraint = getCurrentConstraintEditor();
    ensureEditorUpToDate();
    FilterNode structure = myCurrentType != null ? constraint.createFilterNode(myCurrentType) : FilterNode.ALL_ITEMS;
    //noinspection ConstantConditions
    assert structure != null;
    return structure;
  }

  private ConstraintEditor getCurrentConstraintEditor() {
    ensureEditorUpToDate();
    return myCurrentEditor;
  }

  private void ensureEditorUpToDate() {
    ConstraintDescriptor type = getSelectedType();
    if (type == null) {
      disposeEditor();
    } else if (!type.equals(myCurrentType)) {
      myCurrentEditor.dispose();
      myCurrentType = type;
      try {
        myCreatingEditorState = true;
        myCurrentEditor = myCurrentType.createEditor(this);
      } finally {
        myCreatingEditorState = false;
      }
      assert myCurrentEditor != null;
    }
  }

  public void dispose() {
    disposeEditor();
  }

  private void disposeEditor() {
    myCurrentEditor.dispose();
    myCurrentEditor = nullEditor();
    myCurrentType = null;
  }

  private ConstraintEditor nullEditor() {
    return isSingleLeaf() ? ConstraintEditor.ALL_BUGS : ConstraintEditor.NO_CONSTRAINT;
  }

  private boolean isSingleLeaf() {
    TreeModelBridge<EditorNode> treeNode = getTreeNode();
    TreeModelBridge<? extends EditorNode> parent = treeNode.getParent();
    return parent == null || parent.getParent() == null;
  }

  @Nullable
  public ItemHypercube getContextHypercube() {
    EditorNode root = getTreeNode().getRoot().getUserObject();
    if (root == null)
      return new ItemHypercubeImpl();
    Constraint query = getContextConstraint();
    ItemHypercube parentCube = getContext().getParentHypercube();
    if (query == null)
      return parentCube;
    ItemHypercubeImpl hypercube = ItemHypercubeUtils.getHypercube(query, false);
    return parentCube.intersect(hypercube, false);
    // todo cache cube?
  }

  @Nullable
  private Constraint getContextConstraint() {
    ItemHypercube contextCube = getContext().getParentHypercube();
    java.util.List<Constraint> conjuncted = Collections15.arrayList();
    TreeModelBridge<EditorNode> node = getTreeNode();
    while (true) {
      TreeModelBridge<EditorNode> parent = node.getParent();
      if (parent == null)
        break;
      EditorNode parentNode = parent.getUserObject();
      if (parentNode == null)
        break;
      EditorNodeType type = parentNode.getType();
      if (type == EditorNodeType.AND_NODE) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
          TreeModelBridge<EditorNode> child = parent.getChildAt(i);
          if (child == node)
            break;
          EditorNode peerEditorNode = child.getUserObject();
          if (peerEditorNode == null)
            continue;
          if (!peerEditorNode.isNoConstraint()) {
            Constraint constraint = peerEditorNode.createFilterNodeTree().createConstraint(contextCube);
            if (constraint == null) {
              Log.debug("null constraint: " + constraint);
            } else {
              conjuncted.add(constraint);
            }
          }
        }
      }
      node = parent;
    }
    int size = conjuncted.size();
    if (size == 0)
      return null;
    else if (size == 1)
      return conjuncted.get(0);
    else
      return CompositeConstraint.Simple.and(conjuncted);
  }

  public ModelMapBinding getBinding() {
    return myModelMapBinding;
  }

  public boolean isModified() {
    return !Util.equals(myType, getSelectedType()) || getCurrentConstraintEditor().isModified();
  }

  public boolean isNoConstraint() {
    return getSelectedType() == null;
  }

  public String toString() {
    ConstraintDescriptor descriptor = getSelectedType();
    CanvasRenderable type = descriptor == null ? null : descriptor.getPresentation();
    if (type != null) {
      PlainTextCanvas canvas = new PlainTextCanvas();
      type.renderOn(canvas, CellState.LABEL);
      return "Constraint: " + canvas.getText();
    } else
      return "No constraint";
  }

  private ConstraintDescriptor getSelectedType() {
    ConstraintDescriptor selection = myTypes.getSelectionAccessor().getSelection();
    if (selection != null || myType == null) return selection;
    if (myTypes.getCollectionModel().contains(myType))
      return null;
    EditorContext context = getContext();
    return context.getResolver().getConditionDescriptor(myType.getId(), context.getParentHypercube());
  }

  private class MyEditorWrapper
    implements UIComponentWrapper, SelectionAccessor.Listener<Object>, UIComponentWrapper.DisplayableListener
  {
    private final ACollectionComponent<ConstraintDescriptor> myAttributes;
    private final Detach myDetach;
    private final JSplitPane mySplitPane;
    private final PlaceHolder myConstraintPlace = new PlaceHolder();
    private final JPanel myOptionsPanel;
    private final JLabel myOptionsPanelLabel = new JLabel(" ");

    public MyEditorWrapper(ACollectionComponent<ConstraintDescriptor> attributes, Configuration configuration) {
      myAttributes = attributes;
      JScrollPane scrollpane = new JScrollPane(myAttributes.toComponent());
      scrollpane.setMinimumSize(new Dimension(150, 150));
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(scrollpane, BorderLayout.CENTER);
      panel.add(UIUtil.createLabelFor(myAttributes.getSwingComponent(), "&Field:"), BorderLayout.NORTH);
      myOptionsPanel = SingleChildLayout.envelop(myConstraintPlace, SingleChildLayout.CONTAINER);
      myOptionsPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
      myConstraintPlace.setBorder(new EmptyBorder(11, 11, 11, 11));
      JPanel copanel = new JPanel(new BorderLayout());
      copanel.add(myOptionsPanel, BorderLayout.CENTER);
      copanel.add(myOptionsPanelLabel, BorderLayout.NORTH);
      mySplitPane = UIUtil.createSplitPane(panel, copanel, true, configuration, "splitter", 150);
      mySplitPane.setOneTouchExpandable(false);
      mySplitPane.setResizeWeight(0);
      myDetach = attributes.getSelectionAccessor().addListener(this);
      updateConstraintEditor();
    }

    private void updateConstraintEditor() {
      ConstraintEditor editor = getCurrentConstraintEditor();
      if (editor == null) {
        setConstraintPlaceTitle("Constraint Options");
        myConstraintPlace.show(NO_CONSTRAINT_TYPE_SELECTED);
      } else {
        ConstraintDescriptor descriptor = getSelectedType();
        CanvasRenderable type = descriptor == null ? null : descriptor.getPresentation();
        if (type != null) {
          PlainTextCanvas canvas = new PlainTextCanvas();
          type.renderOn(canvas, CellState.LABEL);
          setConstraintPlaceTitle(/*"Options for " + */canvas.getText());
        }
        myConstraintPlace.show(editor);
      }
      mySplitPane.revalidate();
    }

    private void setConstraintPlaceTitle(String title) {
      myOptionsPanelLabel.setText(title + ":");
    }

    public void onComponentDisplayble() {
      myAttributes.scrollSelectionToView();
    }

    public void onSelectionChanged(Object newSelection) {
      updateConstraintEditor();
    }

    public JComponent getComponent() {
      return mySplitPane;
    }

    public void dispose() {
      myDetach.detach();
    }
  }
}
