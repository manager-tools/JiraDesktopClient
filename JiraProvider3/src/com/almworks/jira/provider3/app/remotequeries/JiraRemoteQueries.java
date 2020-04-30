package com.almworks.jira.provider3.app.remotequeries;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.RemoteQuery2;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.explorer.qbuilder.filter.DateAttribute;
import com.almworks.explorer.qbuilder.filter.DateUnit;
import com.almworks.explorer.qbuilder.filter.FilterGramma;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.util.Pair;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.FormatException;
import com.almworks.util.io.persist.LeafPersistable;
import com.almworks.util.io.persist.PersistableArrayList;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.*;
import org.almworks.util.detach.Lifespan;
import org.jdom.Element;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class JiraRemoteQueries {
  private final Map<String, FilterNodeBuilder> myStaticBuilders;

  private final BasicScalarModel<Collection<RemoteQuery2>> myModel = BasicScalarModel.createWithValue(null, true);
  private final JiraConnection3 myConnection;
  private final Store myStore;

  public JiraRemoteQueries(JiraConnection3 connection, Store store) {
    myConnection = connection;
    myStore = store;
    myStaticBuilders = initStaticBuilders();
  }

  public ScalarModel<Collection<RemoteQuery2>> getModel() {
    return myModel;
  }

  @CanBlock
  private void save() {
    Collection<RemoteQuery2> queries = myModel.getValue();
    if (queries == null)
      return;
    List<RemoteQuery2> saving = Collections15.arrayList(queries);
    PersistableArrayList<RemoteQuery2> persister = new PersistableArrayList(new RemoteQuery2Persister());
    persister.set(saving);
    StoreUtils.storePersistable(myStore, "*", persister);
    persister.clear();
  }

  @CanBlock
  private void load() {
    Collection<RemoteQuery2> value = myModel.getValue();
    if (value != null)
      return;
    PersistableArrayList<RemoteQuery2> persister = new PersistableArrayList(new RemoteQuery2Persister());
    boolean success = StoreUtils.restorePersistable(myStore, "*", persister);
    if (success) {
      List<RemoteQuery2> queries = persister.copy();
      for (Iterator<RemoteQuery2> ii = queries.iterator(); ii.hasNext();) {
        RemoteQuery2 q = ii.next();
        if (q == null)
          ii.remove();
      }
      myModel.commitValue(null, queries);
    }
  }

  //  private FilterNode buildEnumCustomFieldNode(String conditionName, List<Pair<String, Element>> conditions,
//    CustomFieldAttribute<?> attribute, ConstraintType type, ConstraintDescriptor descriptor)
//    throws BadFormatException, InterruptedException
//  {
//    List<String> values = Collections15.arrayList();
//    for (Pair<String, Element> condition : conditions) {
//      Element element = condition.getSecond();
//      String value = JDOMUtils.getAttributeValue(element, "value", "", true).trim();
//      if (value.length() > 0)
//        values.add(value);
//      List<Element> children = element.getChildren("value");
//      for (Element child : children) {
//        value = JDOMUtils.getTextTrim(child);
//        if (value.length() > 0)
//          values.add(value);
//      }
//    }
//    if (values.size() == 0)
//      throw new BadFormatException(conditionName);
//    List<JiraIssue.CustomFieldValue.Scalar> raw = Collections15.arrayList();
//    for (String value : values) {
//      raw.add(new JiraIssue.CustomFieldValue.Scalar(value, null));
//    }
//    JiraIssueCreator issueCreator = myConnection.getContext().getIssueCreator();
//    CustomFieldType<?> fieldType = attribute.getType();
//    Object value = fieldType.getDBValue(raw, issueCreator.getHelper(), attribute.getAttribute());
//    if (value == null) {
//      throw new BadFormatException(conditionName + " " + descriptor + " " + fieldType + " " + raw);
//    }
//    BaseEnumConstraintDescriptor enumDescriptor = (BaseEnumConstraintDescriptor) type;
//    List<ItemKey> list = Collections15.arrayList();
//    if (value instanceof ArtifactPointer) {
//      addKey(list, (ArtifactPointer) value, enumDescriptor, raw);
//    } else if (value instanceof ArtifactPointer[]) {
//      for (ArtifactPointer v : (ArtifactPointer[]) value) {
//        addKey(list, v, enumDescriptor, raw);
//      }
//    } else {
//      assert false : value;
//      Log.warn("unexpected: " + value);
//    }
//    if (list.size() == 0) {
//      throw new BadFormatException(conditionName + " " + descriptor + " " + fieldType);
//    }
//    PropertyMap props = new PropertyMap();
//    props.put(BaseEnumConstraintDescriptor.SUBSET, list);
//    return new ConstraintFilterNode(descriptor, props);
//  }

//  private void addKey(List<ItemKey> list, ArtifactPointer value, BaseEnumConstraintDescriptor descriptor,
//    List<JiraIssue.CustomFieldValue.Scalar> raw)
//  {
//    ResolvedItem key = descriptor.getItemKey(value);
//    if (key != null) {
//      list.add(new ItemKeyStub(key));
//    }
//  }

  private PropertyMap buildDateProps(List<Pair<String, Element>> conditions) {
    PropertyMap props = null;
    long min = Long.MIN_VALUE;
    long max = Long.MIN_VALUE;
    Boolean relative = null;
    for (Pair<String, Element> condition : conditions) {
      Element element = condition.getSecond();
      String nameAttribute = element.getAttributeValue("name");
      if (nameAttribute == null)
        continue;
      String conditionType = StringUtil.substringAfterLast(nameAttribute, ":");
      boolean rel = "relative".equalsIgnoreCase(conditionType);
      boolean abs = "absolute".equalsIgnoreCase(conditionType);
      if (!rel && !abs)
        continue;
      if (relative == null) {
        relative = rel;
      } else if (relative != rel) {
        continue;
      }

      long from = Util.toLong(element.getChildTextTrim(rel ? "previousOffset" : "fromDate"), Long.MIN_VALUE);
      long to = Util.toLong(element.getChildTextTrim(rel ? "nextOffset" : "toDate"), Long.MIN_VALUE);

      if (from != Long.MIN_VALUE) {
        min = min == Long.MIN_VALUE ? from : Math.min(min, from);
      }
      if (to != Long.MIN_VALUE) {
        max = max == Long.MIN_VALUE ? to : Math.max(max, to);
      }
    }

    if (relative != null && (min != Long.MIN_VALUE || max != Long.MIN_VALUE)) {
      DateUnit.DateValue before = null;
      DateUnit.DateValue after = null;
      if (relative) {
        if (min != Long.MIN_VALUE) {
          after = new DateUnit.RelativeDate((int) (min / Const.DAY), DateUnit.DAY, false);
        }
        if (max != Long.MIN_VALUE) {
          before = new DateUnit.RelativeDate((int) (max / Const.DAY), DateUnit.DAY, true);
        }
      } else {
        if (min != Long.MIN_VALUE) {
          after = new DateUnit.AbsoluteDate(new Date(min));
        }
        if (max != Long.MIN_VALUE) {
          before = new DateUnit.AbsoluteDate(new Date(max));
        }
      }
      if (before != null || after != null) {
        props = DateAttribute.createValues(before, after, null);
      }
    }
    return props;
  }


  /**
   * maps condition id (roughly equal to field id) to list of pairs (parameter class, element)
   */
  private Map<String, List<Pair<String, Element>>> buildDefinition(Element root) {
    Map<String, List<Pair<String, Element>>> definition = Collections15.linkedHashMap();
    List<Element> parameters = root.getChildren("parameter");
    for (Element parameter : parameters) {
      String parameterClass = parameter.getAttributeValue("class");
      List<Element> parameterChildren = parameter.getChildren();
      for (Element parameterChild : parameterChildren) {
        String name = parameterChild.getName();
        List<Pair<String, Element>> list = definition.get(name);
        if (list == null) {
          list = Collections15.arrayList();
          definition.put(name, list);
        }
        list.add(Pair.create(parameterClass, parameterChild));
      }
    }
    return definition;
  }

  public void start() {
    Engine engine = Context.require(Engine.class);
    engine.getConnectionManager().whenConnectionsLoaded(Lifespan.FOREVER, ThreadGate.LONG(JiraRemoteQueries.class), new Runnable() {
      public void run() {
        load();
      }
    });
  }


  private Map<String, FilterNodeBuilder> initStaticBuilders() {
    HashMap<String, FilterNodeBuilder> result = Collections15.hashMap();
    //todo uncomment JCO-747
//    result.put("projid", new EnumBuilder(issue.attrProject, md.artProject, md.artProject.myIntIdSearcher));
//    result.put("resolution", new EnumBuilder(issue.attrResolution, md.artResolution, md.artResolution.mySelfAccessor));
//    result.put("priority", new EnumBuilder(issue.attrPriority, md.artPriority, md.artPriority.mySelfAccessor));
//    result.put("status", new EnumBuilder(issue.attrStatus, md.artStatus, md.artStatus.mySelfAccessor));
//    result.put("fixfor", new EnumBuilder(issue.attrVersionsFixed, md.artVersion, md.artVersion.myIntIdSeacher));
//    result.put("version", new EnumBuilder(issue.attrVersions, md.artVersion, md.artVersion.myIntIdSeacher));
//    result.put("component", new EnumBuilder(issue.attrComponents, md.artComponent, md.artComponent.myIntIdSeacher));
//    result.put("type", new EnumBuilder(issue.attrType, md.artType, md.artType.mySelfAccessor));
//
//    result.put("issue_assignee", new UserBuilder(issue.attrAssignee, "unassigned"));
//    result.put("issue_author", new UserBuilder(issue.attrReporter, "issue_no_reporter"));
//
//    result.put("multifieldlucene", new TextFieldsBuilder());
//
//    result.put("duedate", new DateBuilder(issue.attrDue));
//    result.put("created", new DateBuilder(issue.attrCreated));
//    result.put("updated", new DateBuilder(issue.attrUpdated));

    return result;
  }

  private static class RemoteQuery2Persister extends LeafPersistable<RemoteQuery2> {
    private RemoteQuery2 myQuery;

    protected void doClear() {
      myQuery = null;
    }

    protected RemoteQuery2 doAccess() {
      return myQuery;
    }

    protected RemoteQuery2 doCopy() {
      return myQuery;
    }

    protected void doRestore(DataInput in) throws IOException, FormatException {
      myQuery = null;
      String id = CompactChar.readString(in);
      String name = CompactChar.readString(in);
      String formula = CompactChar.readString(in);
      if (id != null && name != null && formula != null) {
        try {
          FilterNode node = FilterGramma.parse(formula);
          if (node == null) {
            assert false : formula;
            Log.warn("null node: " + formula);
          } else {
            myQuery = new JiraRemoteQuery(id, name, node);
          }
        } catch (ParseException e) {
          Log.debug("cannot parse " + formula);
        }
      }
    }

    protected void doSet(RemoteQuery2 value) {
      myQuery = value;
    }

    protected void doStore(DataOutput out) throws IOException {
      if (myQuery == null) {
        assert false : this;
        return;
      }
      CompactChar.writeString(out, myQuery.getId());
      CompactChar.writeString(out, myQuery.getDisplayableName());
      FilterNode node = myQuery.getFilterNode();
      CompactChar.writeString(out, FormulaWriter.write(node));
    }
  }
}
