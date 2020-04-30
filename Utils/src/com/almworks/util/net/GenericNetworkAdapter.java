package com.almworks.util.net;

import com.almworks.util.Pair;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import util.concurrent.SynchronizedBoolean;

import java.io.IOException;
import java.util.Map;

public class GenericNetworkAdapter implements NetworkAdapter {
  private final SynchronizedBoolean myStarted = new SynchronizedBoolean(false);
  private final Map<Class, NetworkMessageSpecification> mySpecifications = Collections15.linkedHashMap();
  private final FireEventSupport<NetworkAdapterListener> myEvents =
    FireEventSupport.createSynchronized(NetworkAdapterListener.class);
  private final ThreadGate mySendingGate;
  private final NetworkTransmitter myTransmitter;
  private final NetworkReceiver myReceiver;

  private int myMaximumMessageLength = 0;

  public GenericNetworkAdapter(ThreadGate sendingGate, NetworkTransmitter transmitter, NetworkReceiver receiver) {
    mySendingGate = sendingGate;
    myTransmitter = transmitter;
    myReceiver = receiver;
  }

  public void defineMessage(NetworkMessageSpecification specification) {
    boolean started = myStarted.get();
    assert !started : this;
    if (started)
      return;
    mySpecifications.put(specification.getMessageClass(), specification);
    int length = specification.getMaximumMessageLength();
    assert length > 0 : specification;
    if (length > myMaximumMessageLength)
      myMaximumMessageLength = length;
  }

  public EventSource<NetworkAdapterListener> events() {
    return myEvents;
  }

  public void send(NetworkMessage message) {
    NetworkMessageSpecification specification = mySpecifications.get(message.getClass());
    if (specification == null) {
      assert false : message;
      return;
    }
    final byte[] bytes;
    try {
      bytes = specification.marshall(message);
    } catch (IOException e) {
      Log.debug("cannot marshall " + message);
      return;
    }
    mySendingGate.execute(new Runnable() {
      public void run() {
        try {
          myTransmitter.send(bytes);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    });
  }

  private void receive(byte[] bytes, MessageTransportData transportData) {
    NetworkMessage message = unmarshall(bytes, transportData);
    if (message != null)
      myEvents.getDispatcher().onMessageReceived(message);
  }

  private NetworkMessage unmarshall(byte[] bytes, MessageTransportData transportData) {
    for (NetworkMessageSpecification specification : mySpecifications.values()) {
      try {
        return specification.unmarshall(bytes, transportData);
      } catch (IOException e) {
        // skip this specification
      }
    }
    Log.debug("cannot unmarshall " + transportData);
    return null;
  }

  public void start() {
    if (mySpecifications.size() == 0 || myMaximumMessageLength == 0) {
      assert false : this;
    }
    if (!myStarted.commit(false, true))
      return;
    myReceiver.setSink(new Procedure<Pair<byte[], MessageTransportData>>() {
      public void invoke(Pair<byte[], MessageTransportData> arg) {
        receive(arg.getFirst(), arg.getSecond());
      }
    });
    myReceiver.setBufferSize(myMaximumMessageLength);
    myReceiver.start();
  }

  public void stop() {
    if (!myStarted.commit(true, false))
      return;
    myReceiver.stop();
  }
}
