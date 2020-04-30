package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.edit.BaseDBDrain;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.IllegalItem;
import com.almworks.items.util.AttributeMap;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class BranchUtil {
  private static final TypedKey<BranchUtil> KEY = TypedKey.create("branches");
  private final TLongObjectHashMap<VersionReader> myTrunk;
  private final TLongObjectHashMap<VersionReader> myServer;
  private final HolderCache myHolders;

  protected BranchUtil(TLongObjectHashMap<VersionReader> trunk, TLongObjectHashMap<VersionReader> server, HolderCache holders) {
    myTrunk = trunk;
    myServer = server;
    myHolders = holders;
  }

  public abstract DBReader getReader();

  @SuppressWarnings({"unchecked"})
  public static BranchUtil instance(DBReader reader) {
    Map map = reader.getTransactionCache();
    BranchUtil util = KEY.getFrom(map);
    if (util == null) {
      util = new Read(reader);
      KEY.putTo(map, util);
    }
    return util;
  }

  @SuppressWarnings({"unchecked"})
  public static Write instance(DBWriter writer) {
    Map map = writer.getTransactionCache();
    BranchUtil existing = KEY.getFrom(map);
    if (existing instanceof Write) return (Write) existing;
    Write util;
    util = existing == null ? new Write(writer) :
      new Write(writer, existing.myTrunk, existing.myServer, existing.myHolders);
    KEY.putTo(map, util);
    return util;
  }

  public VersionReader getExisting(long item, Branch branch) {
    return getCache(branch).get(item);
  }

  protected TLongObjectHashMap<VersionReader> getCache(Branch branch) {
    return branch.isServer() ? myServer : myTrunk;
  }

  @Deprecated
  @NotNull
  public ItemVersion readItem(long item, Branch branch) {
    return readItem(item, branch.isServer());
  }

  @NotNull
  public ItemVersion readItem(long item, boolean server) {
    if (item <= 0) return new IllegalItem(BranchSource.instance(getReader(), server), item);
    return read(item, server);
  }

  @NotNull
  public VersionReader read(long item, boolean server) {
    if (item <= 0) throw new IllegalArgumentException(String.valueOf(item));
    Branch branch = server ? Branch.SERVER : Branch.TRUNK;
    VersionReader version = getExisting(item, branch);
    if (version != null) return version;
    VersionReader newVersion = server ? new ServerReader(myHolders, item) : new ShadowReader(myHolders, null, item, Branch.TRUNK);
    getCache(branch).put(item, newVersion);
    return newVersion;
  }

  @Deprecated
  @NotNull
  public VersionReader read(long item, Branch branch) {
    if (item <= 0) throw new IllegalArgumentException(String.valueOf(item));
    VersionReader version = getExisting(item, branch);
    if (version != null) return version;
    VersionReader newVersion;
    switch (branch) {
    case TRUNK: newVersion = new ShadowReader(myHolders, null, item, Branch.TRUNK); break;
    case SERVER: newVersion = new ServerReader(myHolders, item); break;
    default: Log.error("Wrong branch " + branch); return read(item, false);
    }
    getCache(branch).put(item, newVersion);
    return newVersion;
  }

  public static ItemVersion readServerShadow(DBReader reader, long item, DBAttribute<AttributeMap> shadow, boolean emptyIfNotExists) {
    BranchUtil util = instance(reader);
    VersionReader version = util.getExisting(item, Branch.SERVER);
    HolderCache holders = util.myHolders;
    if (version != null && Util.equals(shadow, version.getShadow())) return version;
    VersionHolder holder = holders.getHolder(item, shadow, emptyIfNotExists);
    if (holder == null) return null;
    return new ShadowReader(holders, shadow, item, Branch.SERVER);
  }

  @Nullable
  public ItemVersion readServerIfExists(long item) {
    if (item <= 0) return null;
    VersionHolder holder = myHolders.getServerHolder(item);
    return holder == null ? null : read(item, true);
  }

  protected void addHolder(VersionHolder holder) {
    myHolders.addHolder(holder);
  }

  protected VersionHolder getServerHolder(long item) {
    return this.myHolders.getServerHolder(item);
  }

  protected VersionHolder getHolder(long item, DBAttribute<AttributeMap> shadow, boolean returnEmpty) {
    return this.myHolders.getHolder(item, shadow, returnEmpty);
  }

  private static class Read extends BranchUtil {
    private final DBReader myReader;

    private Read(DBReader reader) {
      super(new TLongObjectHashMap<>(), new TLongObjectHashMap<>(),
        HolderCache.instance(reader));
      myReader = reader;
    }

    public DBReader getReader() {
      return myReader;
    }
  }

  public static class Write extends BranchUtil {
    private final DBWriter myWriter;

    public Write(DBWriter writer) {
      this(writer, new TLongObjectHashMap<>(), new TLongObjectHashMap<>(),
        HolderCache.instance(writer));
    }

    public Write(DBWriter writer, TLongObjectHashMap<VersionReader> trunk, TLongObjectHashMap<VersionReader> server, HolderCache holders)
    {
      super(trunk, server, holders);
      myWriter = writer;
    }

    @Override
    public DBReader getReader() {
      return myWriter;
    }

    public ItemVersionCreator newItem(BaseDBDrain drain) {
      long item = myWriter.nextItem();
      VersionHolder.WriteTrunk holder = new VersionHolder.WriteTrunk(myWriter, item, drain, true);
      this.addHolder(holder);
      VersionWriter creator = new VersionWriter(drain, holder);
      this.getCache(drain.getBranch()).put(item, creator);
      return creator;
    }

    public ItemVersionCreator write(long item, BaseDBDrain drain) {
      Branch branch = drain.getBranch();
      VersionReader existing = getCache(branch).get(item);
      if (existing instanceof ItemVersionCreator) return (ItemVersionCreator) existing;
      VersionHolder.Write holder;
      if (!branch.isServer()) holder = new VersionHolder.WriteTrunk(myWriter, item, drain, false);
      else {
        VersionHolder readHolder = getServerHolder(item);
        if (readHolder == null) readHolder = getHolder(item, null, true);
        if (readHolder instanceof VersionHolder.Write) Log.error("Duplicated write " + readHolder);
        holder = new VersionHolder.WriteShadow(myWriter, item, readHolder, drain);
      }
      addHolder(holder);
      VersionWriter creator = new VersionWriter(drain, holder);
      this.getCache(branch).put(item, creator);
      return creator;
    }

    public ItemVersionCreator dummyCreator(long item, BaseDBDrain drain) {
      Branch branch = drain.getBranch();
      VersionReader existing = getExisting(item, branch);
      if (existing instanceof ItemVersionCreator) return (ItemVersionCreator) existing;
      if (existing == null) existing = read(item, branch.isServer());
      DummyWriter writer = new DummyWriter(drain, existing.getHolder());
      this.getCache(branch).put(item, writer);
      return writer;
    }
  }

  private static class ShadowReader extends VersionReader {
    private final Branch myBranch;
    private final DBAttribute<AttributeMap> myShadow;
    private final HolderCache myHolders;
    private final long myItem;

    private ShadowReader(HolderCache holders, DBAttribute<AttributeMap> shadow, long item, Branch branch) {
      if (item <= 0) throw new IllegalArgumentException(String.valueOf(item));
      myHolders = holders;
      myShadow = shadow;
      myItem = item;
      myBranch = branch;
    }

    @NotNull
    @Override
    protected Branch getBranch() {
      return myBranch;
    }

    @Override
    public VersionHolder getHolder() {
      return myHolders.getHolder(myItem, myShadow, true);
    }

    @Override
    public long getItem() {
      return myItem;
    }
  }

  private static class ServerReader extends VersionReader {
    private final HolderCache myHolders;
    private final long myItem;

    private ServerReader(HolderCache holders, long item) {
      if (item <= 0) throw new IllegalArgumentException(String.valueOf(item));
      myHolders = holders;
      myItem = item;
    }

    @NotNull
    @Override
    protected Branch getBranch() {
      return Branch.SERVER;
    }

    @Override
    public VersionHolder getHolder() {
      VersionHolder existing = myHolders.getServerHolder(myItem);
      return existing != null ? existing : myHolders.getHolder(myItem, null, true);
    }

    @Override
    public long getItem() {
      return myItem;
    }
  }

}
