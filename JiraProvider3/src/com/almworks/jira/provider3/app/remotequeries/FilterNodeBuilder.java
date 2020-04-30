package com.almworks.jira.provider3.app.remotequeries;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.BadFormatException;
import com.almworks.util.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract class FilterNodeBuilder {
  @Nullable
  public abstract FilterNode createNode(List<Pair<String, Element>> conditions) throws BadFormatException;

//  public abstract class BaseEnumBuilder extends FilterNodeBuilder {
//    protected final TypeBuilder.BaseReferenceLink myLink;
//
//    public BaseEnumBuilder(TypeBuilder.BaseReferenceLink link) {
//      myLink = link;
//    }
//
//    public FilterNode createNode(List<Pair<String, Element>> conditions) throws BadFormatException {
//      BaseEnumConstraintDescriptor descriptor = myCommonMD.getEnumDescriptor(myLink);
//      List<ItemKey> values = Collections15.arrayList();
//      for (Pair<String, Element> condition : conditions) {
//        Element element = condition.getSecond();
//        collectValues(element, values);
//      }
//      if (values.size() == 0) {
//        throw new BadFormatException("no resolvable values: " + conditions);
//      }
//      PropertyMap props = new PropertyMap();
//      props.put(BaseEnumConstraintDescriptor.SUBSET, values);
//      return new ConstraintFilterNode(descriptor, props);
//    }
//
//    protected abstract void collectValues(Element element, List<ItemKey> values) throws BadFormatException;
//  }

//  private class EnumBuilder extends BaseEnumBuilder {
//    private final ArtifactStructure myStructure;
//    private final DBSearcher<Integer> mySearcher;
//
//    public EnumBuilder(TypeBuilder.BaseReferenceLink link, ArtifactStructure structure, DBSearcher<Integer> searcher) {
//      super(link);
//      myStructure = structure;
//      mySearcher = searcher;
//    }
//
//    protected void collectValues(Element element, List<ItemKey> values) {
//      List<Element> children = element.getChildren("value");
//      for (Element child : children) {
//        String text = JDOMUtils.getTextTrim(child);
//        int id = Util.toInt(text, Integer.MIN_VALUE);
//        if (id != Integer.MIN_VALUE) {
//          ArtifactPointer value;
//          if (id == -1)
//            value = myStructure.getMissing();
//          else
//            value = myConnection.getContext().getIssueCreator().find(id, mySearcher);
//          if (value != null) {
//            try {
//              ResolvedFactory factory = myStructure.getResolvedItemFactory();
//              ResolvedItem key = Context.require(NameResolver.class).getCache().getItemKey(value, factory);
//              values.add(new ItemKeyStub(key));
//            } catch (BadItemException e) {
//              Log.debug(e);
//            }
//          }
//        }
//      }
//    }
//  }

//  private class UserBuilder extends BaseEnumBuilder {
//    private final String myEmptyName;
//
//    public UserBuilder(TypeBuilder.BaseReferenceLink link, String emptyName) {
//      super(link);
//      myEmptyName = emptyName;
//    }
//
//    protected void collectValues(Element element, List<ItemKey> values) throws BadFormatException {
//      UserStructure userStructure = myCommonMD.artUser;
//      String value = element.getAttributeValue("value");
//      if (value != null) {
//        ResolvedItem key = null;
//        if (myEmptyName.equals(value)) {
//          key = userStructure.getUnassignedKey();
//        } else {
//          try {
//            ArtifactPointer artifact;
//            if ("issue_current_user".equals(value)) {
//              artifact = myConnection.getContext().getLoggedInUser();
//            } else {
//              artifact = userStructure.findOrCreateById(value, myConnection.getContext().getIssueCreator());
//            }
//            if (artifact != null) {
//              ResolvedFactory factory = userStructure.getResolvedItemFactory();
//              key = Context.require(NameResolver.class).getCache().getItemKey(artifact, factory);
//            }
//          } catch (InterruptedException e) {
//            throw new RuntimeInterruptedException(e);
//          } catch (BadItemException e) {
//            Log.debug(e);
//          }
//        }
//        if (key != null) {
//          values.add(new ItemKeyStub(key));
//        }
//      }
//    }
//  }

//  private class TextFieldsBuilder extends FilterNodeBuilder {
//    public FilterNode createNode(List<Pair<String, Element>> conditions) throws BadFormatException {
//      Set<String> words = Collections15.linkedHashSet();
//      Set<String> fields = Collections15.linkedHashSet();
//
//      for (Pair<String, Element> condition : conditions) {
//        Element element = condition.getSecond();
//        String query = element.getChildTextTrim("query");
//        if (query != null && query.length() > 0) {
//          List<String> w = extractLuceneWords(query);
//          if (w != null) {
//            words.addAll(w);
//          }
//        }
//        List<Element> fieldElements = element.getChildren("field");
//        if (fieldElements != null) {
//          for (Element fieldElement : fieldElements) {
//            String field = JDOMUtils.getTextTrim(fieldElement);
//            if (field != null && field.length() > 0) {
//              fields.add(Util.lower(field));
//            }
//          }
//        }
//      }
//
//      if (fields.size() == 0 || words.size() == 0)
//        return null;
//
//      PropertyMap map = TextAttribute.createValues(StringUtil.implode(words, " "), false);
//      List<FilterNode> fieldNodes = Collections15.arrayList(fields.size());
//
//      if (fields.contains("summary"))
//        fieldNodes.add(new ConstraintFilterNode(myCommonMD.getSummaryDescriptor(), map));
//      if (fields.contains("description"))
//        fieldNodes.add(new ConstraintFilterNode(myCommonMD.getDescriptionDescriptor(), map));
//      if (fields.contains("body") || fields.contains("comments"))
//        fieldNodes.add(new ConstraintFilterNode(myCommonMD.getCommentsDescriptor(), map));
//      if (fields.contains("environment"))
//        fieldNodes.add(new ConstraintFilterNode(myCommonMD.getEnvironmentDescriptor(), map));
//
//
//      if (fieldNodes.size() == 0)
//        return null;
//      else if (fieldNodes.size() == 1)
//        return fieldNodes.get(0);
//      else
//        return new BinaryCommutative.Or(fieldNodes);
//    }
//
//    private List<String> extractLuceneWords(String query) {
//      String[] words = query.split("\\s+");
//      List<String> result = Collections15.arrayList();
//      for (String word : words) {
//        if (word.length() == 0)
//          continue;
//        if ("AND".equals(word) || "OR".equals(word))
//          continue;
//        if (word.startsWith("+") && word.length() > 1)
//          word = word.substring(1);
//        result.add(Util.lower(word));
//      }
//      return result;
//    }
//  }

//  private class DateBuilder extends FilterNodeBuilder {
//    private final SingleValueDataLink<Date> myLink;
//
//    public DateBuilder(SingleValueDataLink<Date> link) {
//      myLink = link;
//    }
//
//    @Nullable
//    public FilterNode createNode(List<Pair<String, Element>> conditions) throws BadFormatException {
//      PropertyMap props = buildDateProps(conditions);
//      if (props == null)
//        return null;
//      ConstraintDescriptor descriptor = myCommonMD.getDateDescriptor(myLink);
//      if (descriptor == null)
//        throw new BadFormatException("no descriptor for " + myLink);
//      return new ConstraintFilterNode(descriptor, props);
//    }
//  }
}
