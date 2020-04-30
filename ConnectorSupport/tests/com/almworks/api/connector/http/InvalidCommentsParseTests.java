package com.almworks.api.connector.http;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.http.DefaultHttpMaterial;
import com.almworks.http.HttpClientProviderImpl;
import com.almworks.http.HttpLoaderFactoryImpl;
import com.almworks.util.files.FileUtil;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;

public class InvalidCommentsParseTests extends BaseTestCase {
  public void testInvalidComment() throws IOException, ConnectorException {
    File file = createFileName();
    checkComment(file, " this is a bad comment because there are -- inside ");
    checkComment(file, "----------");
    checkComment(file, "");
    checkComment(file, "- this is what is seen in bugzilla");
  }

  private void checkComment(File file, String comment) throws IOException, ConnectorException {
    FileUtil.writeFile(file, "<html><body><!--" + comment + "--></body></html>");
    DefaultHttpMaterial material =
      new DefaultHttpMaterial(new HttpClientProviderImpl(null), new HttpLoaderFactoryImpl());
    DocumentLoader loader = new DocumentLoader(material, file);
    Document document = loader.fileLoad().loadHTML();
    Element e = JDOMUtils.searchElement(document.getRootElement(), "body");
    assertNotNull(e);
    assertEquals(1, e.getContentSize());
    assertEquals(Comment.class, e.getContent(0).getClass());
  }
}
