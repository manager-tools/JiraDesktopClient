package com.almworks.util;

public interface Terms {
  String key_ConnectionType = "app.term.ConnectionType";
  String ref_ConnectionType = "$(" + key_ConnectionType + ")";

  String key_artifact = "app.term.artifact";
  String ref_artifact = "$(" + key_artifact + ")";

  String key_Artifact = "app.term.Artifact";
  String ref_Artifact = "$(" + key_Artifact + ")";

  String key_artifacts = "app.term.artifacts";
  String ref_artifacts = "$(" + key_artifacts + ")";

  String key_Artifacts = "app.term.Artifacts";
  String ref_Artifacts = "$(" + key_Artifacts + ")";

  String key_Deskzilla = "app.term.Deskzilla";
  String ref_Deskzilla = "$(" + key_Deskzilla + ")";

  String key_Artifact_ID = "app.term.ArtifactID";
  String ref_Artifact_ID = "$(" + key_Artifact_ID + ")";

  String query = "query";
  String queries = English.getPlural(query);
  String Query = English.capitalize(query);
  String Queries = English.capitalize(queries);

  String folder = "folder";
  String Folder = English.capitalize(folder);

  String userName = "login";

  /**
   * @see com.almworks.api.engine.Connection#getMainColumns
   */
  String key_Main_columns = "app.term.MainColumns";
  String ref_Main_columns = "$(" + key_Main_columns + ")";
  /**
   * @see com.almworks.api.engine.Connection#getAuxiliaryColumns
   */
  String key_Auxiliary_columns = "app.term.AuxiliaryColumns";
  String ref_Auxiliary_columns = "$(" + key_Auxiliary_columns + ")";
}
