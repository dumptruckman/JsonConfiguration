package com.dumptruckman.bukkit.configuration.json;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapDeserializerDoubleAsIntFix implements JsonDeserializer<Map<String, Object>> {

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> deserialize(@NotNull JsonElement json, @NotNull Type typeOfT,
                                           @NotNull JsonDeserializationContext context) {
        return (Map<String, Object>) read(json);
    }

    @Nullable
    public Object read(@NotNull JsonElement in) {
        if (in.isJsonArray()) {
            final List<Object> list = new ArrayList<>();
            final JsonArray arr = in.getAsJsonArray();

            for (JsonElement anArr : arr) {
                list.add(read(anArr));
            }

            return list;
        } else if (in.isJsonObject()) {
            final Map<String, Object> map = new LinkedTreeMap<>();
            final JsonObject obj = in.getAsJsonObject();
            final Set<Map.Entry<String, JsonElement>> entitySet = obj.entrySet();

            for (Map.Entry<String, JsonElement> entry : entitySet) {
                map.put(entry.getKey(), read(entry.getValue()));
            }

            return map;
        } else if (in.isJsonPrimitive()) {
            final JsonPrimitive prim = in.getAsJsonPrimitive();

            if (prim.isBoolean()) {
                return prim.getAsBoolean();
            } else if (prim.isString()) {
                return prim.getAsString();
            } else if (prim.isNumber()) {
                final Number num = prim.getAsNumber();

                if (Math.ceil(num.doubleValue()) == num.longValue()) {
                    return num.longValue();
                } else {
                    return num.doubleValue();
                }
            }
        }

        return null;
    }
}