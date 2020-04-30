package util.external;

import org.almworks.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

/**
 * Adapter from sun.rmi.server.UID.
 * <p/>
 * Unique ID has the following form:
 * HHHHHHHH-TTTT-TTTT-CCCC-IIIIIIIIIIII
 * where
 * HHHHHHHH - unique host (jvm) id
 * TTTT-TTTT - hash function from current time
 * CCCC - object count
 * IIIIIIIIIIII - information gathered from IP address
 */

public final class UID implements java.io.Serializable {
  private static final Object myLock = new Object();
  private static final int myHostNumber = getHostNumber();
  //private static long lastTime = System.currentTimeMillis();
  private static short lastCount = Short.MIN_VALUE;
  private static int myHostIP = 0;

  private final int myHostId;
  private final int myTimeHashed;
  private final short myCount;
  private final int myIP;


  public UID() {
    myHostId = myHostNumber;
    long time = System.currentTimeMillis();
    time = time * ((time << 32) + 301);
    myTimeHashed = new Long(time).hashCode();
    synchronized (UID.class) {
      myCount = lastCount++;
      if (myHostIP == 0)
        myHostIP = getHostIP();
      myIP = myHostIP;
    }
  }

  public UID(int hostId, int timeHashed, short count, int ip) {
    myHostId = hostId;
    myTimeHashed = timeHashed;
    myCount = count;
    myIP = ip;
  }

  private static int getHostNumber() {
    return new Object().hashCode();
  }

  private static int getHostIP() {
    int result = 0;
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
          InetAddress address = inetAddresses.nextElement();
          if (!address.isLoopbackAddress()) {
            result = address.hashCode();
            break;
          }
        }
        if (result != 0)
          break;
      }
    } catch (SocketException e) {
      // todo - log?
      result = -1;
    }
    if (result != 0 && result != -1) {
      // rotate one bit to the right to hide ip
      int lowerbit = result & 1;
      result = result >>> 1;
      if (lowerbit > 0)
        result = result | 0x80000000;
      else
        result = result & 0x7FFFFFFF;
    }
    return result;
  }

  public int hashCode() {
    return (((myCount * 29 + myTimeHashed) * 29 + myIP) * 29) + myHostId;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof UID))
      return false;
    UID that = (UID) obj;
    return myCount == that.myCount && myHostId == that.myHostId && myIP == that.myIP &&
      myTimeHashed == that.myTimeHashed;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    hex(buffer, myHostId, 8);
    buffer.append('-');
    hex(buffer, ((myTimeHashed >> 16) & 0x0000FFFF), 4);
    buffer.append('-');
    hex(buffer, (myTimeHashed & 0x0000FFFF), 4);
    buffer.append('-');
    hex(buffer, myCount, 4);
    buffer.append("-0000");
    hex(buffer, myIP, 8);
    return buffer.toString();
  }

  private static void hex(StringBuffer buffer, int number, int digits) {
    String hexed = Util.upper(Integer.toHexString(number));
    int diff = digits - hexed.length();
    if (diff < 0) {
      hexed = hexed.substring(-diff);
    } else {
      for (digits = digits - hexed.length(); digits > 0; digits--)
        buffer.append('0');
    }
    buffer.append(hexed);
  }

  public void write(DataOutput out) throws IOException {
    out.writeInt(myHostId);
    out.writeInt(myTimeHashed);
    out.writeShort(myCount);
    out.writeInt(myIP);
  }

  public static UID read(DataInput in) throws IOException {
    int hostId = in.readInt();
    int timeHashed = in.readInt();
    short count = in.readShort();
    int ip = in.readInt();
    return new UID(hostId, timeHashed, count, ip);
  }

  public void write(ByteBuffer buffer) {
    buffer.putInt(myHostId);
    buffer.putInt(myTimeHashed);
    buffer.putShort(myCount);
    buffer.putInt(myIP);
  }
}
