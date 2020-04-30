package com.almworks.tracker.alpha;

import com.almworks.dup.util.SimpleValueModel;
import com.almworks.tracker.eapi.alpha.ArtifactLoadOption;
import com.almworks.tracker.eapi.alpha.TrackerConnectionStatus;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.xmlrpc.EndPoint;
import com.almworks.util.xmlrpc.XmlRpcFixture;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.apache.xmlrpc.WebServer;

import java.net.ServerSocket;
import java.util.*;

public class ArtifactLoaderImplTests extends XmlRpcFixture {
  private static final int LISTEN = 0;
  private static final int CONNECT = 31102;

  private ArtifactLoaderImpl myLoader;
  private EndPoint myEndPoint;
  private SimpleValueModel<TrackerConnectionStatus> myStatus;
  private MyWebServer myPeer;
  private OutboxHandler myHandler;

  protected void setUp() throws Exception {
    super.setUp();
    myHandler = new OutboxHandler();
    myPeer = new MyWebServer();
    myPeer.addHandler("almworks", myHandler);
    myPeer.start();

    myEndPoint = new EndPoint(0, myPeer.getPort());
    myStatus = SimpleValueModel.create(TrackerConnectionStatus.NOT_CONNECTED);
    myLoader = new ArtifactLoaderImpl(myEndPoint, myStatus);
    myEndPoint.start();
    myLoader.start();
  }

  protected void tearDown() throws Exception {
    myLoader.shutdown();
    myLoader = null;
    myEndPoint.shutdown();
    myEndPoint = null;
    myStatus = null;
    myPeer.shutdown();
    super.tearDown();
  }

  public void testSimpleListen() throws InterruptedException {
    checkSimpleListen("x", Lifespan.FOREVER, ArtifactLoadOption.NONE, "URL-1");
    checkSimpleListen("y", Lifespan.FOREVER, ArtifactLoadOption.MAYBE_DOWNLOAD, "URL-11", "URL-2", "URL-3");
    checkSimpleListen("w", Lifespan.FOREVER, ArtifactLoadOption.MAYBE_CREATE_CONNECTION_AND_DOWNLOAD, "URL-4", "URL-5");
  }

  public void testRedundantListens() throws InterruptedException {
    checkSimpleListen("x", Lifespan.FOREVER, ArtifactLoadOption.NONE, "URL-1");

    // no urls
    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, Collections15.<String>emptyList(), ArtifactLoadOption.NONE);
    myHandler.silence(500);

    // finished detach
    DetachComposite finished = new DetachComposite();
    finished.detach();
    myLoader.subscribeArtifacts("w", finished, Collections.singleton("URL"), ArtifactLoadOption.NONE);
    myHandler.silence(500);

    // already subscribed
    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, Collections.singleton("URL-1"), ArtifactLoadOption.NONE);
    myHandler.silence(500);
  }

  public void testListenIncreasingFlags() throws InterruptedException {
    checkSimpleListen("x", Lifespan.FOREVER, ArtifactLoadOption.NONE, "URL-1");

    Set<String> c = Collections.singleton("URL-1");

    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_DOWNLOAD);
    myHandler.receive(500, "almworks.subscribe");

    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.NONE);
    myHandler.silence(500);
    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_DOWNLOAD);
    myHandler.silence(500);

    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_CREATE_CONNECTION_AND_DOWNLOAD);
    myHandler.receive(500, "almworks.subscribe");

    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.NONE);
    myHandler.silence(500);
    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_DOWNLOAD);
    myHandler.silence(500);
    myLoader.subscribeArtifacts("w", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_CREATE_CONNECTION_AND_DOWNLOAD);
    myHandler.silence(500);
  }

  public void testDetach() throws InterruptedException {
    DetachComposite detach = new DetachComposite();
    checkSimpleListen("x", detach, ArtifactLoadOption.NONE, "URL-1");
    detach.detach();
    checkUnsubscribed("URL-1");
  }

  public void testCollectingKeys() throws InterruptedException {
    Set<String> c = Collections.singleton("URL-1");

    myLoader.subscribeArtifacts("A", Lifespan.FOREVER, c, ArtifactLoadOption.MAYBE_CREATE_CONNECTION_AND_DOWNLOAD);
    myHandler.receive(500, "almworks.subscribe");

    myLoader.subscribeArtifacts("B", Lifespan.FOREVER, c, ArtifactLoadOption.NONE);
    myHandler.silence(500);

    myLoader.unsubscribeArtifacts("A", c);
    myHandler.silence(500);

    myLoader.unsubscribeArtifacts("B", c);
    checkUnsubscribed("URL-1");
  }



  private void checkUnsubscribed(String ... urls) throws InterruptedException {
    Vector vector = myHandler.receive(500, "almworks.unsubscribe");
    assertEquals(urls.length + 1, vector.size());
    vector.remove(0);
    new CollectionsCompare().unordered(vector, urls);
  }

  private void checkSimpleListen(String key, Lifespan life, ArtifactLoadOption[] options, String ... urls) throws InterruptedException {
    myLoader.subscribeArtifacts(key, life, Arrays.asList(urls), options);
    Vector vector = myHandler.receive(500, "almworks.subscribe");
    assertEquals(2, vector.size());
    Object o = vector.get(1);
    assertEquals(Hashtable.class, o.getClass());
    Hashtable table = (Hashtable) o;
    assertEquals(urls.length, table.size());
    for (String url : urls) {
      Object opts = table.get(url);
      assertNotNull(url, opts);
      assertEquals(Vector.class, opts.getClass());
      Vector v = (Vector) opts;
      Set<ArtifactLoadOption> set = Collections15.hashSet(Arrays.<ArtifactLoadOption>asList(options));
      assertEquals(set.size(), v.size());
      for (Object option : v) {
        ArtifactLoadOption loadOption = ArtifactLoadOption.forExternalName((String) option);
        assertTrue((String)option, set.contains(loadOption));
      }
    }
  }

  private static class MyWebServer extends WebServer {
    public MyWebServer() {
      super(0);
    }

    public int getPort() {
      ServerSocket socket = serverSocket;
      if (socket == null)
        return 0;
      return socket.getLocalPort();
    }
  }
}
