package com.almworks.api.application;

import com.almworks.util.ui.actions.DataRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
*/
public class LifeMode {
  public static final DataRole<LifeMode> LIFE_MODE_DATA = DataRole.createRole(LifeMode.class);

  public static final LifeMode NOT_APPLICABLE = new LifeMode(false, false, false, false, null, "N/A");
  public static final LifeMode TO_BE_FROZEN = new LifeMode(true, false, false, true, null, "->FROZEN");
  public static final LifeMode FROZEN = new LifeMode(true, false, false, false, TO_BE_FROZEN, "FROZEN");
  public static final LifeMode TO_BE_LIFE = new LifeMode(true, true, false, true, null, "->LIFE");
  public static final LifeMode LIFE = new LifeMode(true, true, false, false, TO_BE_LIFE, "LIFE");
  public static final LifeMode HAS_NEW = new LifeMode(true, false, true, false, TO_BE_FROZEN, "HAS_NEW");

  private final boolean myApplicable;
  private final boolean myLife;
  private final boolean myHasNew;
  private final boolean myLoading;
  @NotNull
  private final LifeMode myLoadingMode;
  private final String myDebugString;

  public LifeMode(boolean applicable, boolean life, boolean hasNew, boolean loading, @Nullable LifeMode loadingMode,
    String debugString) {
    myApplicable = applicable;
    myLife = life;
    myHasNew = hasNew;
    myLoading = loading;
    myLoadingMode = loadingMode != null ? loadingMode : this;
    myDebugString = debugString;
  }

  public boolean isApplicable() {
    return myApplicable;
  }

  public boolean isLife() {
    return myLife;
  }

  public boolean hasNewElements() {
    return myHasNew;
  }

  public boolean isLoading() {
    return myLoading;
  }

  @NotNull
  public LifeMode getLoadingMode() {
    return myLoadingMode;
  }

  public String toString() {
    return myDebugString;
  }
}
