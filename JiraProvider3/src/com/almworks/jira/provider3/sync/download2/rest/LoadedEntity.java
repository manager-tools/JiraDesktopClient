package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.services.upload.UploadJsonUtil;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.Objects;

/**
 * Represents an entity with identity and displayable text.
 * An instance may represent itself as JSON.<br>
 * Implementors MUST implement {@link Object#equals(Object)}
 * @author dyoma
 */
public interface LoadedEntity {
  /**
   * @return human-friendly display name of the entity
   */
  @NotNull
  String getDisplayableText();

  @Override
  int hashCode();

  /**
   * Checks for equality against other LoadedEntity.
   * Entities are equal if they identify the same object.
   * If entities has different values those don't affect identity, the entities MUST be equal.
   */
  @Override
  boolean equals(Object obj);

  /**
   * @return an identifier of the entity to use for upload via HTML forms (when API calls are not available)
   */
  @NotNull
  String getFormValueId();

  /**
   * @return default JSON presentation of this entity. The JSON is expected to contain only identity, so it could be used to update reference fields.
   */
  JSONObject toJson();

  class Simple<I> implements LoadedEntity {
    private final String myJsonIdKey;
    private final I myId;
    private final String myDisplayableText;

    public Simple(String jsonIdKey, I id, String displayableText) {
      myJsonIdKey = jsonIdKey;
      myId = id;
      myDisplayableText = displayableText != null ? displayableText : myId.toString();
    }

    @NotNull
    @Override
    public String getFormValueId() {
      return myId != null ? myId.toString() : "";
    }

    @Override
    public JSONObject toJson() {
      if (myId == null) {
        LogHelper.error("Missing id", this);
        return null;
      }
      return UploadJsonUtil.object(myJsonIdKey, String.valueOf(myId));
    }

    public I getId() {
      return myId;
    }

    @NotNull
    @Override
    public String getDisplayableText() {
      return myDisplayableText;
    }

    @Override
    public int hashCode() {
      return Objects.hash(myJsonIdKey, myId);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      Simple<?> other = Util.castNullable(Simple.class, obj);
      return other != null && myJsonIdKey.equals(other.myJsonIdKey) && Objects.equals(myId, other.myId);
    }

    @Override
    public String toString() {
      return String.format("(%s=%s %s)", myJsonIdKey, myId, myDisplayableText);
    }
  }
}
