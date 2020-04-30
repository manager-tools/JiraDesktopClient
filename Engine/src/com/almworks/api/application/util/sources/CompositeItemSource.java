package com.almworks.api.application.util.sources;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.util.Pair;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.progress.ProgressWeightHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class CompositeItemSource extends AbstractItemSource {
  private final TypedKey<Aggregator> AGGREGATOR = key(Aggregator.class);
  private final TypedKey<List<Pair<ItemSource, Integer>>> LIST = key(CompositeItemSource.class);
  private final String myDefaultActivityString;

  public CompositeItemSource(String name, String defaultActivityString) {
    super(name);
    myDefaultActivityString = defaultActivityString;
  }

  public void stop(@NotNull ItemsCollector collector) {
    Aggregator aggregator = collector.getValue(AGGREGATOR);
    if (aggregator != null) {
      aggregator.stop();
      collector.putValue(AGGREGATOR, null);
      getProgressDelegate(collector).setDone();
    }
  }

  public final void reload(@NotNull ItemsCollector collector) {
    reloadingStop(collector);
    reloadingPrepare(collector);

    List<Pair<ItemSource, Integer>> list = getList(collector);
    ItemSource[] sources;
    int[] weights;
    synchronized (list) {
      if (list.size() == 0) {
        getProgressDelegate(collector).setDone();
        return;
      }
      sources = new ItemSource[list.size()];
      weights = new int[list.size()];
      for (int i = 0; i < list.size(); i++) {
        Pair<ItemSource, Integer> pair = list.get(i);
        sources[i] = pair.getFirst();
        weights[i] = pair.getSecond();
      }
    }
    Aggregator aggregator = new Aggregator(myName, myDefaultActivityString, collector, sources, weights);
    collector.putValue(AGGREGATOR, aggregator);
    aggregator.reload();
    getProgressDelegate(collector).delegate(aggregator.getProgressSource());
  }

  /**
   * Override this method to add sources and detaches on reload.
   */
  protected void reloadingPrepare(ItemsCollector collector) {
  }

  private void reloadingStop(ItemsCollector collector) {
    stop(collector);
  }

  public Detach add(final ItemsCollector collector, final ItemSource source, int weight) {
    List<Pair<ItemSource, Integer>> list = getList(collector);
    synchronized (list) {
      list.add(Pair.create(source, weight));
    }
    return new Detach() {
      protected void doDetach() {
        remove(collector, source);
      }
    };
  }

  public void remove(ItemsCollector collector, ItemSource source) {
    List<Pair<ItemSource, Integer>> list = getList(collector);
    synchronized (list) {
      for (Iterator<Pair<ItemSource, Integer>> ii = list.iterator(); ii.hasNext();) {
        Pair<ItemSource, Integer> pair = ii.next();
        if (pair.getFirst().equals(source))
          ii.remove();
      }
    }
  }

  public void clear(ItemsCollector collector) {
    List<Pair<ItemSource, Integer>> list = getList(collector);
    synchronized (list) {
      list.clear();
    }
  }

  protected List<Pair<ItemSource, Integer>> getList(ItemsCollector collector) {
    List<Pair<ItemSource, Integer>> value = collector.getValue(LIST);
    if (value == null) {
      value = Collections15.arrayList();
      collector.putValue(LIST, value);
    }
    return value;
  }

  private static class Aggregator {
    private final ItemSource[] mySources;
    private final ItemsCollector myCollector;
    private final Progress myProgress;
    private final int[] myWeights;

    public Aggregator(String name, final String defaultActivityString, ItemsCollector collector,
      ItemSource[] sources, int[] weights) {
      assert collector != null;
      assert sources != null;
      assert sources.length > 0;
      assert weights.length == sources.length;
      myCollector = collector;
      mySources = sources;
      myWeights = weights;
      myProgress = Progress.delegator("composite:" + name);
    }

    public ProgressSource getProgressSource() {
      return myProgress;
    }

    public synchronized void reload() {
      ProgressWeightHelper helper = new ProgressWeightHelper();
      for (int i = 0; i < mySources.length; i++) {
        mySources[i].reload(myCollector);
        helper.addSource(mySources[i].getProgress(myCollector), myWeights[i]);
      }
      helper.insertTo(myProgress);
    }

    public synchronized void stop() {
      for (ItemSource source : mySources) {
        source.stop(myCollector);
      }
    }
  }
}
