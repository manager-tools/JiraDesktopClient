package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EditorContext;
import com.almworks.api.application.qb.EditorNode;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.collections.FactoryWithParameter;
import com.almworks.util.text.parser.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author dyoma
 */
public abstract class CompositeFilterNode implements FilterNode {
  private final List<FilterNode> myArguments;
  private final CompositeFormulaSerializer mySerializer;

  public CompositeFilterNode(List<FilterNode> arguments, CompositeFormulaSerializer serializer) {
    assert arguments != null;
    mySerializer = serializer;
    for (FilterNode node : arguments) {
      assert node != null;
    }
    myArguments = Collections15.arrayList(arguments);
  }

  public List<FilterNode> getChildren() {
    return Collections.unmodifiableList(myArguments);
  }

  public void normalizeNames(NameResolver resolver, ItemHypercube cube) {
    for (ListIterator<FilterNode> iterator = myArguments.listIterator(); iterator.hasNext();)
      iterator.next().normalizeNames(resolver, cube);
  }

  public final void writeFormula(FormulaWriter writer) {
    mySerializer.serialize(writer, getChildren());
  }

  @NotNull
  public final EditorNode createEditorNode(EditorContext context) {
    GroupingEditorNode node = createEmptyNode(context);
    node.addChildConstraints(getChildren());
    return node;
  }

  @Nullable
  public final ConstraintType getType() {
    return null;
  }

  protected List<FilterNode> createChildrenCopy() {
    List<FilterNode> copy = Collections15.arrayList(myArguments.size());
    for (FilterNode node : myArguments) {
      copy.add(node.createCopy());
    }
    return copy;
  }

  protected abstract GroupingEditorNode createEmptyNode(EditorContext context);

  public static abstract class InfixFormulaSerializer implements CompositeFormulaSerializer, CommutativeParser<FilterNode> {
    private final String myInfixToken;

    public InfixFormulaSerializer(String infixToken) {
      myInfixToken = infixToken;
    }

    public void serialize(FormulaWriter writer, List<FilterNode> nodes) {
      writer.createChild().writeSeparated(nodes, myInfixToken);
    }

    public FilterNode parse(Iterator<ParserContext<FilterNode>> parameters) throws ParseException {
      return create(ParserContext.parseAll(parameters));
    }

    public abstract BinaryCommutative create(List<FilterNode> nodes);

    public void register(TokenRegistry<FilterNode> registry) {
      registry.registerInfixOperation(myInfixToken, this);
    }
  }

  public boolean isSame(FilterNode filterNode) {
    return filterNode != null && getClass().equals(filterNode.getClass()) &&
      areChildrenTheSame(getChildren(), ((CompositeFilterNode) filterNode).getChildren());
  }

  private boolean areChildrenTheSame(List<FilterNode> children, List<FilterNode> children1) {
    assert children != null;
    assert children1 != null;
    if (children.size() != children1.size())
      return false;
    int counter = 0;
    for (FilterNode filterNode : children) {
      boolean isFound = false;
      for (FilterNode node : children1) {
        if (node.isSame(filterNode)) {
          isFound = true;
          counter++;
          break;
        }
      }
      if (!isFound)
        return false;
    }
    return counter == children1.size();
  }

  public interface CompositeFormulaSerializer extends FactoryWithParameter<FilterNode, List<FilterNode>>{
    void serialize(FormulaWriter writer, List<FilterNode> nodes);

    void register(TokenRegistry<FilterNode> registry);
  }
}
