package com.almworks.util.model;

import com.almworks.util.collections.Convertor2;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;

/**
 * #getValue() returns value, obtained from pair of ScalarModel by Converter's converting
 *
 * @author Vasya
 */
public class ConvertingScalarModel<S1, S2, R> extends BasicScalarModel<R> {
  private ScalarModel<S1> myFirstModel;
  private ScalarModel<S2> mySecondModel;
  private final Convertor2<S1, S2, R> myConvertor;
  private final ThreadGate myConvertingGate;
//  private final Detach.Composite myDetach = new Detach.Composite();
  private Detach myFirstDetach, mySecondDetach;

  private boolean myValuesRequested = false;
  private long myVersionPending = 0;
  private long myVersionCommitted = 0;

  ConvertingScalarModel(ScalarModel<S1> firstModel, ScalarModel<S2> secondModel, Convertor2<S1, S2, R> convertor,
    ThreadGate convertingGate, boolean throwIfNoValue, R emptySentinel)
  {
    super(null, true, throwIfNoValue, emptySentinel, false);
    myConvertor = convertor;
    myConvertingGate = convertingGate;
    if (firstModel != null)
      setFirstModel(firstModel);
    if (secondModel != null)
      setSecondModel(secondModel);
    requestContent();
  }

  public static <S1, S2, R> ConvertingScalarModel<S1, S2, R> create(ScalarModel<S1> firstModel,
    ScalarModel<S2> secondModel, Convertor2<S1, S2, R> convertor, ThreadGate convetringGate, boolean throwIfNoValue,
    R emptySentinel)
  {
    return new ConvertingScalarModel<S1, S2, R>(firstModel, secondModel, convertor, convetringGate, throwIfNoValue,
      emptySentinel);
  }

  public static <S1, S2, R> ConvertingScalarModel<S1, S2, R> create(ScalarModel<S1> firstModel,
    ScalarModel<S2> secondModel, Convertor2<S1, S2, R> convertor, ThreadGate convertingGate)
  {
    return create(firstModel, secondModel, convertor, convertingGate, false, null);
  }

  public static <S1, S2, R> ConvertingScalarModel<S1, S2, R> createLong(ScalarModel<S1> firstModel,
    ScalarModel<S2> secondModel, Convertor2<S1, S2, R> convertor, Object sequenceKey)
  {
    return create(firstModel, secondModel, convertor, ThreadGate.LONG(sequenceKey));
  }

  public static <S1, S2, R> ConvertingScalarModel<S1, S2, R> createStraight(ScalarModel<S1> firstModel,
    ScalarModel<S2> secondModel, Convertor2<S1, S2, R> convertor)
  {
    return create(firstModel, secondModel, convertor, ThreadGate.STRAIGHT);
  }

  public boolean commitValue(R r, R r1) throws ValueAlreadySetException {
    throw new UnsupportedOperationException();
  }

  public void setFirstModel(ScalarModel<S1> firstModel) {
    if (myFirstModel == null && firstModel != null) {
      myFirstModel = firstModel;
      myFirstDetach = myFirstModel.getEventSource().addListener(myConvertingGate, new ScalarModel.Adapter<S1>() {
        public void onScalarChanged(ScalarModelEvent<S1> event) {
          runConversion();
        }
      });
    } else {
      throw new IllegalStateException();
    }
  }

  public void setSecondModel(ScalarModel<S2> secondModel) {
    if (mySecondModel == null && secondModel != null) {
      mySecondModel = secondModel;
      mySecondDetach = mySecondModel.getEventSource().addListener(myConvertingGate, new ScalarModel.Adapter<S2>() {
        public void onScalarChanged(ScalarModelEvent<S2> event) {
          runConversion();
        }
      });
    } else {
      throw new IllegalStateException();
    }
  }

  public void setValue(R r) throws ValueAlreadySetException {
    throw new UnsupportedOperationException();
  }

  public void requestContent() {
    if (myValuesRequested)
      return;
    myValuesRequested = true;
  }

  public void dispose() {
    if (myFirstDetach != null)
      myFirstDetach.detach();
    if (mySecondDetach != null)
      mySecondDetach.detach();
  }

  private void runConversion() {
    if (myFirstModel == null || mySecondModel == null)
      return;
    final S1 firstValue;
    final S2 secondValue;
    final long version;
    final boolean contentKnown;

    synchronized (myLock) {
      version = ++myVersionPending;
      contentKnown = isInternalContentKnown();

      if (!contentKnown)
        return;
      firstValue = myFirstModel.getValue();
      secondValue = mySecondModel.getValue();
    }
    myConvertingGate.execute(new Runnable() {
      public void run() {
        final R result = myConvertor.convert(firstValue, secondValue);
        boolean okFlag;
        synchronized (myLock) {
          okFlag = version > myVersionCommitted;
          if (okFlag) {
            myVersionCommitted = version;
            okFlag = !Util.equals(getValueInternal(), result) || !isContentKnown();
          }
        }
        if (okFlag)
          ConvertingScalarModel.super.setValue(result);
      }
    });
  }

  public void setContentKnown() {
    throw new UnsupportedOperationException();
  }

  public boolean hasFirstModel() {
    return myFirstModel != null;
  }

  public boolean hasSecondModel() {
    return mySecondModel != null;
  }

  public boolean isInternalContentKnown() {
    return myFirstModel != null && mySecondModel != null && myFirstModel.isContentKnown() &&
      mySecondModel.isContentKnown();
  }
}
