package com.almworks.items.impl.sqlite;

import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

public interface QueryProcessor {
  void attach(Lifespan life, Client client);

  void process();

  void processClient(Client client);

  interface Client {
    @Nullable
    DatabaseJob createJob();
  }
}
