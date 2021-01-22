package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;

interface Command {
    JsonNode execute(JsonNode data) throws Exception;
}
