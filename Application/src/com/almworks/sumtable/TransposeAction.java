package com.almworks.sumtable;

import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;

public class TransposeAction extends SimpleAction {
  private final AxisConfiguration myAxisA;
  private final AxisConfiguration myAxisB;

  public TransposeAction(AxisConfiguration axisA, AxisConfiguration axisB) {
    super("", Icons.TRANSPOSE_SUMMARY_TABLE_ACTION_SMALL);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Swap rows and columns");
    myAxisA = axisA;
    myAxisB = axisB;
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myAxisA.getDefinitionModifiable());
    context.updateOnChange(myAxisB.getDefinitionModifiable());
    context.setEnabled(myAxisA.getAxisDefinition() != null && myAxisB.getAxisDefinition() != null);
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    AxisDefinition aDefinition = myAxisA.getAxisDefinition();
    STFilter aSortFilter = myAxisA.getSortFilter();
    int aSortDirection = myAxisA.getSortDirection();
    AxisDefinition bDefinition = myAxisB.getAxisDefinition();
    STFilter bSortFilter = myAxisB.getSortFilter();
    int bSortDirection = myAxisB.getSortDirection();

    myAxisA.setAxisDefinition(bDefinition);
    myAxisA.setSorting(bSortFilter, bSortDirection);

    myAxisB.setAxisDefinition(aDefinition);
    myAxisB.setSorting(aSortFilter, aSortDirection);
  }
}
