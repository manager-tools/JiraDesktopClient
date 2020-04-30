package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.*;
import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldIntConstraint;
import com.almworks.api.constraint.IsEmptyConstraint;
import com.almworks.explorer.qbuilder.constraints.AbstractConstraintEditor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPCompare;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.TextUtil;
import com.almworks.util.text.parser.*;
import com.almworks.util.ui.ComponentEnabler;
import com.almworks.util.ui.ComponentKeyBinder;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import java.math.BigDecimal;
import java.util.Map;

import static com.almworks.explorer.qbuilder.filter.EmptyQueryHelper.PK_EMPTY;
import static com.almworks.explorer.qbuilder.filter.EmptyQueryHelper.isEmptyOption;

/**
 * @author : Dyoma
 */
public class NumericAttribute<N extends Number & Comparable> implements AttributeConstraintType<N> {
  public static final AttributeConstraintType INSTANCE = new NumericAttribute();

  protected static final TypedKey<String> LOWER_VALUE = TypedKey.create("lower");
  protected static final TypedKey<String> UPPER_VALUE = TypedKey.create("upper");
  protected static final TypedKey<String> EQUALS_TO_VALUE = TypedKey.create("equalsTo");

  private static final String EQ_STRING = " = ";
  private static final String LE_STRING = " \u2264 ";
  private static final String GE_STRING = " \u2265 ";

  public NumericAttribute() {}

  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return new MyConstraintEditor(node);
  }

  public void writeFormula(FormulaWriter writer, String conditionId, PropertyMap data) {
    final String lower = data.get(LOWER_VALUE);
    final String upper = data.get(UPPER_VALUE);
    if(lower != null && upper != null) {
      writer.addToken(lower);
      writer.addRaw(" < ");
      writer.addToken(conditionId);
      writer.addRaw(" < ");
      writer.addToken(upper);
    } else if(lower != null) {
      writer.addToken(conditionId);
      writer.addRaw(" > ");
      writer.addToken(lower);
    } else if(upper != null) {
      writer.addToken(conditionId);
      writer.addRaw(" < ");
      writer.addToken(upper);
    } else if(isEmptyOption(data)) {
      writer.addToken(conditionId);
      writer.addRaw(" numberIs empty");
    } else {
      final String equalTo = data.get(EQUALS_TO_VALUE);
      writer.addToken(conditionId);
      writer.addRaw(" = ");
      if(equalTo != null) {
        writer.addToken(equalTo);
      }
    }
  }

  @Override
  @Nullable
  public BoolExpr<DP> createFilter(DBAttribute<N> attribute, PropertyMap data) {
    if(isEmptyOption(data)) {
      return DPNotNull.create(attribute).negate();
    }
    final BoolExpr<DP> expr = createNumericFilter(data, attribute);
    return expr != null ? DPNotNull.create(attribute).and(expr) : null;
  }

  @Nullable
  private BoolExpr<DP> createNumericFilter(PropertyMap data, DBAttribute<N> attribute) {
    BigDecimal equals = getIntValue(data, EQUALS_TO_VALUE);
    if (equals != null)
      return DPEquals.create(attribute, toDBValue(attribute, equals));
    BigDecimal lower = getIntValue(data, LOWER_VALUE);
    BigDecimal upper = getIntValue(data, UPPER_VALUE);
    BoolExpr<DP> ge = lower == null ? null : DPCompare.greaterOrEqual(attribute, toDBValue(attribute, lower), false);
    BoolExpr<DP> le = upper == null ? null : DPCompare.lessOrEqual(attribute, toDBValue(attribute, upper), false);
    if (le != null && ge != null) {
      return BoolExpr.and(le, ge);
    } else if (le != null) {
      return le;
    } else if (ge != null) {
      return ge;
    }
    return null;
  }

  @SuppressWarnings({"unchecked", "RedundantCast"})
  private <T extends Number & Comparable> T toDBValue(DBAttribute<T> attribute, BigDecimal value) {
    if (attribute == null) {
      Log.error("Null attribute");
      return null;
    }
    if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) {
      Log.error("Wrong composition " + attribute);
      return null;
    }
    if (value == null) {
      Log.error("Null value for attribute " + attribute);
      return null;
    }
    Class<?> clazz = attribute.getScalarClass();
    if (clazz == BigDecimal.class) return (T) value;
    if (clazz == Long.class)
      try {
        return (T) (Object)value.longValueExact();
      } catch (ArithmeticException e) {
        Log.error("Cannot convert to long " + value, e);
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) return (T) (Object) Long.MAX_VALUE;
        if (value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) < 0) return (T) (Object) Long.MIN_VALUE;
        return (T) (Object) value.longValue();
      }
    if (clazz == Integer.class)
      try {
        return (T) (Object)value.intValueExact();
      } catch (ArithmeticException e) {
        Log.error("Cannot convert to long " + value, e);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) return (T) (Object) Integer.MAX_VALUE;
        if (value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) return (T) (Object) Integer.MIN_VALUE;
        return (T) (Object) value.intValue();
      }
    Log.error("Unsupported scalar class " + attribute);
    return null;
  }

  protected static BigDecimal getIntValue(PropertyMap data, TypedKey<String> valueKey) {
    final String string = data.get(valueKey);
    try {
      return string != null ? TextUtil.parseBigDecimal(string) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  @Nullable
  public Constraint createConstraint(DBAttribute<N> attribute, PropertyMap data) {
    final BigDecimal equals = getIntValue(data, EQUALS_TO_VALUE);
    if(equals != null) {
      return FieldIntConstraint.Simple.equals(attribute, equals);
    }

    if(isEmptyOption(data)) {
      return IsEmptyConstraint.Simple.isEmpty(attribute);
    }

    final BigDecimal lower = getIntValue(data, LOWER_VALUE);
    final Constraint lowerConstraint = lower != null ? FieldIntConstraint.Simple.greaterOrEqual(attribute, lower) : null;

    final BigDecimal upper = getIntValue(data, UPPER_VALUE);
    final Constraint upperConstraint = upper != null ? FieldIntConstraint.Simple.lessOrEqual(attribute, upper) : null;

    if(lowerConstraint == null) {
      return upperConstraint;
    }
    return upperConstraint != null ? CompositeConstraint.Simple.and(lowerConstraint, upperConstraint) : lowerConstraint;
  }

  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return false;
  }

  public PropertyMap getEditorData(PropertyMap data) {
    return MyConstraintEditor.convertToEditorData(data);
  }

  public Icon getDescriptorIcon() {
    return Icons.QUERY_CONDITION_INT_ATTR;
  }

  @Nullable
  public String suggestName(String descriptorName, PropertyMap data, Map<TypedKey<?>, ?> hints) throws CannotSuggestNameException {
    final String equal = data.get(EQUALS_TO_VALUE);
    if(equal != null) {
      return descriptorName + EQ_STRING + equal;
    }

    if(isEmptyOption(data)) {
      return descriptorName + " is empty";
    }

    final String lower = data.get(LOWER_VALUE);
    final String upper = data.get(UPPER_VALUE);
    if(lower == null && upper == null) {
      return null;
    } else if(lower != null && upper != null) {
      return lower + LE_STRING + descriptorName + LE_STRING + upper;
    } else if(lower != null) {
      return descriptorName + GE_STRING + lower;
    } else {
      return descriptorName + LE_STRING + upper;
    }
  }

  private static ConstraintDescriptor descriptorStub(String id) {
    return ConstraintDescriptorProxy.stub(id, INSTANCE);
  }

  public static void register(TokenRegistry<FilterNode> registry) {
    registry.registerInfixConstraint("=", new InfixParser<FilterNode>() {
      @Override
      public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
        return new ConstraintFilterNode(descriptorStub(left.getSingle()), createEqualsValues(right.getSingleOrNull()));
      }
    });

    registry.registerInfixConstraint(">", new InfixParser<FilterNode>() {
      @Override
      public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
        return new ConstraintFilterNode(descriptorStub(left.getSingle()), createRangeValues(right.getSingleOrNull(), null));
      }
    });

    new CommutativeParser.Greater<FilterNode>("<") {
      @Override
      protected FilterNode createOneBound(String arg, String upperBound) {
        return new ConstraintFilterNode(descriptorStub(arg), createRangeValues(null, upperBound));
      }
      @Override
      protected FilterNode createTwoBound(String lowerBound, String arg, String upperBound) {
        return new ConstraintFilterNode(descriptorStub(arg), createRangeValues(lowerBound, upperBound));
      }
    }.register(registry);

    EmptyQueryHelper.registerEmptyParser(registry, "numberIs", INSTANCE);
  }

  private static class MyConstraintEditor extends AbstractConstraintEditor {
    private static final BooleanPropertyKey EQUALS = BooleanPropertyKey.createKey("equals", true);
    private static final PropertyKey.EnablingKey<Document, String> EQUALS_TO =
      PropertyKey.createEnablingText("equalsTo", ConstraintEditorNodeImpl.TEXT, EQUALS);

    private static final BooleanPropertyKey IN_RANGE = BooleanPropertyKey.createKey("inRange", false);

    public static final BooleanPropertyKey GREATER = BooleanPropertyKey.createKey("greater", false);
    public static final PropertyKey.EnablingKey<Document, String> LOWER_BOUND =
      PropertyKey.createEnablingText("lowerBound", ConstraintEditorNodeImpl.TEXT, GREATER);

    public static final BooleanPropertyKey LESS = BooleanPropertyKey.createKey("less", false);
    public static final PropertyKey.EnablingKey<Document, String> UPPER_BOUND =
      PropertyKey.createEnablingText("upperBound", ConstraintEditorNodeImpl.TEXT, LESS);

    private final Form myForm = new Form();

    public MyConstraintEditor(ConstraintEditorNodeImpl node) {
      super(node);
      setupBinding();
      selectInitialOption();
    }

    private void setupBinding() {
      final ComponentKeyBinder binder = getBinder();
      binder.setConditionalText(EQUALS_TO, myForm.myEquals, myForm.myEqualsTo);
      binder.setConditionalText(LOWER_BOUND, myForm.myGreater, myForm.myGreaterThan);
      binder.setConditionalText(UPPER_BOUND, myForm.myLess, myForm.myLessThan);
      binder.setBoolean(IN_RANGE, myForm.myInRange);
      binder.setBoolean(PK_EMPTY, myForm.myIsEmpty);
    }

    private void selectInitialOption() {
      final JToggleButton selectedOption;
      if(isEquals()) {
        selectedOption = myForm.myEquals;
      } else if(isEmpty()) {
        selectedOption = myForm.myIsEmpty;
      } else if(isInRange()) {
        selectedOption = myForm.myInRange;
      } else {
        selectedOption = myForm.myEquals;
      }
      selectedOption.setSelected(true);
    }

    @Override
    public boolean isModified() {
      return wasChanged(EQUALS_TO) || wasChanged(LOWER_BOUND, UPPER_BOUND) || wasChanged(PK_EMPTY);
    }

    @Override
    @NotNull
    public FilterNode createFilterNode(ConstraintDescriptor descriptor) {
      return new ConstraintFilterNode(descriptor, createStoredValues());
    }

    private PropertyMap createStoredValues() {
      if(isEquals()) {
        return createEqualsValues(getValue(EQUALS_TO));
      } else if(isEmpty()) {
        return EmptyQueryHelper.createEmptyValues();
      } else if(isFullRange()) {
        return createRangeValues(getValue(LOWER_BOUND), getValue(UPPER_BOUND));
      } else if(isGreater()) {
        return createRangeValues(getValue(LOWER_BOUND), null);
      } else if(isLess()) {
        return createRangeValues(null, getValue(UPPER_BOUND));
      } else {
        return createEqualsValues(null);
      }
    }

    private boolean isEquals() {
      return getBooleanValue(EQUALS);
    }

    private boolean isInRange() {
      return getBooleanValue(IN_RANGE);
    }

    private boolean isEmpty() {
      return getBooleanValue(PK_EMPTY);
    }

    private boolean isGreater() {
      return getBooleanValue(GREATER);
    }

    private boolean isLess() {
      return getBooleanValue(LESS);
    }

    private boolean isFullRange() {
      return isGreater() && isLess();
    }

    private boolean isHalfRange() {
      return isGreater() != isLess();
    }

    @Override
    public void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      canvas.setIcon(Icons.QUERY_CONDITION_INT_ATTR);
      if(isEquals()) {
        renderEquals(canvas, state, descriptor);
      } else if(isEmpty()) {
        renderEmpty(canvas, state, descriptor);
      } else if(isFullRange()) {
        renderFullRange(canvas, state, descriptor);
      } else if(isHalfRange()) {
        renderHalfRange(canvas, state, descriptor);
      } else {
        renderRangeNotDefined(canvas, state, descriptor);
      }
    }

    private void renderEquals(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      descriptor.getPresentation().renderOn(canvas, state);
      canvas.appendText(" = ");
      canvas.appendText(getValue(EQUALS_TO));
    }

    private void renderEmpty(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      descriptor.getPresentation().renderOn(canvas, state);
      canvas.appendText(" is empty");
    }

    private void renderFullRange(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      canvas.appendText(getValue(LOWER_BOUND));
      canvas.appendText(LE_STRING);
      descriptor.getPresentation().renderOn(canvas, state);
      canvas.appendText(LE_STRING);
      canvas.appendText(getValue(UPPER_BOUND));
    }

    private void renderHalfRange(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      descriptor.getPresentation().renderOn(canvas, state);
      if(isLess()) {
        canvas.appendText(LE_STRING);
      } else {
        canvas.appendText(GE_STRING);
      }
      canvas.appendText(getValue(ConstraintEditorNodeImpl.TEXT));
    }

    private void renderRangeNotDefined(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      descriptor.getPresentation().renderOn(canvas, state);
      canvas.appendText(" range not defined");
    }

    @Override
    public JComponent getComponent() {
      return myForm.myWholePanel;
    }

    public static PropertyMap convertToEditorData(PropertyMap data) {
      final PropertyMap values = new PropertyMap();
      final String equalsTo = data.get(EQUALS_TO_VALUE);
      if(equalsTo != null) {
        EQUALS.setInitialValue(values, true);
        EQUALS_TO.setInitialValue(values, equalsTo);
      } else if(isEmptyOption(data)) {
        EQUALS.setInitialValue(values, false);
        PK_EMPTY.setInitialValue(values, true);
      } else {
        EQUALS.setInitialValue(values, false);
        IN_RANGE.setInitialValue(values, true);
        final String upperValue = data.get(UPPER_VALUE);
        final String lowerValue = data.get(LOWER_VALUE);
        UPPER_BOUND.setInitialValue(values, upperValue != null ? upperValue : lowerValue);
        LESS.setInitialValue(values, upperValue != null);
        LOWER_BOUND.setInitialValue(values, lowerValue != null ? lowerValue : upperValue);
        GREATER.setInitialValue(values, lowerValue != null);
      }
      return values;
    }
  }

  private static class Form {
    private JRadioButton myEquals;
    private JTextField myEqualsTo;
    private JRadioButton myInRange;
    private JCheckBox myGreater;
    private JTextField myGreaterThan;
    private JCheckBox myLess;
    private JTextField myLessThan;
    private JRadioButton myIsEmpty;
    private JPanel myWholePanel;

    public Form() {
      final ButtonGroup group = new ButtonGroup();
      group.add(myEquals);
      group.add(myInRange);
      group.add(myIsEmpty);

      UIUtil.transferFocus(myEquals, myEqualsTo);
      ComponentEnabler.create(myInRange, myGreater, myLess);
      UIUtil.transferFocus(myGreater, myGreaterThan);
      UIUtil.transferFocus(myLess, myLessThan);
    }
  }

  public static PropertyMap createEqualsValues(@Nullable String value) {
    final PropertyMap values = new PropertyMap();
    values.put(EQUALS_TO_VALUE, value);
    return values;
  }

  public static PropertyMap createRangeValues(@Nullable String lower, @Nullable String upper) {
    final PropertyMap values = new PropertyMap();
    values.put(LOWER_VALUE, lower);
    values.put(UPPER_VALUE, upper);
    return values;
  }
}