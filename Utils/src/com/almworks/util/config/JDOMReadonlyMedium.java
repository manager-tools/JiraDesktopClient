package com.almworks.util.config;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.filter.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
class JDOMReadonlyMedium <M extends ReadonlyMedium> implements ReadonlyMedium<M> {
  private final JDOMSubMedium<String> mySettings;
  private final JDOMSubMedium<M> mySubsets;

  protected JDOMReadonlyMedium(Element element, Convertor<Element, M> subsetConvertor) {
    mySettings = new JDOMSubMedium<String>(GET_TEXT, IS_SETTING, element);
    mySubsets = new JDOMSubMedium<M>(subsetConvertor, IS_SETTING.not(), element);
  }

  public static ReadonlyMedium createReadonly(Element element) {
    return new JDOMReadonlyMedium(element, CREATE_RO_MEDIUM);
  }

  public static ReadonlyMedium createReadonly(InputStream stream) throws IOException, JDOMException {
    return createReadonly(JDOMUtils.createBuilder().build(stream).getRootElement());
  }

  public static ReadonlyMedium createReadonly(Reader reader) throws IOException, JDOMException {
    return createReadonly(JDOMUtils.createBuilder().build(reader).getRootElement());
  }

  public JDOMSubMedium<String> getSettings() {
    return mySettings;
  }

  public JDOMSubMedium<M> getSubsets() {
    return mySubsets;
  }

  public String getName() {
    return GET_NAME.convert(getElement());
  }

  protected final Element getElement() {
    return mySettings.myElement;
  }

  private final static Convertor<Element, String> GET_TEXT = new Convertor<Element, String>() {
    public String convert(Element value) {
      return value.getText();
    }
  };

  private final static Convertor<Element, ReadonlyMedium> CREATE_RO_MEDIUM = new Convertor<Element, ReadonlyMedium>() {
    public ReadonlyMedium convert(Element value) {
      return new JDOMReadonlyMedium(value, CREATE_RO_MEDIUM);
    }
  };

  protected static final String EMPTY_SETTING = "empty";
  private final static Condition<Element> IS_SETTING = new Condition<Element>() {
    public boolean isAccepted(Element value) {
      if (value.getAttribute(EMPTY_SETTING) != null)
        return true;
      if (value.getContentSize() == 0)
        return false;
      Content first = value.getContent(0);
      if (first instanceof Text) {
        return ((Text) first).getTextTrim().length() > 0;
      } else {
        return false;
      }
    }
  };

  private final static Convertor<Element, String> GET_NAME = new Convertor<Element, String>() {
    public String convert(Element value) {
      return XMLWriter.decode(value.getName());
    }
  };


  protected static class JDOMSubMedium <T> implements SubMedium<T> {
    private final Convertor<Element, T> myConvertor;
    private final Condition<Element> myFilter;
    private final Element myElement;

    protected JDOMSubMedium(Convertor<Element, T> convertor, Condition<Element> filter, Element element) {
      myConvertor = convertor;
      myFilter = filter;
      myElement = element;
    }

    public T get(String name) {
      Iterator<Element> iterator = getChildren(name).iterator();
      return iterator.hasNext() ? myConvertor.convert(iterator.next()) : null;
    }

    public boolean isSet(String name) {
      final String elementName = name != null ? XMLWriter.encode(name) : null;
      List<Element> children = name == null ? myElement.getChildren() : myElement.getChildren(elementName);
      for (Element child : children) {
        if (myFilter.isAccepted(child)) {
          return true;
        }
      }
      return false;
    }

    public List<T> getAll(String name) {
      return getAll(name, null);
    }

    public List<T> getAll(String name, List<T> buffer) {
      boolean elementNameGathered = false;
      String elementName = null;
      int size = myElement.getContentSize();
      List<T> result = buffer;
      if (result != null) {
        result.clear();
      }
      for (int i = 0; i < size; i++) {
        Content content = myElement.getContent(i);
        if (!(content instanceof Element))
          continue;
        Element child = ((Element) content);
        if (!elementNameGathered) {
          elementName = name != null ? XMLWriter.encode(name) : null;
          elementNameGathered = true;
        }
        if (elementName != null && !elementName.equals(child.getName()))
          continue;
        if (!myFilter.isAccepted(child))
          continue;
        if (result == null)
          result = Collections15.arrayList();
        result.add(myConvertor.convert(child));
      }
      return result == null ? Collections15.<T>emptyList() : result;
    }

    public List<T> getAll() {
      return getAll(null, null);
    }

    public Collection<String> getAllNames() {
      List<Element> children = getChildren(null);
      return GET_NAME.collectOrderedSet(children);
    }

    List<Element> getChildren(String unencodedName) {
      final String elementName = unencodedName != null ? XMLWriter.encode(unencodedName) : null;
      return myElement.getContent(new Filter() {
        public boolean matches(Object obj) {
          if (!(obj instanceof Element))
            return false;
          Element element = (Element) obj;
          if (!myFilter.isAccepted(element))
            return false;
          if (elementName == null)
            return true;
          return elementName.equals(element.getName());
        }
      });
    }
  }
}
