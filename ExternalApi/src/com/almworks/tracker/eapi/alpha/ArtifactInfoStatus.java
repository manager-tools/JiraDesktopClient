package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.ExternalizableEnum;

/**
 * An enumeration that designates whether ArtifactInfo is holding information, and if not - why not.
 */
public class ArtifactInfoStatus extends ExternalizableEnum {
  /**
   * Given URL is not supported by Deskzilla and its plug-ins.
   */
  public static final ArtifactInfoStatus UNRECOGNIZED_URL = new ArtifactInfoStatus("UNRECOGNIZED_URL");

  /**
   * There's no connection that would have retrieved the given URL.
   *
   * @see ArtifactLoadOption#MAYBE_CREATE_CONNECTION_AND_DOWNLOAD
   */
  public static final ArtifactInfoStatus NO_CONNECTION = new ArtifactInfoStatus("NO_CONNECTION");

  /**
   * There's no artifact in the local database that corresponds to the URL.
   *
   * @see ArtifactLoadOption#MAYBE_DOWNLOAD
   */
  public static final ArtifactInfoStatus NO_ARTIFACT = new ArtifactInfoStatus("NO_ARTIFACT");

  /**
   * Artifact information is provided in other ArtifactInfo fields.
   */
  public static final ArtifactInfoStatus OK = new ArtifactInfoStatus("OK");

  /**
   * Connection setup is in progress. Another event will come shortly.
   */
  public static final ArtifactInfoStatus WAIT_CONNECTION_SETUP = new ArtifactInfoStatus("WAIT_CONNECTION_SETUP");

  /**
   * Artifact download is in progress. Another event will come shortly.
   */
  public static final ArtifactInfoStatus WAIT_DOWNLOADING = new ArtifactInfoStatus("DOWNLOADING");

  private ArtifactInfoStatus(String externalName) {
    super(externalName);
  }

  public static ArtifactInfoStatus forExternalName(String name) {
    return (ArtifactInfoStatus) forExternalName(ArtifactInfoStatus.class, name);
  }

  public boolean isOk() {
    return this == OK;
  }
}
