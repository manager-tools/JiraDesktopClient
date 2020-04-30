package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.FilterEditor;
import com.almworks.api.application.qb.FilterEditorProvider;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.config.Configuration;

public class FilterEditorProviderImpl implements FilterEditorProvider {
  private final Configuration myConfiguration;
  private final NameResolver myResolver;

  public FilterEditorProviderImpl(Configuration configuration, NameResolver resolver) {
    myConfiguration = configuration;
    myResolver = resolver;
  }

  public FilterEditor createFilterEditor(FilterNode filter, ItemHypercube hypercube,
    MutableComponentContainer registerContextTo)
  {
    FilterEditorImpl result =
      new FilterEditorImpl(myConfiguration, new QueryEditorContext(myResolver, hypercube), filter);
    if (registerContextTo != null) {
      registerContextTo.registerActor(EditorContext.DATA_ROLE, result.getContext());
    }
    return result;
  }
}
