package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.integers.IntList;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ProjectsAndTypes {
  private final IntArray myProjectIds = new IntArray();
  private final List<IntArray> myTypesInProject = Collections15.arrayList();
  private final List<String> myProjectKeys = Collections15.arrayList();
  private final List<String> myProjectNames = Collections15.arrayList();
  private final IntArray myAllTypes = new IntArray();

  public boolean isKnownType(int typeId) {
    return myAllTypes.binarySearch(typeId) >= 0;
  }

  void addProject(int id, String key, String name) {
    int index = myProjectIds.binarySearch(id);
    if (index < 0) {
      index = -index - 1;
      myProjectIds.insert(index, id);
      myProjectKeys.add(index, null);
      myProjectNames.add(index, null);
      myTypesInProject.add(index, null);
    } else LogHelper.error("Already known project", id);
    LogHelper.assertError(key != null && name != null, "Empty value", key, name);
    myProjectKeys.set(index, key);
    myProjectNames.set(index, name);
  }

  public void setProjectTypes(int prjId, IntList issueTypeIds) {
    int index = getProjectIndex(prjId);
    if (index < 0) return;
    myTypesInProject.set(index, IntArray.copy(issueTypeIds));
    boolean changed = false;
    for (IntIterator cursor : issueTypeIds) {
      if (!isKnownType(cursor.value())) {
        changed = true;
        break;
      }
    }
    if (changed) {
      myAllTypes.addAll(issueTypeIds);
      myAllTypes.sortUnique();
    }
  }

  private int getProjectIndex(int prjId) {
    int index = myProjectIds.binarySearch(prjId);
    LogHelper.assertError(index >= 0, "Missing project", prjId);
    return index;
  }

  @NotNull
  public IntList getProjectIds() {
    return myProjectIds;
  }

  public List<String> projectKeys() {
    return Collections.unmodifiableList(myProjectKeys);
  }

  public int getProjectCount() {
    return myProjectIds.size();
  }

  @Nullable
  public IntList getTypes(int prjId) {
    int index = getProjectIndex(prjId);
    if (index < 0) return null;
    IntArray types = myTypesInProject.get(index);
    LogHelper.assertError(types != null, "No types for project", prjId, myProjectKeys.get(index), myProjectNames.get(index));
    return types;
  }

  @Nullable
  public String getProjectName(int projectId) {
    int index = getProjectIndex(projectId);
    return index >= 0 ? myProjectNames.get(index) : null;
  }

  @Nullable
  public String getDisplayableProjectNameAt(int index) {
    String name = myProjectNames.get(index);
    if (name == null) name = myProjectKeys.get(index);
    return name;
  }

  @Nullable
  public String getProjectKeyAt(int index) {
    return myProjectKeys.get(index);
  }

  public int getProjectIdAt(int index) {
    return myProjectIds.get(index);
  }
}
