package com.almworks.spi.provider;

import com.almworks.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * :todoc:
 *
 * @author sereda
 */
public class GeneralItemProblem extends AbstractItemProblem {
  private final String myShortDescription;
  private final String myLongDescription;
  private final Cause myCause;

  public GeneralItemProblem(long item, String displayableId, long timeCreated, ConnectionContext context, Pair<String, Boolean> credentialState,
    @NotNull String shortDescription, @NotNull String longDescription, Cause cause) {
    super(item, displayableId, timeCreated, context, credentialState);
    myShortDescription = shortDescription;
    myLongDescription = longDescription;
    myCause = cause;
  }

  public String getLongDescription() {
    return myLongDescription;
  }

  public String getShortDescription() {
    return myShortDescription;
  }

  public Cause getCause() {
    return myCause;
  }

}
