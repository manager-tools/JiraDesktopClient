package com.almworks.tracker.alpha;

import com.almworks.dup.util.SimpleValueModel;
import com.almworks.tracker.eapi.alpha.ConnectionState;
import com.almworks.tracker.eapi.alpha.TrackerConnectionStatus;
import com.almworks.util.xmlrpc.*;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

class TrackerPinger {
  private static final String DEFAULT_CONNECTOR_NAME = "External Client #" + Integer.toHexString(((int)System.currentTimeMillis() + new Object().hashCode()));
  private static final String DEFAULT_CONNECTOR_SHORT_NAME = "External Client";

  private final EndPoint myEndPoint;
  private final SimpleValueModel<TrackerConnectionStatus> myConnectionStatus;
  private final Timer myTimer;

  private int myLastConnectionId = 0;
  private String myConnectorName = DEFAULT_CONNECTOR_NAME;
  private String myConnectorShortName = DEFAULT_CONNECTOR_SHORT_NAME;

  public TrackerPinger(EndPoint endPoint, SimpleValueModel<TrackerConnectionStatus> connectionStatus) {
    myEndPoint = endPoint;
    myConnectionStatus = connectionStatus;
    myEndPoint.addIncomingMessageFactory(new IncomingMessageFactory() {
      public String getRpcMethodName() {
        return AlphaProtocol.Messages.ToClient.PING;
      }

      public IncomingMessage createMessage(Vector parameters) throws Exception {
        return new IncomingPing(parameters);
      }
    });
    myTimer = new Timer(5000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sendPing();
      }
    });
  }

  private void sendPing() {
    int port = myEndPoint.getInboxPort();
    if (port <= 0) {
      // not listening, skip ping
      return;
    }
    myEndPoint.getOutbox().enqueue(new OutgoingPing(port, myLastConnectionId, myConnectorName, myConnectorShortName));
  }

  public void setConnectorName(String name) {
    myConnectorName = name == null ? DEFAULT_CONNECTOR_NAME : name;
  }

  public void setConnectorShortName(String name) {
    myConnectorShortName = name == null ? DEFAULT_CONNECTOR_SHORT_NAME : name;
  }

  public void start() {
    myTimer.setInitialDelay(100);
    myTimer.start();
  }

  public void stop() {
    myTimer.stop();
  }

  private void changeConnectionId(int connectionId) {
    if (connectionId != myLastConnectionId) {
      myLastConnectionId = connectionId;
    }
  }

  public void pingNow() {
    sendPing();
  }

  public void forceDisconnect() {
    AlphaImplUtils.inAWT(new Runnable() {
      public void run() {
        myConnectionStatus.setValue(new TrackerConnectionStatus(ConnectionState.CONNECTION_CLOSED));
      }
    });
  }

  private class OutgoingPing extends OutgoingMessage {
    private final List<Object> myParameters;

    public OutgoingPing(int port, int connectionId, String name, String shortName) {
      myParameters = Arrays.asList(new Object[] {port, connectionId, name, shortName});
    }

    protected String getRpcMethod() {
      return AlphaProtocol.Messages.ToTracker.PING;
    }

    protected Collection<?> getRpcParameters() {
      return myParameters;
    }

    protected void requestFailed(Exception problem) {
      AlphaImplUtils.inAWT(new Runnable() {
        public void run() {
          myConnectionStatus.setValue(new TrackerConnectionStatus(ConnectionState.CONNECTION_FAILED));
        }
      });
    }
  }


  private class IncomingPing extends IncomingMessage {
    private final Map<String, String> myMap;
    private final int myConnectionId;

    public IncomingPing(Vector requestParameters) {
      Hashtable hashtable = (Hashtable) requestParameters.get(0);
      myMap = new HashMap<String, String>(hashtable);
      myConnectionId = (Integer) requestParameters.get(1);
    }

    protected void process() throws MessageProcessingException {
      AlphaImplUtils.inAWT(new Runnable() {
        public void run() {
          String name = myMap.get(AlphaProtocol.Messages.ToClient.Ping.TRACKER_NAME);
          String version = myMap.get(AlphaProtocol.Messages.ToClient.Ping.TRACKER_VERSION);
          String workspace = myMap.get(AlphaProtocol.Messages.ToClient.Ping.TRACKER_WORKSPACE);
          myConnectionStatus.setValue(
            new TrackerConnectionStatus(ConnectionState.CONNECTED, name, version, workspace, myConnectionId));
          changeConnectionId(myConnectionId);
        }
      });
    }
  }
}
