package com.almworks.recentitems.gui;

import com.almworks.api.application.LoadedItem;
import com.almworks.recentitems.RecordType;
import org.almworks.util.detach.Detach;

import java.util.Date;

class LoadedRecord {
  final Date myTimestamp;
  final LoadedItem myItem;
  final Detach myLife;
  final RecordType myType;
  final long myKey;

  LoadedRecord(Date timestamp, LoadedItem item, Detach life, RecordType type, long key) {
    myTimestamp = timestamp;
    myItem = item;
    myLife = life;
    myType = type;
    myKey = key;
  }
}
