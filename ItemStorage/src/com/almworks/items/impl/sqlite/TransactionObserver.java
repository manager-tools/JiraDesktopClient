package com.almworks.items.impl.sqlite;

public interface TransactionObserver {
  void notifyTransaction(long icn);
}
