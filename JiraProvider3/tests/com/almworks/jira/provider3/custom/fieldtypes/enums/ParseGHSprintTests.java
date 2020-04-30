package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.util.tests.BaseTestCase;

public class ParseGHSprintTests extends BaseTestCase {
  public void testOldValues() {
    String value =
      "com.atlassian.greenhopper.service.sprint.Sprint@266fac23[name=Sprint 1,closed=false,startDate=2013-03-30T12:30:50.513-07:00,endDate=2013-04-13T12:30:50.513-07:00,completeDate=<null>,id=1]";
    checkSuccess("1", "Sprint 1", value);
  }

  public void testNewValues() {
    checkSuccess("28", "S6: Ship, Network, IPMI",
      "com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[rapidViewId=8,state=ACTIVE,name=S6: Ship, Network, IPMI,startDate=2013-10-07T14:31:04.432-04:00,endDate=2013-10-25T16:31:00.000-04:00,completeDate=<null>,id=28]");
  }

  public void testUnknownKeys() {
    checkSuccess("1", "abc", "com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[id=1,name=abc,YYY=qqq]");
    checkSuccess("1", "abc", "com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[id=1,name=abc,YYY=]");
  }

  public void testWrongValues() {
    checkFailed("");
    checkFailed("abc");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[]");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[name=]");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[name]");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[name=,a=]");
    checkFailed("com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[name=,state=,state=]");
  }

  public void testKeyNameInValue() {
    checkSuccess("3", "X(id=3)", "com.atlassian.greenhopper.service.sprint.Sprint@f94bcc[name=X(id=3),id=3]");
  }

  public void testBugReport() {
    checkSuccess("26", "Winterswijk sprint (id=26)", "com.atlassian.greenhopper.service.sprint.Sprint@6887a146[rapidViewId=1,state=ACTIVE,name=Winterswijk sprint (id=26),startDate=2013-10-09T16:37:57.484+02:00,endDate=2013-10-22T10:00:00.000+02:00,completeDate=<null>,id=26]");
  }

  private void checkSuccess(String id, String name, String value) {
    ParseGHSprint sprint = ParseGHSprint.perform(value);
    assertEquals(id, sprint.getId());
    assertEquals(name, sprint.getName());
  }

  private void checkFailed(String value) {
    ParseGHSprint sprint = ParseGHSprint.perform(value);
    assertTrue(sprint.getName() == null || sprint.getId() == null);
  }
}
