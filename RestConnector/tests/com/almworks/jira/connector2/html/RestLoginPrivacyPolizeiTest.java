package com.almworks.jira.connector2.html;

import com.almworks.util.tests.BaseTestCase;
import org.json.simple.JSONObject;

public class RestLoginPrivacyPolizeiTest extends BaseTestCase {
  public void test() {
    check("{\"username\":\"test\",\"password\":\"oct-2012\"}", "oct-2012");
    check("REQUEST (45 bytes) :\n{\"username\":\"test\",\"password\":\"oct-2012\"}\n---", "oct-2012");

    JSONObject test = new JSONObject();
    test.put("username", "password");
    test.put("password", "12345");
    check(test.toJSONString(), "12345");

    String hardPwd = "ab{\"\"\":e1";
    test.put("password", hardPwd);
    // JSONObject will escape quotes
    check(test.toJSONString(), hardPwd.replaceAll("\"", "\\\\\""));

    hardPwd = "\"password\":";
    test.put("password", hardPwd);
    // JSONObject will escape quotes
    check(test.toJSONString(), hardPwd.replaceAll("\"", "\\\\\""));

    test.put("password", "");
    String actual = RestLoginPrivacyPolizei.INSTANCE.examine(test.toJSONString());
//    assertEquals("{\"username\":\"password\",\"password\":\"***\"}", actual);
    assertTrue(actual, actual.contains("\"username\":\"password\""));
    assertTrue(actual, actual.contains("\"password\":\"***\""));
  }

  private static void check(String msg, String password) {
    assertEquals(msg, msg.replace(password, "***"), RestLoginPrivacyPolizei.INSTANCE.examine(msg));
  }
}
