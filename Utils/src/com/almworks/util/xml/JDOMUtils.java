package com.almworks.util.xml;

import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Condition2;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.jdom.*;
import org.jdom.filter.AbstractFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static com.almworks.util.collections.Functional.*;
import static com.almworks.util.commons.Condition.cond;
import static org.almworks.util.Collections15.iterableOnce;

public class JDOMUtils {
  public final static Condition2 EQUALS_CONDITION = new Condition2<String, String>() {
    public boolean isAccepted(String attValue, String ourParam) {
      return attValue.equalsIgnoreCase(ourParam);
    }
  };

  public final static Condition2<String, String> START_WITH_CONDITION = new Condition2<String, String>() {
    public boolean isAccepted(String attValue, String ourParam) {
      return ourParam != null && attValue != null && attValue.startsWith(ourParam);
    }
  };

  public final static Condition2<String, Pattern> MATCHES_PATTERN = new Condition2<String, Pattern>() {
    @Override
    public boolean isAccepted(String attrValue, Pattern pattern) {
      return pattern != null && pattern.matcher(attrValue).matches();
    }
  };

  public static final Condition2<String, String> CONTAINS_WORD = new Condition2<String, String>() {
    @Override
    public boolean isAccepted(String attrValue, String wanted) {
      if(attrValue == null) {
        return false;
      }
      for(final String word : attrValue.split("\\s+")) {
        if(wanted.equals(word)) {
          return true;
        }
      }
      return false;
    }
  };

  public final static Convertor<Element, String> GET_TEXT_TRIM = new Convertor<Element, String>() {
    @Override
    public String convert(Element value) {
      return value != null ? value.getText().trim() : "";
    }
  };

  public static final Condition2<Element, Element> IS_ANCESTOR = new Condition2<Element, Element>() {
    @Override
    public boolean isAccepted(Element p, Element c) {
      return p == null || c == null ? false : p.isAncestor(c);
    }
  };

  private static volatile boolean ourTriedXerces = false;

  private static volatile boolean ourUseXerces = false;

  private JDOMUtils() {
  }

  public static SAXBuilder createBuilder() {
    if (!ourTriedXerces) {
      synchronized (JDOMUtils.class) {
        if (!ourTriedXerces) {
          SAXBuilder saxBuilder = createXercesBuilder();
          try {
            saxBuilder.build(new StringReader("<test/>"));
            ourUseXerces = true;
          } catch (JDOMException e) {
            Log.warn("cannot use xerces, falling back to default parser", e);
            ourUseXerces = false;
          } catch (IOException e) {
            assert false : e;
            Log.debug("should not happen, ignoring", e);
            ourUseXerces = false;
          }
          ourTriedXerces = true;
        }
      }
    }

    if (ourUseXerces)
      return createXercesBuilder();
    else
      return new SAXBuilder();
  }

  private static SAXBuilder createXercesBuilder() {
    Thread thread = Thread.currentThread();
    ClassLoader loader = JDOMUtils.class.getClassLoader();
    assert loader != null;
    ClassLoader contextLoader = thread.getContextClassLoader();
    if (!loader.equals(contextLoader)) {
      Log.warn("bad context class loader, resetting: " + contextLoader);
      Log.debug("thread: " + Thread.currentThread(), new Throwable());
      thread.setContextClassLoader(loader);
    }
    return new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
  }

  /**
   * Searches for an element with a given tagName and with defined attribute that is equal to specified value.
   * NB: all string parameters are case-insensitive. Returns first found item.
   *
   * @param root           parent tagName to search under.
   * @param tagName        element name to search for, case insensitive.
   * @param attributeName  [nullable] name of attribute that should be equals to attributeValue, case insensitive.
   *                       If null, the search would return first element that matches tagName.
   * @param attributeValue [nullable] value of attribute to search for, case insensitive.
   * @return found element, or null if not found.
   */
  public static Element searchElement(final Element root, String tagName, String attributeName, String attributeValue) {
    Iterator it = searchElementIterator(root, tagName, attributeName, attributeValue);
    return it.hasNext() ? (Element) it.next() : null;
  }

  public static Element searchElement(Element root, String tagName) {
    return searchElement(root, tagName, null, null);
  }

  /**
   * Looks for the element and if found, returns getTextTrim(element)
   *
   * @return found element's text or null if element is not found
   */
  @Nullable
  public static String searchElementTextTrim(Element root, String tagName) {
    Element element = searchElement(root, tagName);
    return element == null ? null : getTextTrim(element);
  }

  public static List<Element> searchElements(final Element root, String tagName) {
    return searchElements(root, tagName, null, null);
  }

  public static Iterable<Element> searchElements(final Element root, final Collection<String> tags) {
    if(tags == null || tags.isEmpty()) {
      return Collections15.emptyIterable();
    }

    final class Filter extends AbstractFilter implements Iterable<Element> {
      @Override
      public boolean matches(Object o) {
        if(o instanceof Element) {
          return tags.contains(((Element)o).getName());
        }
        return false;
      }

      @Override
      public Iterator<Element> iterator() {
        return root.getDescendants(this);
      }
    }

    return new Filter();
  }

  @NotNull
  public static List<Element> searchElements(final Element root, String tagName, String attributeName,
    String attributeValue)
  {
    Iterator<Element> it = searchElementIterator(root, tagName, attributeName, attributeValue);
    return Containers.collectList(it);
  }

  public static String getAttributeValue(Element element, String attributeName, String defaultValue,
    boolean replaceHtmlEntities)
  {
    String value = getRawAttributeValue(element, attributeName, defaultValue);
    if (value == null)
      return value;
    if (replaceHtmlEntities)
      value = replaceXmlEntities(value, true);
    return value;
  }

  private static String getRawAttributeValue(Element element, String attributeName, String defaultValue) {
    String value = element.getAttributeValue(attributeName);
    if (value != null)
      return value;
    List<Attribute> attributes = element.getAttributes();
    for (int i = 0; i < attributes.size(); i++) {
      Attribute attribute = attributes.get(i);
      if (attribute.getName().equalsIgnoreCase(attributeName))
        return attribute.getValue();
    }
    return defaultValue;
  }


  public static Iterator<Element> searchElementIterator(final Element root, String tagName) {
    return searchElementIterator(root, tagName, null, null);
  }

  public static Iterator<Element> searchElementIterator(final Element root, final String tagName,
    final String attributeName, final String ourValue)
  {
    return searchElementIterator2(root, tagName, attributeName, ourValue, EQUALS_CONDITION);
  }

  /**
   * @return all nodes <tt>b</tt> such that their paths are of the form {@code root ... a_1 ... a_2 ... (a_n = b)} and {@code c_1(a_1) && c_2(a_2) && ... && c_n(a_n)} holds, <br/>
   * where <tt>root</tt> is the root of the searched tree (see parameter), and <tt>a_i</tt> is the i-th search condition.
   * */
  // todo Refactor: unite this method with searchElementIerator, make it return object that implements Iterable and allows to sub-filter its results' subtrees
  @NotNull
  public static Iterable<Element> multiSearchElements(Element root, ElementSearch... searches) {
    return searchElements(repeat(root, 1), Arrays.asList(searches));
  }

  /** @see #multiSearchElements(org.jdom.Element, com.almworks.util.xml.JDOMUtils.ElementSearch...)  */
  public static Iterable<Element> searchElements(Iterable<Element> elements, List<ElementSearch> searches) {
    final ElementSearch search = first(searches);
    final List<ElementSearch> tail = search == null ? null : searches.subList(1, searches.size());
    return search == null ? elements :
      selectMany(elements,
        new Convertor<Element, Iterable<Element>>() { @Override public Iterable<Element> convert(Element root) {
          return searchElements(
            filter(
            iterableOnce(searchElementIterator(root, search.tag, search.attribute, search.value)),
            cond(reduceWithLast(null, IS_ANCESTOR.fun())).not()),
          tail);
      }
    });
  }

  public static ElementSearch byTag(@NotNull String tag) {
    return new ElementSearch(tag, null, null);
  }

  public static ElementSearch byTagAndAttr(@NotNull String tag, String attribute, String value) {
    return new ElementSearch(tag, attribute, value);
  }

  public static class ElementSearch {
    @NotNull public final String tag;
    public final String attribute;
    public final String value;

    private ElementSearch(String tag, String attribute, String value) {
      this.tag = tag;
      this.attribute = attribute;
      this.value = value;
    }
  }

  public static <C> Iterator<Element> searchElementIterator2(final Element root, final String tagName,
    final String attributeName, final C conditionValue, final Condition2<String, C> acceptCondition)
  {
    return root.getDescendants(new AbstractFilter() {
      public boolean matches(Object obj) {
        if (obj instanceof Element) {
          Element element = (Element) obj;
          if (tagName != null && !element.getName().equalsIgnoreCase(tagName))
            return false;
          if (attributeName != null && acceptCondition != null) {
            List<Attribute> attributes = element.getAttributes();
            for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
              Attribute attr = it.next();
              if (attr.getName().equalsIgnoreCase(attributeName)) {
                if (acceptCondition.isAccepted(attr.getValue().trim(), conditionValue))
                  return true;
                continue;
              }
            }
          } else {
            return true;
          }
        }
        return false;
      }
    });
  }

  public static Iterator<Element> searchElementIterator2(Element root, String tagName, String attrName, final Condition<String> c) {
    return searchElementIterator2(root, tagName, attrName, null, new Condition2<String, Void>() {
      @Override
      public boolean isAccepted(String attrValue, Void nothing) {
        return c == null || c.isAccepted(attrValue);
      }
    });
  }

  public static void dump(Element element, String fileName) throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(fileName);
      dump(element, out);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(out);
    }
  }

  public static void dump(Element element, File file) throws IOException {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file);
      dump(element, out);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(out);
    }
  }

  public static void dump(Element element, OutputStream out) throws IOException {
    Writer writer = new OutputStreamWriter(out, "UTF-8");
    XMLOutputter outputter = new XMLOutputter();
    Format format = Format.getPrettyFormat();
    format.setIndent("  ");
    outputter.setFormat(format);
    outputter.output(element, writer);
    writer.close();
  }

  public static String dump(Element element) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      dump(element, out);
      out.close();
      return new String(out.toByteArray(), "UTF-8");
    } catch (IOException e) {
      throw new Failure(e);
    }
  }

  public static String replaceXmlEntities(String string, boolean replaceHtmlEntities) {
    return replaceXmlEntities(string, replaceHtmlEntities, true, true);
  }

  public static String replaceXmlEntities(String string, boolean replaceHtmlEntities, boolean replaceAmp,
    boolean replaceOtherXMLEntities)
  {
    if (string == null)
      return null;
    int k = string.indexOf('&');
    if (k < 0)
      return string;
    int len = string.length();
    StringBuffer result = new StringBuffer(len);
    int p = 0;
    while (k >= 0) {
      if (k > p)
        result.append(string.substring(p, k));
      int z = -1;
      for (int i = k + 1; i < len && i < k + 6; i++)
        if (string.charAt(i) == ';') {
          z = i;
          break;
        }
      if (z < 0) {
        p = k + 1;
        result.append('&');
      } else {
        p = z + 1;
        String name = string.substring(k + 1, z);
        char e = xmlEntity(name);
        if (e != 0) {
          if (!replaceAmp && "amp".equalsIgnoreCase(name))
            e = 0;
          if (e != 0 && !replaceOtherXMLEntities && !"amp".equalsIgnoreCase(name))
            e = 0;
        } else {
          if (replaceHtmlEntities)
            e = htmlEntity(name);
        }
        if (e != 0)
          result.append(e);
        else
          result.append('&').append(name).append(';');
      }
      k = string.indexOf('&', p);
    }
    if (len > p)
      result.append(string.substring(p));
    return result.toString();
  }


  /**
   * Gets text contained in the element (recursive), replacing entity references and trimming the result.
   */
  @NotNull
  public static String getTextTrim(Element element) {
    return getText(element).trim();
  }


  @NotNull
  public static String getText(Element element) {
    return getText(element, false);
  }

  @NotNull
  public static String getText(Element element, boolean commentsAsText) {
    return getText(element, commentsAsText, null);
  }

  /**
   * Gets text contained in the element (recursive), replacing entity references.
   *
   * @param commentsAsText if true, treat comments as text nodes. this is usful only for getting scripts from HTML
   */
  @NotNull
  public static String getText(Element element, boolean commentsAsText, XmlTextAdjuster adjuster) {
    if (element == null)
      return "";
    StringBuffer buffer = new StringBuffer();
    extractText(buffer, element, commentsAsText, adjuster, new XmlTextAdjusterHelper());
    return buffer.toString();
  }

  private static void extractText(StringBuffer buffer, @NotNull Element element, boolean commentsAsText,
    XmlTextAdjuster adjuster, XmlTextAdjusterHelper helper)
  {
    List<Content> contents = (List<Content>) element.getContent();
    for (Content content : contents) {
      if (content instanceof Text) {
        Text text = (Text) content;
        String value = text.getText();
        value = convertSmartUnicodeChars(value);
        if (adjuster != null) {
          adjuster.appendText(buffer, text, value, helper);
        } else {
          buffer.append(value);
        }
      } else if (content instanceof Element) {
        Element elem = (Element) content;
        if (adjuster == null) {
          extractText(buffer, elem, commentsAsText, adjuster, helper);
        } else {
          boolean process = adjuster.beforeElement(buffer, elem, helper);
          if (process)
            extractText(buffer, elem, commentsAsText, adjuster, helper);
          adjuster.afterElement(buffer, elem, helper);
        }
      } else if (content instanceof EntityRef) {
        EntityRef ref = (EntityRef) content;
        String refName = ref.getName();
        char c = xmlEntity(refName);
        if (c == 0)
          c = htmlEntity(refName);
        if (c != 0) {
          buffer.append(c);
        } else {
          buffer.append('&').append(refName).append(';');
        }
      } else if (commentsAsText && (content instanceof Comment)) {
        String text = ((Comment) content).getText();
        buffer.append(convertSmartUnicodeChars(text));
      }
    }
  }

  public static char htmlEntity(String name) {
    char c = HTMLEntityResolver.getInstance().getEntityChar(name);
    c = convertSmartUnicodeChar(c);
    return c;
  }

  /**
   * These "smart" chars ruin the UI because fonts don't support them
   */
  private static char convertSmartUnicodeChar(char c) {
    if (c == '\u00A0') {
      // convert non-breaking space into normal space
      return ' ';
    }
    if (c == '\u2011') {
      // convert non-breaking hyphen into just hyphen
      return '-';
    }
    return c;
  }

  public static String convertSmartUnicodeChars(String text) {
    if (text == null)
      return null;
    StringBuffer result = null;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      char r = convertSmartUnicodeChar(c);
      if (c != r && result == null)
        result = new StringBuffer(text.substring(0, i));
      if (result != null)
        result.append(r);
    }
    return result == null ? text : result.toString();
  }

  public static char xmlEntity(String name) {
    if ("lt".equalsIgnoreCase(name))
      return '<';
    else if ("gt".equalsIgnoreCase(name))
      return '>';
    else if ("amp".equalsIgnoreCase(name))
      return '&';
    else if ("quot".equalsIgnoreCase(name))
      return '"';
    else if ("apos".equalsIgnoreCase(name))
      return '\'';
    else
      return 0;
  }

  public static Document parse(String xml) throws JDOMException {
    try {
      SAXBuilder builder = createBuilder();
      return builder.build(new InputSource(new StringReader(xml)));
    } catch (IOException e) {
      throw new Failure(e);
    }
  }

  public static String escapeXmlEntities(String s) {
    StringBuffer result = new StringBuffer();
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      switch (c) {
      case '&':
        result.append("&amp;");
        break;
      case '<':
        result.append("&lt;");
        break;
      case '>':
        result.append("&gt;");
        break;
      case '\'':
        // In HTML, &apos; won't work, although numeric reference will do. so we use a generic solution
        result.append("&#39;");
        break;
      case '"':
        result.append("&quot;");
        break;
      default:
        result.append(c);
        break;
      }
    }
    return result.toString();
  }

  public static List<Element> getChildren(Element parent, final String tagName) {
    if (tagName == null) {
      return parent.getChildren();
    } else {
      return parent.getContent(new AbstractFilter() {
        public boolean matches(Object object) {
          if (!(object instanceof Element))
            return false;
          return tagName.equalsIgnoreCase(((Element) object).getName());
        }
      });
    }
  }

  @NotNull
  public static String getChildText(Element parent, String tagName) {
    Element child = getChild(parent, tagName);
    return child == null ? "" : getText(child);
  }

  @Nullable
  public static Element getChild(Element parent, String tagName) {
    if (tagName != null) {
      int size = parent.getContentSize();
      for (int i = 0; i < size; i++) {
        Content c = parent.getContent(i);
        if (c instanceof Element && ((Element) c).getName().equalsIgnoreCase(tagName)) {
          return (Element) c;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Element getChild(Element parent, String tagName, String attributeName, String attributeValue,
    boolean replaceHtmlEntities)
  {
    if (tagName != null) {
      int size = parent.getContentSize();
      for (int i = 0; i < size; i++) {
        Content c = parent.getContent(i);
        if (c instanceof Element) {
          Element e = (Element) c;
          if (e.getName().equalsIgnoreCase(tagName)) {
            if (getAttributeValue(e, attributeName, "", replaceHtmlEntities).equals(attributeValue)) {
              return e;
            }
          }
        }
      }
    }
    return null;
  }

  public static Element getRoot(Element element) {
    Element result = null;
    while (element != null) {
      result = element;
      element = element.getParentElement();
    }
    return result;
  }

  @Nullable
  public static Element getAncestor(@NotNull Content content, @NotNull String name) {
    return getAncestor(content, name, Integer.MAX_VALUE);
  }

  public static Element getAncestor(@NotNull Content content, @NotNull final String name, int maxDistance) {
    final Condition<Element> c = new Condition<Element>() {
      @Override
      public boolean isAccepted(Element value) {
        return name.equalsIgnoreCase(value.getName());
      }
    };
    return getAncestor(content, c, maxDistance);
  }

  @Nullable
  public static Element getAncestor(@NotNull Content content, @NotNull Condition<Element> predicate, int maxDistance) {
    Element element = content.getParentElement();
    int distance = 0;
    while(element != null && distance < maxDistance) {
      if(predicate.isAccepted(element)) {
        return element;
      }
      element = element.getParentElement();
      distance++;
    }
    return null;
  }

  @Nullable
  public static Element getPrecedingSibling(Element element) {
    if (element == null)
      return null;
    Parent parent = element.getParent();
    if (parent == null)
      return null;
    int i = parent.indexOf(element);
    if (i < 0) {
      assert false : parent + " " + element;
      return null;
    }
    while (i > 0) {
      Content c = parent.getContent(--i);
      if (c instanceof Element)
        return (Element) c;
    }
    return null;
  }

  /**
   * Accepts path from the specified base {@link Element}, returns Iterable of all elements that have the specified relative path.
   * Example: for query {@code [root element], "a", "b"} elements with ids 1, 3, 8 will be returned.
   * <pre>
   * {@code
<root>
  <a id="0">
    <b id="1">
      <b id="2"></b>
    </b>
    <b id="3"></b>
    <c id="4"></c>
  </a>
  <b id="5"></b>
  <c id="6"></c>
  <a id="7">
    <b id="8"></b>
  </a>
</root>
  }
   * </pre>
   * */
  public static Iterable<Element> queryPath(Element base, String... path) {
    if (path == null || path.length == 0) return Collections.EMPTY_LIST;
    final String[] tail = Arrays.copyOfRange(path, 1, path.length);
    Iterable<Element> directChildren = base.getChildren(path[0]);
    return tail.length == 0 ? directChildren : selectMany(directChildren, new Convertor<Element, Iterable<Element>>() {
      @Override
      public Iterable<Element> convert(Element element) {
        return queryPath(element, tail);
      }
    });
  }
}

