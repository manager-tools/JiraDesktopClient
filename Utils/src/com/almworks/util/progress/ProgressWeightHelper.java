package com.almworks.util.progress;

import com.almworks.util.Pair;
import org.almworks.util.Collections15;

import java.util.List;

public class ProgressWeightHelper {
  private int myTotalWeight = 0;
  private final List<Pair<ProgressSource, Integer>> mySources = Collections15.arrayList();

  public ProgressWeightHelper addSource(ProgressSource source, int weight) {
    if (weight <= 0) {
      assert false : weight;
    } else {
      mySources.add(Pair.create(source, weight));
      myTotalWeight += weight;
    }
    return this;
  }

  public ProgressWeightHelper insertTo(Progress progress, double totalSpan) {
    if (myTotalWeight == 0) {
      assert false : this + " " + progress;
      return this;
    }
    double available = progress.getOwnSpan();
    if (totalSpan > available + 1e-6) {
      assert false : progress + " " + totalSpan + " " + available;
      return this;
    }
    for (Pair<ProgressSource, Integer> source : mySources) {
      double span = (double)(((double)totalSpan) * source.getSecond() / myTotalWeight);
      if (span > 1e-4) {
        // otherwise insignificant
        progress.delegate(source.getFirst(), span);
      }
    }
    return this;
  }

  public ProgressWeightHelper insertTo(Progress progress) {
    return insertTo(progress, progress.getOwnSpan());
  }
}
