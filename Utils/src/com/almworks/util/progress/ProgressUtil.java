package com.almworks.util.progress;

import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public class ProgressUtil {
  public static Pair<Progress, Progress> splitProgress(Progress progress, String name1, double part1, String name2, double part2) {
    double total = part1 + part2;
    double span1 = part1 / total;
    double span2 = part2 / total;
    Progress progress1 = new Progress(name1);
    Progress progress2 = new Progress(name2);
    progress.delegate(progress1, span1);
    progress.delegate(progress2, span2);
    return Pair.create(progress1, progress2);
  }

  public static Pair<Progress, Progress> chipOff(Progress whole, String chippedName, double chippedPart) {
    return splitProgress(whole, chippedName, chippedPart, whole.getName(), 1D - chippedPart);
  }

  public static Progress unitProgress(Progress progress1, double part1, Progress progress2, double part2) {
    double total = part1 + part2;
    double span1 = part1 / total;
    double span2 = part2 / total;
    Progress progress = new Progress();
    progress.delegate(progress1, span1);
    progress.delegate(progress2, span2);
    return progress;
  }

  public static Progress setupLoggingProgress(Lifespan life, final Progress p, final Level logLevel) {
    p.getModifiable().addChangeListener(life, ThreadGate.STRAIGHT, new ChangeListener() {
      @Override
      public void onChange() {
        String activity = String.valueOf(Util.NN(p.getActivity(), ""));
        if (!activity.isEmpty())
          LogHelper.log(logLevel, activity);
      }
    });
    return p;
  }
  
  public static void waitForProgress(Lifespan life, final Progress p) throws InterruptedException {
    final CountDownLatch cdl = new CountDownLatch(1);
    p.getModifiable().addChangeListener(life, ThreadGate.STRAIGHT, new ChangeListener() {
      @Override
      public void onChange() {
        if (p.isDone()) cdl.countDown();
      }
    });
    if (!p.isDone()) cdl.await();
  }

  public static Progress[] equalSlices(Progress progress, int count) {
    if (count <= 1) {
      LogHelper.assertError(count == 1, "Positive count expected", count, progress);
      return new Progress[]{progress.createDelegate()};
    }
    Progress[] array = new Progress[count];
    float span = 1F / count;
    for (int i = 0; i < count; i++) array[i] = progress.createDelegate(span);
    return array;
  }
}
