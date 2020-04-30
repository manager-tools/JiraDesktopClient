package com.almworks.util.bool;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reusable builder for creating boolean expressions.
 * <p>
 * This builder acts as a tree builder, combining terms and operations. Builder has "current node", which is the
 * target of most operations.There's a "top node" which will finally be represented as a top-level instance of
 * BoolExpr.
 * <p>
 * You can traverse builder nodes up (to current's node parent), or create a sub-node and traverse to it. This
 * allows using BoolExprBuilder in recursive functions, where builder is passed as a parameter with the current
 * node set to the current recursion context.
 * <p>
 * A newly created node is "empty" - you should specify it to be either term or operation or literal. If you
 * don't specify node, you won't be able to build expression.
 * <p>
 * Most of the methods return <i>this</i> for method chaining.
 * <p>
 * This class is not thread-safe and must not be used concurrently.
 *
 * @param <P> predicate type
 * @see BoolExpr
 */
public class BoolExprBuilder<P> {
  /**
   * Top node that will be used to create final BoolExpr.
   */
  @NotNull
  private final Node myTop = new Node();

  /**
   * Current node, which all methods apply to.
   */
  @NotNull
  private Node myCurrent = myTop;

  public static <P> BoolExprBuilder<P> create() {
    return new BoolExprBuilder<P>();
  }

  /**
   * Clears all data from the nodes and makes top node current.
   * <p>
   * Note that no node objects are freed, so if you allocate builder once and then reuse it after calling clearAll,
   * the code will produce less garbage.
   */
  public final BoolExprBuilder<P> clearAll() {
    myTop.clear();
    myCurrent = myTop;
    return this;
  }

  /**
   * Clears current node so you can once more set its value.
   */
  public BoolExprBuilder<P> clearCurrent() {
    myCurrent.clear();
    return this;
  }

  /**
   * Creates boolean expression based on the current state of the builder. State is not modified.
   *
   * @return boolean expression
   * @throws UnderflowException if build is unsuccessful, for example, node type is not specified 
   */
  public BoolExpr<P> build() throws UnderflowException {
    return myTop.build();
  }

  /**
   * Sets current node to represent an already constructed expression. Note that "negated" status of
   * the node is not affected, so if it was negated before expression is set, you will get negated expression as a
   * result.
   */
  public BoolExprBuilder<P> setExpression(BoolExpr<P> expression) {
    myCurrent.setExpression(expression);
    return this;
  }

  public BoolExprBuilder<P> setLiteral(boolean literal) {
    return setExpression(BoolExpr.<P>literal(literal));
  }

  /**
   * Sets current node to represent a boolean operation.
   *
   * @param operation AND or OR
   * @param negated if true, the resulting expression will be negated
   */
  public BoolExprBuilder<P> setOperation(BoolOperation operation, boolean negated) {
    myCurrent.setOperation(operation);
    myCurrent.setNegated(negated);
    return this;
  }

  /**
   * Convenience method to copy operation and negated state from an existing operation.
   */
  public BoolExprBuilder<P> likeOperation(BoolExpr.Operation<?> operation) {
    return setOperation(operation.getOperation(), operation.isNegated());
  }

  /**
   * Sets current node to represent a terminal predicate.
   *
   * @param term predicate
   * @param negated negated state
   */
  public BoolExprBuilder<P> setTerm(P term, boolean negated) {
    myCurrent.setTerm(term);
    myCurrent.setNegated(negated);
    return this;
  }

  /**
   * Adds expression as a child node of the current operation node, which remains current.
   */
  public BoolExprBuilder<P> add(BoolExpr<P> expression) {
    return add().setExpression(expression).up();
  }

  /**
   * Adds all arguments as children of the current operation node, which remains current. 
   */
  public BoolExprBuilder<P> addAll(Collection<? extends BoolExpr<P>> arguments) {
    for (BoolExpr<P> argument : arguments) {
      add(argument);
    }
    return this;
  }

  /**
   * Adds an empty node as a child of the current operation node. The new node becomes current.
   */
  public BoolExprBuilder<P> add() {
    assert myCurrent.getOperation() != null;
    myCurrent = myCurrent.addNode();
    return this;
  }

  /**
   * Removes current node from its parent operation node. Current node is set to the parent.
   * If called when current node is the top of the tree, exception is thrown.
   */
  public void remove() {
    if (myCurrent == myTop)
      throw new UnderflowException();
    Node removed = myCurrent;
    up();
    myCurrent.remove(removed);
  }

  /**
   * Goes up the tree, making the parent of the current node current.
   */
  public BoolExprBuilder<P> up() {
    Node parent = myCurrent.getParent();
    if (parent == null)
      throw new UnderflowException();
    myCurrent = parent;
    return this;
  }

  /**
   * Negates current node, making the resulting expression from this node to be inverted.
   * <p>
   * Note that calling <code>builder.negate().negate()</code> will bring node back to original non-inverted state.
   *
   * @see #setNegated 
   */
  public BoolExprBuilder<P> negate() {
    myCurrent.negate();
    return this;
  }

  /**
   * Sets negated flag of the current node, regardless of the current state of this flag.
   */
  public BoolExprBuilder<P> setNegated(boolean negated) {
    myCurrent.setNegated(negated);
    return this;
  }


  private class Node {
    @Nullable
    private final Node myParent;

    @Nullable
    private BoolExpr<P> myExpression;

    @Nullable
    private P myTerm;

    @Nullable
    private BoolOperation myOperation;

    @Nullable
    private List<Node> myChildren;
    private int myChildCount;

    private boolean myNegated;

    private Node(Node parent) {
      myParent = parent;
    }

    private Node() {
      myParent = null;
    }

    public BoolExpr<P> build() {
      if (myExpression != null) {
        return myNegated ? myExpression.negate() : myExpression;
      }
      if (myTerm != null) {
        return BoolExpr.term(myTerm, myNegated);
      }
      if (myOperation != null) {
        List<BoolExpr<P>> arguments;
        if (myChildCount == 0) {
          arguments = Collections.emptyList();
        } else {
          arguments = Collections15.arrayList(myChildCount);
          assert myChildren != null;
          for (Node child : myChildren.subList(0, myChildCount)) {
            arguments.add(child.build());
          }
        }
        return BoolExpr.operation(myOperation, arguments, myNegated, true);
      }
      throw new UnderflowException();
    }

    public void setExpression(BoolExpr<P> expression) {
      assert isEmpty() : this;
      myExpression = expression;
    }

    public void setOperation(BoolOperation operation) {
      assert isEmpty() : this;
      myOperation = operation;
      myChildCount = 0;
    }

    public void setTerm(P term) {
      assert isEmpty() : this;
      myTerm = term;
    }

    public Node addNode() {
      assert myOperation != null : this;
      if (myChildren == null)
        myChildren = Collections15.arrayList();
      Node node;
      if (myChildCount >= myChildren.size()) {
        node = new Node(this);
        myChildren.add(node);
      } else {
        node = myChildren.get(myChildCount);
        node.clear();
      }
      myChildCount++;
      return node;
    }

    private boolean isEmpty() {
      return myOperation == null && myExpression == null && myTerm == null;
    }

    @Nullable
    public BoolOperation getOperation() {
      return myOperation;
    }

    @Nullable
    public Node getParent() {
      return myParent;
    }

    public void negate() {
      myNegated = !myNegated;
    }

    public void setNegated(boolean negated) {
      myNegated = negated;
    }

    public void clear() {
      myChildCount = 0;
      myExpression = null;
      myTerm = null;
      myOperation = null;
      myNegated = false;
      if (myChildren != null) {
        for (Node child : myChildren) {
          child.clear();
        }
      }
    }

    public void remove(Node node) {
      if (myChildren != null && myChildren.remove(node)) {
        myChildCount--;
      }
    }
  }

  public static class UnderflowException extends RuntimeException {
  }
}
