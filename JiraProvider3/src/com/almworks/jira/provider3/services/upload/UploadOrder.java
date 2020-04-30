package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.CancelledException;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.i18n.text.LocalizedAccessor;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;

import java.util.*;

class UploadOrder {
  private static final LocalizedAccessor.MessageStr M_PROGRESS_BLOCK = JiraUploadComponent.I18N.messageStr("upload.info.progress.block");
  private final ProgressInfo myWholeProgress;
  private final List<UnitsBlock> myBlocks;
  private final ArrayList<UploadUnit> myOtherUnits;
  private final ProgressInfo myOtherProgress;
  private final Map<UploadUnit, UploadProblem> myWaitReason = Collections15.hashMap();
  private final Set<UploadUnit> myCompleted = Collections15.hashSet();
  private UnitsBlock myCurrentBlock = null;

  private UploadOrder(ProgressInfo progress, List<UnitsBlock> blocks, ArrayList<UploadUnit> otherUnits, ProgressInfo otherProgress) {
    myWholeProgress = progress;
    myBlocks = blocks;
    myOtherUnits = otherUnits;
    myOtherProgress = otherProgress;
  }


  public int getBlockCount() {
    return myBlocks.size();
  }

  public int getCompleteCount() {
    return myCompleted.size();
  }

  public Collection<UploadUnit> startBlock(int i) throws CancelledException {
    UnitsBlock block = myBlocks.get(i);
    myCurrentBlock = block;
    ArrayList<UploadUnit> result = Collections15.arrayList(block.myUnits);
    if (!result.isEmpty()) myWholeProgress.startActivity(M_PROGRESS_BLOCK.formatMessage(block.myDisplayableMaster));
    return result;
  }

  public Collection<UploadUnit> startOtherUnits() throws CancelledException {
    myCurrentBlock = null;
    ArrayList<UploadUnit> result = Collections15.arrayList(myOtherUnits);
    if (!result.isEmpty()) myWholeProgress.startActivity(M_PROGRESS_BLOCK.formatMessage(""));
    return result;
  }

  public void onUnitComplete(UploadUnit unit) {
    if (myCompleted.contains(unit)) return;
    if (myCurrentBlock != null && completeBlockUnit(myCurrentBlock, unit)) return;
    if (myOtherUnits.contains(unit)) {
      myOtherUnits.remove(unit);
      myOtherProgress.spawn(1.0/(myOtherUnits.size() + 1)).setDone();
      myWaitReason.remove(unit);
      myCompleted.add(unit);
      return;
    }
    UnitsBlock block = findBlock(unit);
    if (block != null) {
      LogHelper.error("Unit complete out of order", unit);
      completeBlockUnit(myCurrentBlock, unit);
      return;
    }
    LogHelper.error("Unknown unit complete", unit);
  }

  public void onUnitWaits(UploadUnit unit, UploadProblem problem) {
    myWaitReason.put(unit, problem);
  }

  private UnitsBlock findBlock(UploadUnit unit) {
    for (UnitsBlock block : myBlocks) if (block.myUnits.contains(unit)) return block;
    return null;
  }

  private boolean completeBlockUnit(UnitsBlock block, UploadUnit unit) {
    if (!block.myUnits.remove(unit)) return false;
    block.myProgress.spawn(1.0 / (block.myUnits.size() + 1)).setDone();
    myWaitReason.remove(unit);
    myCompleted.add(unit);
    return true;
  }

  public static UploadOrder prepare(List<UploadUnit> units, ProgressInfo progress) {
    ArrayList<UploadUnit> otherUnits = Collections15.arrayList();
    TLongObjectHashMap<UnitsBlock> blocks = new TLongObjectHashMap<>();
    for (UploadUnit unit : units) {
      Collection<Pair<Long,String>> masterItems = unit.getMasterItems();
      Pair<Long,String> singleMaster = masterItems.size() == 1 ? masterItems.iterator().next() : null;
      Long item = singleMaster != null ? singleMaster.getFirst() : null;
      if (item == null || item < 0) {
        LogHelper.assertError(singleMaster == null, "Wrong master item", singleMaster);
        otherUnits.add(unit);
        continue;
      }
      UnitsBlock block = blocks.get(item);
      if (block == null) {
        block = new UnitsBlock(item, singleMaster.getSecond());
        blocks.put(item, block);
      }
      block.myUnits.add(unit);
    }
    //noinspection unchecked
    List<UnitsBlock> unitsByMaster = (List<UnitsBlock>)(List<?>)Arrays.asList(blocks.getValues());
    int total = units.size();
    progress.split(total);
    for (UnitsBlock block : unitsByMaster) {
      int count = block.myUnits.size();
      block.myProgress = progress.spawn(((double) count) / total);
      total -= count;
    }
    ProgressInfo otherProgress = progress.spawnAll();
    return new UploadOrder(progress, unitsByMaster, otherUnits, otherProgress);
  }

  public Collection<UploadUnit> getLeftUnits() {
    ArrayList<UploadUnit> result = Collections15.arrayList();
    for (UnitsBlock block : myBlocks) {
      result.addAll(block.myUnits);
    }
    result.addAll(myOtherUnits);
    return result;
  }

  public void logNotDone(UploadContextImpl context) {
    for (UploadUnit unit : getLeftUnits()) {
      UploadProblem problem = myWaitReason.get(unit);
      if (problem != null) {
        context.addProblem(unit, problem.toFatalProblem());
        LogHelper.warning("Still waiting upload", unit, problem);
      } else LogHelper.error("Not upload without any reason", unit);
    }
  }

  private static class UnitsBlock {
    private final long myMasterItem;
    private final String myDisplayableMaster;
    private final List<UploadUnit> myUnits = Collections15.arrayList();
    public ProgressInfo myProgress;

    private UnitsBlock(long masterItem, String displayableMaster) {
      myMasterItem = masterItem;
      myDisplayableMaster = displayableMaster;
    }
  }
}
