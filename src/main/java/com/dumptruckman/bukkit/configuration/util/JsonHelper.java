package com.dumptruckman.bukkit.configuration.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.*;

public final class JsonHelper {

	private JsonHelper() {
	}

	@NotNull
	public static Optional<Object> jsonValueAsObject(@NotNull JsonValue value) {
		final Object object;

		if (value.isBoolean()) {
			object = value.asBoolean();
		} else if (value.isNumber()) {
			object = value.asInt();
		} else if (value.isString()) {
			object = value.asString();
		} else if (value.isArray()) {
			object = jsonArrayAsList(value.asArray());
		} else if (value.isObject()) {
			object = jsonObjectAsMap(value.asObject());
		} else {
			object = null;
		}

		return Optional.ofNullable(object);
	}

	@NotNull
	public static List<Object> jsonArrayAsList(@NotNull JsonArray array) {
		final List<Object> list = new ArrayList<>(array.size());

		for(JsonValue element:array) {
			list.add(jsonValueAsObject(element));
		}

		return list;
	}

	@NotNull
	public static Map<String,Object> jsonObjectAsMap(@NotNull JsonValue value) {
		if (!(value instanceof JsonObject)) {
			return new HashMap<>();
		}

		final JsonObject jsonObject = (JsonObject) value;
		final Map<String,Object> map = new HashMap<>(jsonObject.size(), 1.f);

		jsonObject.forEach(member ->
			jsonValueAsObject(member.getValue()).ifPresent(o ->
				map.put(member.getName(), o)
			)
		);

		return map;
	}

	@NotNull
	public static Optional<JsonValue> objectAsJsonValue(@NotNull Object object) {
		final JsonValue value;

		if (object instanceof Boolean) {
			value = Json.value((Boolean)object);
		} else if (object instanceof Integer) {
			value = Json.value((Integer)object);
		} else if (object instanceof Long) {
			value = Json.value((Long)object);
		} else if (object instanceof Float) {
			value = Json.value((Float)object);
		} else if (object instanceof Double) {
			value = Json.value((Double)object);
		} else if (object instanceof String) {
			value = Json.value((String)object);
		} else if (object instanceof Collection) {
			value = collectionAsJsonArray((Collection<?>)object);
		} else if (object instanceof Map) {
			value = mapAsJsonObject((Map<?,?>)object);
		} else if (object instanceof ConfigurationSection) {
			value = mapAsJsonObject(((ConfigurationSection) object).getValues(false));
		} else {
			value = null;
		}

		return Optional.ofNullable(value);
	}

	@NotNull
	public static JsonArray collectionAsJsonArray(@NotNull Collection<?> collection) {
		final JsonArray array = new JsonArray();

		collection.forEach(o ->
			objectAsJsonValue(o).ifPresent(array::add)
		);

		return array;
	}

	@NotNull
	public static JsonObject mapAsJsonObject(@NotNull Map<?,?> map) {
		final JsonObject object = new JsonObject();

		map.forEach((key, value) ->
			objectAsJsonValue(value).ifPresent(jsonValue ->
				object.add(String.valueOf(key), jsonValue)
			)
		);

		return object;
	}
} 