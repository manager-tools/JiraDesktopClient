package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.qb.ConstraintDescriptorProxy;
import com.almworks.api.application.qb.ConstraintFilterNode;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.util.properties.BooleanPropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.InfixParser;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.text.parser.ParserContext;
import com.almworks.util.text.parser.TokenRegistry;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

public class EmptyQueryHelper {
  public static final TypedKey<Boolean> TK_EMPTY = TypedKey.create("empty");
  public static final BooleanPropertyKey PK_EMPTY = BooleanPropertyKey.createKey("empty", false);

  public static PropertyMap createEmptyValues() {
    final PropertyMap values = new PropertyMap();
    values.put(TK_EMPTY, true);
    return values;
  }

  public static boolean isEmptyOption(PropertyMap data) {
    return Boolean.TRUE.equals(data.get(TK_EMPTY));
  }

  public static void registerEmptyParser(
    TokenRegistry<FilterNode> registry, final String symbol, final ConstraintType type)
  {
    registry.registerInfixConstraint(symbol, new InfixParser<FilterNode>() {
      @Override
      public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
        final String empty = Util.lower(right.getSingle());
        if("empty".equals(empty)) {
          return new ConstraintFilterNode(ConstraintDescriptorProxy.stub(left.getSingle(), type), createEmptyValues());
        }
        throw right.createException("'empty' expected after '" + symbol + "'", 0, empty.length());
      }
    });
  }

  private EmptyQueryHelper() {}
}
