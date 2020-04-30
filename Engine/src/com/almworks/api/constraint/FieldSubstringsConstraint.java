package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public interface FieldSubstringsConstraint extends OneFieldConstraint {
  TypedKey<FieldSubstringsConstraint> MATCHES_ALL = TypedKey.create("allSubstrings");
  TypedKey<FieldSubstringsConstraint> MATCHES_ANY = TypedKey.create("anySubstring");

  TypedKey<? extends FieldSubstringsConstraint> getType();

  /**
   * @return valid constraint returns not null, not empty list, noone element is null
   */
  @Nullable
  List<String> getSubstrings();

  public class Simple implements FieldSubstringsConstraint {
    private final TypedKey<FieldSubstringsConstraint> myType;
    private final DBAttribute myAttribute;
    private final List<String> myStrings;

    private Simple(TypedKey<FieldSubstringsConstraint> type, DBAttribute attribute, List<String> strings) {
      myType = type;
      myAttribute = attribute;
      myStrings = strings;
    }

    public List<String> getSubstrings() {
      return myStrings;
    }

    public TypedKey<? extends FieldSubstringsConstraint> getType() {
      return myType;
    }

    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public static Simple any(DBAttribute attribute, Collection<String> strings) {
      return new Simple(MATCHES_ANY, attribute, Collections15.arrayList(strings));
    }

    public static FieldSubstringsConstraint any(DBAttribute attribute, String single) {
      List<String> strings = Collections.singletonList(single);
      return any(attribute, strings);
    }

    public static FieldSubstringsConstraint any(DBAttribute attribute, String one, String two) {
      List<String> list = Collections15.arrayList(2);
      list.add(one);
      list.add(two);
      return any(attribute, list);
    }

    public static FieldSubstringsConstraint all(DBAttribute attribute, String single) {
      return new Simple(MATCHES_ALL, attribute, Collections.singletonList(single));
    }

    public static FieldSubstringsConstraint all(DBAttribute attribute, List<String> words) {
      return new Simple(MATCHES_ALL, attribute, Collections15.arrayList(words));
    }

    public static FieldSubstringsConstraint all(DBAttribute attribute, String one, String two) {
      List<String> list = Collections15.arrayList(2);
      list.add(one);
      list.add(two);
      return new Simple(MATCHES_ALL, attribute, list);
    }
  }
}
