package com.almworks.restconnector.json.sax;

import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public interface LocationHandler {
  /**
   * @param what what does start or end
   * @param start true - start, false - end. Not applicable to {@link com.almworks.restconnector.json.sax.LocationHandler.Location#PRIMITIVE}, must be true.
   * @param key applicable to {@link com.almworks.restconnector.json.sax.LocationHandler.Location#ENTRY}, object entry name. Null for other location kinds
   * @param value applicable to {@link com.almworks.restconnector.json.sax.LocationHandler.Location#PRIMITIVE} value of the primitive. Null for other location kinds
   */
  void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException;

  enum Location {
    TOP,
    OBJECT,
    ENTRY,
    ARRAY,
    PRIMITIVE;

    public void delegate(ContentHandler handler, boolean start, String key, Object value) throws IOException, ParseException {
      if (start)
        switch (this) {
        case TOP: handler.startJSON(); break;
        case OBJECT: handler.startObject(); break;
        case ENTRY: handler.startObjectEntry(key); break;
        case ARRAY: handler.startArray(); break;
        case PRIMITIVE: handler.primitive(value);break;
        default:
          LogHelper.error("Unknown location", this, start, key, value);
          throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
        }
      else
        switch (this) {
        case TOP: handler.endJSON(); break;
        case OBJECT: handler.endObject(); break;
        case ENTRY: handler.endObjectEntry(); break;
        case ARRAY: handler.endArray(); break;
        case PRIMITIVE:
        default:
          LogHelper.error("Unknown location", this, start, key, value);
          throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
        }
    }

    public static void startJSON(LocationHandler handler) throws IOException, ParseException {
      handler.visit(TOP, true, null, null);
    }

    public static void endJSON(LocationHandler handler) throws IOException, ParseException {
      handler.visit(TOP, false, null, null);
    }
  }

  class ContentAdapter implements ContentHandler {
    private final LocationHandler myHandler;

    public ContentAdapter(LocationHandler handler) {
      myHandler = handler;
    }

    @Override
    public void startJSON() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.TOP, true, null, null);
    }

    @Override
    public void endJSON() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.TOP, false, null, null);
    }

    @Override
    public boolean startObject() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.OBJECT, true, null, null);
      return true;
    }

    @Override
    public boolean endObject() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.OBJECT, false, null, null);
      return true;
    }

    @Override
    public boolean startObjectEntry(String key) throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.ENTRY, true, key, null);
      return true;
    }

    @Override
    public boolean endObjectEntry() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.ENTRY, false, null, null);
      return true;
    }

    @Override
    public boolean startArray() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.ARRAY, true, null, null);
      return true;
    }

    @Override
    public boolean endArray() throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.ARRAY, false, null, null);
      return true;
    }

    @Override
    public boolean primitive(Object value) throws ParseException, IOException {
      myHandler.visit(LocationHandler.Location.PRIMITIVE, true, null, value);
      return true;
    }
  }
}
