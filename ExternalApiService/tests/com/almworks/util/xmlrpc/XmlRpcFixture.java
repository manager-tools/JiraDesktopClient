package com.almworks.util.xmlrpc;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.apache.xmlrpc.XmlRpcHandler;

import java.util.Vector;

public abstract class XmlRpcFixture extends BaseTestCase {
  protected XmlRpcFixture() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  public class TestHandler {
    protected String myCalledMethod;
    protected Vector myCalledParameters;

    public synchronized void check(long timeout, String method) throws InterruptedException {
      check(timeout, method, new Object[] {});
    }

    public void check(long timeout, String method, Object ... parameters) throws InterruptedException {
      if (method == null) {
        silence(timeout);
      } else {
        Vector v = receive(timeout, method);
        new CollectionsCompare().order(parameters, v.toArray());
      }
    }

    public synchronized Vector receive(long timeout, String method) throws InterruptedException {
      long finish = System.currentTimeMillis() + timeout;
      while (true) {
        String calledMethod = myCalledMethod;
        Vector calledParameters = myCalledParameters;
        if (calledMethod != null) {
          assertEquals(method, calledMethod);
          myCalledMethod = null;
          myCalledParameters = null;
          return new Vector(calledParameters);
        }
        if (System.currentTimeMillis() > finish)
          break;
        wait(500);
      }
      fail("message did not come");
      return null;
    }

    public void silence(long timeout) {
      sleep((int) timeout);
      assertNull(myCalledMethod);
      assertNull(myCalledParameters);
      return;
    }

    protected void set(String method, Vector params) {
      synchronized (this) {
        myCalledMethod = method;
        myCalledParameters = params;
        notifyAll();
      }
    }
  }


  public class OutboxHandler extends TestHandler implements XmlRpcHandler {
    public Object execute(final String method, final Vector params) throws Exception {
      set(method, params);
      return "ok";
    }
  }


  public class InboxHandler extends TestHandler {
    public IncomingMessageFactory getMessageFactory() {
      return new CloningIncomingMessage("test") {
        protected void process() throws MessageProcessingException {
          set(getRpcMethodName(), getParameters());
        }
      };
    }
  }
}
