package com.almworks.items.util;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBWriter;
import com.almworks.util.commons.Function;

public class InitIdentified implements Function<DBWriter, Long> {
  private final DBIdentifiedObject myObject;

  public InitIdentified(DBIdentifiedObject object) {
    if (object == null)
      throw new NullPointerException();
    myObject = object;
  }

  public Long invoke(DBWriter writer) {
    return writer.materialize(myObject);
  }
}
