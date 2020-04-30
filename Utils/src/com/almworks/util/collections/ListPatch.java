package com.almworks.util.collections;

import com.almworks.util.commons.Condition2;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.List;

public class ListPatch<F, T> {
  private final List<F> mySubject;
  private final List<T> myGoal;
  private ListPatchPolicy myPolicy = ListPatchPolicy.SINGLE_UNREPEATING_ITEMS;
  private Condition2<F, T> myEquality = null;

  public ListPatch(List<F> subject, List<T> goal) {
    mySubject = subject;
    myGoal = goal;
  }

  public static <F, T> ListPatch<F, T> create(List<F> subject, List<T> goal) {
    return new ListPatch<F, T>(subject, goal);
  }

  public void setPolicy(ListPatchPolicy policy) {
    myPolicy = policy;
  }

  public void setEquality(Condition2<F, T> equality) {
    myEquality = equality;
  }

  public List<ListPatchStep> generate() {
    // todo add ListPatchImpl when there are different policies
    assert myPolicy == ListPatchPolicy.SINGLE_UNREPEATING_ITEMS;

    // SINGLE_UNREPEATING_ITEMS - works for N^2 comparisons in the worst case (completely different lists)

    List<ListPatchStep> result = Collections15.arrayList();

    int subjectCount = mySubject.size();
    int goalCount = myGoal.size();

    // next "subject" index goal be compared/acted
    int si = 0;

    // next "goal" index goal be compared/acted
    int gi = 0;

    // difference between resulting array index and si due goal insertions and deletions
    int sdif = 0;

    while (si < subjectCount && gi < goalCount) {
      if (equal(si, gi)) {
        si++;
        gi++;
        continue;
      }
      int maxDepth = Math.max(subjectCount - si, goalCount - gi);
      boolean found = false;
      for (int i = 1; i < maxDepth; i++) {
        if (gi + i < goalCount && equal(si, gi + i)) {
          // insert a chunk
          for (int k = 0; k < i; k++) {
            result.add(ListPatchStep.add(si + sdif, gi + k));
            sdif++;
          }
          si++;
          gi = gi + i + 1;
          found = true;
          break;
        } else if (si + i < subjectCount && equal(si + i, gi)) {
          // remove a chunk
          for (int k = 0; k < i; k++) {
            result.add(ListPatchStep.remove(si + sdif));
          }
          si = si + i + 1;
          sdif -= i;
          gi++;
          found = true;
          break;
        }
      }
      if (!found) {
        result.add(ListPatchStep.remove(si + sdif));
        result.add(ListPatchStep.add(si + sdif, gi));
        si++;
        gi++;
      }
    }
    assert si == subjectCount || gi == goalCount;
    if (si < subjectCount) {
      int newSize = si + sdif;
      while (si < subjectCount) {
        result.add(ListPatchStep.remove(newSize));
        si++;
      }
    } else if (gi < goalCount) {
      while (gi < goalCount) {
        result.add(ListPatchStep.add(si + sdif, gi));
        gi++;
        sdif++;
      }
    }
    return result;
  }

  private boolean equal(int fi, int ti) {
    F f = mySubject.get(fi);
    T t = myGoal.get(ti);
    if (myEquality == null)
      return Util.equals(f, t);
    else
      return myEquality.isAccepted(f, t);
  }
}
