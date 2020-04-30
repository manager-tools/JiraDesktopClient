package com.almworks.items.sync.edit;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.util.AttributeMap;
import gnu.trove.TLongObjectHashMap;

interface CommitCounterpart {
  /**
   * Collects base shadows (if not collected earlier).
   * @param reader reader
   * @return Collected base shadows or null - means that commit not possible now
   */
  TLongObjectHashMap<AttributeMap> prepareCommit(DBReader reader);

  void commitFinished(EditCommit commit, boolean success, boolean release);

  CommitCounterpart CREATE_ONLY = new CommitCounterpart() {
    @Override
    public TLongObjectHashMap<AttributeMap> prepareCommit(DBReader reader) {
      return new TLongObjectHashMap<>();
    }

    @Override
    public void commitFinished(EditCommit commit, boolean success, boolean release) {}
  };
}
