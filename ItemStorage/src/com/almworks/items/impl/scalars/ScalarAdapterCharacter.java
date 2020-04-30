package com.almworks.items.impl.scalars;

import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ScalarAdapterCharacter extends ScalarValueAdapter<Character> {
  private final static char ZERO = (char)0;
  @Override
  public Class<Character> getAdaptedClass() {
    return Character.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return INT_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return true;
  }

  @Override
  public Character loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context) throws SQLiteException {
    Integer codePoint = select.columnInt(columnIndex);
    if (codePoint == null) return null;
    if (!Character.isValidCodePoint(codePoint)) return null;
    char[] chars = Character.toChars(codePoint);
    if (chars.length != 1) return null;
    return chars[0];
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, Character userValue, TransactionContext context) throws SQLiteException {
    if (userValue == null) {
      Log.error("cannot bind null");
      userValue = 0;
    }
    int v = userValue.charValue();
    statement.bind(bindIndex, v);
  }

  @Override
  protected Character readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    char c = CompactChar.readChar(in);
    if (c != ZERO) return c;
    // distinguish between 0 and null
    char litmus = CompactChar.readChar(in);
    return litmus == ZERO ? null : Character.valueOf(ZERO);
  }

  @Override
  protected void writeValueToStream(DataOutput out, Character userValue, TransactionContext context) throws IOException {
    if (userValue == null) {
      CompactChar.writeChar(out, ZERO);
      CompactChar.writeChar(out, ZERO);
    } else if (userValue == ZERO) {
      CompactChar.writeChar(out, ZERO);
      CompactChar.writeChar(out, 'a');
    } else {
      CompactChar.writeChar(out, userValue);
    }
  }

  @Override
  public Object toSearchValue(Character userValue) {
    return userValue == null ? null : (int) userValue.charValue();
  }
}
