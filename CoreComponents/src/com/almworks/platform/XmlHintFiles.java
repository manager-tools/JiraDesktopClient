package com.almworks.platform;

import com.almworks.util.xml.JDOMUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Sample xml hint file:<p/>
 * &lt;library&gt;<br>
 * &nbsp;&nbsp;&lt;component class="com.almworks.haba.Haba"/&gt;<br>
 * &nbsp;&nbsp;&lt;component class="com.almworks.daba.Daba"/&gt;<br>
 * &lt;library&gt;
 *
 * @author sereda
 */
class XmlHintFiles {
  private static final String LIBRARY_TAG = "library";
  private static final String COMPONENT_TAG = "component";
  private static final String COMPONENT_TAG_CLASS_ATTRIBUTE = "class";

  static Set<String> readHintStream(InputStream xmlStream) throws IOException, JDOMException {
    Set<String> result = new HashSet<String>();
    SAXBuilder builder = JDOMUtils.createBuilder();
    Document document = builder.build(xmlStream);
    Iterator<Element> libraries = document.getDescendants(new ElementFilter(LIBRARY_TAG));
    while (libraries.hasNext()) {
      Element library = libraries.next();
      Iterator<Element> components = library.getChildren(COMPONENT_TAG).iterator();
      while (components.hasNext()) {
        Element component = components.next();
        String className = component.getAttributeValue(COMPONENT_TAG_CLASS_ATTRIBUTE);
        if (className == null)
          continue;
        result.add(className);
      }
    }
    return result;
  }

  static void writeHintStream(OutputStream stream, Collection<String> classNames) throws IOException {
    Element root = new Element(LIBRARY_TAG);
    for (Iterator<String> it = classNames.iterator(); it.hasNext();)
      root.addContent(new Element(COMPONENT_TAG).setAttribute(COMPONENT_TAG_CLASS_ATTRIBUTE, it.next()));
    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    outputter.output(root, stream);
  }
}