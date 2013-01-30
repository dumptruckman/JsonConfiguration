/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.json;

import org.bukkit.configuration.file.FileConfigurationOptions;
import org.jetbrains.annotations.NotNull;

/**
 * Mandatory configuration options class for JsonConfiguration.
 */
public class JsonConfigurationOptions extends FileConfigurationOptions {

    protected JsonConfigurationOptions(@NotNull final JsonConfiguration configuration) {
        super(configuration);
    }

    @Override
    public JsonConfiguration configuration() {
        return (JsonConfiguration) super.configuration();
    }

    @Override
    public JsonConfigurationOptions copyDefaults(final boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @Override
    public JsonConfigurationOptions pathSeparator(final char value) {
        super.pathSeparator(value);
        return this;
    }

    @Override
    public JsonConfigurationOptions header(final String value) {
        super.header(value);
        return this;
    }

    @Override
    public JsonConfigurationOptions copyHeader(final boolean value) {
        super.copyHeader(value);
        return this;
    }
}
