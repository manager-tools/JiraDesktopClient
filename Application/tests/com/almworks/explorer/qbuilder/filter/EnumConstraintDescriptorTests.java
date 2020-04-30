package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ItemOrder;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor.removeDuplicates;

public class EnumConstraintDescriptorTests extends BaseTestCase {
  private CollectionsCompare myCompare = new CollectionsCompare();

  private static final ItemKeyStub A = new ItemKeyStub("a");
  private static final ItemKeyStub B = new ItemKeyStub("b");
  private static final ItemKeyStub C = new ItemKeyStub("c");

  private static final ItemKey RB = resolved(2, "b");
  private static final ItemKey RC = resolved(3, "c");
  private static final ItemKey RD = resolved(4, "d");
  private static final ItemKey RBD = resolved(2, "d");
  private static final ItemKey RA = resolved(1, "a");

  private static ItemKey resolved(int itemKey, String name) {
    return new TestItemKey(name, itemKey);
  }


  public void testRemoveDuplicates() {
    List<ItemKey> list = Collections15.arrayList();
    List<ItemKey> list2;

    assertNull(removeDuplicates(null));
    assertEquals(0, removeDuplicates(Collections.<ItemKey>emptyList()).size());

    list.add(A);
    assertSame(list, removeDuplicates(list));
    list.add(B);
    assertSame(list, removeDuplicates(list));
    list.add(C);
    assertSame(list, removeDuplicates(list));

    list.add(B);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {A, B, C, B}, list);
    myCompare.order(new Object[] {A, B, C}, list2);

    list.add(A);
    list.add(B);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {A, B, C, B, A, B}, list);
    myCompare.order(new Object[] {A, B, C}, list2);

    list.add(RB);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {A, B, C, B, A, B, RB}, list);
    myCompare.order(new Object[] {A, RB, C}, list2);

    list.add(RA);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {A, B, C, B, A, B, RB, RA}, list);
    myCompare.order(new Object[] {RA, RB, C}, list2);

    list.add(RBD);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {A, B, C, B, A, B, RB, RA, RBD}, list);
    myCompare.order(new Object[] {RA, RB, C}, list2);

    list.add(0, RBD);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {RBD, A, B, C, B, A, B, RB, RA, RBD}, list);
    myCompare.order(new Object[] {RBD, RA, B, C}, list2);

    list.add(RD);
    list2 = removeDuplicates(list);
    myCompare.order(new Object[] {RBD, A, B, C, B, A, B, RB, RA, RBD, RD}, list);
    myCompare.order(new Object[] {RBD, RA, B, C, RD}, list2);
  }

  private static class TestItemKey extends ItemKey {
    private final String myName;
    private final ItemOrder myOrder;
    private final long myResolvedItem;

    public TestItemKey(String name, long resolvedItem) {
      myName = name;
      myOrder = ItemOrder.byString(name);
      myResolvedItem = resolvedItem;
    }

    @NotNull
    public String getId() {
      return myName;
    }

    @NotNull
    public ItemOrder getOrder() {
      return myOrder;
    }

    @Nullable
    public long getResolvedItem() {
      return myResolvedItem;
    }
  }
}
