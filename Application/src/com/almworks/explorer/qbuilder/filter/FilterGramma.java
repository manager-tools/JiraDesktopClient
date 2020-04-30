package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.AllItemsFilterNode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.exec.Context;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.text.parser.ParserContext;
import com.almworks.util.text.parser.TokenRegistry;

/**
 * @author : Dyoma
 */
public class FilterGramma {
  public static void registerParsers(TokenRegistry<FilterNode> registry) {
    for (CompositeFilterNode.CompositeFormulaSerializer serializer : BinaryCommutative.COMMUTATIVE_OPERATIONS) {
      serializer.register(registry);
    }

    TextAttribute.register(registry);
    NumericAttribute.register(registry);
    BaseEnumConstraintDescriptor.register(registry, EnumConstraintKind.INCLUSION_OPERATION);
    BaseEnumConstraintDescriptor.register(registry, EnumConstraintKind.INTERSECTION_OPERATION);
    BaseEnumConstraintDescriptor.register(registry, EnumConstraintKind.TREE_OPERATION);
    AllItemsFilterNode.register(registry);
    DateAttribute.register(registry);

    final CustomParserProvider cpp = Context.get(CustomParserProvider.class);
    if(cpp != null) {
      cpp.registerParsers(registry);
    }
  }

  public static FilterNode parse(String queryString) throws ParseException {
    if (queryString.trim().length() == 0)
      return createEmpty();
    // todo static or thread local? (eats ~1.5% of startup time with many nodes)
    TokenRegistry<FilterNode> registry;
    registry = new TokenRegistry<FilterNode>();
    registerParsers(registry);
    ParserContext<FilterNode> context = registry.tokenize(queryString).stripBraces();
    if (context.isEmpty())
      return createEmpty();
    return context.parseNode();
  }

  public static FilterNode createEmpty() {
    return FilterNode.ALL_ITEMS;
  }
}
