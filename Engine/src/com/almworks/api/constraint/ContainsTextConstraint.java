package com.almworks.api.constraint;

import org.almworks.util.TypedKey;

import java.util.Arrays;
import java.util.List;

public interface ContainsTextConstraint extends Constraint {
  TypedKey<ContainsTextConstraint> CONTAINS_TEXT = TypedKey.create(ContainsTextConstraint.class);

  TypedKey<? extends ContainsTextConstraint> getType();

  List<String> getWords();

  class Simple implements ContainsTextConstraint {
    private final List<String> myWords;

    private Simple(List<String> words) {
      myWords = words;
    }

    public TypedKey<? extends ContainsTextConstraint> getType() {
      return CONTAINS_TEXT;
    }

    public List<String> getWords() {
      return myWords;
    }

    public static Simple create(String[] words) {
      return new Simple(Arrays.asList(words));
    }
  }
}
