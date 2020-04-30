package com.almworks.items.impl.sqlite;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.almworks.util.bool.BoolOperation.AND;
import static java.util.Collections.emptyList;
import static org.almworks.util.Collections15.arrayList;

/**
 * todo rewrite doc for general case
 * Builder of a tree of P based on boolean expressions on T. For a given constraint,
 * it finds the closest parent constraint in the cache and creates a new item source with the
 * remainder constraint. Parent of the created FIS
 * is the FIS for the found cache constraint. If the remainder is empty, the existing FIS is returned.
 * If no direct parent is found in the cache, a closest matching constraint is searched for.
 * In this case, a new constraint
 * is created which will be parent for the query constraint and the closest
 * cache constraint, and a new filtering item source  for it.
 * <p/>
 * Item source retrieval is not time-optimized.
 *
 * @author igor baltiyskiy
 * @author sereda
 */
public abstract class BoolSubjectTreeMapper<T, S> {
  private final CopyOnWriteArrayList<Entry> myEntries = new CopyOnWriteArrayList<Entry>();

  protected abstract S create(BoolExpr<T> expr);

  protected abstract void setParent(S object, S parent, BoolExpr<T> workingExpr, TransactionContext context);

  protected MapOp createOp() {
    return new MapOp();
  }

  public final S map(BoolExpr<T> expr, TransactionContext context) {
    BoolSubjectTreeMapper<T, S>.Entry entry = map0(expr, arrayList(myEntries), context);
    return entry.getSubject();
  }

  @NotNull
  public final Entry map0(BoolExpr<T> expr, List<Entry> entries, TransactionContext context) {
    expr = Reductions.simplify(expr);
    List<BoolExpr<T>> conjuncts = Reductions.toOperandList(expr, AND);
    MapOp op = createOp();

    findBestEntry(conjuncts, op, entries);

    if (op.bestEntry != null) {
      BoolExpr<T> remainder = and(op.bestPrivateConj);
      if (op.bestCachedPrivateConjCount == 0) {
        // parent found
        if (op.bestPrivateConj.isEmpty()) {
          // parent IS the result
          return op.bestEntry;
        }
        return addNew(expr, conjuncts, op.bestEntry, remainder, context);
      }

      BoolExpr<T> peerExpr = and(op.bestEntry.getConjuncts());
      BoolExpr<T> parentExpr = and(op.bestCommonConj);
      if (isCoupling(op, remainder, peerExpr, parentExpr)) {
        // todo when a parent is extracted from a filter, that filter's filter remains (should be converted to remainder)
        entries.remove(op.bestEntry);
        Entry newParent = map0(parentExpr, entries, context);
        op.bestEntry.setEntryParent(newParent, and(op.bestEntryPrivateConj), context);

        if (op.bestPrivateConj.isEmpty()) {
          return newParent;
        } else {
          return addNew(expr, conjuncts, newParent, remainder, context);
        }
      }
    }
    return addNew(expr, conjuncts, null, expr, context);
  }

  /**
   * we need to create a new FIS and chain the closest cache entry to it
   * new parent filter is added to the cache and both the found closest filter and the newly created filter are chained to it iff
   * the new parent filter is valid
   * the new parent filter expr has greater priority than the aggregate priority of the new filter and the closest filter exprs
   * the new parent filter has greater priority than that of the existing parent of the closest cache filter
   */
  private boolean isCoupling(MapOp op, BoolExpr<T> remainder, BoolExpr<T> peerExpr, BoolExpr<T> parentExpr) {
    return parentExpr != BoolExpr.TRUE() &&
      getExprPriority(parentExpr) > getExprPriority(remainder) + getExprPriority(peerExpr) &&
      getExprPriority(parentExpr) > getExprPriority(op.bestEntry.getParentExpr());
  }

  /**
   * Retrieves priority of the expr in the specific field of its application.
   * Expr priority provides hint to the filter manager for caching filter results: filter for the expr with higher priority will be preferred over ones for exprs with lower priorities.
   * <p/>
   * In a simple filter manager, all constraints have the same priority of -1 except NO_CONSTRAINT, which has the lowest priority -2. -2 if NO_CONSTRAINT, -1 otherwise
   *
   * @param expr Expr to calculate the priority
   * @return Expr priority in the selected field of application
   */
  protected int getExprPriority(@Nullable BoolExpr<T> expr) {
    return (expr == null || expr instanceof BoolExpr.Literal) ? -2 : -1;
  }

  /**
   * Finds an entry in the filter cache with the conjuncts closest to the specified query conjunctions.
   */
  private void findBestEntry(List<BoolExpr<T>> conjuncts, MapOp op, Collection<Entry> entries) {
    boolean tentative = false;
    // first pass: find only known to be better
    for (Entry entry : entries) {
      Boolean r = isBetterEntry(entry, conjuncts, op);
      if (r == null) {
        tentative = true;
      } else if (r) {
        setBestEntry(entry, op);
      }
    }
    if (op.bestEntry == null && tentative) {
      // second pass: also accept known to be not worse
      for (Entry entry : entries) {
        Boolean r = isBetterEntry(entry, conjuncts, op);
        if (r == null || r) {
          setBestEntry(entry, op);
        }
      }
    }
  }

  @Nullable
  protected Boolean isBetterEntry(Entry entry, List<BoolExpr<T>> conj, MapOp op) {
    if (op.tempCommonConj == null)
      op.tempCommonConj = arrayList();
    if (op.tempPrivateConj == null)
      op.tempPrivateConj = arrayList();
    if (op.tempEntryPrivateConj == null)
      op.tempEntryPrivateConj = arrayList();
    diff(conj, entry.getConjuncts(), op.tempCommonConj, op.tempPrivateConj, op.tempEntryPrivateConj);
    op.tempCachedPrivateConjCount = entry.getConjuncts().size() - op.tempCommonConj.size();
    assert op.tempCachedPrivateConjCount >= 0 : entry.getConjuncts() + " " + conj + " " + op.tempCommonConj;

    if (op.bestCachedPrivateConjCount > 0 && op.tempCachedPrivateConjCount == 0) {
      // current optimum is not parent, entry is parent
      return true;
    }

    if (op.bestCachedPrivateConjCount == 0 && op.tempCachedPrivateConjCount > 0) {
      // current optimum is parent, entry is not parent
      return false;
    }

    // both are parents or not parents

    if (op.tempCommonConj.size() > op.bestCommonConj.size()) {
      // bigger common part is better
      return true;
    }

    if (op.tempCachedPrivateConjCount < op.bestCachedPrivateConjCount) {
      // smaller private count in the cached item is better
      return true;
    }

//      int candCount = entry.getParentable().getCountable().getCount();
//      better = (candCount != -1) && (candCount < xxx);

    return null;
  }

  protected void setBestEntry(Entry entry, MapOp op) {
    op.bestEntry = entry;
    op.bestCachedPrivateConjCount = op.tempCachedPrivateConjCount;
    op.bestPrivateConj = op.tempPrivateConj;
    op.bestEntryPrivateConj = op.tempEntryPrivateConj;
    op.bestCommonConj = op.tempCommonConj;
    op.tempPrivateConj = null;
    op.tempCommonConj = null;
    op.tempEntryPrivateConj = null;
  }

  /**
   * Accepts a list of conjunctions in query expr and cache expr, and finds different parts and equal parts in the lists.
   *
   * @param conj1  Conjunctions in the query expr [in]
   * @param conj2  Conjunctions in the cache expr [in]
   * @param common Conjunctions that are equal in the query and cache expr [out]
   * @param only1  Conjunctions present only in query expr [out]
   */
  private static <T> void diff(Collection<BoolExpr<T>> conj1, Collection<BoolExpr<T>> conj2,
    Collection<BoolExpr<T>> common, Collection<BoolExpr<T>> only1, Collection<BoolExpr<T>> only2)
  {
    common.clear();
    common.addAll(conj1);
    common.retainAll(conj2);

    only1.clear();
    only1.addAll(conj1);
    only1.removeAll(common);

    only2.clear();
    only2.addAll(conj2);
    only2.removeAll(common);
  }

  private BoolExpr<T> and(Collection<BoolExpr<T>> conjuncts) {
    if (conjuncts.isEmpty())
      return BoolExpr.TRUE();
    else if (conjuncts.size() == 1)
      return conjuncts.iterator().next();
    else
      return BoolExpr.operation(AND, arrayList(conjuncts), false, true);
  }

  private Entry addNew(BoolExpr<T> expr, List<BoolExpr<T>> conjuncts, Entry parent, BoolExpr<T> workingExpr, TransactionContext context) {
    Entry entry = new Entry(expr, conjuncts, create(expr), parent, workingExpr, context);
    myEntries.add(entry);
    return entry;
  }

  /**
   * Finds a filter in the cache and returns its parent expr. If there is no such filter in the cache, returns null.
   * Note that a filter's parent expr may also be null.
   *
   * @param filter Filter for which to find parent expr
   */
  @Nullable
  BoolExpr<T> getParentExprFromCache(S filter) {
    for (Entry e : myEntries) {
      if (e.getSubject().equals(filter)) {
        return e.getParentExpr();
      }
    }
    return null;
  }

  /**
   * Returns current cache size.
   *
   * @return cache size.
   */
  int getCacheSize() {
    return myEntries.size();
  }

  /**
   * Given a expr, checks that filter for that expr is in the cache.
   *
   * @param expr Expr that must have a corresponing entry in the cache.
   * @return true if there is a filter for that expr in the cache.
   */
  boolean checkInCache(BoolExpr<T> expr) {
    BoolExpr<T> simplifiedExpr = Reductions.simplify(expr);
    for (Entry e : myEntries) {
      List<BoolExpr<T>> l = arrayList(e.getConjuncts());
      BoolExpr<T> cacheExpr = and(l);
      if (cacheExpr.equals(simplifiedExpr)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Entry of the filter cache.
   */
  protected class Entry {
    /**
     * Expression that this entry corresponds to
     */
    @NotNull
    private final BoolExpr<T> myExpr;

    /**
     * myExpr broken into conjuncts
     */
    @NotNull
    private final List<BoolExpr<T>> myConjuncts;


    /**
     * Subject, created for this expression
     */
    @NotNull
    private final S mySubject;

    /**
     * Parent entry
     */
    @Nullable
    private Entry myParent;

    /**
     * Expr that is joined with myParent.myExpr to get myExpr
     */
    @NotNull
    private BoolExpr<T> myWorkingExpr;


    public Entry(@NotNull BoolExpr<T> expr, @NotNull List<BoolExpr<T>> conjuncts, @NotNull S subject, Entry parent, @NotNull BoolExpr<T> workingExpr, TransactionContext context)
    {
      assert Reductions.toOperandList(expr, AND).equals(conjuncts) : expr + " " + conjuncts;
      assert parent != null || expr.equals(workingExpr) : expr + " " + workingExpr + " " + parent;
      myExpr = expr;
      myConjuncts = conjuncts;
      mySubject = subject;
      myParent = parent;
      myWorkingExpr = workingExpr;
      updateParent(context);
    }

    public void setEntryParent(Entry parent, @NotNull BoolExpr<T> workingExpr, TransactionContext context) {
      assert parent != null || myExpr.equals(workingExpr) : myExpr + " " + workingExpr + " " + parent;
      myParent = parent;
      myWorkingExpr = workingExpr;
      updateParent(context);
    }

    private void updateParent(TransactionContext context) {
      setParent(mySubject, myParent == null ? null : myParent.mySubject, myWorkingExpr, context);
    }

    @NotNull
    public List<BoolExpr<T>> getConjuncts() {
      return myConjuncts;
    }

    @NotNull
    public S getSubject() {
      return mySubject;
    }

    @Nullable
    public BoolExpr<T> getParentExpr() {
      return myParent == null ? null : myParent.getExpr();
    }

    @NotNull
    public BoolExpr<T> getExpr() {
      return myExpr;
    }
  }


  @SuppressWarnings({"InstanceVariableNamingConvention"})
  protected class MapOp {
    private Entry bestEntry;

    /**
     * Temporary sets for calculating diffs and comparing against best
     */
    private List<BoolExpr<T>> tempPrivateConj;
    private List<BoolExpr<T>> tempEntryPrivateConj;
    private List<BoolExpr<T>> tempCommonConj;
    private int tempCachedPrivateConjCount;

    /**
     * Conjuncts only in the query set
     */
    private List<BoolExpr<T>> bestPrivateConj = emptyList();

    /**
     * Conjuncts only in the entry set
     */
    private List<BoolExpr<T>> bestEntryPrivateConj = emptyList();

    /**
     * Conjuncts common for the query and closest cache entry lists
     */
    private List<BoolExpr<T>> bestCommonConj = emptyList();

    /**
     * The number of private conjs (not common with the requested conjs) in the best matching entry.
     * If == 0, then best found entry is the parent.
     */
    private int bestCachedPrivateConjCount = Integer.MAX_VALUE;
  }
}
