package com.almworks.api.connector.http;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.DefaultHttpMaterial;
import com.almworks.api.http.HttpClientProvider;
import com.almworks.http.HttpLoaderFactoryImpl;
import com.almworks.util.tests.BaseTestCase;
import org.jdom.Document;

/**
 * @author Igor Sereda
 */
public class BadlyFormedXmlTests extends BaseTestCase {
  public void testHangingAmpersand() throws ConnectorException {
    Document document = parseXML("<xml>a&&b&bb;b&&#& ##&# &#x&#343434343;&#34lk</xml>");
    // all characters are kept except for &#343434343; that is replaced with ?
    assertEquals("a&&b&bb;b&&#& ##&# &#x?&#34lk", document.getRootElement().getText());
  }

  private Document parseXML(String text) throws ConnectorException {
    DefaultHttpMaterial material = new DefaultHttpMaterial(HttpClientProvider.SIMPLE, new HttpLoaderFactoryImpl());
    DocumentLoader loader = new DocumentLoader(material);
    loader.setResponse(new TestResponseData(text));
    return loader.loadXML();
  }
}
