package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.ExternalizableEnum;

/**
 * Combination of these options are used when requesting artifact info from Deskzilla.
 * <p>
 * Use preset combinations from this class.
 */
public class ArtifactLoadOption extends ExternalizableEnum {
  /**
   * If artifact's connection is not configured, try to create it automatically.
   */
  public static final ArtifactLoadOption MAY_CREATE_CONNECTION = new ArtifactLoadOption("MAY_CREATE_CONNECTION");

  /**
   * If there's no such artifact in the local database, try to download it from a remote server.
   */
  public static final ArtifactLoadOption MAY_DOWNLOAD = new ArtifactLoadOption("MAY_DOWNLOAD");

  public static final ArtifactLoadOption[] NONE = {};
  public static final ArtifactLoadOption[] MAYBE_DOWNLOAD = {MAY_DOWNLOAD};
  public static final ArtifactLoadOption[] MAYBE_CREATE_CONNECTION_AND_DOWNLOAD = {MAY_CREATE_CONNECTION, MAY_DOWNLOAD};

  private ArtifactLoadOption(String externalName) {
    super(externalName);
  }

  public static ArtifactLoadOption forExternalName(String name) {
    return (ArtifactLoadOption) forExternalName(ArtifactLoadOption.class, name);
  }
}
