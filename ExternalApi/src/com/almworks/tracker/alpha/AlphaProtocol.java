package com.almworks.tracker.alpha;

public class AlphaProtocol {
  public static final String PROPERTY_EXEC_SUFFIX = ".exec";
  public static final String PROPERTY_EXEC_PATH_SUFFIX = ".exec.dir";

  // =debug to use debug version
  public static final String PROPERTY_USE_PREFIX = "use.";

  public static final String PREFERENCES_PATH = "com/almworks/applications";
  public static final String START_DIRECTORY_SUFFIX = ".path";

  public interface Protocols {
    String ALPHA = "alpha";
  }

  public interface Messages {
    interface ToTracker {
      String OPEN_ARTIFACTS = "openArtifacts";
      String PING = "ping";
      String SUBSCRIBE = "subscribe";
      String UNSUBSCRIBE = "unsubscribe";
      String TO_FRONT = "toFront";
      String GET_SUPPORTED_PROTOCOLS = "getSupportedProtocols";
      String REQUEST_ADD_COLLECTION_ACTION = "requestAddCollectionAction";
      String WATCH_COLLECTION = "watchCollection";
      String UNWATCH_COLLECTION = "unwatchCollection";
    }

    interface ToClient {
      String PING = "pong";
      String ARTIFACT_INFO = "artifactInfo";
      String SUPPORTED_PROTOCOLS = "supportedProtocols";
      String FIND_ARTIFACTS = "findArtifacts";
      String ADD_WATCHED_COLLECTION = "addWatchedCollection";
      String COLLECTION_UPDATE = "collectionUpdate";


      interface Ping {
        String TRACKER_NAME = "trackerName";
        String TRACKER_VERSION = "trackerVersion";
        String TRACKER_WORKSPACE = "workspace";
      }

      interface ArtifactInfo {
        String URL = "url";
        String STATUS = "status";
        String TIMESTAMP_SECONDS = "timestampSeconds";
        String SHORT_DESCRIPTION = "shortDescription";
        String LONG_DESCRIPTION = "longDescription";
        String ID = "id";
        String SUMMARY = "summary";
      }

      interface CollectionProps {
        String NAME = "name";
        String ICON = "icon";
      }
    }
  }
}
