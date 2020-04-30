package com.almworks.tracker.alpha;

import com.almworks.dup.util.ApiLog;
import com.almworks.dup.util.SimpleValueModel;
import com.almworks.tracker.eapi.alpha.*;
import com.almworks.util.xmlrpc.CloningIncomingMessage;
import com.almworks.util.xmlrpc.EndPoint;
import com.almworks.util.xmlrpc.MessageProcessingException;
import com.almworks.util.xmlrpc.SimpleOutgoingMessage;
import org.almworks.util.detach.Lifecycle;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.Vector;

import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToClient;
import static com.almworks.tracker.alpha.AlphaProtocol.Messages.ToTracker;

/**
 * AlphaConnector is the implementation of the client part of
 * Deskzilla external API, version Alpha.
 */
public class AlphaConnector implements TrackerConnector, ArtifactOpener {
  private final EndPoint myEndPoint;
  private final Object myLock = new Object();
  private final SimpleValueModel<TrackerConnectionStatus> myConnectionStatus;
  private final TrackerPinger myPinger;
  private final ArtifactLoaderImpl myArtifactLoader;
  private final TrackerStarterImpl myTrackerStarter;
  private final CollectionsManager myCollectionsManager;
  private final FindArtifactManagerImpl myFindArtifactManager;

  private boolean myStarted;
  private final Lifecycle myLife = new Lifecycle();

  public AlphaConnector() {
    myConnectionStatus = SimpleValueModel.create(TrackerConnectionStatus.NOT_CONNECTED);
    myEndPoint = new EndPoint();
    myPinger = new TrackerPinger(myEndPoint, myConnectionStatus);
    myArtifactLoader = new ArtifactLoaderImpl(myEndPoint, myConnectionStatus);
    myTrackerStarter = new TrackerStarterImpl(myEndPoint, myPinger, myConnectionStatus);
    myCollectionsManager = new CollectionsManager(myEndPoint, myConnectionStatus);
    myFindArtifactManager = new FindArtifactManagerImpl(myEndPoint);
  }

  public TrackerStarter getTrackerStarter() {
    return myTrackerStarter;
  }

  public ArtifactLoader getArtifactLoader() {
    return myArtifactLoader;
  }

  public ArtifactOpener getArtifactOpener() {
    return this;
  }

  public ArtifactCollectionConfigurationManager getArtifactCollectionConfigurationManager() {
    return myCollectionsManager;
  }

  public void configureLogging(File logDirectory) {
    ApiLog.configureLogging(logDirectory);
  }

  public ArtifactCollectionMonitor getArtifactCollectionMonitor() {
    return myCollectionsManager;
  }

  public FindArtifactManager getFindArtifactManager() {
    return myFindArtifactManager;
  }

  public <T> void setProperty(ConnectorProperty<T> property, T value) {
    if (property == ConnectorProperty.NAME) {
      myPinger.setConnectorName((String)value);
    } else if (property == ConnectorProperty.SHORT_NAME) {
      myPinger.setConnectorShortName((String)value);
    } else {
      assert false : property;
    }
  }

  public void start() {
    ApiLog.debug(this + " is starting");
    AlphaImplUtils.assertAWT();
    myLife.cycle();
    myConnectionStatus.setValue(TrackerConnectionStatus.NOT_CONNECTED);
    synchronized(myLock) {
      if (myStarted)
        return;
      myStarted = true;
    }
    startCompatibilityTest();
    new Thread(this + " starter") {
      public void run() {
        myEndPoint.start();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            myArtifactLoader.start();
            myCollectionsManager.start();
            myPinger.start();
            myTrackerStarter.start();
            ApiLog.debug(AlphaConnector.this + " has started");
          }
        });
      }
    }.start();
  }

  private void startCompatibilityTest() {
    myEndPoint.addIncomingMessageFactory(new CloningIncomingMessage(ToClient.SUPPORTED_PROTOCOLS) {
      protected void process() throws MessageProcessingException {
        Vector protocols = getParameters();
        for (Object protocol : protocols) {
          assert protocol instanceof String;
          if (AlphaProtocol.Protocols.ALPHA.equals(protocol)) {
            ApiLog.debug("confirmed protocol " + AlphaProtocol.Protocols.ALPHA);
            return;
          }
        }
        // what to do??
        ApiLog.warn("incompatible api protocol (" + protocols + ")");
      }
    });
    new ConnectionStatusWatcher(myConnectionStatus) {
      protected void onDisconnect() {
        ApiLog.debug("tracker disconnected");
      }

      protected void onConnect(int connectionId) {
        ApiLog.debug("tracker connected: " + myConnectionStatus.getValue());
        SimpleOutgoingMessage message = new SimpleOutgoingMessage(ToTracker.GET_SUPPORTED_PROTOCOLS, port());
        myEndPoint.getOutbox().enqueue(message);
      }
    }.start(myLife.lifespan());
  }

  private int port() {
    return myEndPoint.getInboxPort();
  }

  public void stop() {
    AlphaImplUtils.assertAWT();
    ApiLog.debug("stopping " + this);
    myLife.cycle();
    myTrackerStarter.stop();
    myPinger.stop();
    synchronized (myLock) {
      if (!myStarted)
        return;
    }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        myArtifactLoader.shutdown();
        myCollectionsManager.shutdown();
      }
    });
    new Thread("shutdown " + this) {
      public void run() {
        try {
          myEndPoint.shutdown();
        } catch (InterruptedException e) {
          // ignore
        }
        synchronized (myLock) {
          myStarted = false;
        }
      }
    }.start();
    myConnectionStatus.setValue(new TrackerConnectionStatus(ConnectionState.CONNECTION_CLOSED));
  }

  public void openArtifacts(Collection<String> urls) {
    if (urls == null || urls.size() == 0)
      return;
    myEndPoint.getOutbox().enqueue(new OMOpenArtifacts(urls, port()));
  }


  public String toString() {
    return "alpha-connector";
  }
}
