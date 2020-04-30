package com.almworks.spi.provider;

import com.almworks.api.connector.CancelledException;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.properties.Role;

public class CancelFlag extends BasicScalarModel<Boolean> {
  public static final Role<CancelFlag> ROLE = Role.role(CancelFlag.class);

  public CancelFlag() {
    super(Boolean.FALSE, true, true, null, true);
  }

  public void checkCancelled() throws CancelledException {
    if (isCancelled())
      throw new CancelledException();
  }

  public boolean isCancelled() {
    Boolean b = getValue();
    return b != null && b.booleanValue();
  }
}
