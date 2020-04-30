package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.qb.*;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.constraints.AbstractConstraintEditor;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ListModelUtils;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.properties.PropertyKey;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

class EnumAttributeConstraintEditor extends AbstractConstraintEditor {
  private final EnumAttributeForm myForm;
  private final PropertyKey<OrderListModel<ItemKey>, List<ItemKey>> myModelKey;
  private final EnumConstraintType myType;
  private final Comparator<? super ItemKey> myOrder;
  private final BasicScalarModel<AListModel<ItemKey>> myRelevantModel =
    BasicScalarModel.create(true, false);
  private DetachComposite myRelevantModelValueLife = new DetachComposite(true);
  private ItemHypercube myLastContextHypercube = null;

  public EnumAttributeConstraintEditor(PropertyKey<OrderListModel<ItemKey>, List<ItemKey>> modelKey,
    EnumConstraintType type, ConstraintEditorNodeImpl node, Comparator<? super ItemKey> order,
    CanvasRenderer<ItemKey> variantsRenderer, boolean searchSubstring)
  {
    super(node);
    myForm = new EnumAttributeForm(variantsRenderer, searchSubstring);
    myModelKey = modelKey;
    myType = type;
    myOrder = order;

    Lifespan lifespan = getLifespan();

    final TreeModelBridge<EditorNode> thisNode = node.getTreeNode();
    thisNode.getRoot().getTreeEventSource().addAWTListener(lifespan, new TreeListener() {
      public void onTreeEvent(TreeEvent event) {
        if (event.getSource() == thisNode)
          return;
        updateRelevantModel(true);
      }
    });
    updateRelevantModel(false);

    AListModel<ItemKey> fullModel = myType.getEnumFullModel();
    fullModel = ListModelUtils.addFilterAndSorting(lifespan, fullModel, null, myOrder);

    myForm.setModels(lifespan, fullModel, myRelevantModel);
    getBinder().setMultipleSelection(myModelKey, myForm.getSelectionAccessor());
  }

  private void updateRelevantModel(boolean postponeOnISE) {
    ItemHypercube cube = getContextHypercube();
    if ((cube == null && cube != myLastContextHypercube) || (cube != null && !cube.isSame(myLastContextHypercube))) {
      DetachComposite toDetach = myRelevantModelValueLife;
      myRelevantModelValueLife = new DetachComposite(true);
      addToDispose(myRelevantModelValueLife);
      myLastContextHypercube = cube;
      AListModel<ItemKey> model = myType.getEnumModel(myRelevantModelValueLife, cube);
      model = ListModelUtils.addFilterAndSorting(myRelevantModelValueLife, model, null, myOrder);
      try {
        myRelevantModel.setValue(model);
      } catch (IllegalStateException e) {
        // reentrancy: myRelevantModel is being changed, and we're trying to change it int the notification
        Log.debug(e);
        if (postponeOnISE) {
          ThreadGate.AWT_QUEUED.execute(new Runnable() {
            public void run() {
              updateRelevantModel(false);
            }
          });
        }
      }
      toDetach.detach();
    }
  }

  public void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
    canvas.setIcon(Icons.QUERY_CONDITION_ENUM_ATTR);
    descriptor.getPresentation().renderOn(canvas, state);
    canvas.appendText(" in {");
    List<ItemKey> subset = getValue(myModelKey);
    String separator = "";
    HashSet<String> printedNames = Collections15.hashSet();
    for (ItemKey artifact : subset) {
      if (artifact == null)
        continue;
      String name = artifact.getDisplayName();
      if (printedNames.add(name)) {
        canvas.appendText(separator);
        canvas.appendText(name);
        separator = ", ";
      }
    }
    canvas.appendText("}");
  }

  @NotNull
  public FilterNode createFilterNode(ConstraintDescriptor descriptor) {
    List<ItemKey> values = getValue(myModelKey);
    return new ConstraintFilterNode(descriptor, BaseEnumConstraintDescriptor.createValues(values));
  }

  public boolean isModified() {
    return wasChanged(myModelKey);
  }

  public JComponent getComponent() {
    return myForm.getComponent();
  }

  public void onComponentDisplayble() {
    myForm.scrollSelectionToView();
  }
}
