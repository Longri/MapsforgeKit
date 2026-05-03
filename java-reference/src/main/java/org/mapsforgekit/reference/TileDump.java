/*
 * MapsforgeKit — Native Mapsforge implementation for Apple platforms
 * Copyright (C) 2026 MapsforgeKit contributors
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 */

package org.mapsforgekit.reference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

/**
 * Serializable record types used to dump tile contents as JSON fixtures.
 *
 * <p>These are intentionally simple, language-neutral structures so that the
 * Swift port can reproduce the same JSON byte-for-byte and tests can compare
 * the two outputs directly.
 */
public final class TileDump {

    private TileDump() {
        // utility container
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"generatedBy", "mapFile", "fileVersion", "tiles"})
    public static final class Document {
        public String generatedBy;
        public String mapFile;
        public Integer fileVersion;
        public List<TileEntry> tiles;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "zoomLevel", "tileX", "tileY",
            "boundingBox",
            "poiCount", "wayCount",
            "pois", "ways"
    })
    public static final class TileEntry {
        public int zoomLevel;
        public long tileX;
        public long tileY;
        public BoundingBoxDump boundingBox;
        public int poiCount;
        public int wayCount;
        public List<PoiEntry> pois;
        public List<WayEntry> ways;
    }

    @JsonPropertyOrder({"minLat", "minLon", "maxLat", "maxLon"})
    public static final class BoundingBoxDump {
        public double minLat;
        public double minLon;
        public double maxLat;
        public double maxLon;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"layer", "lat", "lon", "tags"})
    public static final class PoiEntry {
        public byte layer;
        public double lat;
        public double lon;
        public Map<String, String> tags;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"layer", "tags", "labelPosition", "geometry"})
    public static final class WayEntry {
        public byte layer;
        public Map<String, String> tags;
        public LatLonDump labelPosition;
        /** List of polygons; each polygon is a list of [lat, lon] pairs. */
        public List<List<LatLonDump>> geometry;
    }

    @JsonPropertyOrder({"lat", "lon"})
    public static final class LatLonDump {
        public double lat;
        public double lon;

        public LatLonDump() { }

        public LatLonDump(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}
