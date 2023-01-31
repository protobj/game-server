package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name = "custom", category = StrLookup.CATEGORY)
public class CustomLookup implements StrLookup {

    private static final String dft = "dft";

    public CustomLookup() {
    }

    @Override
    public String lookup(String key) {
        return dft;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        Marker marker = event.getMarker();
        if (marker != null) {
            return marker.getName();
        }
        return dft;
    }
}
