/*
 * Copyright (c) 2010-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j Spatial.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.index.curves;

import org.neo4j.gis.spatial.rtree.Envelope;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ZOrderSpaceFillingCurve2D extends SpaceFillingCurve {

    /**
     * Description of the space filling curve structure
     */
    static class ZOrderCurve2D extends CurveRule {

        private ZOrderCurve2D(int... npointValues) {
            super(2, npointValues);
            assert npointValues[0] == 1 && npointValues[3] == 2;
        }

        public String name() {
            return "Z";
        }

        @Override
        public CurveRule childAt(int npoint) {
            return this;
        }
    }

    private static final ZOrderCurve2D rootCurve = new ZOrderCurve2D(1, 3, 0, 2);

    public static final int MAX_LEVEL = 63 / 2 - 1;

    public ZOrderSpaceFillingCurve2D(Envelope range) {
        this(range, MAX_LEVEL);
    }

    public ZOrderSpaceFillingCurve2D(Envelope range, int maxLevel) {
        super(range, maxLevel);
        assert maxLevel <= MAX_LEVEL;
        assert range.getDimension() == 2;
    }

    @Override
    protected CurveRule rootCurve() {
        return rootCurve;
    }
}
