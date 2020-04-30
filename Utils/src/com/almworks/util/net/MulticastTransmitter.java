package com.almworks.util.net;

import com.almworks.util.Pair;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class MulticastTransmitter implements NetworkTransmitter {
  private static final long SOCKET_CACHE_TTL = 10000;

  private final Set<InetAddress> myTargetAddresses;
  private final int myPort;

  private List<MulticastSocket> mySockets;
  private long mySocketsCreationTime;

  /**
   * Will transmit to every pair of target address and target port.
   */
  public MulticastTransmitter(Collection<InetAddress> targetAddresses, int port) {
    for (InetAddress address : targetAddresses) {
      assert address.isMulticastAddress() : address;
    }
    assert port > 0 && port < 65536;
    myTargetAddresses = Collections15.linkedHashSet(targetAddresses);
    myPort = port;
  }

  @CanBlock
  public void send(byte[] bytes) throws InterruptedException {
    byte[] bytesCopy = bytes.clone();
    List<MulticastSocket> sockets = getSockets();
    boolean first = true;
    for (MulticastSocket socket : sockets) {
      for (InetAddress address : myTargetAddresses) {
        if (first)
          first = false;
        else
          pause();
        DatagramPacket packet = new DatagramPacket(bytesCopy, bytesCopy.length, address, myPort);
        try {
          socket.send(packet);
        } catch (IOException e) {
          Log.debug(
            "cannot send: " + socket.getLocalAddress() + ":" + socket.getLocalPort() + " to " + address + ":" + myPort);
          // ignore
        }
      }
    }
  }

  private static void pause() throws InterruptedException {
    Thread.sleep(250);
  }

  private synchronized List<MulticastSocket> getSockets() {
    long now = System.currentTimeMillis();
    if (mySockets != null) {
      if (now - mySocketsCreationTime > SOCKET_CACHE_TTL) {
        closeSockets(mySockets);
        mySockets = null;
      }
    }
    if (mySockets == null) {
      mySockets = Collections15.arrayList();
      mySocketsCreationTime = now;
      Collection<Pair<InetAddress, NetworkInterface>> pairs = NetworkUtils.getInetAddressesAndInterfaces();
      if (pairs.size() > 0) {
        for (Pair<InetAddress, NetworkInterface> pair : pairs) {
          InetAddress address = pair.getFirst();
          NetworkInterface intf = pair.getSecond();
          if (address.isLoopbackAddress())
            continue;
          InetSocketAddress sockaddr = new InetSocketAddress(address, myPort);
          try {
            MulticastSocket socket = new MulticastSocket(sockaddr);
            try {
              socket.setNetworkInterface(intf);
            } catch (Exception e) {
              // ignore - this happens on vista for no obvious reason
              // http://bugzilla.almworks.com/show_bug.cgi?id=816
              // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6458027
            }
            try {
              socket.setTimeToLive(255);
            } catch (Exception e) {
              // ignore
            }
            mySockets.add(socket);
          } catch (IOException e) {
            Log.debug("cannot work with " + sockaddr, e);
          }
        }
      } else {
        try {
          mySockets.add(new MulticastSocket());
        } catch (IOException e) {
          Log.debug(e);
        }
      }
    }
    return mySockets;
  }

  private void closeSockets(List<MulticastSocket> sockets) {
    for (MulticastSocket socket : sockets) {
      try {
        socket.close();
      } catch (Exception e) {
        Log.debug(e);
      }
    }
  }
}
