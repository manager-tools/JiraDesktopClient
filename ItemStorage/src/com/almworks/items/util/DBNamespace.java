package com.almworks.items.util;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.util.Pair;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DBNamespace {
  private final String myModule;
  private final String myLocal;

  public DBNamespace(String module, String local) {
    myModule = module;
    myLocal = local;
  }

  public static DBNamespace moduleNs(String modulePackage) {
    assert Pattern.compile("\\w+([.]\\w+)*").matcher(modulePackage).matches();
    return new DBNamespace(modulePackage, "");
  }

  public DBNamespace subNs(String nsId) {
    assert !nsId.endsWith(".");
    return new DBNamespace(myModule, subId(nsId));
  }

  public DBNamespace subModule(String nsId) {
    assert myLocal.isEmpty();
    return new DBNamespace(myModule + "." + nsId, "");
  }

  public String attr(String id) {
    return idAttribute(id);
  }

  public String obj(String id) {
    return fullId(id, ":o:");
  }

  public DBIdentifiedObject object(String id, String name) {
    return new DBIdentifiedObject(obj(id), name);
  }

  public DBIdentifiedObject object(String id) {
    return object(id, id);
  }

  public DBItemType type() {
    return new DBItemType(idType(), myLocal.length() > 0 ? myLocal : myModule);
  }

  public DBItemType type(String id, String name) {
    return new DBItemType(idType(id), name);
  }

  public String idType(String id) {
    return fullId(id, ":t:");
  }

  public DBAttribute<Long> master(String id) {
    return SlaveUtils.masterReference(idAttribute(id), id);
  }

  public DBAttribute<Long> link(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Link(idAttribute(id), name, false), shadowable);
  }

  public DBAttribute<Long> link(String id) {
    return link(id, id, false);
  }

  public DBAttribute<Integer> integer(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Int(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Integer> integer(String id) {
    return integer(id, id, false);
  }

  public DBAttribute<List<Integer>> integerList(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.IntList(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Character> character(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Scalar(idAttribute(id), name, Character.class), shadowable);
  }

  public DBAttribute<Set<Long>> linkSet(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.LinkSet(idAttribute(id), name, false), shadowable);
  }

  public DBAttribute<Set<Long>> linkSet(String id) {
    return attribute(DBAttribute.LinkSet(idAttribute(id), id, false), false);
  }

  public DBAttribute<List<Long>> linkList(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.LinkList(idAttribute(id), name, false), shadowable);
  }

  public DBAttribute<String> string(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.String(idAttribute(id), name), shadowable);
  }

  public DBAttribute<String> string(String id) {
    return string(id, id, false);
  }

  public DBAttribute<List<String>> stringList(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.StringList(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Boolean> bool(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Bool(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Boolean> bool(String id) {
    return bool(id, id, false);
  }

  public DBAttribute<Date> date(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Date(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Date> date(String id) {
    return date(id, id, false);
  }

  public DBAttribute<BigDecimal> decimal(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Decimal(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Long> longAttr(String id, String name, boolean shadowable) {
    return attribute(DBAttribute.Long(idAttribute(id), name), shadowable);
  }

  public DBAttribute<Long> longAttr(String id) {
    return longAttr(id, id, false);
  }

  public DBAttribute<LongList> longList(String id, String name) {
    return attribute(DBAttribute.Scalar(idAttribute(id), name, LongList.class), false);
  }

  public DBAttribute<AttributeMap> attributeMap(String id, String name) {
    return attribute(DBAttribute.AttributeMap(idAttribute(id), name), false);
  }

  private <T> DBAttribute<T> attribute(DBAttribute<T> attr, boolean shadowable) {
    if (shadowable) SyncAttributes.initShadowable(attr);
    return attr;
  }

  private String subId(String id) {
    return myLocal.length() > 0 ? myLocal + "." + id : id;
  }

  public String idAttribute(String id) {
    return fullId(id, ":a:");
  }

  private String fullId(String id, String kind) {
    return myModule + kind + subId(id);
  }

  /**
   * Reverses composition of {@link #fullId(String, String) fullId}. The ID must be from this namespace.<br>
   * NOTE! A Parent namespace may reverse fullID from its child namespace. The localId part starts with child's local part (relative to the parent).
   *
   * @param id full ID to reverse
   * @return [kind, localId] if the full ID belongs to this namespace. null if the fullID is from other NS, or is not well-formed
   */
  public Pair<String, String> reverseFullId(String id) {
    if (!id.startsWith(myModule)) return null;
    id = id.substring(myModule.length());
    if (!id.startsWith(":")) return null;
    int kindEnd = id.indexOf(':', 1);
    if (kindEnd < 2 || kindEnd >= id.length() - 1) return null; // If not found, or two colons in a row, or nothing after the second colon
    String kind = id.substring(1, kindEnd);
    id = id.substring(kindEnd + 1);
    if (!id.startsWith(myLocal)) return null;
    id = id.substring(myLocal.length() + (myLocal.isEmpty() ? 0 : 1)); // Cut myLocal and possible dot before in-namespace id
    return Pair.create(kind, id);
  }

  public String idType() {
    return myModule + ":t:" + myLocal;
  }

  public DBAttribute<byte[]> bytes(String id) {
    return bytes(id, id);
  }

  public DBAttribute<byte[]> bytes(String id, String name) {
    return attribute(DBAttribute.ByteArray(idAttribute(id), name), false);
  }

  @Override
  public String toString() {
    return "NS[" + myModule + "::" + myLocal + "]";
  }
}
