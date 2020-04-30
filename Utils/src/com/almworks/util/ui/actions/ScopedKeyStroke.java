package com.almworks.util.ui.actions;

import com.almworks.util.collections.CollectionUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Set;

/**
 * A {@code ScopedKeyStroke} is an entity that combines a {@link KeyStroke} and
 * a {@code Set} of {@code String} scope identifiers. The precise semantics of
 * a scope identifier is not defined, but it could be a window or other control
 * identifier if the same keystroke leads to different actions in different
 * contexts.
 * @author Pavel Zvyagin
 */
public class ScopedKeyStroke {
  private final KeyStroke myKeyStroke;
  private final Set<String> myScopes;

  /**
   * Constructs a {code ScopedKeyStroke} with the given {@link KeyStroke}
   * and scope identifiers.
   * @param keyStroke The {@link KeyStroke} instance.
   * @param scopes The collection of scope identifiers; {@code null} is treated as empty collection.
   */
  public ScopedKeyStroke(@NotNull KeyStroke keyStroke, @Nullable Collection<String> scopes) {
    assert keyStroke != null;
    myKeyStroke = keyStroke;
    myScopes = Collections15.unmodifiableSetCopy(scopes);
  }

  /**
   * Constructs a catch-all {@code ScopedKeyStroke}.
   * @param keyStroke The {@link KeyStroke} instance.
   */
  public ScopedKeyStroke(@NotNull KeyStroke keyStroke) {
    this(keyStroke, null);
  }

  /**
   * @return The corresponding {@link KeyStroke}.
   */
  @NotNull
  public KeyStroke getKeyStroke() {
    return myKeyStroke;
  }

  /**
   * @return The scope identifier set.
   */
  @NotNull
  public Set<String> getScopes() {
    return myScopes;
  }

  /**
   * Determines if there's a conflict between this instance and
   * some other ScopedKeyStroke. Two ScopedKeyStroke are in conflict,
   * if they represent the same KeyStroke and their scope identifier
   * sets intersect.
   * A ScopedKeyStroke with an empty scope set is considered a catch-all
   * ScopedKeyStroke. A catch-all instance is in conflict only with
   * another catch-all instance.
   * @param that Another {@code ScopedKeyStroke}.
   * @return {@code true} if this instance is in conflict with {@code that}.
   */
  public boolean conflictsWith(@NotNull ScopedKeyStroke that) {
    if(!myKeyStroke.equals(that.myKeyStroke)) {
      return false;
    }

    if(myScopes.isEmpty() && that.myScopes.isEmpty()) {
      return true;
    }

    if(myScopes.isEmpty() || that.myScopes.isEmpty()) {
      return false;
    }

    for(final String scope : myScopes) {
      if(that.myScopes.contains(scope)) {
        return true;
      }
    }

    return false;
  }

  public boolean equals(Object o) {
    if(o == null) {
      return false;
    }

    if(o == this) {
      return true;
    }

    if(getClass() == o.getClass()) {
      final ScopedKeyStroke that = (ScopedKeyStroke) o;
      return myKeyStroke.equals(that.myKeyStroke)
          && myScopes.equals(that.myScopes);
    }

    return false;
  }

  public int hashCode() {
    return (myKeyStroke.hashCode() - 31) * 31 + myScopes.hashCode();
  }
  
  public String toString() {
    return myKeyStroke.toString() + " @ [" + CollectionUtil.stringJoin(myScopes, " ") + "]";
  }
}
