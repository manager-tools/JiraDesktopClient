package com.almworks.syncreg;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldEqualsConstraint;
import com.almworks.api.constraint.FieldSubsetConstraint;
import com.almworks.api.engine.Engine;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Collections15;

import java.util.List;

public class ItemHypercubeUtilsTests extends BaseTestCase {
  private static final DBNamespace NS = Engine.NS.subNs("test.cube");
  private static DBAttribute<Long> A1 = NS.link("1", "1", false);
  private static DBAttribute<Long> A2 = NS.link("2", "2", false);
  private static DBAttribute<Long> A3 = NS.link("3", "3", false);

  private static final DBNamespace GEN_NS = NS.subNs("gen");

  /**
   * see #961
   */
  public void testBuildPreciseCubeWithOredDifferentAxes() {
    List<Constraint> anded = Collections15.arrayList();
    List<Constraint> ored = Collections15.arrayList();

    ored.add(new FieldEqualsConstraint.Simple(A2, 1002L));
    ored.add(new FieldEqualsConstraint.Simple(A3, 1003L));

    anded.add(new CompositeConstraint.Simple(CompositeConstraint.OR, ored));
    anded.add(new FieldEqualsConstraint.Simple(A1, 1001L));

    Constraint c = new CompositeConstraint.Simple(CompositeConstraint.AND, anded);
    ItemHypercubeImpl hypercube = ItemHypercubeUtils.getHypercube(c, true);
    assertNull(hypercube);
  }

  /**
   * see http://jira.almworks.com/browse/DZO-580
   */
  public void testMultipleConstraintsForSameAxis() {
    List<Constraint> anded = Collections15.arrayList();

    anded.add(FieldSubsetConstraint.Simple.intersection(A2, Collections15.arrayList(1002L, 1003L)));
    anded.add(new FieldEqualsConstraint.Simple(A2, 1003L));
    Constraint c = new CompositeConstraint.Simple(CompositeConstraint.AND, anded);

    ItemHypercubeImpl hypercube = ItemHypercubeUtils.getHypercube(c, true);
    assertEquals("cube(2+1003)", hypercube.toString());
  }

  public void testCubeIntersection() {
    ItemHypercubeImpl cube1;
    ItemHypercubeImpl cube2;

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setPlus(new ItemHypercubeImpl(), 2, 2001, 2002);
    assertEquals("cube(1+1001,1002; 2+2001,2002)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002; 2+2001,2002)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    assertEquals("cube(1+1001,1002)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1003);
    assertEquals("cube(1+1001)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002, 1003);
    assertEquals("cube(1+1001,1002)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    setPlus(cube1, 2, 2001, 2002, 2003);
    cube2 = setPlus(new ItemHypercubeImpl(), 2, 2001, 2002);
    setPlus(cube2, 3, 3001);
    assertEquals("cube(1+1001,1002; 2+2001,2002; 3+3001)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002; 2+2001,2002; 3+3001)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    setPlus(cube1, 2, 2001);
    cube2 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1003);
    setPlus(cube2, 2, 2002);
    assertEquals("cube(1+1001)", cube1.intersect(cube2, false).toString());
    assertNull(cube1.intersect(cube2, true));
  }

  public void testCubeIntersection2() {
    ItemHypercubeImpl cube1;
    ItemHypercubeImpl cube2;

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setMinus(new ItemHypercubeImpl(), 2, 2001, 2002);
    assertEquals("cube(1+1001,1002; 2-2001,2002)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002; 2-2001,2002)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setMinus(new ItemHypercubeImpl(), 1, 1003);
    assertEquals("cube(1+1001,1002-1003)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001,1002-1003)", cube1.intersect(cube2, true).toString());

    cube1 = setPlus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setMinus(new ItemHypercubeImpl(), 1, 1002);
    assertEquals("cube(1+1001-1002)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1+1001-1002)", cube1.intersect(cube2, true).toString());

    cube1 = setMinus(new ItemHypercubeImpl(), 1, 1001, 1002);
    cube2 = setMinus(new ItemHypercubeImpl(), 1, 1001, 1002, 1003);
    assertEquals("cube(1-1001,1002,1003)", cube1.intersect(cube2, false).toString());
    assertEquals("cube(1-1001,1002,1003)", cube1.intersect(cube2, true).toString());
  }

  private static DBAttribute<Long> gen(int i) {
    String id = String.valueOf(i);
    return GEN_NS.link(id, id, false);
  }

  public void testRemoveAxis() {
    ItemHypercubeImpl c = new ItemHypercubeImpl();
    c.removeAxis(gen(1));
    assertEquals("cube()", c.toString());
    c.removeAxis(null);
    assertEquals("cube()", c.toString());

    int C = 5;
    c = createAxes(C);
    for (int i = 1; i <= C; i++) {
      c.removeAxis(gen(i));
    }
    assertEquals("cube()", c.toString());
    c.removeAxis(gen(1));
    assertEquals("cube()", c.toString());

    c = createAxes(C);
    for (int i = C; i >= 1; i--) {
      c.removeAxis(gen(i));
    }
    assertEquals("cube()", c.toString());

    c = createAxes(11);
    for (int i = 0; i < 11; i++) {
      c.removeAxis(gen(5 + (i % 2 == 0 ? i / 2 + 1 : -(i / 2))));
    }
    assertEquals("cube()", c.toString());
  }

  private ItemHypercubeImpl createAxes(int n) {
    ItemHypercubeImpl c = new ItemHypercubeImpl();
    for (int i = 1; i <= n; i++)
      c = setPlus(c, i, 1000);
    return c;
  }

  private ItemHypercubeImpl setPlus(ItemHypercubeImpl hypercube, int axis, int ... values) {
    for (int value : values) {
      hypercube.addValue(gen(axis), Long.valueOf(value), true);
    }
    return hypercube;
  }

  private ItemHypercubeImpl setMinus(ItemHypercubeImpl hypercube, int axis, int ... values) {
    for (int value : values) {
      hypercube.addValue(gen(axis), Long.valueOf(value), false);
    }
    return hypercube;
  }


}
