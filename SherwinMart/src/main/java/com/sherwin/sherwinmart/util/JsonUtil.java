package com.sherwin.sherwinmart.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Central Gson instance used across servlets for JSON (de)serialization. */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }
}
