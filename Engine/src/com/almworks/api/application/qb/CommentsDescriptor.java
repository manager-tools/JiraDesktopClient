package com.almworks.api.application.qb;

import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.TextAttribute;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.ItemStorageAdaptor;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.almworks.util.collections.Functional.infiniteSaturated;

public class CommentsDescriptor extends AttributeConstraintDescriptor<String> {
  private final DBAttribute<Long> myParentAttribute;
  private final DBAttribute<String> myTextAttribute;

  public CommentsDescriptor(DBAttribute<Long> master, String displayName, String id, DBAttribute<String> textAttribute) {
    super(TextAttribute.INSTANCE, displayName, id, textAttribute, Modifiable.NEVER);
    myParentAttribute = master;
    myTextAttribute = textAttribute;
  }

  @Nullable
  @Override
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    if(data == null) {
      return null;
    }

    final List<String> list = TextAttribute.getSubstrings(data);
    if(list == null || list.isEmpty()) {
      return BoolExpr.FALSE();
    }

    final boolean matchAll = TextAttribute.isAll(data);
    return ItemStorageAdaptor.dpReferredBy(myParentAttribute,
      createCommentFilter(list, matchAll, hypercube));
  }

  private BoolExpr<DP> createCommentFilter(List<String> list, boolean matchAll, ItemHypercube hypercube) {
    final List<BoolExpr<DP>> ands = Collections15.arrayList();
    final Collection<Long> conns = ItemHypercubeUtils.getIncludedConnections(hypercube);
    if(!conns.isEmpty()) {
      ands.add(DPEquals.equalOneOf(SyncAttributes.CONNECTION, conns));
    }
    ands.add(DPNotNull.create(myTextAttribute));
    ands.add(new CommentFilter(list, matchAll, myTextAttribute).term());
    return BoolExpr.and(ands);
  }

  private static class CommentFilter extends DPAttribute<String> {
    private final boolean myMatchAll;
    private final char[][] myCharData;
    private final DBAttribute<String> myTextAttribute;

    public CommentFilter(List<String> strings, boolean matchAll, DBAttribute<String> textAttribute) {
      super(textAttribute);
      myMatchAll = matchAll;
      myCharData = new char[strings.size()][];
      for (int i = 0; i < strings.size(); i++) {
        myCharData[i] = strings.get(i).toCharArray();
      }
      myTextAttribute = textAttribute;
    }

    @Override
    protected boolean acceptValue(String value, DBReader reader) {
      if(value == null) {
        return false;
      }
      final int found = StringUtil.findAny(value.toCharArray(), myCharData, null, true, !myMatchAll);
      return myMatchAll ? (found == myCharData.length) : (found > 0);
    }

    @Override
    protected boolean equalDPA(DPAttribute other) {
      CommentFilter that = (CommentFilter) other;

      if (myMatchAll != that.myMatchAll)
        return false;
      if (myTextAttribute != null ? !myTextAttribute.equals(that.myTextAttribute) :
        that.myTextAttribute != null)
        return false;
      if (myCharData.length != that.myCharData.length)
        return false;
      for (int i = 0; i < myCharData.length; i++) {
        if (!Arrays.equals(myCharData[i], that.myCharData[i]))
          return false;
      }

      return true;
    }

    @Override
    protected int hashCodeDPA() {
      int result = 0;
      // todo char data
      result = 31 * result + (myMatchAll ? 1 : 0);
      result = 31 * result + (myTextAttribute != null ? myTextAttribute.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      Iterator<String> sep = infiniteSaturated("", ", ").iterator();
      for (char[] data : myCharData) sb.append(sep.next()).append(data);
      return "CommentText containsAny " + sb;
    }
  }
}
