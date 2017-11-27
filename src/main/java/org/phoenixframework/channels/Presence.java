package org.phoenixframework.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by lucas on 7/31/17.
 */

public class Presence {

    public final class Events {
        public static final String STATE = "presence_state";
        public static final String DIFF = "presence_diff";
    }

    ObjectNode state = JsonNodeFactory.instance.objectNode();

    public Presence() {

    }

    IPresenceCallback callback = null;
    public void setCallback(IPresenceCallback cb) {
        callback = cb;
    }

    public void sync(Envelope envelope) {
        if(envelope.getEvent().equals(Events.STATE)) {
            JsonNode payload = envelope.getPayload();

            Iterator<Map.Entry<String, JsonNode>> nodes = payload.fields();
            while (nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
                if(entry.getValue().has("metas") && entry.getValue().get("metas").isArray()) {
                    state.set(entry.getKey(), (ArrayNode)entry.getValue().get("metas"));
                }
            }
        }
        else if(envelope.getEvent().equals(Events.DIFF)) {
            JsonNode payload = envelope.getPayload();
            if(payload.has("leaves")) {
                syncLeaves(payload.get("leaves"));
            }
            if(payload.has("joins")) {
                syncJoins(payload.get("joins"));
            }
        }

        if(callback != null) {
            callback.onStateChanged(state);
        }
    }

    public void syncLeaves(JsonNode info) {
        Iterator<Map.Entry<String, JsonNode>> nodes = info.fields();
        while (nodes.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
            String id = entry.getKey();

            if(!state.has(id)) {
                continue;
            }

            if(state.get(id).size() == 1) {
                state.remove(id);
                continue;
            }

            ArrayNode existing = (ArrayNode)state.get(id);

            if(entry.getValue().has("metas") && entry.getValue().get("metas").isArray()) {
                ArrayNode metas = (ArrayNode)entry.getValue().get("metas");
                Iterator<Map.Entry<String, JsonNode>> metaIt = metas.fields();

                ArrayList<String> refs = new ArrayList<>();
                while(metaIt.hasNext()) {
                    JsonNode meta = metaIt.next().getValue();
                    if(meta.has("phx_ref")) {
                        refs.add(meta.get("phx_ref").asText());
                    }
                }

                for(int i = existing.size() - 1; i >= 0; i--) {
                    JsonNode extMeta = existing.get(i);
                    if(extMeta.has("phx_ref")) {
                        String ref = extMeta.get("phx_ref").asText();
                        if(refs.contains(ref)) {
                            existing.remove(i);
                        }
                    }
                }
            }
        }


        if(callback != null) {
            nodes = info.fields();
            while (nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
                String id = entry.getKey();

                if(entry.getValue().has("metas") && entry.getValue().get("metas").isArray()) {
                    callback.onLeave(id, entry.getValue().get("metas"));
                }
            }
        }
    }

    public void syncJoins(JsonNode info) {
        Iterator<Map.Entry<String, JsonNode>> nodes = info.fields();
        while (nodes.hasNext()) {
            Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
            if(entry.getValue().has("metas") && entry.getValue().get("metas").isArray()) {
                ArrayNode metas = (ArrayNode)entry.getValue().get("metas");
                if(state.has(entry.getKey())) {
                    JsonNode existing = state.get(entry.getKey());
                    Presence.merge(existing, metas);
                }
                else {
                    state.set(entry.getKey(), metas);
                }

                if(callback != null) {
                    callback.onJoin(entry.getKey(), metas);
                }
            }
        }
    }


    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() &&
                    updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }
}
