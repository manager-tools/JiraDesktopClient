package com.almworks.util.xmlrpc;

import com.almworks.dup.util.ApiLog;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcHandler;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MessageInbox {
  private final Map<String, IncomingMessageFactory> myFactories = new HashMap<String, IncomingMessageFactory>();

  private InboxServer myServer;
  private int myPort = 0;

  private String myLastLogMsg = null;
  private int myLastLogMsgCount = 0;

  public MessageInbox(int port) {
    setPort(port);
  }

  public MessageInbox() {
    this(0);
  }

  public void setPort(int port) {
    if (port != myPort) {
      synchronized (this) {
        boolean started = myServer != null;
        shutdown();
        myPort = port;
        if (started) {
          start();
        }
      }
    }
  }

  public synchronized int getActivePort() {
    InboxServer server = myServer;
    return server == null ? 0 : server.getActivePort();
  }

  public synchronized void addFactory(IncomingMessageFactory factory) {
    String name = factory.getRpcMethodName();
    IncomingMessageFactory pushed = myFactories.put(name, factory);
    if (pushed != null) {
      ApiLog.error("two factories for " + name + ": " + pushed + ", " + factory);
    }
  }

  public synchronized <T extends IncomingMessage> void addFactory(Class<T> clazz) {
    addFactory(new IntrospectionFactory(clazz));
  }

  public synchronized void start() {
    if (myServer == null) {
      InetAddress address;
      try {
        address = InetAddress.getByName(XmlRpcUtils.LOCAL_HOST);
      } catch (UnknownHostException e) {
        ApiLog.error("bad host " + XmlRpcUtils.LOCAL_HOST, e);
        return;
      }
      myServer = new InboxServer(MessageInbox.this.myPort, address);
      myServer.addHandler(XmlRpcUtils.SERVICE_NAME, new XmlRpcHandler() {
        public Object execute(String method, Vector params) throws Exception {
          Object result = processRequest(method, params);
          if (result != XmlRpcUtils.RESPONSE_OK) {
            ApiLog.debug("** -- recv error: " + result);
          }
          return result;
        }
      });
      myServer.setParanoid(true);
      myServer.acceptClient(XmlRpcUtils.LOCAL_HOST);
      try {
        myServer.start();
      } catch (Exception e) {
        ApiLog.warn("cannot setup service on port " + myPort, e);
        try {
          myServer.shutdown();
        } catch (Exception ee) {
          // ignore
        }
        myServer = null;
      }
    }
  }

  public void shutdown() {
    WebServer server = null;
    synchronized (this) {
      if (myServer != null) {
        server = myServer;
        myServer = null;
      }
    }
    if (server != null)
      server.shutdown();
  }

  private synchronized Object processRequest(String method, Vector params) {
    if (method == null)
      return XmlRpcUtils.RESPONSE_UNKNOWN_METHOD;
    method = extractName(method);
    if (method == null)
      return XmlRpcUtils.RESPONSE_UNKNOWN_METHOD;
    logRecv(method, params);
    IncomingMessageFactory factory = myFactories.get(method);
    if (factory == null) {
      return XmlRpcUtils.RESPONSE_UNKNOWN_METHOD;
    }
    IncomingMessage message;
    try {
      message = factory.createMessage(params);
      if (message == null)
        return XmlRpcUtils.RESPONSE_MALFORMED_MESSAGE + ":null message";
    } catch (Exception e) {
      ApiLog.debug("** -- recv cannot create message", e);
      return XmlRpcUtils.RESPONSE_MALFORMED_MESSAGE + ":" + e;
    }
    try {
      message.process();
    } catch (MessageProcessingException e) {
      ApiLog.debug("** -- recv message processing exception", e);
      return XmlRpcUtils.RESPONSE_MESSAGE_PROCESSING_EXCEPTION + ":" + e;
    }
    return XmlRpcUtils.RESPONSE_OK;
  }

  private void logRecv(String method, Vector params) {
    if (!ApiLog.isLogging())
      return;
    String msg = method + XmlRpcUtils.dumpCollection(new StringBuffer(), params);
    if (msg.length() > 120)
      msg = msg.substring(0, 119) + "\u2026";
    msg = msg.replace('\n', ' ');
    if (msg.equals(myLastLogMsg)) {
      myLastLogMsgCount++;
    } else {
      if (myLastLogMsgCount > 0) {
        ApiLog.debug("** last recv message repeated " + myLastLogMsgCount + " times");
        myLastLogMsgCount = 0;
      }
      myLastLogMsg = msg;
      ApiLog.debug("** recv: " + msg);
    }
  }

  private String extractName(String method) {
    if (!method.startsWith(XmlRpcUtils.SERVICE_NAME))
      return null;
    int len = XmlRpcUtils.SERVICE_NAME.length();
    if (method.length() == len || method.charAt(len) != '.')
      return null;
    method = method.substring(len + 1);
    if (method.length() == 0)
      return null;
    return method;
  }

  public int getPort() {
    return myPort;
  }

  public synchronized void reset() {
    boolean started = myServer != null;
    shutdown();
    if (started) {
      start();
    }
  }

  private class InboxServer extends WebServer {
    public InboxServer(int port, InetAddress address) {
      super(port, address);
    }

    /**
     * Returns a port this server is listening on, or 0.
     */
    public synchronized int getActivePort() {
      ServerSocket socket = serverSocket;
      if (socket == null)
        return 0;
      int localPort = socket.getLocalPort();
      return localPort < 0 ? 0 : localPort;
    }
  }
}
