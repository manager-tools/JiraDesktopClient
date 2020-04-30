package com.almworks.util.threads;

import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;

import java.util.List;

/**
 * @author : Dyoma
 */
public class FlushingQueue <T> {
  private final FlushHandler<T> myHandler;
  private final ThreadGate myThreadGate;
  private final List<T> myQueue = Collections15.arrayList();
  private Job myFlushRequest = null;

  public FlushingQueue(ThreadGate threadGate, FlushHandler<T> handler) {
    myHandler = handler;
    myThreadGate = threadGate;
  }

  public void add(T element) {
    Job flushRequest;
    synchronized (myQueue) {
      myQueue.add(element);
      if (myFlushRequest != null)
        return;
      myFlushRequest = new Job() {
        public void perform() {
          flush();
        }
      };
      flushRequest = myFlushRequest;
    }
    myThreadGate.execute(flushRequest);
  }

  private void flush() {
    List<T> copied;
    synchronized (myQueue) {
      if (myFlushRequest == null)
        return;
      myFlushRequest = null;
      copied = Collections15.arrayList(myQueue);
      myQueue.clear();
    }
    myHandler.handle(copied);
  }

  public static <T> FlushingQueue<T> create(ThreadGate threadGate, FlushHandler<T> handler) {
    return new FlushingQueue<T>(threadGate, handler);
  }

  public interface FlushHandler <T> {
    void handle(List<T> elements);
  }
}
