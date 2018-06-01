/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.hocon;

import com.dumptruckman.bukkit.configuration.SerializableSet;
import com.dumptruckman.bukkit.configuration.util.SerializationHelper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Hocon Configuration for Bukkit based on {@link FileConfiguration}.
 *
 * Able to store all the things you'd expect from a Bukkit configuration.
 */
public class HoconConfiguration extends FileConfiguration {

    protected static final String BLANK_CONFIG = "{}\n";

    private static final Logger LOG = Logger.getLogger(HoconConfiguration.class.getName());

    @NotNull
    @Override
    public String saveToString() {
        ConfigValue hoconConfig = SerializationHelper.buildHoconConfig(getValues(false));
        String dump = hoconConfig.render();

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

        Config hoconConfig = ConfigFactory.parseString(contents);
        hoconConfig = hoconConfig.resolve();
        Map<String, Object> unwrapped = hoconConfig.root().unwrapped();

        convertMapsToSections(unwrapped, this);
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
        // TODO implement header
        return "";
    }

    @Override
    public HoconConfigurationOptions options() {
        if (options == null) {
            options = new HoconConfigurationOptions(this);
        }

        return (HoconConfigurationOptions) options;
    }

    private static HoconConfiguration loadConfiguration(@NotNull final HoconConfiguration config, @NotNull final File file) {
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
     * Loads up a configuration from a hocon formatted file.
     *
     * If the file does not exist, it will be created.  This will attempt to use UTF-8 encoding for the file, if it fails
     * to do so, the system default will be used instead.
     *
     * @param file The file to load the configuration from.
     * @return The configuration loaded from the file contents.
     */
    public static HoconConfiguration loadConfiguration(@NotNull final File file) {
        return loadConfiguration(new HoconConfiguration(), file);
    }

    public HoconConfiguration() {
        ConfigurationSerialization.registerClass(SerializableSet.class);
    }
}
