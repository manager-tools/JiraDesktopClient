package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.application.qb.CannotSuggestNameException;
import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ConstraintNegation;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.EditorGroupNode;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolOperation;
import com.almworks.util.text.parser.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class NeitherFilterNode extends CompositeFilterNode {
  private static final String TOKEN_NOT = "not";
  static final CompositeFormulaSerializer SERIALIZER = new CompositeFormulaSerializer() {
    public void serialize(FormulaWriter writer, List<FilterNode> nodes) {
      writer.addRaw(TOKEN_NOT);
      writer.addRaw(" ");
      BinaryCommutative.OR_SERIALIZER.serialize(writer.createChild(), nodes);
    }

    public void register(TokenRegistry<FilterNode> registry) {
      registry.registerFunction(TOKEN_NOT, new FunctionParser<FilterNode>() {
        public FilterNode parse(ParserContext<FilterNode> context) throws ParseException {
          if (context.isEmpty())
            return create(Collections15.<FilterNode>emptyList());
          FilterNode negated = context.parseNode();
          List<FilterNode> children;
          if (negated instanceof BinaryCommutative.Or)
            children = ((BinaryCommutative.Or) negated).getChildren();
          else
            children = Collections.singletonList(negated);
          return create(children);
        }
      });
    }

    public FilterNode create(List<FilterNode> children) {
      return new NeitherFilterNode(children);
    }
  };

  public NeitherFilterNode(List<FilterNode> arguments) {
    super(arguments, SERIALIZER);
  }

  @Override
  @Nullable
  public BoolExpr<DP> createFilter(ItemHypercube hypercube) throws UnresolvedNameException {
    List<FilterNode> children = getChildren();
    if (children.size() == 0)
      return null;
    List<BoolExpr<DP>> list = Collections15.arrayList(children.size());
    for (FilterNode child : children) {
      BoolExpr<DP> f = child.createFilter(hypercube);
      if (f == null) return null;
      list.add(f);
    }
    return BoolExpr.operation(BoolOperation.OR, list, true, true);
  }

  @Nullable
  public ConstraintNegation createConstraint(ItemHypercube cube) {
    List<Constraint> constraints = Collections15.arrayList();
    for (FilterNode node : getChildren()) {
      Constraint constraint = node.createConstraint(cube);
      if (constraint == null)
        return null;
      constraints.add(constraint);
    }
    return new ConstraintNegation.Simple(CompositeConstraint.Simple.or(constraints));
  }

  protected GroupingEditorNode createEmptyNode(EditorContext context) {
    return EditorGroupNode.createNegation(context, null);
  }

  public FilterNode createCopy() {
    return new NeitherFilterNode(createChildrenCopy());
  }

  @Nullable
  public String getSuggestedName(Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException {
    List<FilterNode> list = getChildren();
    if (list == null || list.size() == 0)
      return null;
    if (list.size() > 1) {
      throw new CannotSuggestNameException();
    }
    FilterNode child = list.get(0);
    if (child instanceof CompositeFilterNode) {
      // too complex
      throw new CannotSuggestNameException();
    }
    String childName = child.getSuggestedName(hints);
    if (childName == null)
      return null;
    return TOKEN_NOT + " " + childName;
  }
}
