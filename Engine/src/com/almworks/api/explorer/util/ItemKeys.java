package com.almworks.api.explorer.util;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.EnumNarrower;
import com.almworks.explorer.qbuilder.filter.ItemKeyModelCollector;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author : Dyoma
 */
public class ItemKeys {
  private static final Convertor<String, ItemKey> UNRESOLVED =
    new Convertor<String, ItemKey>() {
      public ItemKey convert(String value) {
        return createItemKey(value);
      }
    };

  public static Convertor<String, ItemKey> itemId() {
    return UNRESOLVED;
  }

  public static ItemKey createItemKey(String id) {
    return new ItemKeyStub(id);
  }

  public static ItemKey unresolvedCopy(ItemKey value) {
    return new ItemKeyStub(value);
  }

  public static ItemKey unresolvedUnion(List<ItemKey> keys) {
    int size = keys.size();
    if (size == 0) {
      assert false;
      return ItemKeyStub.ABSENT;
    } else if (size == 1) {
      return unresolvedCopy(keys.get(0));
    } else {
      ItemKey first = keys.get(0);
      String id = first.getId();
      String name = first.getDisplayName();
      Icon icon = first.getIcon();
      Set<String> names = null;
      for (int i = 1; i < size; i++) {
        ItemKey key = keys.get(i);
        if (!id.equals(key.getId())) {
          Log.warn("key id mismatch: " + keys);
//          assert false : keys;
          continue;
        }
        String keyName = key.getDisplayName();
        if (names != null || !keyName.equals(name)) {
          if (names == null) {
            names = Collections15.linkedHashSet();
            names.add(name);
          }
          names.add(keyName);
        }
        Icon keyIcon = key.getIcon();
        if (icon != null && !icon.equals(keyIcon))
          icon = null;
      }
      if (names != null) {
        StringBuffer sb = new StringBuffer();
        for (String n : names) {
          if (sb.length() > 0)
            sb.append(", ");
          sb.append(n);
        }
        name = sb.toString();
      }
      ItemOrder order = getGroupOrder(keys);
      return new ItemKeyStub(id, name, order, icon);
    }
  }

  public static ItemOrder getGroupOrder(List<? extends ItemKey> items) {
    int count = items.size();
    if (count == 0) {
      return ItemOrder.NO_ORDER;
    } else if (count == 1) {
      return items.get(0).getOrder();
    } else {
      long[] values = new long[count];
      StringBuffer buffer = new StringBuffer();
      int i = 0;
      for (ItemKey item : items) {
        ItemOrder order = item.getOrder();
        values[i++] = order.getNumericMagnitude() > 0 ? order.getNumeric(0) : Long.MAX_VALUE;
        String s = order.getString();
        if (s != null) {
          buffer.append(s).append(" ");
        }
      }
      return ItemOrder.byGroup(buffer.toString(), values);
    }
  }

  public static ItemKey findInModel(ItemKey target, Iterable<? extends ItemKey> model) {
    for(final ItemKey user : model) {
      if(user.equals(target)) {
        return user;
      }
    }
    return null;
  }

  public static boolean resolveItemId(String itemId, ItemHypercube cube, Collection<? super ResolvedItem> result,
    EnumNarrower narrower, ItemKeyModelCollector<?> collector)
  {
    if (cube == null) cube = new ItemHypercubeImpl();
    Collection<Long> connections = ItemHypercubeUtils.getIncludedConnections(cube);
    boolean resolutionsFound = false;
    List<? extends ResolvedItem> candidates = collector.getAllResolvedListCopyOrNull(itemId);
    if (candidates != null && !candidates.isEmpty()) {
      for (ResolvedItem candidate : candidates) {
        long c = candidate.getConnectionItem();
        if (connections.isEmpty() || c == 0 || connections.contains(c)) {
          List<ResolvedItem> list = narrower.narrowList(Collections.singletonList(candidate), cube);
          result.addAll(list);
          if (list.size() > 0) {
            resolutionsFound = true;
          }
        }
      }
    }
    return resolutionsFound;
  }
}
