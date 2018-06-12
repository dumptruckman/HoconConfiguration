/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.hocon;

import com.dumptruckman.bukkit.configuration.SerializableSet;
import com.dumptruckman.bukkit.configuration.util.SerializationHelper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Hocon Configuration for Bukkit based on {@link FileConfiguration}.
 *
 * Able to store all the things you'd expect from a Bukkit configuration.
 */
public class HoconConfiguration extends FileConfiguration {

    protected static final String COMMENT_PREFIX = "# ";
    protected static final String BLANK_CONFIG = "{}\n";

    private static final Logger LOG = Logger.getLogger(HoconConfiguration.class.getName());

    private Map<String, List<String>> allComments = new HashMap<>();

    @NotNull
    @Override
    public String saveToString() {
        ConfigValue hoconConfig = SerializationHelper
                .createSerializationHelper(allComments, options.pathSeparator())
                .buildHoconConfig(getValues(false));
        String dump = hoconConfig.render(options().renderOptions());

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
        loadComments(hoconConfig.root(), "");
    }

    private void loadComments(@NotNull ConfigValue value, @NotNull String currentPath) {
        List<String> comments = value.origin().comments();
        if (!comments.isEmpty()) {
            allComments.put(currentPath, comments);
        }
        if (value.valueType() == ConfigValueType.OBJECT) {
            ConfigObject config = (ConfigObject) value;
            for (Entry<String, ConfigValue> entry : config.entrySet()) {
                String key = entry.getKey();
                loadComments(entry.getValue(), currentPath.isEmpty() ? key : currentPath + options().pathSeparator() + key);
            }
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
        String header = options().header();

        if (options().copyHeader()) {
            Configuration def = getDefaults();

            if ((def != null) && (def instanceof FileConfiguration)) {
                try {
                    Method m = FileConfiguration.class.getDeclaredMethod("buildHeader");
                    m.setAccessible(true);
                    String defaultsHeader = (String) m.invoke(def);

                    if ((defaultsHeader != null) && (defaultsHeader.length() > 0)) {
                        return defaultsHeader;
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    return "";
                }
            }
        }

        if (header == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = header.split("\r?\n", -1);
        boolean startedHeader = false;

        for (int i = lines.length - 1; i >= 0; i--) {
            builder.insert(0, "\n");

            if ((startedHeader) || (lines[i].length() != 0)) {
                builder.insert(0, lines[i]);
                builder.insert(0, COMMENT_PREFIX);
                startedHeader = true;
            }
        }

        return builder.toString();
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
            LOG.log(Level.SEVERE, "Cannot load " + file, ex);
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

    /**
     * Sets the comments for a given path.
     *
     * @param path The config path to comment.
     * @param comments The comments for the path, one string per line. Put no comments to remove comments for a path.
     */
    public void setComments(@NotNull final String path, final String... comments) {
        if (comments.length == 0) {
            allComments.remove(path);
        } else {
            allComments.put(path, Arrays.asList(comments));
        }
    }

    /**
     * Gets the comments for a given path.
     *
     * @param path The config path to retrieve the comments of.
     * @return The comments for the given path, or an empty list if no comments.
     */
    @NotNull
    public List<String> getComments(@NotNull final String path) {
        return allComments.getOrDefault(path, Collections.emptyList());
    }
}
