package com.almworks.items.sync.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author dyoma
 */
public class BranchSource extends BasicVersionSource {
  private static final TypedKey<Map<Boolean, BranchSource>> BRANCHES = TypedKey.create("branchSources");
  private final DBReader myReader;
  private final boolean myServer;

  private BranchSource(DBReader reader, boolean server) {
    myReader = reader;
    myServer = server;
  }

  @NotNull
  public static BranchSource trunk(DBReader reader) {
    return instance(reader, false);
  }

  @NotNull
  public static BranchSource server(DBReader reader) {
    return instance(reader, true);
  }

  @NotNull
  public static BranchSource instance(DBReader reader, boolean server) {
    Map cache = reader.getTransactionCache();
    Map<Boolean, BranchSource> map = BRANCHES.getFrom(cache);
    if (map == null) {
      map = Collections15.hashMap();
      BRANCHES.putTo(cache, map);
    }
    BranchSource source = map.get(server);
    if (source == null) {
      source = new BranchSource(reader, server);
      map.put(server, source);
    }
    return source;
  }

  @NotNull
  @Override
  public DBReader getReader() {
    return myReader;
  }

  @NotNull
  @Override
  public ItemVersion forItem(long item) {
    return myServer ? SyncUtils.readServer(myReader, item) : SyncUtils.readTrunk(myReader, item);
  }
}
