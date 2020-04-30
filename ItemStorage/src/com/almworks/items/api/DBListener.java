package com.almworks.items.api;

public interface DBListener {
  void onDatabaseChanged(DBEvent event, DBReader reader);

  DBListener DEAF = new Deaf();

  class Deaf implements DBListener {
    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {}
  }
}
