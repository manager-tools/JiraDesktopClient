package com.almworks.util.progress;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class Progress implements ProgressSource, ChangeListener {
  private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("0.00%");
  private static final double EPS = 1e-6;
  private static final double DONT_CHANGE_PROGRESS = -1D;
  private static final double NO_CACHED_VALUE = -1D;
  private static final double DELEGATE_ALL = -1D;
  static final Object DONT_CHANGE_ACTIVITY = Progress.class;

  /**
   * Progress may be in two states:
   * "started" - myDone is false, myCompleted has meaningful value, there may be delegates.
   * "done" - myDone is true, progress is 1D, no delegates.
   */
  private boolean myDone;

  /**
   * modifiable is initialized upon request
   */
  private SimpleModifiable myModifiable = null;

  /**
   * Maximum progress which may be set to this object. Initially equal to 1D, decreased by each sub.
   */
  private double myTotal = 1D;

  /**
   * Completed progress, from 0 to myTotal.
   */
  private double myComplete = 0D;

  /**
   * Sub-progresses are delegated parts of this progress. Span is the maximum progress the delegate contributes
   * to this progress.
   */
  private ProgressSource[] myDelegates = null;
  private double[] myDelegateSpans = null;
  private int myDelegateCount = 0;

  /**
   * Any object may be current activity. Activities from delegates are built into a tree when
   * getActivity() is called.
   */
  private Object myActivity = null;

  /**
   * Currently accumulated errors.
   */
  private List<String> myErrors;

  private double myProgressCache = NO_CACHED_VALUE;

  /**
   * For debugging and readability
   */
  @NotNull
  private final String myName;

  /**
   * Counts how many times the progress has been started
   */
  private int myCount;

  public Progress() {
    this(true, "", null);
  }

  public Progress(@NotNull String name) {
    this(true, name, null);
  }

  public Progress(@NotNull String name, @Nullable Object initialActivity) {
    this(true, name, initialActivity);
  }

  public static Progress delegator() {
    return new Progress(false, "", null);
  }

  public static Progress delegator(@NotNull String name) {
    return new Progress(false, name, null);
  }
  
  private Progress(boolean started, @NotNull String name, @Nullable Object initialActivity) {
    myName = name;
    myActivity = initialActivity;
    myDone = !started;
  }

  public synchronized boolean isDone() {
    return myDone;
  }

  @Nullable
  public synchronized List<String> getErrors(List<String> target) {
    if (myErrors != null && myErrors.size() > 0) {
      if (target == null) {
        target = Collections15.arrayList(1);
      }
      target.addAll(myErrors);
    }
    for (int i = 0; i < myDelegateCount; i++) {
      target = myDelegates[i].getErrors(target);
    }
    return target;
  }

  @NotNull
  public synchronized ProgressData getProgressData() {
    if (isDone() && myErrors == null)
      return ProgressData.DONE;
    else
      return new ProgressData(getProgress(), getActivity(), getErrors(null), isDone());
  }

  public synchronized double getProgress() {
    if (myDone)
      return 1D;
    if (myProgressCache == NO_CACHED_VALUE)
      myProgressCache = calculateComplete();
    return myProgressCache;
  }

  @Nullable
  public synchronized ProgressActivity getActivity() {
    ProgressActivity subactivitiesHead = null;
    for (int i = myDelegateCount - 1; i >= 0; i--) {
      ProgressActivity activity = myDelegates[i].getActivity();
      if (activity != null) {
        if (subactivitiesHead != null) {
          activity.setNext(subactivitiesHead);
        }
        subactivitiesHead = activity;
      }
    }
    if (myActivity != null || subactivitiesHead != null) {
      return new ProgressActivity(myActivity, subactivitiesHead);
    } else {
      return null;
    }
  }

  private double calculateComplete() {
    double complete = myComplete;
    for (int i = 0; i < myDelegateCount; i++) {
      double delegateProgress = myDelegates[i].getProgress();
      if (delegateProgress < 0D || delegateProgress > 1D + EPS) {
        assert false : myDelegates[i] + " " + delegateProgress;
        continue;
      }
      complete += delegateProgress * myDelegateSpans[i];
    }
    return Math.min(1D, Math.max(0D, complete));
  }

  @NotNull
  public synchronized RemoveableModifiable getModifiable() {
    if (myModifiable == null)
      myModifiable = new SimpleModifiable();
    return myModifiable;
  }

  public void setProgress(double progress, @Nullable Object activity) {
    announce(update(progress, activity));
  }

  protected synchronized boolean update(double progress, Object activity) {
    start();
    boolean changed = false;
    if (progress != DONT_CHANGE_PROGRESS) {
      double old = myComplete;
      myComplete = Math.min(Math.max(progress, 0D), myTotal);
      changed = Math.abs(old - myComplete) > EPS;
      if (changed)
        myProgressCache = NO_CACHED_VALUE;
    }
    if (activity != DONT_CHANGE_ACTIVITY) {
      if (!changed)
        changed = !Util.equals(myActivity, activity);
      myActivity = activity;
    }
    maybeDone();
    changed = changed | myDone;
    return changed;
  }

  private void maybeDone() {
    assert Thread.holdsLock(this) : this;
    if (getProgress() >= 1D - EPS)
      done();
  }

  private void done() {
    assert Thread.holdsLock(this);
    if (myDelegateCount > 0) {
      // keep errors
      List<String> allErrors = getErrors(null);
      if (allErrors != null && (myErrors == null || myErrors.size() != allErrors.size())) {
        myErrors = allErrors;
      }
    }
    myDone = true;
    for (int i = 0; i < myDelegateCount; i++) {
      myDelegates[i].getModifiable().removeChangeListener(this);
    }
    myDelegateCount = 0;
    myDelegates = null;
    myDelegateSpans = null;
    myTotal = 1D;
    myComplete = 0D;
    myActivity = null;
    myProgressCache = NO_CACHED_VALUE;
  }

  public void setProgress(double progress) {
    announce(update(progress, DONT_CHANGE_ACTIVITY));
  }

  public void setActivity(Object activity) {
    announce(update(DONT_CHANGE_PROGRESS, activity));
  }

  public void setDone() {
    try {
      boolean fire;
      synchronized (this) {
        fire = !myDone;
        done();
      }
      announce(fire);
    } catch (Exception e) {
      Log.error(e);
    }
  }

  public void setStarted() {
    announce(start());
  }

  private synchronized boolean start() {
    boolean changed = myDone;
    myDone = false;
    if (changed) {
      myErrors = null;
      myCount++;
    }
    return changed;
  }

  public Progress createDelegate() {
    return createDelegate(DELEGATE_ALL, "", null);
  }

  public Progress createDelegate(String debugName) {
    return createDelegate(DELEGATE_ALL, debugName, null);
  }

  public Progress createDelegate(double progressSpan, String debugName) {
    return createDelegate(progressSpan, debugName, null);
  }

  public Progress createDelegate(double progressSpan) {
    return createDelegate(progressSpan, "", null);
  }

  public Progress createDelegate(double progressSpan, String debugName, Object initialActivity) {
    Progress result = new Progress(debugName, initialActivity);
    delegate(result, progressSpan);
    return result;
  }

  public void delegate(ProgressSource source, double span) {
    addDelegate(source, span);
    announce(true);
  }

  public void delegate(ProgressSource source) {
    delegate(source, DELEGATE_ALL);
  }

  protected synchronized void addDelegate(ProgressSource source, double span) {
    if (span == DELEGATE_ALL) {
      span = myTotal;
    }
    if (span > myTotal + EPS) {
      assert false : this + " " + source + " " + span;
      Log.warn("cannot delegate more than remains (" + this + " " + source + " " + span + ")");
      return;
    } else if (span < EPS) {
      return;
    }
    start();
    if (myDelegates == null || myDelegateCount == myDelegates.length) {
      int newCount = myDelegateCount * 2 + 1;
      ProgressSource[] newDelegates = new ProgressSource[newCount];
      double[] newSpans = new double[newCount];
      if (myDelegates != null) {
        System.arraycopy(myDelegates, 0, newDelegates, 0, myDelegateCount);
        System.arraycopy(myDelegateSpans, 0, newSpans, 0, myDelegateCount);
      }
      myDelegates = newDelegates;
      myDelegateSpans = newSpans;
    }
    int index = myDelegateCount++;
    myDelegates[index] = source;
    myDelegateSpans[index] = span;
    myTotal -= span;
    myProgressCache = NO_CACHED_VALUE;
    maybeDone();
    if (!myDone) {
      source.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, this);
    }
  }

  private void announce(boolean changed) {
    assert !Thread.holdsLock(this) : "mustn't hold lock when firing event: " + this;
    if (!changed)
      return;
    SimpleModifiable modifiable;
    synchronized (this) {
      modifiable = myModifiable;
    }
    if (modifiable != null)
      modifiable.fireChanged();
    if (isDone())
      dispose();
  }

  protected synchronized void dispose() {
    for (int i = 0; i < myDelegateCount; i++) {
      myDelegates[i].getModifiable().removeChangeListener(this);
    }
  }

  public void onChange() {
    synchronized(this) {
      myProgressCache = NO_CACHED_VALUE;
      maybeDone();
    }
    announce(true);
  }

  public synchronized String toString() {
    StringBuffer result = new StringBuffer();
    if (myName.length() > 0)
      result.append(myName).append(' ');
    if (myDone) {
      result.append("done");
    } else {
      result.append(PERCENT_FORMAT.format(getProgress()));
      if (myTotal != 1D) {
        result.append(" (own ").append(PERCENT_FORMAT.format(myComplete));
        result.append(" total ").append(PERCENT_FORMAT.format(myTotal)).append(')');
      }
      int progressOffset = result.length();
      ProgressActivityFormat.DEFAULT.format(getActivity(), result);
      if (result.length() > progressOffset)
        result.insert(progressOffset, "; ");
    }
    if (myErrors != null && myErrors.size() > 0) {
      result.append("; ").append(myErrors.size()).append(" errors");
    }
    return result.toString();
  }

  public void addError(String error) {
    synchronized (this) {
      if (myErrors == null)
        myErrors = Collections15.arrayList();
      myErrors.add(error);
    }
    announce(true);
  }

  public synchronized double getOwnSpan() {
    return myTotal;
  }

  /**
   * Makes this progress switch to "done" state when lifespan ends. Does nothing if the progress has not been started.
   */
  public synchronized void setLifespan(Lifespan lifespan) {
    if (myDone || lifespan.isEnded())
      return;
    final int count = myCount;
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        boolean done;
        synchronized(Progress.this) {
          done = count == myCount;
        }
        if (done) {
          setDone();
        }
      }
    });
  }

  /**
   * @return debug name
   * */
  @NotNull
  public String getName() {
    return myName;
  }

  public static class Deaf extends Progress {
    @Override
    public void setProgress(double progress, Object activity) {}
    @Override
    public void setActivity(Object activity) {}
  }
}
