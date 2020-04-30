package com.almworks.http;

import com.almworks.util.tests.BaseTestCase;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

public class URIHackTests extends BaseTestCase {
  public void testUnderscoreLosesPort() throws URIException {
    URI uri = new URI("http://dyoma_comp:8080/whatever", true);
    assertEquals("dyoma_comp", uri.getHost());
    assertEquals(8080, uri.getPort());
  }
}
