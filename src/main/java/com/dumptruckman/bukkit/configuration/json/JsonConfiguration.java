/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.json;

import com.dumptruckman.bukkit.configuration.SerializableSet;
import com.dumptruckman.bukkit.configuration.util.SerializationHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JSON Configuration for Bukkit based on {@link FileConfiguration}.
 *
 * Able to store all the things you'd expect from a Bukkit configuration.
 */
public class JsonConfiguration extends FileConfiguration {

    protected static final String BLANK_CONFIG = "{}\n";

    private static final Logger LOG = Logger.getLogger(JsonConfiguration.class.getName());

    @NotNull
    @Override
    public String saveToString() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (options().prettyPrint()) {
            gsonBuilder.setPrettyPrinting();
        }
        Gson gson = gsonBuilder.create();

        String dump = gson.toJson(SerializationHelper.serialize(getValues(false)));

        if (dump.equals(BLANK_CONFIG)) {
            dump = "";
        }

        return dump;
    }

    @Override
    public void loadFromString(@NotNull final String contents) throws InvalidConfigurationException {
        if (contents.isEmpty()) {
            return;
        }

        Map<?, ?> input;
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(new TypeToken<Map <String, Object>>(){}.getType(),
                            new MapDeserializerDoubleAsIntFix())
                    .create();
            input = gson.fromJson(contents, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (JsonSyntaxException e) {
            throw new InvalidConfigurationException("Invalid JSON detected.", e);
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.", e);
        }

        if (input != null) {
            convertMapsToSections(input, this);
        } else {
            throw new InvalidConfigurationException("An unknown error occurred while attempting to parse the json.");
        }
    }

    private void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull final ConfigurationSection section) {
        final Object result = SerializationHelper.deserialize(input);
        if (result instanceof Map) {
            input = (Map<?, ?>) result;
            for (Map.Entry<?, ?> entry : input.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();

                if (value instanceof Map) {
                    convertMapsToSections((Map<?, ?>) value, section.createSection(key));
                } else {
                    section.set(key, value);
                }
            }
        } else {
            section.set("", result);
        }
    }

    @Override
    protected String buildHeader() {
        // json does not support comments of any kind.
        return "";
    }

    @Override
    public JsonConfigurationOptions options() {
        if (options == null) {
            options = new JsonConfigurationOptions(this);
        }

        return (JsonConfigurationOptions) options;
    }

    private static JsonConfiguration loadConfiguration(@NotNull final JsonConfiguration config, @NotNull final File file) {
        try {
            config.load(file);
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, "Cannot find file " + file, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Cannot load " + file, ex);
        } catch (InvalidConfigurationException ex) {
            LOG.log(Level.SEVERE, "Cannot load " + file , ex);
        }
        return config;
    }

    /**
     * Loads up a configuration from a json formatted file.
     *
     * If the file does not exist, it will be created.  This will attempt to use UTF-8 encoding for the file, if it fails
     * to do so, the system default will be used instead.
     *
     * @param file The file to load the configuration from.
     * @return The configuration loaded from the file contents.
     */
    public static JsonConfiguration loadConfiguration(@NotNull final File file) {
        return loadConfiguration(new JsonConfiguration(), file);
    }

    public JsonConfiguration() {
        ConfigurationSerialization.registerClass(SerializableSet.class);
    }

    // per https://stackoverflow.com/questions/36508323/how-can-i-prevent-gson-from-converting-integers-to-doubles/36529534#36529534
    public static class MapDeserializerDoubleAsIntFix implements JsonDeserializer<Map<String, Object>>{

        @Override  @SuppressWarnings("unchecked")
        public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return (Map<String, Object>) read(json);
        }

        public Object read(JsonElement in) {

            if(in.isJsonArray()){
                List<Object> list = new ArrayList<Object>();
                JsonArray arr = in.getAsJsonArray();
                for (JsonElement anArr : arr) {
                    list.add(read(anArr));
                }
                return list;
            }else if(in.isJsonObject()){
                Map<String, Object> map = new LinkedTreeMap<String, Object>();
                JsonObject obj = in.getAsJsonObject();
                Set<Map.Entry<String, JsonElement>> entitySet = obj.entrySet();
                for(Map.Entry<String, JsonElement> entry: entitySet){
                    map.put(entry.getKey(), read(entry.getValue()));
                }
                return map;
            }else if( in.isJsonPrimitive()){
                JsonPrimitive prim = in.getAsJsonPrimitive();
                if(prim.isBoolean()){
                    return prim.getAsBoolean();
                }else if(prim.isString()){
                    return prim.getAsString();
                }else if(prim.isNumber()){
                    Number num = prim.getAsNumber();
                    // here you can handle double int/long values
                    // and return any type you want
                    // this solution will transform 3.0 float to long values
                    if(Math.ceil(num.doubleValue())  == num.longValue())
                        return num.longValue();
                    else{
                        return num.doubleValue();
                    }
                }
            }
            return null;
        }
    }
}
