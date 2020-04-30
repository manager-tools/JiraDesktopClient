package com.almworks.util.xmlrpc;

import com.almworks.dup.util.ApiLog;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class MessageOutbox {
  private final List<OutgoingMessage> myRequestQueue = new ArrayList<OutgoingMessage>();

  private XmlRpcClient myClient;
  private Thread myThread;
  private boolean myWorking = false;
  private int myPort;

  private String myLastLogMsg = null;
  private int myLastLogMsgCount = 0;

  public MessageOutbox() {
    this(0);
  }

  public MessageOutbox(int port) {
    setPort(port);
  }

  public synchronized void setPort(int port) {
    if (port < 0 || port > 65535)
      throw new IllegalArgumentException(String.valueOf(port));
    if (port != myPort) {
      myPort = port;
      resetClient();
      notify();
    }
  }

  public void enqueue(OutgoingMessage outgoingMessage) {
    synchronized (myRequestQueue) {
      myRequestQueue.add(outgoingMessage);
      myRequestQueue.notifyAll();
    }
  }

  public void clear() {
    synchronized (myRequestQueue) {
      myRequestQueue.clear();
      myRequestQueue.notifyAll();
    }
  }

  public synchronized void start() {
    if (myThread == null) {
      myWorking = true;
      myThread = new Thread(new Runnable() {
        public void run() {
          processRequests();
        }
      }, "RP");
      myThread.setDaemon(true);
      myThread.start();
    }
  }

  public void shutdown() throws InterruptedException {
    Thread thread = null;
    synchronized (this) {
      if (myThread != null) {
        assert myWorking : this;
        myWorking = false;
        thread = myThread;
        myThread = null;
        thread.interrupt();
      }
    }
    if (thread != null)
      thread.join();
  }

  private void processRequests() {
    try {
      while (true) {
        synchronized (this) {
          if (!myWorking)
            return;
          if (myPort == 0) {
            wait(500);
            continue;
          }
        }
        OutgoingMessage outgoingMessage;
        synchronized (myRequestQueue) {
          while (myRequestQueue.size() == 0)
            myRequestQueue.wait(XmlRpcUtils.ROBUST_DELAY);
          outgoingMessage = myRequestQueue.remove(0);
        }
        IOException problem = null;
        for (int i = 0; i < XmlRpcUtils.SEND_ATTEMPTS; i++) {
          XmlRpcClient client = getClient();
          if (client == null) {
            if (problem == null)
              problem = new IOException("null client");
            break;
          }
          try {
            String method = makeMethod(outgoingMessage.getRpcMethod());
            Vector params = makeParameters(outgoingMessage.getRpcParameters());
            logSend(outgoingMessage);
            Object result = client.execute(method, params);
            problem = null;
            outgoingMessage.requestDone(result);
            break;
          } catch (XmlRpcException e) {
            ApiLog.debug("** -- send error", e);
            problem = null;
            outgoingMessage.requestFailed(e);
            break;
          } catch (IOException e) {
            ApiLog.debug("** -- send i/o failure", e);
            problem = e;
            resetClient();
            if (i < XmlRpcUtils.SEND_ATTEMPTS - 1)
              waitSomeTime();
          }
        }
        if (problem != null)
          outgoingMessage.requestFailed(problem);
      }
    } catch (InterruptedException e) {
      synchronized (this) {
        if (!myWorking)
          return;
        // something had caused interruption? reincarnate
        assert myThread != null;
        myThread = null;
        start();
      }
    }
  }

  private void logSend(OutgoingMessage outgoingMessage) {
    if (!ApiLog.isLogging())
      return;
    String msg = String.valueOf(outgoingMessage);
    if (msg.length() > 120)
      msg = msg.substring(0, 119) + "\u2026";
    msg = msg.replace('\n', ' ');
    if (msg.equals(myLastLogMsg)) {
      myLastLogMsgCount++;
    } else {
      if (myLastLogMsgCount > 0) {
        ApiLog.debug("** last send message repeated " + myLastLogMsgCount + " times");
        myLastLogMsgCount = 0;
      }
      myLastLogMsg = msg;
      ApiLog.debug("** send: " + msg);
    }
  }

  private void waitSomeTime() throws InterruptedException {
    Thread.sleep(500);
  }

  private String makeMethod(String rpcMethod) {
    return XmlRpcUtils.SERVICE_NAME + "." + rpcMethod;
  }

  private synchronized void resetClient() {
    myClient = null;
  }

  private synchronized XmlRpcClient getClient() {
    if (myClient == null) {
      int port = myPort;
      if (port == 0)
        return null;
      try {
        ApiLog.debug("** will send to port " + port);
        myClient = new XmlRpcClient(XmlRpcUtils.LOCAL_HOST, port);
      } catch (MalformedURLException e) {
        return null;
      }
      myClient.setMaxThreads(1);
    }
    return myClient;
  }

  private Vector makeParameters(Collection<?> parameters) {
    if (parameters == null || parameters.size() == 0)
      return XmlRpcUtils.EMPTY_PARAMETERS;
    if (parameters instanceof Vector)
      return (Vector) parameters;
    Vector vector = new Vector();
    for (Object p : parameters) {
      vector.add(p);
    }
    return vector;
  }

  /**
   * for tests
   */
  Thread getThread() {
    return myThread;
  }

  public int getPort() {
    return myPort;
  }
}
