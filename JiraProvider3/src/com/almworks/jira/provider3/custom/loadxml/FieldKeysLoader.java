package com.almworks.jira.provider3.custom.loadxml;

import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.FieldType;
import com.almworks.jira.provider3.custom.impl.TypeConfigSchema;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldKeysLoader extends DefaultHandler {
  private static final ConfigKey<String> EDITOR = ConfigKey.string(ConfigKeys.EDITOR);
  private static final ConfigKey<String> UPLOAD = ConfigKey.string(ConfigKeys.UPLOAD);
  private static final ConfigKey<String> UPLOAD_JSON = ConfigKey.string(ConfigKeys.UPLOAD_JSON);
  @SuppressWarnings("unchecked")
  private static final List<ConfigKey<?>> TAG_EDITABLE = Collections15.<ConfigKey<?>>unmodifiableListCopy(EDITOR, UPLOAD, UPLOAD_JSON);
  public static final String ROOT_TAG = "fields";
  public static final String A_VERSION = "version";
  public static final String A_REVISION = "revision";
  public static final String EXPECTED_VERSION = "3.0.1";
  public static final String TAG_FIELD = "field";

  public static final FieldKind UNKNOWN = new UnknownFieldKind();

  private final TypeConfigSchema mySchema;
  private final List<Pair<String, Map<TypedKey<?>, ?>>> myLoaded = Collections15.arrayList();
  private boolean myVersionChecked = false;
  private int myRevision = -1;
  private Locator myLocator;
  private Map<TypedKey<?>, ?> myCurrentField;

  @NotNull
  public static FieldKeysLoader load(String resource, TypeConfigSchema schema) throws SAXException, ParserConfigurationException, IOException {
    InputStream stream = FieldKeysLoader.class.getResourceAsStream(resource);
    return load(new InputSource(stream), schema);
  }

  @NotNull
  public static FieldKeysLoader load(InputSource source, TypeConfigSchema schema) throws SAXException, ParserConfigurationException, IOException {
    FieldKeysLoader handler = new FieldKeysLoader(schema);
    SAXParserFactory.newInstance().newSAXParser().parse(source, handler);
    return handler;
  }

  public FieldKeysLoader(TypeConfigSchema schema) {
    mySchema = schema;
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    myLocator = locator;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    try {
      if (ROOT_TAG.equals(qName)) processRootTag(attributes);
      else if (TAG_FIELD.equals(qName)) {
        if (!myVersionChecked) {
          LogHelper.error("Missing version");
          throw new SAXException("Missing version");
        }
        if (myCurrentField != null) throw error("Tag '" + qName + "' not allowed here");
        myCurrentField = loadMap(attributes, mySchema.getAllConfigKeys());
      } else if ("editable".equals(qName)) {
        if (myCurrentField == null) throw error("Tag 'editable' requires outer tag 'field'");
        Map<TypedKey<?>, ?> map = loadMap(attributes, TAG_EDITABLE);
        ConfigKeys.EDITABLE.putTo(myCurrentField, map);
      } else throw error("Unknown tag '" + qName + "'");
    } catch (LoadProblem loadProblem) {
      throw error("Field description error", loadProblem);
    }
  }

  private void processRootTag(Attributes attributes) throws SAXException {
    String version = attributes.getValue(A_VERSION);
    if (EXPECTED_VERSION.equals(version)) myVersionChecked = true;
    else {
      LogHelper.error("Unexpected version", version);
      throw new SAXException("Unexpected fields version: " + version);
    }
    String strRevision = attributes.getValue(A_REVISION);
    int revision;
    if (strRevision == null) revision = 0;
    else {
      try {
        revision = Integer.parseInt(strRevision);
      } catch (NumberFormatException e) {
        LogHelper.error("Wrong revision", strRevision);
        revision = -2;
      }
    }
    myRevision = revision;
  }

  public static Map<TypedKey<?>, ?> loadMap(Attributes attributes, List<ConfigKey<?>> allowedKeys, String ... ignoreAttributes) throws LoadProblem {
    HashMap<TypedKey<?>, ?> result = Collections15.hashMap();
    for (int i = 0; i < attributes.getLength(); i++) {
      String name = attributes.getQName(i);
      if (ArrayUtil.indexOf(ignoreAttributes, name) >= 0) continue;
      ConfigKey<?> key = ConfigKey.findKey(allowedKeys, name);
      if (key == null) throw new LoadProblem("Unknown attribute '" + name + "'");
      putToMap(result, key, attributes.getValue(i));
    }
    return result;
  }

  private static <T> void putToMap(Map<TypedKey<?>, ?> map, ConfigKey<T> key, String strValue) throws LoadProblem {
    T value = key.parseValue(strValue);
    if (value == null) throw new LoadProblem("Attribute '" + key.getDisplayName() + "' does not support value '" + strValue + "'");
    if (!key.putToMap(map, value)) throw new LoadProblem("Value of '" + key.getDisplayName() + "' is overridden with '" + strValue + "'");
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("field".equals(qName)) {
      Map<TypedKey<?>, ?> kindValues = myCurrentField;
      if (kindValues == null) return;
      myCurrentField = null;
      String key = ConfigKeys.KEY.getFrom(kindValues);
      if (key == null) throw error("Missing key");
      String typeName = ConfigKeys.TYPE.getFrom(kindValues);
      if (typeName == null) throw error("Missing type for '" + key + "'");
      FieldType type = mySchema.getTypeByName(typeName);
      if (type == null) throw error("Unknown type '" + typeName + "'");
      checkField(type, kindValues);
      for (Pair<String, ?> pair : myLoaded) if (key.equals(pair.getFirst())) throw error("Duplicated key '" + key + "'");
      try {
        type.createKind(kindValues);
      } catch (FieldType.CreateProblem createProblem) {
        throw error("Field description error", createProblem);
      }
      myLoaded.add(Pair.<String, Map<TypedKey<?>, ?>>create(key, kindValues));
    }
  }

  private void checkField(FieldType type, Map<TypedKey<?>, ?> kind) throws SAXException {
    List<TypedKey<?>> allowedKeys = type.getKeys();
    for (TypedKey<?> typedKey : kind.keySet()) {
      if (allowedKeys.contains(typedKey)) continue;
      if (mySchema.isCommonKey(typedKey)) continue;
      if (ConfigKeys.EDITABLE.equals(typedKey)) continue;
      throw error("Unsupported value '" + typedKey.getName() + "'");
    }
  }

  private SAXException error(String message) {
    return error(message, null);
  }

  private SAXException error(String message, @Nullable Exception details) {
    StringBuilder builder = new StringBuilder();
    builder.append(message).append(" at ").append(myLocator.getLineNumber()).append(":").append(myLocator.getColumnNumber());
    if (details == null) return new SAXException(builder.toString());
    builder.append(" ").append(details.getMessage());
    return new SAXException(builder.toString(), details);
  }

  @NotNull
  public List<Map<TypedKey<?>, ?>> getLoadedKinds() {
    ArrayList<Map<TypedKey<?>, ?>> list = Collections15.arrayList();
    for (Pair<?, Map<TypedKey<?>, ?>> pair : myLoaded) list.add(pair.getSecond());
    return list;
  }
  
  public int getRevision() {
    return myRevision;
  }

  public static class LoadProblem extends Exception {
    public LoadProblem(String message) {
      super(message);
    }
  }
}
