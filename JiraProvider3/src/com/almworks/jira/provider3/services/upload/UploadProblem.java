package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.SyncProblem;
import com.almworks.jira.provider3.app.sync.ExceptionItemProblem;
import com.almworks.spi.provider.ConnectionContext;
import com.almworks.spi.provider.GeneralItemProblem;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class UploadProblem {
  private static final LocalizedAccessor.Value M_INTERNAL_ERROR_SHORT = JiraUploadComponent.I18N.getFactory("upload.problem.internalError.short");
  public static final LocalizedAccessor.MessageInt M_SERVER_ERROR_SHORT = JiraUploadComponent.I18N.messageInt("upload.problem.serverErrorCode.short");
  public static final LocalizedAccessor.MessageIntStr M_SERVER_ERROR_FULL = JiraUploadComponent.I18N.messageIntStr("upload.problem.serverErrorCode.full");
  public static final LocalizedAccessor.Value M_PARSE_SHORT = JiraUploadComponent.I18N.getFactory("upload.problem.parse.short");
  public static final LocalizedAccessor.Value M_PARSE_FULL = JiraUploadComponent.I18N.getFactory("upload.problem.parse.full");

  private final ConnectorException myException;
  private final ItemSyncProblem.Cause myCause;
  private final long myTime;
  private final boolean myTemporary;
  private final String myShortDescription;
  private final String myLongDescription;
  private final long myActualItem;

  private UploadProblem(ConnectorException exception, ItemSyncProblem.Cause cause, boolean temporary, String shortDescription, @Nullable String longDescription, long actualItem) {
    myException = exception;
    myCause = cause;
    myTemporary = temporary;
    myActualItem = actualItem;
    myTime = System.currentTimeMillis();
    myShortDescription = shortDescription;
    myLongDescription = longDescription;
  }

  @NotNull
  Collection<SyncProblem> toSyncProblems(UploadContextImpl config, UploadUnit unit) {
    Collection<Pair<Long, String>> masters = unit.getMasterItems();
    if (masters.isEmpty()) {
      LogHelper.error("No master items", unit);
      return Collections.emptyList();
    }
    Pair<Long, String> singleMaster = findSingleMaster(config, masters);
    if (singleMaster != null) return Collections.singleton(toSyncProblem(config, singleMaster));
    ArrayList<SyncProblem> result = Collections15.arrayList();
    for (Pair<Long, String> master : masters) {
      Long item = master.getFirst();
      String displayable = master.getSecond();
      if (item == null || displayable == null) LogHelper.error("Cannot show unit problem", unit, master);
      else {
        SyncProblem problem = toSyncProblem(config, master);
        if (problem != null) result.add(problem);
        else LogHelper.error("No problem", unit, this);
      }
    }
    return result;
  }

  /**
   * DB item related to the problem. This value can be null if the problem is not associated with the only DB item.<br>
   * {@link #conflict(long, String, String) Conflict} problem must has valid item because it always associated with some DB item.
   * @return associated DB item or 0 if no such item
   */
  public long getActualItem() {
    return myActualItem;
  }

  public ItemSyncProblem.Cause getCause() {
    return myCause;
  }

  public boolean isTemporary() {
    return myTemporary;
  }

  @Nullable
  private Pair<Long, String> findSingleMaster(UploadContextImpl config, Collection<Pair<Long, String>> masters) {
    if (masters == null || masters.isEmpty()) return null;
    if (masters.size() == 1) return masters.iterator().next();
    Pair<Long, String> single = null;
    for (Pair<Long, String> master : masters) {
      if (config.isInitiallyRequested(master.getFirst())) {
        if (single == null) single = master;
        else return null; // Second initially requested master found
      }
    }
    return single;
  }

  @Nullable
  private SyncProblem toSyncProblem(UploadContextImpl config, Pair<Long, String> itemInfo) {
    ConnectionContext context = config.getConnection().getContext();
    long item = itemInfo.getFirst();
    String displayableItem = itemInfo.getSecond();
    if (myException != null) return new ExceptionItemProblem(item, displayableItem, myException, config.getConnection(), myCause);
    if (myShortDescription != null || myLongDescription != null) {
      String shortDescription = myShortDescription;
      String longDescription = myLongDescription;
      if (shortDescription == null) shortDescription = myLongDescription;
      if (longDescription == null) longDescription = myShortDescription;
      return new GeneralItemProblem(item, displayableItem, myTime, context, config.getConnection().getCredentialState(), shortDescription, longDescription, myCause);
    }
    LogHelper.error("Illegal problem", itemInfo);
    return null;
  }

  public static UploadProblem exception(ConnectorException e) {
    return new UploadProblem(e, ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE, false, null, null, 0);
  }

  public static UploadProblem notNow(String reason) {
    return new UploadProblem(null, ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE, true, reason, null, 0);
  }
  
  public static UploadProblem fatal(String shortDescription, @Nullable String longDescription) {
    return fatal(ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE, shortDescription, longDescription);
  }

  public static UploadProblem illegalData(String shortDescription, @Nullable String longDescription) {
    return fatal(ItemSyncProblem.Cause.ILLEGAL_DATA, shortDescription, longDescription);
  }

  private static UploadProblem fatal(ItemSyncProblem.Cause failure, String shortDescription, String longDescription) {
    return new UploadProblem(null, failure, false, shortDescription, longDescription, 0);
  }

  public static UploadProblem conflict(long conflictItem, String shortDescription, @Nullable String longDescription) {
    return new UploadProblem(null, ItemSyncProblem.Cause.UPLOAD_CONFLICT, false, shortDescription, longDescription, conflictItem);
  }

  public static UploadProblem internalError() {
    return new UploadProblem(null, ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE, false, M_INTERNAL_ERROR_SHORT.create(), null, 0);
  }

  /**
   * An upload problem cause by failure of another unit
   */
  public static UploadProblem stop(UploadUnit dependency) {
    return new UploadProblem(null, ItemSyncProblem.Cause.GENERIC_UPLOAD_FAILURE, false, M_INTERNAL_ERROR_SHORT.create(), null, 0);
  }

  public Thrown toException() {
    return new Thrown(this);
  }

  public UploadProblem toFatalProblem() {
    if (!myTemporary) return this;
    // Seems this may happen only due to internal error. Update cause if the assumption is not right.
    return new UploadProblem(null, ItemSyncProblem.Cause.COMPATIBILITY, false, "Upload was not started", "Upload was not started due to internal error", myActualItem);
  }

  public Collection<? extends UploadProblem> toCollection() {
    return Collections.singleton(this);
  }

  @Override
  public String toString() {
    return "Problem[" + (myTemporary ? "temp," : "") + myShortDescription + "]";
  }

  public static UploadProblem serverError(String operation, int statusCode, String statusText) {
    return fatal(operation + ": " + M_SERVER_ERROR_SHORT.formatMessage(statusCode), operation + ". " + M_SERVER_ERROR_FULL.formatMessage(statusCode, statusText));
  }

  public static UploadProblem parseProblem(String operation) {
    return fatal(operation + ": " + M_PARSE_SHORT.create(), operation + ". " + M_PARSE_FULL.create());
  }

  public static class Thrown extends Exception {
    private final UploadProblem myProblem;

    public Thrown(UploadProblem problem) {
      myProblem = problem;
    }

    public UploadProblem getProblem() {
      return myProblem;
    }
  }
}
