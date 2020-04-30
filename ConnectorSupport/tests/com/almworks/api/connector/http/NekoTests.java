package com.almworks.api.connector.http;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.DefaultHttpMaterial;
import com.almworks.api.http.HttpClientProvider;
import com.almworks.http.HttpClientProviderImpl;
import com.almworks.http.HttpLoaderFactoryImpl;
import com.almworks.util.files.FileUtil;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

public class NekoTests extends BaseTestCase {
  public void testMeta() throws IOException, ConnectorException {
    File file = createFileName();
    FileUtil.writeFile(file, "<html><head>" + "<title>[#NORTELBIZSPHERE-1069] [test ticket, see description]</title>" +
      "<meta name=\"decorator\" content=\"navigator\">" + "</head><body>" + "</body</html>");

    DefaultHttpMaterial material =
      new DefaultHttpMaterial(new HttpClientProviderImpl(null), new HttpLoaderFactoryImpl());
    DocumentLoader loader = new DocumentLoader(material, file);
    Document document = loader.fileLoad().loadHTML();
    Element e = JDOMUtils.searchElement(document.getRootElement(), "meta", "name", "decorator");
    assertNotNull(e);
  }

  public void testNekoUnresolvedProblem() throws IOException {
    String S = "<http://www.processbench.de/de/Produkte/Analycess.html>";
    try {
      HtmlUtils.buildHtmlDocument(new InputSource(new StringReader(S)));
    } catch (SAXException e) {
      System.err.println("Neko problem is still there!");
    }
  }

  public void testHTMLBadAttributeNameProblem() throws ConnectorException {
    URL url =
      NekoTests.class.getClassLoader().getResource("com/almworks/api/connector/http/BadHtmlAttribute.html");
    assertNotNull(url);
    String urlString = url.toExternalForm();
    if (!urlString.startsWith("file:")) return; // can't test
    urlString = urlString.substring(5);
    File file = new File(urlString);
    assertTrue(file.toString(), file.isFile());
    DefaultHttpMaterial material = new DefaultHttpMaterial(HttpClientProvider.SIMPLE, new HttpLoaderFactoryImpl());
    DocumentLoader loader = new DocumentLoader(material, file);
    loader.fileLoad();
    Document html = loader.loadHTML();
    assertNotNull(html);
  }

  public void testTables() throws IOException, SAXException {
    String HTML = "<body><table><tr><td>cell</td></tr></table></body>";
    Document doc = HtmlUtils.buildHtmlDocument(new InputSource(new StringReader(HTML)));
    List<Element> table = JDOMUtils.searchElements(doc.getRootElement(), "table");
    List<Element> rows = JDOMUtils.getChildren(table.get(0), "tr");
    assertEquals(1, rows.size());
  }
}
