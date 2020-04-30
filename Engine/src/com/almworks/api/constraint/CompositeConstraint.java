package com.almworks.api.constraint;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public interface CompositeConstraint extends Constraint {
  ComplementaryKey AND = ComplementaryKey._AND;
  ComplementaryKey OR = ComplementaryKey._OR;

  TypedKey<? extends CompositeConstraint> getType();

  /**
   * @return valid CompositeConstraint returns not null, not empty list, noone of elements is null
   */
  List<? extends Constraint> getChildren();


  class ComplementaryKey extends TypedKey<CompositeConstraint> {
    private static final ComplementaryKey _AND;
    private static final ComplementaryKey _OR;

    private ComplementaryKey myComplementary;

    private ComplementaryKey(String name) {
      super(name, null, null);
    }

    public ComplementaryKey getComplementary() {
      return myComplementary;
    }

    static {
      ComplementaryKey and = new ComplementaryKey("and");
      ComplementaryKey or = new ComplementaryKey("or");
      and.myComplementary = or;
      or.myComplementary = and;
      _AND = and;
      _OR = or;
    }
  }

  static CompositeConstraint and(Constraint ... constraints) {
    return Simple.and(constraints);
  }

  public class Simple implements CompositeConstraint {
    private final List<Constraint> myChildren;
    private final TypedKey<? extends CompositeConstraint> myType;

    public Simple(TypedKey<? extends CompositeConstraint> type, List<? extends Constraint> children) {
      assert type != null;
      assert children != null;
      myType = type;
      myChildren = Collections.unmodifiableList(children);
    }

    public static Simple and(Constraint ... constraints) {
      return new Simple(AND, filterNulls(constraints));
    }

    @NotNull
    private static List<Constraint> filterNulls(Constraint[] constraints) {
      ArrayList<Constraint> result = new ArrayList<>();
      for (Constraint constraint : constraints) if (constraint != null) result.add(constraint);
      return result;
    }

    public static Constraint and(List<Constraint> list) {
      return new Simple(AND, list);
    }

    public static Simple create(TypedKey<? extends CompositeConstraint> type, List<Constraint> children) {
      return new Simple(type, children);
    }

    public static Simple or(List<Constraint> constraints) {
      return new Simple(OR, constraints);
    }

    public static Simple or(Constraint ... constraints) {
      return or(filterNulls(constraints));
    }

    public TypedKey<? extends CompositeConstraint> getType() {
      return myType;
    }

    public List<? extends Constraint> getChildren() {
      return myChildren;
    }
  }
}
