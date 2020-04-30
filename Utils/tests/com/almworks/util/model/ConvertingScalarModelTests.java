package com.almworks.util.model;

import com.almworks.util.collections.Convertor2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Lifespan;

/**
 * @author Vasya
 */
public class ConvertingScalarModelTests extends BaseTestCase {
  private static final String SENTINEL = "Sentinel";
  private BasicScalarModel<String> myFirstModel;
  private BasicScalarModel<String> mySecondModel;
  private ConvertingScalarModel<String, String, String> myConvertingScalarModel;
  private Convertor2<String, String, String> myConvertor;

  protected void setUp() throws Exception {
    super.setUp();
    myFirstModel = BasicScalarModel.createWithValue("x", true);
    mySecondModel = BasicScalarModel.createWithValue("y", false);
    myConvertor = new Convertor2<String, String, String>() {
      public String convert(String first, String second) {
        return first + "_" + second;
      }
    };
  }

  protected void tearDown() throws Exception {
    myFirstModel = null;
    mySecondModel = null;
    myConvertor = null;
    myConvertingScalarModel = null;
    super.tearDown();
  }

  public void testBasicContracts() throws InterruptedException {
    myConvertingScalarModel = ConvertingScalarModel.createStraight(myFirstModel,
      mySecondModel,
      myConvertor);
    assertEquals("x_y", myConvertingScalarModel.getValue());
    myConvertingScalarModel = ConvertingScalarModel.create(myFirstModel,
      mySecondModel,
      myConvertor,
      ThreadGate.NEW_THREAD, true, SENTINEL);
    assertEquals("x_y", myConvertingScalarModel.getValueBlocking());
  }

  public void testListeners() {
    myConvertingScalarModel = ConvertingScalarModel.createStraight(myFirstModel, mySecondModel, myConvertor);
    myFirstModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        assertTrue(event.getSource() == myFirstModel);
      }
    });
    myConvertingScalarModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter() {
      public void onScalarChanged(ScalarModelEvent event) {
        assertTrue(event.getSource() == myConvertingScalarModel);
      }
    });
    myFirstModel.commitValue(myFirstModel.getValue(), "z");
    assertEquals("z_y", myConvertingScalarModel.getValue());
  }

  public void testSelfListeners() {
    final IllegalStateException[] thrown = {null};

    myConvertingScalarModel = new ConvertingScalarModel(myFirstModel, mySecondModel, myConvertor,
      ThreadGate.STRAIGHT, false, null) {
      public boolean doSetValue(Object o, Object o1, boolean checkOldValue) {
        try {
          return super.doSetValue(o, o1, checkOldValue);
        } catch (IllegalStateException e) {
          thrown[0] = e;
          return false;
        }
      }
    };

    myConvertingScalarModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter() {
      public void onScalarChanged(ScalarModelEvent event) {
        myFirstModel.setValue("new");
      }
    });

    assertNotNull(thrown[0]);
  }

  public void testEmpty() {
    myConvertingScalarModel =
      new ConvertingScalarModel(BasicScalarModel.create(), BasicScalarModel.create(), null, ThreadGate.STRAIGHT, false,
        "EMPTY");
    assertEquals("EMPTY", myConvertingScalarModel.getValue());
    myConvertingScalarModel =
      new ConvertingScalarModel(BasicScalarModel.create(), BasicScalarModel.create(), null, ThreadGate.STRAIGHT, true,
        "EMPTY");
    try {
      myConvertingScalarModel.getValue();
      fail("There is no value");
    } catch (NoValueException e) {
      //normal
    }
  }

  public void testAbsence() {
    myConvertingScalarModel = ConvertingScalarModel.createStraight(myFirstModel, mySecondModel, myConvertor);
    assertTrue(myConvertingScalarModel.hasFirstModel());
    assertTrue(myConvertingScalarModel.hasSecondModel());
    assertTrue(myConvertingScalarModel.isInternalContentKnown());
    try {
      myConvertingScalarModel.setFirstModel(BasicScalarModel.<String>create());
      fail("Allow to reset first model");
    } catch (IllegalStateException e) {
      //normal
    }
    try {
      myConvertingScalarModel.setSecondModel(BasicScalarModel.<String>create());
      fail("Allow to reset second model");
    } catch (IllegalStateException e) {
      //normal
    }
    myConvertingScalarModel = ConvertingScalarModel.createStraight(null, mySecondModel, myConvertor);
    assertFalse(myConvertingScalarModel.hasFirstModel());
    assertTrue(myConvertingScalarModel.hasSecondModel());
    assertFalse(myConvertingScalarModel.isInternalContentKnown());
    myConvertingScalarModel.setFirstModel(myFirstModel);
    assertTrue(myConvertingScalarModel.hasFirstModel());
    assertTrue(myConvertingScalarModel.hasSecondModel());
    assertTrue(myConvertingScalarModel.isInternalContentKnown());
    myConvertingScalarModel = ConvertingScalarModel.createStraight(myFirstModel, null, myConvertor);
    assertTrue(myConvertingScalarModel.hasFirstModel());
    assertFalse(myConvertingScalarModel.hasSecondModel());
    assertFalse(myConvertingScalarModel.isInternalContentKnown());
    myConvertingScalarModel.setSecondModel(mySecondModel);
    assertTrue(myConvertingScalarModel.hasFirstModel());
    assertTrue(myConvertingScalarModel.hasSecondModel());
    assertTrue(myConvertingScalarModel.isInternalContentKnown());
  }
}
