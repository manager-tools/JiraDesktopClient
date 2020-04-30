package com.almworks.recentitems;

public enum RecordType {
  NEW_UPLOAD(1),
  EDIT_UPLOAD(2);

  private final int myId;

  RecordType(int id) {
    myId = id;
  }

  int getId() {
    return myId;
  }

  public static RecordType forId(int id) {
    for(final RecordType type : values()) {
      if(type.myId == id) {
        return type;
      }
    }
    assert false : id;
    return null;
  }
}
