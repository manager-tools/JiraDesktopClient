package com.almworks.util.advmodel;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.List;

public class ImageBasedDecorator2Tests extends BaseTestCase {
  private OrderListModel<String> mySource;
  private String myPrefix;
  private ImageBasedDecorator2<String, String> myDecorator;
  private AListModel<String> myImage;
  private AListModel<String> mySorted;


  public ImageBasedDecorator2Tests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    mySource = OrderListModel.create();
    mySource.addAll("a", "b", "cd");
    myPrefix = "+";
  }

  protected void tearDown() throws Exception {
    mySource = null;
    myPrefix = null;
    myDecorator = null;
    myImage = null;
    mySorted = null;
    super.tearDown();
  }

  public void test() {
    initSimple();
    check("+a", "+b", "+c", "+d");

    AListLogger logger = new AListLogger();
    myImage.addListener(logger);

    mySource.addAll("", "", "x", "", "y", "01");
    logger.checkInsert(4, 4);
    check("+a", "+b", "+c", "+d", "+x", "+y", "+0", "+1");
    checkSorted("+0", "+1", "+a", "+b", "+c", "+d", "+x", "+y");

    mySource.removeRange(1, 5);
    logger.checkRemove(1, 4);
    check("+a", "+y", "+0", "+1");
    checkSorted("+0", "+1", "+a", "+y");
    // source: "a", "", "y", "01"

    mySource.sort(1, 3, String.CASE_INSENSITIVE_ORDER);
    // source: "a", "", "01", "y" 
    logger.checkLogSize(1);
    check("+a", "+0", "+1", "+y");
    checkSorted("+0", "+1", "+a", "+y");
    logger.clear();

    mySource.updateAt(1);
    logger.checkSilence();
    mySource.updateAt(2);
    logger.checkUpdate(1, 2);
    mySource.updateRange(0, 3);
    logger.checkUpdate(0, 4);

    myPrefix = "-";
    mySource.updateRange(0, 3);
    logger.clear();
    check("-a", "-0", "-y");
    checkSorted("-0", "-a", "-y");

    myPrefix = "";
    mySource.updateRange(0, 2);
    logger.checkLogSize(2);
    check("-y");
    checkSorted("-y");

    mySource.updateRange(3, 3);
    check();
    checkSorted();
  }

  private void initSimple() {
    init(new ImageBasedDecorator2<String, String>(mySource) {
      protected List<? extends String> createImage(String sourceItem, int sourceIndex, boolean update) {
        if (update) {
          if ("+".equals(myPrefix)) {
            return null;
          } else if ("-".equals(myPrefix)) {
            return Collections.singletonList(myPrefix + sourceItem.charAt(0));
          } else {
            return Collections15.emptyList();
          }
        } else {
          int length = sourceItem.length();
          if (length == 0)
            return null;
          List<String> result = Collections15.arrayList();
          for (int i = 0; i < length; i++)
            result.add(myPrefix + sourceItem.charAt(i));
          return result;
        }
      }
    });
  }

  private void init(ImageBasedDecorator2<String, String> decorator) {
    myDecorator = decorator;
    myDecorator.attach(Lifespan.FOREVER);
    myImage = myDecorator.getDecoratedImage();
    mySorted = SortedListDecorator.create(Lifespan.FOREVER, myImage, String.CASE_INSENSITIVE_ORDER);
  }

  private void check(String... values) {
    new CollectionsCompare().order(values, myImage.toList());
  }

  private void checkSorted(String... values) {
    new CollectionsCompare().order(values, mySorted.toList());
  }
}
