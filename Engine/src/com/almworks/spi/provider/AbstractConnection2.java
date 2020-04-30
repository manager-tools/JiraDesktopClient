package com.almworks.spi.provider;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.engine.ItemProvider;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import util.concurrent.SynchronizedBoolean;

public abstract class AbstractConnection2<P extends ItemProvider> extends AbstractConnection<P, MockContext> {
  protected AbstractConnection2(P provider, MutableComponentContainer container, String connectionID) {
    super(provider, container, connectionID);
    MockContext context = new MockContext(this);
    myContainer.registerActor(MockContext.ROLE, context);
    initContext(context);
  }

  protected void startContext() {}

  public abstract void doStop();

  public ComponentContainer getContainer() {
    return myContainer;
  }

  @Override
  public P getProvider() {
    return (P) super.getProvider();
  }

  @Override
  protected final Boolean doInitDB() throws InterruptedException {
    final FireEventSupport<Procedure<Boolean>> finishListeners = (FireEventSupport) FireEventSupport.create(Procedure.class);
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    final DBResult<Object> result = getSyncManager().writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        getContext().loadInitState(drain.getReader());
        doInitDB(drain, finishListeners);
      }

      @Override
      public void onFinished(DBResult<?> result) {
        finishListeners.getDispatcher().invoke(result.isSuccessful());
        LogHelper.assertError(getConnectionItem() > 0, "Connection not initialized", AbstractConnection2.this);
        done.set(true);
      }
    });
    done.waitForValue(true);
    return result.isSuccessful();
  }

  protected abstract void doInitDB(DBDrain drain, FireEventSupport<Procedure<Boolean>> finishListeners);
}
