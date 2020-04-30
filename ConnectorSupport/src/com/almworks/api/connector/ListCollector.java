package com.almworks.api.connector;



import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;

public class ListCollector<T> implements CollectionSink<T> {
  private List<T> myList = null;
  private boolean myFinished = false;

  public synchronized void pushStarted() throws ConnectorException {
    assert myList == null : this;
    assert !myFinished : this;
    myList = Collections15.arrayList();
  }

  public synchronized void pushFinished() throws ConnectorException {
    assert myList != null : this;
    myFinished = true;
    myList = Collections.unmodifiableList(myList);
  }

  public synchronized void push(T element) throws ConnectorException {
    assert myList != null : this;
    assert !myFinished : this;
    myList.add(element);
  }

  public synchronized List<T> getResult() {
    assert myFinished : this;
    return myList;
  }
}
