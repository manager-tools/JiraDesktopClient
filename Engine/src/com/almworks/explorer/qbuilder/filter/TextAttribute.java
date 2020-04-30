package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.*;
import com.almworks.api.application.tree.UserQueryNode;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldSubstringsConstraint;
import com.almworks.api.constraint.IsEmptyConstraint;
import com.almworks.explorer.qbuilder.constraints.AbstractConstraintEditor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPAttribute;
import com.almworks.items.dp.DPTextMatch;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolOperation;
import com.almworks.util.collections.Comparing;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.images.IconHandle;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.parser.*;
import com.almworks.util.ui.ComponentKeyBinder;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.almworks.explorer.qbuilder.filter.EmptyQueryHelper.PK_EMPTY;
import static com.almworks.explorer.qbuilder.filter.EmptyQueryHelper.isEmptyOption;

/**
 * @author : Dyoma
 */
public class TextAttribute implements AttributeConstraintType<String> {
  public static final AttributeConstraintType<String> INSTANCE = new TextAttribute();
  public static final char ESCAPE_CHAR = '\\';
  public static final char QUOTE_CHAR = '"';
  private static final IconHandle MY_ICON = Icons.QUERY_CONDITION_TEXT_ATTR;
  public static final TypedKey<String> TEXT = TypedKey.create("text");
  private static final TypedKey<Boolean> ALL_WORDS = TypedKey.create("all");

  private static final Map<Boolean, String> TOKENS;
  static {
    final Map<Boolean, String> tokens= Collections15.hashMap();
    tokens.put(Boolean.TRUE, "containsAll");
    tokens.put(Boolean.FALSE, "hasAny");
    TOKENS = Collections.unmodifiableMap(tokens);
  }

  private TextAttribute() {}

  @Override
  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return createEditor(node, true, null, null);
  }

  public static MyConstraintEditor createEditor(ConstraintEditorNodeImpl node, boolean allowAny, String textLabel, String hintText) {
    return new MyConstraintEditor(node, allowAny, textLabel, hintText);
  }

  @Override
  public void writeFormula(FormulaWriter writer, String conditionId, PropertyMap data) {
    writer.addToken(conditionId);
    if(isEmptyOption(data)) {
      writer.addRaw(" textIs empty");
    } else {
      writer.addRaw(" " + TOKENS.get(isAll(data)) + " ");
      writer.addToken(Util.NN(data.get(TEXT)));
    }
  }

  @Override
  @Nullable
  public BoolExpr<DP> createFilter(DBAttribute<String> attribute, PropertyMap data) {
    if(isEmptyOption(data)) {
      return new TextIsEmptyFilter(attribute).term();
    } else {
      return createTextFilter(attribute, data);
    }
  }

  private BoolExpr<DP> createTextFilter(DBAttribute<String> attribute, PropertyMap data) {
    final List<String> fragments = getSubstrings(data);
    if(fragments == null || attribute == null) {
      return null;
    }
    final boolean all = isAll(data);
    if (fragments.isEmpty())
      return all ? BoolExpr.<DP>TRUE() : BoolExpr.<DP>FALSE();
    List<BoolExpr<DP>> args = Collections15.arrayList(fragments.size());
    for (String fragment : fragments) {
      if (fragment != null && fragment.length() > 0) {
        args.add(DPTextMatch.contains(attribute, fragment));
      }
    }
    return BoolExpr.operation(all ? BoolOperation.AND : BoolOperation.OR, args, false, true);
  }

  /**
   * Returns true when ALL is selected, false when ANY
   */
  public static boolean isAll(PropertyMap data) {
    Boolean all = data.get(ALL_WORDS);
    return all != null && all;
  }

  @Override
  @Nullable
  public Constraint createConstraint(DBAttribute<String> attribute, PropertyMap data) {
    if(isEmptyOption(data)) {
      return IsEmptyConstraint.Simple.isEmpty(attribute);
    } else if(isAll(data)) {
      return FieldSubstringsConstraint.Simple.all(attribute, getSubstrings(data));
    } else {
      return FieldSubstringsConstraint.Simple.any(attribute, getSubstrings(data));
    }
  }

  @Override
  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return isEmptyOption(data1) == isEmptyOption(data2)
      && isAll(data1) == isAll(data2)
      && Comparing.areSetsEqual(getSubstrings(data1), getSubstrings(data2));
  }

  @Override
  public Icon getDescriptorIcon() {
    return Icons.QUERY_CONDITION_TEXT_ATTR;
  }

  @Override
  public PropertyMap getEditorData(PropertyMap data) {
    return MyConstraintEditor.convertToEditorData(data);
  }

  @Override
  @Nullable
  public String suggestName(String descriptorName, PropertyMap data, Map<TypedKey<?>, ?> hints)
    throws CannotSuggestNameException
  {
    if(isEmptyOption(data)) {
      return descriptorName + " is empty";
    }

    String textValue = data.get(TextAttribute.TEXT);
    if (textValue == null || textValue.length() == 0) {
      return null;
    }
    textValue = textValue.trim();
    Integer maxLength = UserQueryNode.MAX_NAME_LENGTH.getFrom(hints);
    if (maxLength != null && textValue.length() > maxLength && maxLength > 3) {
      throw new CannotSuggestNameException();
    }
    return textValue;
  }

  @NotNull
  public static List<String> getSubstrings(PropertyMap data) {
    return parseTextFragments(data.get(TEXT));
  }

  public static void register(TokenRegistry<FilterNode> registry) {
    for(final Boolean type : TOKENS.keySet()) {
      Parser.register(registry, type);
    }
    EmptyQueryHelper.registerEmptyParser(registry, "textIs", INSTANCE);
  }

  private static class MyConstraintEditor extends AbstractConstraintEditor {
    private static final BooleanPropertyKey ALL = BooleanPropertyKey.createKey("all", false);

    private JComponent myComponent;
    private JTextField myField;
    private JRadioButton myAll;
    private JRadioButton myAny;
    private JCheckBox myEmpty;
    private JLabel myPatternLabel;
    private JLabel myHint;
    private JLabel myMatchesLabel;
    private final boolean myAllowAny;

    public MyConstraintEditor(ConstraintEditorNodeImpl node, boolean allowAny, String textLabel, @Nullable String hintText) {
      super(node);
      myAllowAny = allowAny;

      myPatternLabel.setLabelFor(myField);
      if (textLabel != null) NameMnemonic.parseString(textLabel).setToLabel(myPatternLabel);
      if (allowAny) {
        UIUtil.transferFocus(myAll, myField);
        UIUtil.transferFocus(myAny, myField);
      } else {
        myAll.setVisible(false);
        myAny.setVisible(false);
        myEmpty.setVisible(false);
        myMatchesLabel.setVisible(false);
      }

      final ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          final boolean notEmpty = !isEmpty();
          myField.setEnabled(notEmpty);
          if (myAllowAny) {
            myAll.setEnabled(notEmpty);
            myAny.setEnabled(notEmpty);
            myMatchesLabel.setEnabled(notEmpty);
            myPatternLabel.setEnabled(notEmpty);
          }
        }
      };
      final ComponentKeyBinder binder = getBinder();
      binder.setDocument(ConstraintEditorNodeImpl.TEXT, myField);
      if (allowAny) {
        binder.setBoolean(ALL, myAll);
        binder.setInvertedBoolean(ALL, myAny);
        binder.setBoolean(PK_EMPTY, myEmpty);
        myEmpty.addActionListener(listener);
      }

      listener.actionPerformed(null);
      if (hintText != null) myHint.setText(hintText);
      else {
        myHint.setText("");
        myHint.setVisible(false);
      }
    }

    @Override
    public boolean isModified() {
      return myAllowAny ?
        wasChanged(ConstraintEditorNodeImpl.TEXT, ALL, PK_EMPTY) :
        wasChanged(ConstraintEditorNodeImpl.TEXT);
    }

    private String getCurrentText() {
      return getValue(ConstraintEditorNodeImpl.TEXT);
    }

    @Override
    @NotNull
    public FilterNode createFilterNode(ConstraintDescriptor descriptor) {
      final PropertyMap data = isEmpty()
        ? EmptyQueryHelper.createEmptyValues()
        : createValues(getCurrentText(), isAll());
      return new ConstraintFilterNode(descriptor, data);
    }

    private boolean isAll() {
      return !myAllowAny || getValue(ALL);
    }

    private boolean isEmpty() {
      return myAllowAny && getBooleanValue(PK_EMPTY);
    }

    @Override
    public void renderOn(Canvas canvas, CellState state, ConstraintDescriptor descriptor) {
      canvas.setIcon(MY_ICON);
      descriptor.getPresentation().renderOn(canvas, state);
      if(isEmpty()) {
        canvas.appendText(" is empty");
      } else {
        canvas.appendText(" " + TOKENS.get(isAll()));
        canvas.appendText(" " + getCurrentText());
      }
    }

    @Override
    public JComponent getComponent() {
      return myComponent;
    }

    public static PropertyMap convertToEditorData(PropertyMap data) {
      final PropertyMap values = new PropertyMap();
      ConstraintEditorNodeImpl.TEXT.setInitialValue(values, data.get(TEXT));
      ALL.setInitialValue(values, TextAttribute.isAll(data));
      PK_EMPTY.setInitialValue(values, isEmptyOption(data));
      return values;
    }
  }

  @NotNull
  public static List<String> parseTextFragments(String text) {
    if (text == null) return Collections15.emptyList();
    text = text.trim();
    if (text.length() == 0) return Collections15.emptyList();
    List<String> result = Collections15.arrayList();
    StringBuilder builder = new StringBuilder();
    boolean escaped = false;
    boolean quoting = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == ESCAPE_CHAR) {
        if (escaped) {
          builder.append(ESCAPE_CHAR);
          escaped = false;
        }
        else escaped = true;
      } else if (c == QUOTE_CHAR) {
        if (escaped) {
          builder.append(QUOTE_CHAR);
          escaped = false;
        } else {
          quoting = !quoting;
          if (builder.length() > 0) {
            result.add(builder.toString());
            builder.setLength(0);
          }
        }
      } else if (Character.isWhitespace(c)) {
        if (quoting) builder.append(c);
        else {
          if (builder.length() > 0) {
            result.add(builder.toString());
            builder.setLength(0);
          }
        }
      } else builder.append(c);
    }
    if (builder.length() > 0) result.add(builder.toString());
    return result;
  }

  private static class Parser implements InfixParser<FilterNode> {
    private final boolean myType;

    public Parser(boolean type) {
      myType = type;
    }

    @Override
    public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
      return new ConstraintFilterNode(
        ConstraintDescriptorProxy.stub(left.getSingle(), INSTANCE),
        createValues(right.getSingleOrNull(), myType));
    }

    public static void register(TokenRegistry<FilterNode> registry, Boolean type) {
      registry.registerInfixConstraint(TOKENS.get(type), new Parser(type));
    }
  }

  private static class TextIsEmptyFilter extends DPAttribute<String> {
    public TextIsEmptyFilter(DBAttribute<String> stringDBAttribute) {
      super(stringDBAttribute);
    }

    @Override
    protected boolean acceptValue(String value, DBReader reader) {
      return value == null || value.isEmpty() || value.trim().isEmpty();
    }

    @Override
    protected boolean equalDPA(DPAttribute other) {
      return true;
    }

    @Override
    protected int hashCodeDPA() {
      return TextIsEmptyFilter.class.hashCode();
    }

    @Override
    public String toString() {
      return getAttribute() + " is empty";
    }
  }

  public static PropertyMap createValues(@Nullable String text, boolean all) {
    PropertyMap values = new PropertyMap();
    values.put(TEXT, text);
    values.put(ALL_WORDS, all);
    return values;
  }
}
