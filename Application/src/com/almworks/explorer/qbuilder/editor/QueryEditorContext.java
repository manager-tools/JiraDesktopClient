package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.filter.FilterGramma;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.text.parser.TokenRegistry;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author : Dyoma
 */
class QueryEditorContext implements EditorContext {
  private final AListModel<ConstraintDescriptor> myAttributes;
  private final DetachComposite myLifespan = new DetachComposite(true);

  @NotNull
  private final NameResolver myResolver;

  private final TokenRegistry<FilterNode> myParser = new TokenRegistry<FilterNode>();

  @NotNull
  private final ItemHypercube myHypercube;

  public QueryEditorContext(@NotNull NameResolver resolver, @NotNull ItemHypercube hypercube) {
    myResolver = resolver;
    myHypercube = hypercube;
    myAttributes = resolver.getConstraintDescriptorModel(myLifespan, myHypercube);
    FilterGramma.registerParsers(myParser);
  }

  public AListModel<ConstraintDescriptor> getConditions() {
    return myAttributes;
  }

  public TokenRegistry<FilterNode> getParser() {
    return myParser;
  }

  @NotNull
  public NameResolver getResolver() {
    return myResolver;
  }

  public void dispose() {
    myLifespan.detach();
  }

  public void registerDataProvider(JComponent component) {
    DataProvider.DATA_PROVIDER.putClientValue(component, ConstProvider.singleData(DATA_ROLE, this));
  }

  public ItemHypercube getParentHypercube() {
    return myHypercube;
  }

  public Lifespan getLifespan() {
    return myLifespan;
  }
}
