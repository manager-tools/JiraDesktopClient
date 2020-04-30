package com.almworks.util.collections;

import com.almworks.util.commons.Condition2;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;

import java.util.Arrays;
import java.util.List;

public class ListPatchTests extends BaseTestCase {
  private ListPatch<String, String> myPatch;
  private List<String> mySubject;
  private List<String> myGoal;
  private CollectionsCompare myCompare;
  private int myComparisonCount;

  protected void setUp() throws Exception {
    super.setUp();
    mySubject = Collections15.arrayList();
    myGoal = Collections15.arrayList();
    myPatch = ListPatch.create(mySubject, myGoal);
    myPatch.setPolicy(ListPatchPolicy.SINGLE_UNREPEATING_ITEMS);
    myPatch.setEquality(new Condition2<String, String>() {
      public boolean isAccepted(String value1, String value2) {
        myComparisonCount++;
        return value1.equals(value2);
      }
    });
    myCompare = new CollectionsCompare();
    myComparisonCount = 0;
  }

  protected void tearDown() throws Exception {
    mySubject = null;
    myGoal = null;
    myPatch = null;
    myCompare = null;
  }

  public void testSimple() {
    check("a,b,c", "a,b,c", 0);
    check("a,b,c", "a,b", 1);
    check("a,b,c", "a,b,c,d", 1);
  }

  public void testWorstCase() {
    check("a,b,c", "d,e,f", 6);
    check("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z");
  }

  public void testMoving() {
    // good - moving back and forward
    check("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", "a,b,y,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,z", 2);
    check("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", "a,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,b,w,x,y,z", 2);

    // bad - double move in different directions
    check("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", "a,b,d,e,y,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,z,c");
    check("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", "a,b,y,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,z,c");
  }

  public void testBoundary() {
    check("", "");
    check("", "a");
    check("", "a,b,c,d,e,f");
    check("a", "");
    check("a,b,c,d,e,f", "");
  }

  private void check(String subject, String goal) {
    check(subject, goal, Integer.MAX_VALUE);
  }

  private void check(String subject, String goal, int maxSteps) {
    myComparisonCount = 0;
    fill(mySubject, subject);
    fill(myGoal, goal);
    List<String> result = Collections15.arrayList(mySubject);
    List<ListPatchStep> steps = myPatch.generate();
    apply(result, myGoal, steps);
    myCompare.order(myGoal, result);
    int stepCount = steps.size();
    assertTrue("steps: " + stepCount + " > " + maxSteps, stepCount <= maxSteps);
    System.out.println(stepCount + " steps; " + myComparisonCount + " comparisons: (" + subject + ") => (" + goal + ")");
  }

  private void apply(List<String> subject, List<String> goal, List<ListPatchStep> steps) {
    for (ListPatchStep step : steps) {
      ListPatchStep.Action action = step.getAction();
      if (action == ListPatchStep.Action.ADD) {
        subject.add(step.getSubjectIndex(), goal.get(step.getGoalIndex()));
      } else if (action == ListPatchStep.Action.REMOVE) {
        subject.remove(step.getSubjectIndex());
      } else {
        assert false : action;
      }
    }
  }

  private void fill(List<String> target, String encoded) {
    target.clear();
    if (encoded.length() > 0) {
      String[] strings = encoded.split("\\s*,\\s*");
      target.addAll(Arrays.asList(strings));
    }
  }
}
