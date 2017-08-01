package org.phoenixframework.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

/**
 * Created by lucas on 8/1/17.
 */

public interface IPresenceCallback {
    void onStateChanged(JsonNode state);
    void onJoin(String id, JsonNode meta);
    void onLeave(String id, JsonNode meta);
}
