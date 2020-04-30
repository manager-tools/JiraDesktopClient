package com.almworks.items.impl.sqlite;

import com.almworks.util.collections.CollectionRemove;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import org.almworks.util.detach.Lifespan;

import java.util.concurrent.CopyOnWriteArrayList;

class QueryProcessorImpl implements QueryProcessor, TransactionObserver {
  private final CopyOnWriteArrayList<Client> myClients = new CopyOnWriteArrayList<Client>();
  private final DatabaseQueue myQueue;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public QueryProcessorImpl(DatabaseQueue queue) {
    myQueue = queue;
  }

  public void attach(Lifespan life, Client client) {
    if (life.isEnded())
      return;
    myClients.add(client);
    life.add(CollectionRemove.create(myClients, client));
  }

  public void processClient(Client client) {
    DatabaseJob job = client.createJob();
    enqueueJob(job);
  }

  public void enqueueJob(DatabaseJob job) {
    if (job != null)
      myQueue.execute(job);
  }

  public void process() {
    myModifiable.fireChanged();
    if (myClients.isEmpty())
      return;
    for (Client client : myClients) {
      processClient(client);
    }
  }

  public void notifyTransaction(long icn) {
    process();
  }

  public Modifiable getTransactionStartEvent() {
    return myModifiable;
  }
}
