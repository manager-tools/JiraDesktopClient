package com.almworks.store;

import com.almworks.api.store.StoreFeature;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Apr 29, 2010
 * Time: 9:58:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Storer {
  void store(String prefix, byte[] data, StoreFeature[] features) throws IOException;

  byte[] load(String id, StoreFeature[] features) throws IOException, InterruptedException;

  void clear(String id) throws IOException;

  boolean isSupported(StoreFeature[] features);
}
