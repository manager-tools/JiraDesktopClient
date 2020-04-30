package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SlaveValues {
  @Nullable("For not submitted items")
  private Integer myId;

  protected SlaveValues(@Nullable Integer id) {
    myId = id;
  }

  @Nullable
  public Integer getId() {
    return myId;
  }

  public void setId(int slaveId) {
    LogHelper.assertError(myId == null, "Slave id overridden", myId, slaveId, this);
    myId = slaveId;
  }

  public abstract boolean matchesFailure(EntityHolder slave, @NotNull String thisUser);
}
