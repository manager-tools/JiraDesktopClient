package com.almworks.api.application.qb;

import com.almworks.util.ui.DialogEditor;

public interface FilterEditor extends DialogEditor {
  FilterNode getCurrentFilter();

  FilterNode getUpToDateFilter();
}
