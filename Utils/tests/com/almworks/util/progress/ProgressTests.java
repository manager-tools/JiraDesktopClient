package com.almworks.util.progress;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;

public class ProgressTests extends BaseTestCase {
  private static final double EPS = 1e-6;
  private static final ProgressActivityFormat F = ProgressActivityFormat.DEFAULT;

  private ProgressActivityFormat myFormat;

  public ProgressTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myFormat = new ProgressActivityFormat();
  }

  protected void tearDown() throws Exception {
    myFormat = null;
    super.tearDown();
  }

  public void testBasic() {
    Progress p = new Progress();
    check(p, 0F, "");
    p.setProgress(0.1F);
    check(p, 0.1F, "");
    p.setProgress(0.1F, "haba");
    check(p, 0.1F, "haba");
    p.setProgress(0.9F);
    check(p, 0.9F, "haba");
    p.setActivity("joe");
    check(p, 0.9F, "joe");
    p.setProgress(1F, null);
    check(p, 1F, "");
  }

  public void testSub() {
    Progress p = new Progress();
    p.setProgress(0.5F);

    Progress sub = p.createDelegate(0.4F);
    check(p, 0.5F);
    check(sub, 0F);

    sub.setProgress(0.5F);
    check(p, 0.7F);
    check(sub, 0.5F);

    p.setProgress(0.6F);
    sub.setProgress(0.75F);
    check(p, 0.9F);

    Progress subsub = sub.createDelegate(0.25F);
    subsub.setProgress(0.5F);
    check(sub, 0.875F);
    check(p, 0.95F);

    subsub.setProgress(1F);
    check(sub, 1F);
    check(p, 1F);
  }

  public void testAggregating() {
    Progress p = new Progress();
    Progress[] subs = new Progress[10];
    for (int i = 0; i < subs.length; i++)
      subs[i] = p.createDelegate(1F / subs.length);

    check(p, 0F);

    int steps = 9;
    float step = 1F / steps;
    float overallStep = step / subs.length;
    for (int i = 0; i < steps; i++)  {
      for (int j = 0; j < subs.length; j++) {
        subs[j].setProgress((i + 1) * step);
        check(p, overallStep * (i * subs.length + j + 1));
      }
    }

    check(p, 1F);
  }

  public void testActivityFormat() {
    Progress p = new Progress();
    Progress sub1 = new Progress();
    Progress sub2 = new Progress();
    Progress sub1_sub1 = new Progress();
    Progress sub1_sub2 = new Progress();

    p.delegate(sub1, 0.5F);
    p.delegate(sub2, 0.5F);
    sub1.delegate(sub1_sub1, 0.5F);
    sub1.delegate(sub1_sub2, 0.5F);

    check(p, "");

    sub1_sub1.setActivity("sub1_sub1");
    check(p, "sub1_sub1");

    sub2.setActivity("sub2");
    check(p, "sub1_sub1, sub2");

    sub1.setActivity("sub1");
    check(p, "sub1 (sub1_sub1), sub2");

    sub1_sub2.setActivity("sub1_sub2");
    check(p, "sub1 (sub1_sub1, sub1_sub2), sub2");

    p.setActivity("p");
    check(p, "p (sub1 (sub1_sub1, sub1_sub2), sub2)");
  }

  private void check(Progress p, float progress, String activity) {
    assertEquals(progress, p.getProgress(), EPS);
    assertEquals(activity, F.format(p.getActivity()));
  }

  private void check(Progress p, String activity) {
    assertEquals(activity, F.format(p.getActivity()));
  }

  private void check(Progress p, float progress) {
    assertEquals(progress, p.getProgress(), EPS);
  }
}
