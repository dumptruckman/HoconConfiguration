package com.dumptruckman.bukkit.configuration.hocon;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemFactory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Bukkit.class)
@PowerMockIgnore("javax.management.*")
public class HoconConfigurationTest {

    private HoconConfiguration config;

    boolean mocked = false;

    @Before
    public void setUp() throws Exception {
        config = new HoconConfiguration();
    }

    @Test
    public void testConfig() throws Exception {
        config.set("someNumber", 123);
        config.setComments("someNumber", "Let's test some comments.", "Woo!");
        config.set("a.nested.value", "Howdy");
        config.setComments("a.nested", "Just look at this fantastic nest!");
        config.setComments("a.nested.value", "So fancy.");

        System.out.println(config.saveToString());
    }

    @Test
    public void testConfig2() throws Exception {
        config.setComments("a", "Just testin'");
        String sampleConfig = "a {\n  # Just look at this fantastic nest!\n  nested {\n    # So Fancy.\n    value=Howdy\n  }\n}\n# Let's test some comments.\n# Woo!\n# WOOOOOOO!\nsomeNumber=123\n";
        config.loadFromString(sampleConfig);

        System.out.println(config.saveToString());
    }
}