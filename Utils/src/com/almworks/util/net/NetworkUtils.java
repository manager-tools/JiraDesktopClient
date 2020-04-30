package com.almworks.util.net;

import com.almworks.util.Pair;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.net.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {
  public static NetworkAdapter createMulticastAdapter(ThreadGate sendingGate, Collection<InetAddress> addresses,
    int port)
  {
    MulticastTransmitter transmitter = new MulticastTransmitter(addresses, port);
    MulticastReceiver receiver = new MulticastReceiver(addresses, port);
    return new GenericNetworkAdapter(sendingGate, transmitter, receiver);
  }

  public static Collection<InetAddress> getInetIP4Addresses() {
    Enumeration<NetworkInterface> interfaces = getNetworkInterfaces();
    if (!interfaces.hasMoreElements()) {
      return Collections15.emptyCollection();
    }
    List<InetAddress> result = Collections15.arrayList();
    while (interfaces.hasMoreElements()) {
      NetworkInterface intf = interfaces.nextElement();
      Enumeration<InetAddress> addresses = intf.getInetAddresses();
      if (addresses != null) {
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address instanceof Inet4Address) {
            result.add(address);
          }
        }
      }
    }
    return result;
  }

  public static Collection<Pair<InetAddress, NetworkInterface>> getInetAddressesAndInterfaces() {
    Enumeration<NetworkInterface> interfaces = getNetworkInterfaces();
    if (!interfaces.hasMoreElements()) {
      return Collections15.emptyCollection();
    }
    List<Pair<InetAddress, NetworkInterface>> result = Collections15.arrayList();
    while (interfaces.hasMoreElements()) {
      NetworkInterface intf = interfaces.nextElement();
      Enumeration<InetAddress> addresses = intf.getInetAddresses();
      if (addresses != null) {
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address instanceof Inet4Address) {
            result.add(Pair.create(address, intf));
          }
        }
      }
    }
    return result;
  }

  public static Enumeration<NetworkInterface> getNetworkInterfaces() {
    Enumeration<NetworkInterface> interfaces = null;
    try {
      interfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      Log.debug("no interfaces", e);
    } catch (Error e) {
      if (e.getClass().equals(Error.class)) {
        Log.debug("getNetworkInterfaces failed", e);
      } else {
        throw e;
      }
    }
    if (interfaces != null)
      return interfaces;
    else
      return Collections15.emptyEnumeration();
  }

  public static int addressToInt(String address) {
    try {
      InetAddress inet = InetAddress.getByName(address);
      return IPV4toInt(inet.getAddress());
    } catch (UnknownHostException e) {
      return -1;
    }
  }

  public static byte[] IPV4toBytes(int ipv4address) {
    byte[] result = new byte[4];
    result[0] = (byte) ((ipv4address >>> 24) & 0xFF);
    result[1] = (byte) ((ipv4address >>> 16) & 0xFF);
    result[2] = (byte) ((ipv4address >>> 8) & 0xFF);
    result[3] = (byte) (ipv4address & 0xFF);
    return result;
  }

  public static int IPV4toInt(byte[] address) {
    if (address.length != 4) {
      assert false : address;
      return 0;
    }
    int result = address[3] & 0xFF;
    result |= ((address[2] << 8) & 0xFF00);
    result |= ((address[1] << 16) & 0xFF0000);
    result |= ((address[0] << 24) & 0xFF000000);
    return result;
  }
}