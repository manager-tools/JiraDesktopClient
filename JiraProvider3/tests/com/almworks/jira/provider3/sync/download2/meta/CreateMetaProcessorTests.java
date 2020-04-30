package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.jira.provider3.sync.download2.TestResources;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.util.Trio;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CreateMetaProcessorTests extends BaseTestCase {
  private static final TestResources RESOURCES = TestResources.create(CreateMetaProcessorTests.class, "com/almworks/jira/provider3/sync/download2/meta/");

  public void test() throws IOException, ParseException {
    FieldsCollector fields = new FieldsCollector();
    LocationHandler handler = fields.createMetaHandler();
    RESOURCES.parseJsonResource("createmeta.json", handler);
    RESOURCES.assertTextEquals("createmeta.Result.txt", fields.printCollected());
  }

  private static class FieldsCollector extends CreateMetaFields {
    private final List<Trio<Integer, Integer, String>> myFields = Collections15.arrayList();

    @Override
    protected void addField(int projectId, int typeId, String id, JSONObject field) {
      String name = JRField.NAME.getValue(field);
      myFields.add(Trio.create(projectId, typeId, id + ":" + name));
    }


    public String printCollected() {
      Collections.sort(myFields, new Comparator<Trio<Integer, Integer, String>>() {
        @Override
        public int compare(Trio<Integer, Integer, String> o1, Trio<Integer, Integer, String> o2) {
          int diff = o1.getFirst().compareTo(o2.getFirst());
          if (diff == 0) diff = o1.getSecond().compareTo(o2.getSecond());
          if (diff == 0) diff = o1.getThird().compareTo(o2.getThird());
          return diff;
        }
      });
      int prj = -1;
      int type = -1;
      StringBuilder result = new StringBuilder();
      for (Trio<Integer, Integer, String> trio : myFields) {
        int p = trio.getFirst();
        int t = trio.getSecond();
        if (prj != p || type != t) {
          result.append(p).append(":").append(t).append("\n");
          prj = p;
          type = t;
        }
        result.append("  ").append(trio.getThird()).append("\n");
      }
      return result.toString();
    }
  }
}
