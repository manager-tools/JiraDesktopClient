package com.almworks.actions.distribution;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.tree.DistributionFolderNode;
import com.almworks.api.application.tree.DistributionParameters;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.util.L;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ConvertingListDecorator;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import util.concurrent.Synchronized;

import javax.swing.*;
import java.awt.*;

public class CreateDistribution implements UIComponentWrapper {
  private final DistributionActionContext myContext;
  private final DialogBuilder myBuilder;
  private final Lifecycle myLife = new Lifecycle();
  private final CreateDistributionForm myForm;
  private final Synchronized<Params> myParams = new Synchronized<Params>(null);
  private final boolean myEdit;
  private final GenericNode myParentNode;
  private final ItemHypercube myHypercube;

  public CreateDistribution(DistributionActionContext context) {
    assert context.isCreate() ^ context.isEdit() : context;
    myContext = context;
    myEdit = context.isEdit();
    myBuilder = myContext.getDialogManager().createBuilder("distribution");

    GenericNode actionNode = myContext.getNavigationNode();
    myParentNode = myEdit ? actionNode.getParent() : actionNode;
    assert myParentNode != null : myContext + " " + actionNode;
    ItemHypercube cube = myParentNode.getHypercube(false);
    if (cube == null) cube = new ItemHypercubeImpl();
    myHypercube = cube;
    AListModel<EnumConstraintType> attributesModel = createAttributesModel(myHypercube);
    Configuration config = myBuilder.getConfiguration();
    if (myEdit) {
      myForm = CreateDistributionForm.forEdit(myHypercube, attributesModel, config, actionNode);
    } else {
      myForm = CreateDistributionForm.forCreate(myHypercube, attributesModel, config);
    }
    myForm.connectValues(myLife.lifespan(), new Runnable() {public void run() {
      myBuilder.pressOk();
    }});
  }

  private void initDialog() {
    myBuilder.setModal(true);
    myBuilder.setOkAction(new AnAbstractAction(L.actionName("OK")) {
      public void perform(ActionContext context) {
        myParams.set(new Params(myForm.getSelectedEnum(), myForm.buildDistributionParameters()));
      }
    });
    myBuilder.setEmptyCancelAction();
    myBuilder.setContent(this);
    Component initialFocusOwner = myForm.getInitialFocusOwner();
    if (initialFocusOwner != null)
      myBuilder.setInitialFocusOwner(initialFocusOwner);
    String title = myEdit ? "Edit Distribution Parameters" : "Select a Field to Create Distribution";
    myBuilder.setTitle(L.dialog(title));
    myBuilder.setIgnoreStoredSize(true);
    myBuilder.setPreferredSize(myForm.getLastSize());
    myBuilder.setBottomLineComponent(myForm.createBottomLineComponent());
  }

  public void dispose() {
    myLife.cycle();
  }

  private final AListModel<EnumConstraintType> createAttributesModel(ItemHypercube hypercube) {
    Lifespan lifespan = myLife.lifespan();
    NameResolver resolver = myContext.getNameResolver();
    final AListModel<ConstraintDescriptor> conditions = resolver.getConstraintDescriptorModel(lifespan, hypercube);
    //noinspection RedundantCast,OverlyStrongTypeCast,RawUseOfParameterizedType

    final FilteringListDecorator<ConstraintDescriptor> enumAttributes =
      (FilteringListDecorator) FilteringListDecorator.create(lifespan, conditions);
    enumAttributes.setFilter(new Condition<ConstraintDescriptor>() {
      public boolean isAccepted(ConstraintDescriptor descriptor) {
        ConstraintType type = descriptor.getType();
        return type instanceof EnumConstraintType && ((EnumConstraintType) type).isNotEmpty();
      }
    });
    AListModel<EnumConstraintType> types =
      ConvertingListDecorator.create(enumAttributes, new Convertor<ConstraintDescriptor, EnumConstraintType>() {
        public EnumConstraintType convert(ConstraintDescriptor value) {
          return (EnumConstraintType) value.getType();
        }
      });
    AListModel<EnumConstraintType> sorted =
      SortedListDecorator.create(lifespan, types, EnumConstraintType.ORDER_BY_DISPLAY_NAME);

    // subscribe to zero-count models update
    for (int i = 0; i < conditions.getSize(); i++)
      waitValuesAndResync(conditions.getAt(i), enumAttributes);

    lifespan.add(conditions.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        for (int i = 0; i < length; i++) {
          ConstraintDescriptor descriptor = conditions.getAt(index + i);
          waitValuesAndResync(descriptor, enumAttributes);
        }
      }
    }));
    return sorted;  
  }

  private void waitValuesAndResync(ConstraintDescriptor descriptor, final FilteringListDecorator<?> enumAttributes) {
    ConstraintType type = descriptor.getType();
    if (!(type instanceof EnumConstraintType))
      return;
    EnumConstraintType enumType = (EnumConstraintType) type;
    if (enumType.isNotEmpty()) return;
    final AListModel<ItemKey> keys = enumType.getEnumModel(myLife.lifespan(), myHypercube);
    final DetachComposite changeDetach = new DetachComposite(true);
    if (keys != null) {
      keys.addAWTChangeListener(changeDetach, new ChangeListener() {
        public void onChange() {
          if (keys.getSize() > 0) {
            enumAttributes.resynch();
            changeDetach.detach();
          }
        }
      });
    }
    myLife.lifespan().add(changeDetach);
  }
                                 
  private void createOrEdit() {
    Params params = myParams.get();
    if (params == null)
      return;
    EnumConstraintType constraint = params.getConstraint();
    if (constraint == null)
      return;
    ConstraintDescriptor newDescriptor = constraint.getDescriptor();

    DistributionFolderNode node;
    if (myEdit) {
      GenericNode n = myContext.getNavigationNode();
      if (!(n instanceof DistributionFolderNode)) {
        assert false : n;
        return;
      }
      node = ((DistributionFolderNode) n);
    } else {
      GenericNode targetNode = myParentNode;
      node = myContext.getTreeNodeFactory().createDistributionFolderNode(targetNode);
      node.expandAfterNextUpdate();
      myContext.getTreeNodeFactory().selectNode(targetNode, true);
      myContext.expandTreeNode(targetNode.getTreeNode());
    }

    node.setParameters(newDescriptor, params.getDistributionParameters());
    myContext.getTreeNodeFactory().selectNode(node, true);
  }

  private void perform() {
    initDialog();
    myBuilder.showWindow();
    createOrEdit();
  }

  public static AnAction createAction() {
    return new MyAction();
  }

  public JComponent getComponent() {
    return myForm.getComponent();
  }


  private static final class MyAction extends SimpleAction {
    private MyAction() {
      super(L.actionName("&Distribution"), Icons.ACTION_CREATE_DISTRIBUTION);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
      watchRole(GenericNode.NAVIGATION_NODE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      boolean presented = false;
      try {
        DistributionActionContext dac = new DistributionActionContext(context);
        if (dac.isCreate() || dac.isEdit()) {
          context.setEnabled(EnableState.ENABLED);
          if (dac.isCreate()) {
            context.putPresentationProperty(PresentationKey.NAME, "Create &Distribution");
            context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
              "Create a series of sub-queries, one for every option of a single attribute");
          } else {
            context.putPresentationProperty(PresentationKey.NAME, "Edit &Distribution");
            context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
              "View or change distribution parameters");
          }
          presented = true;
        }
      } finally {
        if (!presented) {
          context.putPresentationProperty(PresentationKey.NAME, "Create or Edit &Distribution");
          context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION,
            "Create or edit a series of sub-queries, one for every option of a single attribute");
        }
      }
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      try {
        DistributionActionContext dac = new DistributionActionContext(context);
        if (dac.isCreate() || dac.isEdit()) {
          new CreateDistribution(dac).perform();
        }
      } catch (CantPerformException e) {
        throw new CantPerformExceptionSilently(e.getMessage());
      }
    }
  }


  private static class Params {
    private final EnumConstraintType myCondition;
    private final DistributionParameters myDistributionParameters;

    public Params(EnumConstraintType selectedEnum, DistributionParameters parameters) {
      myCondition = selectedEnum;
      myDistributionParameters = parameters;
    }

    public EnumConstraintType getConstraint() {
      return myCondition;
    }

    public DistributionParameters getDistributionParameters() {
      return myDistributionParameters;
    }
  }
}
