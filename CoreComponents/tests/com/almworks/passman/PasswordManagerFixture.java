package com.almworks.passman;

import com.almworks.api.store.StoreFeature;
import com.almworks.store.StoreImpl;
import com.almworks.store.StorerFixture;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.InstanceProvider;
import com.almworks.util.exec.LongEventQueue;
import com.almworks.util.exec.LongEventQueueImpl;

public abstract class PasswordManagerFixture extends StorerFixture {
  protected PasswordManagerImpl myPassman;
  private static final String STORE_KEY = "storeKey";
  protected StoreImpl myStore;

  protected void setUp() throws Exception {
    super.setUp();
    Context.add(InstanceProvider.instance(new LongEventQueueImpl()), "");
    myStore = new StoreImpl(myFile, STORE_KEY, StoreFeature.SECURE_STORE);
    myPassman = new PasswordManagerImpl(myStore);
    myPassman.start();
  }

  protected void tearDown() throws Exception {
    myPassman.stop();
    LongEventQueue.instance().shutdownGracefully();
    Context.pop();
    myStore = null;
    myPassman = null;
    super.tearDown();
  }
}
