package com.almworks.util.model;

import com.almworks.integers.LongArray;
import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import util.concurrent.Synchronized;

import java.util.*;

public class SetHolderModelTests extends BaseTestCase {
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void test() {
    final SetHolderModel<String> model = new SetHolderModel<String>();
    model.addInitListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, new SetHolder.Listener<String>() {
      long version = 0;
      @Override
      public void onSetChanged(@NotNull SetHolder.Event<String> event) {
        version = event.actualize(version);
        for (String s : event.getAdded()) {
          if (s.length() % 2 == 0) model.remove(s);
        }
      }
    });
    model.add("1", "22", "3");
    CHECK.unordered(model.copyCurrent().toArray(), "1", "3");
    ImageCollector imageCollector = new ImageCollector(true);
    model.addInitListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, imageCollector);
    model.add("44", "5");
    imageCollector.checkEvents("+1 3 -", "+5 -", "+ -");
  }

  public void testComplexConcurrency() {
    class SetUpdater implements SetHolder.Listener<String> {
      private final List<Pair<Collection<String>, Collection<String>>> myChanges = Collections15.arrayList();
      private final SetHolderModel<String> myModel;

      SetUpdater(SetHolderModel<String> model) {
        myModel = model;
      }

      @Override
      public void onSetChanged(@NotNull SetHolder.Event<String> event) {
        if (myChanges.isEmpty()) return;
        Pair<Collection<String>, Collection<String>> change = myChanges.remove(0);
        myModel.changeSet(change.getFirst(), change.getSecond());
      }

      public void checkEmpty() {
        assertTrue(myChanges.isEmpty());
      }

      public void addChange(Collection<String> add, Collection<String> remove) {
        myChanges.add(Pair.create(add, remove));
      }
    }
    SetHolderModel<String> model = new SetHolderModel<String>();
    SetUpdater updater = new SetUpdater(model);
    model.addInitListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, updater);
    ImageCollector imageCollector = new ImageCollector(false);
    model.addInitListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, imageCollector);
    imageCollector.checkEvents();
    model.add("a");
    imageCollector.checkEvents("+a -");
    imageCollector.clearEvents();
    CHECK.unordered(model.copyCurrent(), "a");

    updater.checkEmpty();
    updater.addChange(null, Arrays.asList("a"));
    updater.addChange(Arrays.asList("a"), null);
    model.add("b");
    imageCollector.checkEvents("+b -");
    model.remove("a");
    CHECK.unordered(model.copyCurrent(), "b");
    imageCollector.clearEvents();

    updater.checkEmpty();
    updater.addChange(Arrays.asList("a"), null);
    updater.addChange(null, Arrays.asList("a"));
    model.remove("b");
    imageCollector.checkEvents("+ -b");

    model.add("a");
    imageCollector.clearEvents();
    CHECK.unordered(model.copyCurrent(), "a");
    updater.checkEmpty();
    updater.addChange(null, Arrays.asList("a"));
    updater.addChange(Arrays.asList("a"), null);
    model.add("b");
    imageCollector.checkEvents("+b -");
    CHECK.unordered(model.copyCurrent(), "a", "b");
  }

  private static class Image implements SetHolder.Listener<Integer> {
    private final List<Integer> myImage = Collections15.arrayList();
    private long myVersion = 0;
    private final Image[] myOthers;
    private final LongArray myVersions = new LongArray();

    public Image(Image[] others) {
      myOthers = others;
    }

    @Override
    public void onSetChanged(@NotNull SetHolder.Event<Integer> event) {
//      List<Integer> copy;
//      long version;
      synchronized (myImage) {
        myVersion = event.actualize(myVersion);
        if (event.isEmpty()) {
//          myVersions.add(-myVersion);
          return;
        }
//        myVersions.add(myVersion);
//        if (Containers.containsAny(myImage, event.getAdded()))
//          System.out.println("");
//        if (!myImage.containsAll(event.getRemoved()))
//          System.out.println("");
        myImage.addAll(event.getAdded());
        myImage.removeAll(event.getRemoved());
//        copy = Collections15.arrayList(myImage);
//        version = myVersion;
      }
//      Collections.sort(copy);
//      synchronized (myOthers) {
//        for (Image other : myOthers) {
//          if (other != null) other.checkImage(copy, version, event);
//        }
//      }
    }

    private void checkImage(List<Integer> sorted, long version, SetHolder.Event<?> event) {
      List<Integer> copy;
      synchronized (myImage) {
        if (myVersion != version) return;
        copy = Collections15.arrayList(myImage);
      }
      Collections.sort(copy);
      if (!copy.equals(sorted)) {
        ((SetHolderModel) event.getSet()).testPrintHistory();
        System.out.println("Own  : " + TextUtil.separateToString(copy, ","));
        System.out.println("Other: " + TextUtil.separateToString(sorted, ","));
        System.out.println("OwnVersions : " + myVersions);
      }
    }

    public void checkImage(List<Integer> expected) {
      List<Integer> own;
      synchronized (myImage) {
        own = Collections15.arrayList(myImage);
      }
      Collections.sort(own);
      CHECK.order(expected, own);
    }
  }

  private static class Updater extends Thread {
    private final Random myRandom = new Random();
    private final Synchronized<Integer> myState = new Synchronized<Integer>(0);
    private final SetHolderModel<Integer> model;

    Updater(SetHolderModel<Integer> model) {
      this.model = model;
    }

    @Override
    public void run() {
      while (true) {
        int value = myRandom.nextInt(100);
        if (value % 10 == 0) {
          List<Integer> remove = collectCurrent(3, (value % 10));
          model.changeSet(null, remove);
        } else {
          List<Integer> add = Collections15.arrayList();
          for (int i = 0; i < 10; i++) add.add(myRandom.nextInt(100));
          List<Integer> remove = collectCurrent(7, value % 10);
          model.changeSet(add, remove);
        }
        if (myState.commit(1, 2))
          try {
            myState.waitForValue(0);
          } catch (InterruptedException e) {
            return;
          }
      }
    }

    private List<Integer> collectCurrent(int step, int reminder) {
      List<Integer> current = model.copyCurrent();
      List<Integer> remove = Collections15.arrayList();
      for (int i = 0; i < current.size(); i++) {
        Integer v = current.get(i);
        if (i % step == reminder) remove.add(v);
      }
      return remove;
    }

    public void pauseUpdate() {
      myState.commit(0, 1);
    }

    public void resumeUpdate() {
      try {
        myState.waitForValue(3);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
      myState.commit(3, 0);
    }

    public void waitPaused() {
      try {
        myState.waitForValue(2);
        myState.commit(2, 3);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }

  private static class ImageCollector implements SetHolder.Listener<String> {
    long version;
    private final List<String> myEvents = Collections15.arrayList();
    private final List<String> myImage = Collections15.arrayList();
    private final boolean myCollectEmpty;

    public ImageCollector(boolean collectEmpty) {
      myCollectEmpty = collectEmpty;
      version = 0;
    }

    @Override
    public void onSetChanged(@NotNull SetHolder.Event<String> event) {
      version = event.actualize(version);
      if (myCollectEmpty || !event.isEmpty()) {
        String strEvent = "+" + toText(event.getAdded()) + " -" + toText(event.getRemoved());
        myEvents.add(strEvent);
      }
      myImage.addAll(event.getAdded());
      myImage.removeAll(event.getRemoved());
      CHECK.unordered(event.getSet().copyCurrent(), myImage);
    }

    private String toText(Collection<String> collection) {
      List<String> list = Collections15.arrayList(collection);
      Collections.sort(list);
      return TextUtil.separate(list, " ");
    }

    public void checkEvents(String ... expected) {
      CHECK.order(myEvents, expected);
    }

    public void clearEvents() {
      myEvents.clear();
    }
  }
}
