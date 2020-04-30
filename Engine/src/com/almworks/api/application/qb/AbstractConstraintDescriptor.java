package com.almworks.api.application.qb;

import com.almworks.util.threads.CanBlock;
import org.almworks.util.Util;

public abstract class AbstractConstraintDescriptor implements ConstraintDescriptor {
  public final boolean equals(Object obj) {
    if (!(obj instanceof ConstraintDescriptor))
      return false;
    return getId().equals(((ConstraintDescriptor) obj).getId());
  }

  public final int hashCode() {
    return getId().hashCode();
  }

  @CanBlock
  public void waitForInitialization() throws InterruptedException {
  }

  /**
   * This is needed for quick navigation in lists
   * @return
   */
  public final String toString() {
    return getDisplayName();
  }

  @Override
  public <T> T cast(Class<T> descriptorClass) {
    return Util.castNullable(descriptorClass, this);
  }
}
