/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.json;

import com.google.common.io.Files;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

/**
 * This class allows for creating a JsonConfiguration that saves files using a specified file encoding.
 */
class EncodedJsonConfiguration extends JsonConfiguration {

    @NotNull
    private final Charset charset;

    EncodedJsonConfiguration(@NotNull final Charset charset) throws UnsupportedEncodingException {
        this.charset = charset;
    }

    EncodedJsonConfiguration(@NotNull final String charset) throws UnsupportedEncodingException, IllegalCharsetNameException {
        this(Charset.forName(charset));
    }

    @Override
    public void save(@NotNull final File file) throws IOException {
        Files.createParentDirs(file);

        final String data = saveToString();

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset));
            writer.write(data);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}