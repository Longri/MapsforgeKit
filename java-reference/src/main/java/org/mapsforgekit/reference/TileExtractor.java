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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Reads tiles from a Mapsforge {@code .map} file and converts them into the
 * neutral {@link TileDump} representation that can be serialised as JSON.
 *
 * <p>The output format is the canonical reference that the Swift port is
 * expected to reproduce.
 */
public final class TileExtractor {

    private final MapFile mapFile;

    public TileExtractor(MapFile mapFile) {
        this.mapFile = mapFile;
    }

    /**
     * Extracts a single tile and converts it into a {@link TileDump.TileEntry}.
     */
    public TileDump.TileEntry extract(int zoomLevel, int tileX, int tileY, int tileSize) {
        Tile tile = new Tile(tileX, tileY, (byte) zoomLevel, tileSize);
        MapReadResult result = mapFile.readMapData(tile);

        TileDump.TileEntry entry = new TileDump.TileEntry();
        entry.zoomLevel = zoomLevel;
        entry.tileX = tileX;
        entry.tileY = tileY;
        entry.boundingBox = toBoundingBox(tile.getBoundingBox());

        List<TileDump.PoiEntry> pois = new ArrayList<>();
        if (result != null && result.pointOfInterests != null) {
            for (PointOfInterest poi : result.pointOfInterests) {
                pois.add(toPoi(poi));
            }
        }

        List<TileDump.WayEntry> ways = new ArrayList<>();
        if (result != null && result.ways != null) {
            for (Way way : result.ways) {
                ways.add(toWay(way));
            }
        }

        entry.poiCount = pois.size();
        entry.wayCount = ways.size();
        entry.pois = pois;
        entry.ways = ways;
        return entry;
    }

    private static TileDump.BoundingBoxDump toBoundingBox(BoundingBox bbox) {
        TileDump.BoundingBoxDump dump = new TileDump.BoundingBoxDump();
        dump.minLat = bbox.minLatitude;
        dump.minLon = bbox.minLongitude;
        dump.maxLat = bbox.maxLatitude;
        dump.maxLon = bbox.maxLongitude;
        return dump;
    }

    private static TileDump.PoiEntry toPoi(PointOfInterest poi) {
        TileDump.PoiEntry dump = new TileDump.PoiEntry();
        dump.layer = poi.layer;
        dump.lat = poi.position.latitude;
        dump.lon = poi.position.longitude;
        dump.tags = tagsToMap(poi.tags);
        return dump;
    }

    private static TileDump.WayEntry toWay(Way way) {
        TileDump.WayEntry dump = new TileDump.WayEntry();
        dump.layer = way.layer;
        dump.tags = tagsToMap(way.tags);
        if (way.labelPosition != null) {
            dump.labelPosition = new TileDump.LatLonDump(
                    way.labelPosition.latitude,
                    way.labelPosition.longitude);
        }
        List<List<TileDump.LatLonDump>> rings = new ArrayList<>();
        if (way.latLongs != null) {
            for (LatLong[] ring : way.latLongs) {
                List<TileDump.LatLonDump> points = new ArrayList<>(ring.length);
                for (LatLong p : ring) {
                    points.add(new TileDump.LatLonDump(p.latitude, p.longitude));
                }
                rings.add(points);
            }
        }
        dump.geometry = rings;
        return dump;
    }

    private static java.util.Map<String, String> tagsToMap(List<Tag> tags) {
        // LinkedHashMap to preserve order for stable JSON output
        java.util.Map<String, String> map = new LinkedHashMap<>();
        if (tags != null) {
            for (Tag t : tags) {
                map.put(t.key, t.value);
            }
        }
        return map;
    }
}
