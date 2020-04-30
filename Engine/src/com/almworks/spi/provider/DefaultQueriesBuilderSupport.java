package com.almworks.spi.provider;

import com.almworks.api.config.ConfigNames;
import com.almworks.util.English;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jdom.Element;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class DefaultQueriesBuilderSupport {
  protected static final Convertor<String, String> UPPERCASE = new Convertor<String, String>() {
    public String convert(String s) {
      return Util.upper(s);
    }
  };
  protected static final int MAX_DEPENDENT_SUBQUERIES = 33;

  public DefaultQueriesBuilderSupport() {
  }

  public static Element createQuery(Element parent, String queryName, String queryFormula) {
    Element result = new Element(ConfigNames.PRESET_QUERY);
    result.addContent(new Element("name").addContent(queryName));
    result.addContent(new Element("query").addContent(queryFormula));
    parent.addContent(result);
    return result;
  }

  protected static Element createDistributionQuery(Element parent, String queryName, String queryFormula) {
    Element result = new Element(ConfigNames.PRESET_DISTRIBUTION_QUERY);
    result.addContent(new Element("name").addContent(queryName));
    result.addContent(new Element("query").addContent(queryFormula));
    parent.addContent(result);
    return result;
  }

  protected static List<Element> createDistribution(Element parent, Collection<String> values, String formulaAttribute,
    String name, boolean hideEmpty)
  {
    Element node = createDistributionNode(parent, name, formulaAttribute, hideEmpty);
    if (values != null) {
      List<Element> result = Collections15.arrayList(values.size());
      for (String value : values) {
        result.add(createDistributionQuery(node, value, formulaAttribute + " in (\"" + value + "\")"));
      }
      return result;
    } else {
      return null;
    }
  }

  public static Element createDistributionNode(Element parent, String name, String attributeId, boolean hideEmpty) {
    return createDistributionNodeInternal(ConfigNames.PRESET_DISTRIBUTION_FOLDER, parent, attributeId, name, hideEmpty);
  }

  private static Element createDistributionNodeInternal(String tag, Element parent, String attributeId, String name,
    boolean hideEmpty) {
    Element folder = new Element(tag);
    folder.addContent(new Element("name").addContent(name));
    folder.addContent(new Element("attributeId").addContent(attributeId));
    if (hideEmpty != emptyChildrenHidden(parent))
      folder.addContent(new Element("hideEmptyChildren").addContent(Boolean.toString(hideEmpty)));
    parent.addContent(folder);
    return folder;
  }

  protected static Element createLazyDistribution(Element parent, String attributeId, boolean hideEmpty) {
    if (parent == null)
      return null;
    String name = figureName(attributeId);
    return createLazyDistribution(parent, attributeId, name, hideEmpty);
  }

  protected static Element createLazyDistribution(Element parent, String attributeId, String name, boolean hideEmpty) {
    Element node = createDistributionNodeInternal(ConfigNames.PRESET_LAZY_DISTRIBUTION_QUERY, parent, attributeId, name, hideEmpty);
    Element prototype = new Element(ConfigNames.PROTOTYPE_TAG);
    node.addContent(prototype);
    return prototype;
  }

  private static String figureName(String attributeId) {
    String[] words = attributeId.split("[\\s_]+");
    StringBuffer result = new StringBuffer();
    for (String word : words) {
      if (result.length() > 0)
        result.append(' ');
      result.append(English.capitalize(word));
    }
    return result.toString();
  }


  private static boolean emptyChildrenHidden(Element parent) {
    while (parent != null) {
      String value = parent.getChildTextTrim("hideEmptyChildren");
      if ("true".equals(value))
        return true;
      if ("false".equals(value))
        return false;
      parent = parent.getParentElement();
    }
    return false;
  }

  protected static List<Element> createQueries(Element parent, Collection<String> values, String formulaAttribute) {
    List<Element> result = Collections15.arrayList(values.size());
    for (Iterator<String> ii = values.iterator(); ii.hasNext();) {
      String value = ii.next();
      result.add(createQuery(parent, value, formulaAttribute + " in (\"" + value + "\")"));
    }
    return result;
  }

  protected static String stackValues(List<String> values) {
    StringBuffer r = new StringBuffer();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0)
        r.append(' ');
      r.append('\"').append(values.get(i)).append('\"');
    }
    return r.toString();
  }

  public static Element createFolder(Element parent, String name) {
    Element folder = new Element(ConfigNames.PRESET_FOLDER).addContent(new Element("name").addContent(name));
    parent.addContent(folder);
    return folder;
  }
}
