package com.almworks.tracker.alpha;

import com.almworks.tracker.eapi.alpha.ArtifactInfo;
import com.almworks.tracker.eapi.alpha.ArtifactInfoStatus;
import com.almworks.tracker.eapi.alpha.ArtifactPresentationKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class ArtifactInfoImpl implements ArtifactInfo {
  private final ArtifactInfoStatus myStatus;
  private final String myUrl;
  private final long myTimestamp;
  private final Map<ArtifactPresentationKey<?>, ?> myProperties = new HashMap<ArtifactPresentationKey<?>, Object>();

  private boolean myReadOnly = false;

  public ArtifactInfoImpl(ArtifactInfoStatus status, String url, long timestamp)
  {
    myStatus = status;
    myUrl = url;
    myTimestamp = timestamp;
  }

  public <T> void setPresentation(ArtifactPresentationKey<T> key, T value) {
    assert !myReadOnly : this;
    if (myReadOnly)
      return;
    key.putToMap(myProperties, value);
  }

  public void setReadOnly() {
    myReadOnly = true;
  }

  public String getUrl() {
    return myUrl;
  }

  public long getTimestamp() {
    return myTimestamp;
  }

  public ArtifactInfoStatus getStatus() {
    return myStatus;
  }

  public <T> T getPresentation(ArtifactPresentationKey<T> key) {
    return key.getFromMap(myProperties);
  }


  public Collection<ArtifactPresentationKey<?>> getApplicableKeys() {
    return myProperties.keySet();
  }
}
