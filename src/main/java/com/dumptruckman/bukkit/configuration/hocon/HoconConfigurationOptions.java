/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.bukkit.configuration.hocon;

import com.typesafe.config.ConfigRenderOptions;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.jetbrains.annotations.NotNull;

/**
 * Mandatory configuration options class for HoconConfiguration.
 */
public class HoconConfigurationOptions extends FileConfigurationOptions {

    protected HoconConfigurationOptions(@NotNull final HoconConfiguration configuration) {
        super(configuration);
    }

    private ConfigRenderOptions renderOptions = ConfigRenderOptions.defaults()
            .setOriginComments(false)
            .setJson(false);

    @Override
    public HoconConfiguration configuration() {
        return (HoconConfiguration) super.configuration();
    }

    @Override
    public HoconConfigurationOptions copyDefaults(final boolean value) {
        super.copyDefaults(value);
        return this;
    }

    @Override
    public HoconConfigurationOptions pathSeparator(final char value) {
        super.pathSeparator(value);
        return this;
    }

    @Override
    public HoconConfigurationOptions header(final String value) {
        super.header(value);
        return this;
    }

    @Override
    public HoconConfigurationOptions copyHeader(final boolean value) {
        super.copyHeader(value);
        return this;
    }

    /**
     * Sets the render options for the configuration.
     *
     * @param renderOptions the new render options.
     * @return This options for chaining.
     */
    public HoconConfigurationOptions renderOptions(ConfigRenderOptions renderOptions) {
        this.renderOptions = renderOptions;
        return this;
    }

    /**
     * Returns the render options for the configuration.
     *
     * @return the render options for the configuration.
     */
    public ConfigRenderOptions renderOptions() {
        return renderOptions;
    }
}
