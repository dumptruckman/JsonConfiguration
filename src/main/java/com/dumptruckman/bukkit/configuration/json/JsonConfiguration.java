/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.json;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
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
        String dump = JSONValue.toJSONString(buildMap(getValues(false)));

        if (dump.equals(BLANK_CONFIG)) {
            dump = "";
        }

        return dump;
    }

    /**
     * Takes a Map and parses through the values, to ensure that, before saving, all objects are as appropriate as
     * possible for storage in json format.
     *
     * Specifically it does the following:
     *   for Map: calls this method recursively on the Map before putting it in the returned Map.
     *   for List: calls {@link #buildList(java.util.Collection)} which functions similar to this method.
     *   for ConfigurationSection: gets the values as a map and calls this method recursively on the Map before putting
     *       it in the returned Map.
     *   for ConfigurationSerializable: add the {@link ConfigurationSerialization#SERIALIZED_TYPE_KEY} to a new Map
     *       along with the Map given by {@link org.bukkit.configuration.serialization.ConfigurationSerializable#serialize()}
     *       and calls this method recursively on the new Map before putting it in the returned Map.
     *   for Everything else: stores it as is in the returned Map.
     */
    @NotNull
    private Map<String, Object> buildMap(@NotNull final Map<?, ?> map) {
        final Map<String, Object> result = new LinkedHashMap<String, Object>(map.size());
        try {
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Object[]) {
                    value = new ArrayList<Object>(Arrays.asList((Object[]) value));
                }
                if (value instanceof ConfigurationSection) {
                    result.put(entry.getKey().toString(), buildMap(((ConfigurationSection) value).getValues(false)));
                } else if (value instanceof Map) {
                    result.put(entry.getKey().toString(), buildMap(((Map) value)));
                } else if (value instanceof List) {
                    result.put(entry.getKey().toString(), buildList((List) value));
                } else if (value instanceof ConfigurationSerializable) {
                    ConfigurationSerializable serializable = (ConfigurationSerializable) value;
                    Map<String, Object> values = new LinkedHashMap<String, Object>();
                    values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
                    values.putAll(serializable.serialize());
                    result.put(entry.getKey().toString(), buildMap(values));
                } else {
                    result.put(entry.getKey().toString(), value);
                }
            }
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Error while building configuration map.", e);
        }
        return result;
    }

    /**
     * Takes a Collection and parses through the values, to ensure that, before saving, all objects are as appropriate
     * as possible for storage in json format.
     *
     * Specifically it does the following:
     *   for Map: calls {@link #buildMap(java.util.Map)} on the Map before adding to the returned list.
     *   for List: calls this method recursively on the List.
     *   for ConfigurationSection: gets the values as a map and calls {@link #buildMap(java.util.Map)} on the Map
     *       before adding to the returned list.
     *   for ConfigurationSerializable: add the {@link ConfigurationSerialization#SERIALIZED_TYPE_KEY} to a new Map
     *       along with the Map given by {@link org.bukkit.configuration.serialization.ConfigurationSerializable#serialize()}
     *       and calls {@link #buildMap(java.util.Map)} on the new Map before adding to the returned list.
     *   for Everything else: stores it as is in the returned List.
     */
    private List<Object> buildList(@NotNull final Collection<?> collection) {
        final List<Object> result = new ArrayList<Object>(collection.size());
        try {
            for (Object o : collection) {
                if (o instanceof Object[]) {
                    o = new ArrayList<Object>(Arrays.asList((Object[]) o));
                }
                if (o instanceof ConfigurationSection) {
                    result.add(buildMap(((ConfigurationSection) o).getValues(false)));
                } else if (o instanceof Map) {
                    result.add(buildMap(((Map) o)));
                } else if (o instanceof List) {
                    result.add(buildList((List) o));
                } else if (o instanceof ConfigurationSerializable) {
                    ConfigurationSerializable serializable = (ConfigurationSerializable) o;
                    Map<String, Object> values = new LinkedHashMap<String, Object>();
                    values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
                    values.putAll(serializable.serialize());
                    result.add(buildMap(values));
                } else {
                    result.add(o);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while building configuration list.", e);
        }
        return result;
    }

    @Override
    public void loadFromString(@NotNull final String contents) throws InvalidConfigurationException {
        if (contents.isEmpty()) {
            return;
        }

        Map<?, ?> input;
        try {
            input = (Map<?, ?>) new JSONParser(JSONParser.USE_INTEGER_STORAGE).parse(contents);
        } catch (ParseException e) {
            throw new InvalidConfigurationException("Invalid JSON detected.");
        } catch (ClassCastException e) {
            throw new InvalidConfigurationException("Top level is not a Map.");
        }

        if (input != null) {
            convertMapsToSections(input, this);
        } else {
            throw new InvalidConfigurationException("An unknown error occurred while attempting to parse the json.");
        }
    }

    private void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull final ConfigurationSection section) {
        final Object result = dealWithSerializedObjects(input);
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

    /**
     * Parses through the input map to deal with serialized objects a la {@link ConfigurationSerializable}.
     *
     * Called recursively first on Maps and Lists before passing the parsed input over to
     * {@link ConfigurationSerialization#deserializeObject(java.util.Map)}.  Basically this means it will deserialize
     * the most nested objects FIRST and the top level object LAST.
     */
    private Object dealWithSerializedObjects(@NotNull final Map<?, ?> input) {
        final Map<String, Object> output = new LinkedHashMap<String, Object>(input.size());
        for (final Map.Entry<?, ?> e : input.entrySet()) {
            if (e.getValue() instanceof Map) {
                output.put(e.getKey().toString(), dealWithSerializedObjects((Map<?, ?>) e.getValue()));
            }  else if (e.getValue() instanceof List) {
                output.put(e.getKey().toString(), dealWithSerializedObjects((List<?>) e.getValue()));
            } else {
                output.put(e.getKey().toString(), e.getValue());
            }
        }
        if (output.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
            try {
                return ConfigurationSerialization.deserializeObject(output);
            } catch (IllegalArgumentException ex) {
                throw new YAMLException("Could not deserialize object", ex);
            }
        }
        return output;
    }

    /**
     * Parses through the input list to deal with serialized objects a la {@link ConfigurationSerializable}.
     *
     * Functions similarly to {@link #dealWithSerializedObjects(java.util.Map)} but only for detecting lists within
     * lists and maps within lists.
     */
    protected Object dealWithSerializedObjects(@NotNull final List<?> input) {
        final List<Object> output = new ArrayList<Object>(input.size());
        for (final Object o : input) {
            if (o instanceof Map) {
                output.add(dealWithSerializedObjects((Map<?, ?>) o));
            } else if (o instanceof List) {
                output.add(dealWithSerializedObjects((List<?>) o));
            } else {
                output.add(o);
            }
        }
        return output;
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

    private static JsonConfiguration loadConfiguration(@NotNull final JsonConfiguration config, @NotNull final String json) {
        try {
            config.loadFromString(json);
        } catch (InvalidConfigurationException ex) {
            LOG.log(Level.SEVERE, "Cannot parse " + json , ex);
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
        try {
            return loadConfiguration(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return loadConfiguration(new JsonConfiguration(), file);
        }
    }

    /**
     * Loads up a configuration from a json formatted file with the specified file encoding.
     *
     * If the file does not exist, it will be created.
     *
     * @param file The file to load the configuration from.
     * @param charset The file encoding to use.
     * @return The configuration loaded from the file contents.
     * @throws UnsupportedEncodingException if the file encoding is not supported on this system.
     * @throws IllegalCharsetNameException if the charset name is not a valid charset.
     */
    public static JsonConfiguration loadConfiguration(@NotNull final File file, @NotNull final String charset) throws UnsupportedEncodingException, IllegalCharsetNameException {
        return loadConfiguration(new EncodedJsonConfiguration(charset), file);
    }

    /**
     * Loads up a configuration from a json formatted file with the specified file encoding.
     *
     * If the file does not exist, it will be created.
     *
     * @param file The file to load the configuration from.
     * @param charset The file encoding to use.
     * @return The configuration loaded from the file contents.
     * @throws UnsupportedEncodingException if the file encoding is not supported on this system.
     */
    public static JsonConfiguration loadConfiguration(@NotNull final File file, @NotNull final Charset charset) throws UnsupportedEncodingException {
        return loadConfiguration(new EncodedJsonConfiguration(charset), file);
    }

    public JsonConfiguration() { }
}
