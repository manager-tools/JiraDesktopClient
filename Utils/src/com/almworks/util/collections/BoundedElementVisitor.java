package com.almworks.util.collections;

/**
 * Interface Visitor could be used in any kind of iteration as a receptacle point. It is
 * usually passed to methods that iterate over a collection of objects and delegate actions
 * upon each object to outside entity.
 * <p/>
 * This interface imposes the following contract upon anyone who calls Visitor methods.
 * <ol>
 * <li> Before iteration starts, {@link #startVisit()} must be called, even if there are zero elements.
 * <li> After iteration ends, {@link #endVisit()} must be called, even if error was encountered.
 * <li> Whenever agent (entity that is iterating) cannot finish iteration, it must call visitException(), unless visitor
 * has returned false from one of the visiting methods.
 * <li> Implementation is not thread-safe. All methods should be called from the same thread.
 * <li> After visiting is over, it is possible to reuse the visitor, if that makes sense.
 * </ol>
 * <p/>
 * The following contract is imposed upon anyone implementing this interface.
 * <ol>
 * <li> If any visitor method returns false, indicating that visiting should end as soon as possible (does not make
 * sense), it is only a <b>hint</b> to active agent. It may anyway continue to call visitor methods according to its
 * contract.
 * <li> Implementation of visiting methods should not contain time-consuming operations such as disk i/o.
 * <li> If error is thrown from any visitor's method except {@link #visitException(Exception)}, then
 * {@link #visitException(Exception)} <b>may</b> be called with that error. (Or it may not.)
 * </ol>
 * <p/>
 * This default contract may be narrowed in some cases.
 *
 * @author sereda
 * @see
 */
public interface BoundedElementVisitor <E> extends ElementVisitor<E> {
  /**
   * Indicates start of visit from active agent. This method is called before any other methods could be called.
   *
   * @return hint to iterating agent whether to continue iteration. If Visitor returns false, it lets know active agent
   *         that it may finish the visit. This is only a hint, and visiting methods could be called according to the contract
   *         anyway.
   */
  boolean startVisit();

  /**
   * Passes next element to visitor.
   *
   * @return hint to iterating agent whether to continue iteration. See {@link #startVisit()}.
   */
  boolean visit(E element);

  /**
   * Indicates that visit is over. After this method is called, no other method could be called except for
   * {@link #startVisit()} that would indicate a new visit.
   */
  void endVisit();

  /**
   * Indicates a problem with iteration at the agent's side. No further calls to {@link #visit} will be performed
   * and a call to {@link #endVisit()} will be performed soon.
   *
   * @param e (not null) error that has been raised in active agent.
   */
  void visitException(Exception e);


  /**
   * This is the default implementation of Visitor. It does almost nothing, providing base class to quickly
   * write visitors.
   */
  public static class Default <E> implements BoundedElementVisitor<E> {
    protected boolean myVisiting = false;

    public boolean startVisit() {
      if (myVisiting)
        throw new IllegalStateException(this + " is already visiting");
      myVisiting = true;
      return true;
    }

    public boolean visit(E element) {
      if (!myVisiting)
        throw new IllegalStateException(this + " is not visiting");
      return true;
    }

    public void endVisit() {
      if (!myVisiting)
        throw new IllegalStateException(this + " is not visiting");
      myVisiting = false;
    }

    public void visitException(Exception e) {
      if (!myVisiting)
        throw new IllegalStateException(this + " is not visiting");
    }
  }
}
