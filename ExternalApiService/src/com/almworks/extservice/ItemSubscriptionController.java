package com.almworks.extservice;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.Database;
import com.almworks.util.config.Configuration;
import com.almworks.util.xmlrpc.OutgoingMessage;

import java.util.Map;

interface ItemSubscriptionController {
  Engine getEngine();

  Map<Configuration, Connection> getCreatingConnectionsLockMap();

  void send(OutgoingMessage message);

  Database getDatabase();
}
