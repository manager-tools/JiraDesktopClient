package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.Terms;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.i18n.Local;
import com.almworks.util.text.parser.*;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class AllItemsFilterNode implements FilterNode {
  static final AllItemsFilterNode INSTANCE = new AllItemsFilterNode();

  private static final String All_TOKEN = "all";

  /**
   * @deprecated remove when all upgrade to newer version
   */
  private static final String INVALID_TOKEN = "notConfigured";

  private static final FunctionParser<FilterNode> INVALID_LEAF_PARSER = new FunctionParser<FilterNode>() {
    public FilterNode parse(ParserContext<FilterNode> context) throws ParseException {
      if (!context.isEmpty())
        throw ParseException.semanticError(context.toString());
      return FilterNode.ALL_ITEMS;
    }
  };

  private AllItemsFilterNode() {
  }

  public FilterNode createCopy() {
    return this;
  }

  public static void register(TokenRegistry<FilterNode> registry) {
    registry.registerFunction(INVALID_TOKEN, INVALID_LEAF_PARSER);
    registry.registerFunction(All_TOKEN, INVALID_LEAF_PARSER);
  }

  @NotNull
  public EditorNode createEditorNode(EditorContext context) {
    return new ConstraintEditorNodeImpl(context);
  }

  public BoolExpr<DP> createFilter(ItemHypercube hypercube) {
    return BoolExpr.TRUE();
  }

  @Nullable
  public final ConstraintType getType() {
    return null;
  }

  @NotNull
  public String getSuggestedName(Map<TypedKey<?>, ?> hints) {
    return Local.parse("All " + Terms.ref_Artifacts);
  }

  public void normalizeNames(NameResolver resolver, ItemHypercube cube) {
  }

  public boolean isSame(FilterNode filterNode) {
    return filterNode == AllItemsFilterNode.this;
  }

  public Constraint createConstraint(ItemHypercube cube) {
    return Constraint.NO_CONSTRAINT;
  }

  public void writeFormula(FormulaWriter writer) {
    writer.addToken(All_TOKEN);
    writer.createChild();
  }
}
