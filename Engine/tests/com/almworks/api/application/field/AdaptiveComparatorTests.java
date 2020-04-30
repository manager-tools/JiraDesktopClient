package com.almworks.api.application.field;

import com.almworks.util.tests.BaseTestCase;

public class AdaptiveComparatorTests extends BaseTestCase {
  public void testIsNumber() {
    num("0");
    num(" 0");
    num("0 ");
    num(" 0 ");

    num("239");
    num(" 239");
    num("239 ");
    num(" 239 ");

    num("2.3");
    num(" 2.3");
    num("2.3 ");
    num(" 2.3 ");

    num("2.39");
    num(" 2.39");
    num("2.39 ");
    num(" 2.39 ");

    num("23.90");
    num(" 23.90");
    num("23.90 ");
    num(" 23.90 ");

    num("2.");
    num(" 2.");
    num("2. ");
    num(" 2. ");

    num(".2");
    num(" .2");
    num(".2 ");
    num(" .2 ");
    
    num("+0");
    num(" +0");
    num("+0 ");
    num(" +0 ");

    num("+239");
    num(" +239");
    num("+239 ");
    num(" +239 ");

    num("+2.3");
    num(" +2.3");
    num("+2.3 ");
    num(" +2.3 ");

    num("+2.39");
    num(" +2.39");
    num("+2.39 ");
    num(" +2.39 ");

    num("+23.90");
    num(" +23.90");
    num("+23.90 ");
    num(" +23.90 ");

    num("+2.");
    num(" +2.");
    num("+2. ");
    num(" +2. ");

    num("+.2");
    num(" +.2");
    num("+.2 ");
    num(" +.2 ");

    num("-0");
    num(" -0");
    num("-0 ");
    num(" -0 ");

    num("-239");
    num(" -239");
    num("-239 ");
    num(" -239 ");

    num("-2.3");
    num(" -2.3");
    num("-2.3 ");
    num(" -2.3 ");

    num("-2.39");
    num(" -2.39");
    num("-2.39 ");
    num(" -2.39 ");

    num("-23.90");
    num(" -23.90");
    num("-23.90 ");
    num(" -23.90 ");

    num("-2.");
    num(" -2.");
    num("-2. ");
    num(" -2. ");

    notNum("+");
    notNum(".");
    notNum(" +");
    notNum(" .");
    notNum("+.");
    notNum(".+");
    notNum("++");
    notNum("..");

    notNum("1 2");
    notNum("1a");
    notNum("1..2");
    notNum(".1.2");
    notNum("+1+2");
  }

  private void num(String s) {
    check(s, true);
  }

  private void notNum(String s) {
    check(s, false);
  }

  private void check(String s, boolean isNum) {
    assertEquals(isNum, AdaptiveComparator.isNumber(s));
  }
}
