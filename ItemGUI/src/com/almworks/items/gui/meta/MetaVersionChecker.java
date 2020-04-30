package com.almworks.items.gui.meta;

import com.almworks.engine.items.DatabaseCheck;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;

class MetaVersionChecker implements DatabaseCheck {
  private static final DBNamespace NS = MetaModule.NS.subNs("versionCheck");
  private static final DBIdentifiedObject VERSION_HOLDER = NS.object("versionHolder");
  private static final DBAttribute<Integer> NUMERIC_VERSION = NS.integer("dbVersion");
  private static final DBAttribute<String> VERSION_NAME = NS.string("dbVersionName");
  private static final int CURRENT_VERSION = 14;
  private static final String CURRENT_VERSION_NAME = "JIRA Client 3.0 Beta 1";
  public static final DatabaseCheck INSTANCE = new MetaVersionChecker();

  @Override
  public void check(DBReader reader, DBProblems problems) {
    long holder = reader.findMaterialized(VERSION_HOLDER);
    Integer version;
    String description;
    if (holder > 0) {
      version = reader.getValue(holder, NUMERIC_VERSION);
      description = reader.getValue(holder, VERSION_NAME);
    } else {
      version = null;
      description = null;
    }
    if (version != null && version == CURRENT_VERSION) {
      LogHelper.debug("Valid meta DB version detected", description);
      return;
    }
    throw problems.addFatalProblem(Local.parse("To upgrade to this version of " + Terms.ref_Deskzilla +
      ", your local database will have to be cleared.\n(Your queries will remain intact.)"));
  }

  @Override
  public void init(DBWriter writer) {
    long holder = writer.materialize(VERSION_HOLDER);
    Integer version = writer.getValue(holder, NUMERIC_VERSION);
    if (version == null) writer.setValue(holder, NUMERIC_VERSION, CURRENT_VERSION);
    String name = writer.getValue(holder, VERSION_NAME);
    if (!CURRENT_VERSION_NAME.equals(name)) writer.setValue(holder, VERSION_NAME, CURRENT_VERSION_NAME);
  }
}
