package com.almworks.util.net;

import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;
import java.net.*;
import java.util.Collection;

class MulticastReceiver implements NetworkReceiver {
  private static final long RECREATE_SOCKET_PAUSE = 60000;
  private static final int SOCKET_TIMEOUT = 30000;
  private static final long SOCKET_TTL = Const.HOUR;
  private static final int UDP_LIMIT = 65500;

  private final Object myLock = new Object();
  private final Collection<InetAddress> myAddresses;
  private final int myPort;
  private final SynchronizedBoolean myStarted = new SynchronizedBoolean(false, myLock);

  private Thread myThread;

  private int myThreadIncarnation = 0;
  private int mySocketIncarnation = 0;
  private int myBufferSize = UDP_LIMIT;
  private long mySequence = 0;

  private Procedure<Pair<byte[], MessageTransportData>> mySink;

  public MulticastReceiver(Collection<InetAddress> addresses, int port) {
    myAddresses = Collections15.linkedHashSet(addresses);
    myPort = port;
  }

  public void start() {
    synchronized (myLock) {
      assert mySink != null;
      if (!myStarted.commit(false, true))
        return;
      recreateThread();
    }
  }

  private void recreateThread() {
    assert Thread.holdsLock(myLock) : Thread.currentThread();
    assert myStarted.get() : this;
    myThread = new Thread("sds-reader#" + (++myThreadIncarnation)) {
      public void run() {
        runReading();
      }
    };
    myThread.setDaemon(true);
    myThread.start();
  }

  public void stop() {
    synchronized (myLock) {
      if (!myStarted.commit(true, false))
        return;
      Thread thread = myThread;
      if (thread != null)
        thread.interrupt();
    }
  }

  private void runReading() {
    try {
      while (myStarted.get()) {
        MulticastSocket socket = createSocket();
        if (socket == null) {
          synchronized (myLock) {
            myLock.wait(RECREATE_SOCKET_PAUSE);
            continue;
          }
        }
        long socketCreated = System.currentTimeMillis();
        try {
          byte[] bytes = new byte[myBufferSize];
          DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
          while (myStarted.get()) {
            long now = System.currentTimeMillis();
            if (now - socketCreated > SOCKET_TTL)
              break;
            try {
              packet.setLength(bytes.length);
              socket.receive(packet);
              if (packet.getLength() > 0) {
                sink(packet);
                bytes = new byte[myBufferSize];
                packet = new DatagramPacket(bytes, bytes.length);
              }
            } catch (SocketTimeoutException e) {
              // continue
            } catch (IOException e) {
              Log.debug(e);
              break;
            }
          }
        } finally {
          closeSocket(socket);
        }
      }
    } catch (InterruptedException e) {
      // fall through
    } finally {
      synchronized (myLock) {
        if (myStarted.get()) {
          // exit due to error
          recreateThread();
        } else {
          myThread = null;
        }
      }
    }
  }

  private void sink(DatagramPacket packet) {
    Procedure<Pair<byte[], MessageTransportData>> sink = mySink;
    if (sink == null)
      return;
    int length = packet.getLength();
    if (length > myBufferSize) {
      Log.debug("message too long: " + length);
      return ;
    }
    byte[] bytes = new byte[length];
    try {
      System.arraycopy(packet.getData(), packet.getOffset(), bytes, 0, length);
    } catch (ArrayIndexOutOfBoundsException e) {
      Log.debug("bad " + packet);
      return;
    }
    MessageTransportData transportData = new MessageTransportData();
    transportData.setSequence(sequence());
    transportData.setSourceAddress(packet.getAddress());
    transportData.setSourcePort(packet.getPort());
    sink.invoke(Pair.create(bytes, transportData));
  }

  private MulticastSocket createSocket() {
    synchronized (myLock) {
      try {
        MulticastSocket socket = new MulticastSocket(myPort);
        int joined = 0;
        for (InetAddress address : myAddresses) {
          try {
            socket.joinGroup(address);
            joined++;
          } catch (IOException e) {
            // ignore
          }
        }
        if (joined > 0) {
          try {
            socket.setSoTimeout(SOCKET_TIMEOUT);
          } catch (SocketException e) {
            // ignore
          }
          mySocketIncarnation++;
          return socket;
        } else {
          Log.debug(socket + "did not join");
          closeSocket(socket);
          return null;
        }
      } catch (IOException e) {
        Log.debug(e);
        return null;
      }
    }
  }

  private void closeSocket(MulticastSocket socket) {
    if (socket == null)
      return;
    for (InetAddress address : myAddresses) {
      try {
        socket.leaveGroup(address);
      } catch (IOException e) {
        // ignore

      }
    }
    try {
      socket.close();
    } catch (Exception e) {
      // ignore
    }
  }

  private synchronized long sequence() {
    return ++mySequence;
  }


  public void setSink(Procedure<Pair<byte[], MessageTransportData>> sink) {
    synchronized (myLock) {
      if (myStarted.get()) {
        assert false : this;
        return;
      }
      mySink = sink;
    }
  }

  public void setBufferSize(int bufferSize) {
    synchronized (myLock) {
      if (myStarted.get()) {
        assert false : this;
        return;
      }
      myBufferSize = Math.min(Math.max(bufferSize, 1024), UDP_LIMIT);
    }
  }
}

