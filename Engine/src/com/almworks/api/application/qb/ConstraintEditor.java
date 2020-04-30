package com.almworks.api.application.qb;

import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.UIComponentWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * Represents editing panel for a single leaf constraint type in QB.
 *
 * @author : Dyoma
 */
public interface ConstraintEditor extends UIComponentWrapper, UIComponentWrapper.DisplayableListener {
  ConstraintEditor ALL_BUGS =
    new NamedStubConstraintEditor(L.treeNode(Local.parse("All " + Terms.ref_Artifacts)), L.content("No constraint selected"));

  ConstraintEditor NO_CONSTRAINT =
    new NamedStubConstraintEditor(L.treeNode("No Constraint"), L.content("No constraint selected"));

  @NotNull
  FilterNode createFilterNode(ConstraintDescriptor descriptor);

  boolean isModified();

  void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor);

}
