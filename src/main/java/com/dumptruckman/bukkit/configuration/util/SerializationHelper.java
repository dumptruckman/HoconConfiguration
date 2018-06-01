package com.dumptruckman.bukkit.configuration.util;

import com.dumptruckman.bukkit.configuration.SerializableSet;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigOriginFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jeremy Wood
 * @version 6/14/2017
 */
public class SerializationHelper {

    private static final Logger LOG = Logger.getLogger(SerializationHelper.class.getName());

    public static ConfigValue buildHoconConfig(@NotNull Object value) {
        if (value instanceof Object[]) {
            value = new ArrayList<>(Arrays.asList((Object[]) value));
        }
        if (value instanceof Set && !(value instanceof SerializableSet)) {
            value = new SerializableSet((Set) value);
        }
        if (value instanceof ConfigurationSection) {
            return buildMap(((ConfigurationSection) value).getValues(false));
        } else if (value instanceof Map) {
            return buildMap((Map) value);
        } else if (value instanceof List) {
            return buildList((List) value);
        } else if (value instanceof ConfigurationSerializable) {
            ConfigurationSerializable serializable = (ConfigurationSerializable) value;
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
            values.putAll(serializable.serialize());
            return buildMap(values);
        } else {
            return ConfigValueFactory.fromAnyRef(value, "HoconConfiguration");
        }
    }

    /**
     * Takes a Map and parses through the values, to ensure that, before saving, all objects are as appropriate as
     * possible for storage in most data formats.
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
    private static ConfigValue buildMap(@NotNull final Map<?, ?> map) {
        final Map<String, ConfigValue> result = new LinkedHashMap<>(map.size());
        try {
            for (final Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey().toString(), buildHoconConfig(entry.getValue()));
            }
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Error while building configuration map.", e);
        }
        return newConfigObject(result);
    }

    /**
     * Takes a Collection and parses through the values, to ensure that, before saving, all objects are as appropriate
     * as possible for storage in most data formats.
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
    private static ConfigValue buildList(@NotNull final Collection<?> collection) {
        final List<ConfigValue> result = new ArrayList<>(collection.size());
        try {
            for (Object o : collection) {
                result.add(buildHoconConfig(o));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error while building configuration list.", e);
        }
        return newConfigList(result);
    }

    /**
     * Parses through the input map to deal with serialized objects a la {@link ConfigurationSerializable}.
     *
     * Called recursively first on Maps and Lists before passing the parsed input over to
     * {@link ConfigurationSerialization#deserializeObject(java.util.Map)}.  Basically this means it will deserialize
     * the most nested objects FIRST and the top level object LAST.
     */
    public static Object deserialize(@NotNull final Map<?, ?> input) {
        final Map<String, Object> output = new LinkedHashMap<String, Object>(input.size());
        for (final Map.Entry<?, ?> e : input.entrySet()) {
            if (e.getValue() instanceof Map) {
                output.put(e.getKey().toString(), deserialize((Map<?, ?>) e.getValue()));
            }  else if (e.getValue() instanceof List) {
                output.put(e.getKey().toString(), deserialize((List<?>) e.getValue()));
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
     * Functions similarly to {@link #deserialize(java.util.Map)} but only for detecting lists within
     * lists and maps within lists.
     */
    private static Object deserialize(@NotNull final List<?> input) {
        final List<Object> output = new ArrayList<Object>(input.size());
        for (final Object o : input) {
            if (o instanceof Map) {
                output.add(deserialize((Map<?, ?>) o));
            } else if (o instanceof List) {
                output.add(deserialize((List<?>) o));
            } else {
                output.add(o);
            }
        }
        return output;
    }

    static ConfigValue newConfigObject(Map<String, ConfigValue> vals) {
        try {
            return CONFIG_OBJECT_CONSTRUCTOR.newInstance(ORIGIN, vals);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e); // rethrow
        }

    }

    static ConfigValue newConfigList(List<ConfigValue> vals) {
        try {
            return CONFIG_LIST_CONSTRUCTOR.newInstance(ORIGIN, vals);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e); // rethrow
        }
    }

    // -- Comment handling -- this might have to be updated as the hocon dep changes (But tests should detect this
    // breakage
    private static final ConfigOrigin ORIGIN = ConfigOriginFactory.newSimple("HoconConfiguration");
    private static final Constructor<? extends ConfigValue> CONFIG_OBJECT_CONSTRUCTOR;
    private static final Constructor<? extends ConfigValue> CONFIG_LIST_CONSTRUCTOR;
    static {
        Class<? extends ConfigValue> objectClass, listClass;
        try {
            objectClass = Class.forName("com.typesafe.config.impl.SimpleConfigObject").asSubclass(ConfigValue.class);
            listClass = Class.forName("com.typesafe.config.impl.SimpleConfigList").asSubclass(ConfigValue.class);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            CONFIG_OBJECT_CONSTRUCTOR = objectClass.getDeclaredConstructor(ConfigOrigin.class, Map.class);
            CONFIG_OBJECT_CONSTRUCTOR.setAccessible(true);
            CONFIG_LIST_CONSTRUCTOR = listClass.getDeclaredConstructor(ConfigOrigin.class, List.class);
            CONFIG_LIST_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
