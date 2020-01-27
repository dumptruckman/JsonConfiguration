/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.json;

import com.dumptruckman.bukkit.configuration.util.JsonHelper;
import com.dumptruckman.bukkit.configuration.util.SerializationHelper;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.exceptions.InvalidConfigurationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
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
        final JsonObject jsonObject = JsonHelper.mapAsJsonObject(getValues(false));
        final String dump = jsonObject.toString(WriterConfig.PRETTY_PRINT);

        if (dump.equals(BLANK_CONFIG)) {
            return "";
        }

        return dump;
    }

    @Override
    public void loadFromString(@NotNull final String contents) {
        if (contents.isEmpty()) {
            return;
        }

        convertMapsToSections(
            JsonHelper.jsonObjectAsMap(
                Json.parse(contents)
            ),
            this
        );
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
        } catch (IOException | InvalidConfigurationException ex) {
            LOG.log(Level.SEVERE, "Cannot load " + file, ex);
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

}
