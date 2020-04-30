package com.almworks.items.cache;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.BadUtil;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.List;

/**
 * @author dyoma
 */
public interface DataLoader<T> {
  /**
   * (Bulk) load values for given items from DB. For each index i in range [0, items.size()) loads value for items.get(i) and stores
   * it in result at index i. So the result has the same size as original items.<br>
   * Also loader can subscribe caller if loaded values depends on anything (not only on item ICN). TBD<br>
   * If the caller requires current values only it may provide {@link Lifespan#NEVER} and {@link Procedure.Stub#INSTANCE}.
   * @param items items to load values
   * @param life subscription life
   * @param invalidate invalidation procedure. Loader may notify caller that values for some items are out of date.
   * @return values for each item in same order as items
   */
  List<T> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate);

  DataLoader<DBIdentity> IDENTITY_LOADER = new DataLoader<DBIdentity>() {
    @Override
    public List<DBIdentity> loadValues(DBReader reader, LongList items, Lifespan life, Procedure<LongList> invalidate) {
      List<DBIdentity> identities = Collections15.arrayList(items.size());
      for (int i = 0; i < items.size(); i++) identities.add(DBIdentity.load(reader, items.get(i)));
      return identities;
    }

    @Override
    public String toString() {
      return "identity";
    }
  };

  DataLoader<DBAttribute<?>> ATTRIBUTE = new DataLoader<DBAttribute<?>>() {
    @Override
    public List<DBAttribute<?>> loadValues(DBReader reader, LongList items, Lifespan life,
      Procedure<LongList> invalidate)
    {
      List<DBAttribute<?>> result = Collections15.arrayList();
      for (int i = 0; i < items.size(); i++) result.add(BadUtil.getAttribute(reader, items.get(i)));
      return result;
    }

    @Override
    public String toString() {
      return "attributes";
    }
  };
}
