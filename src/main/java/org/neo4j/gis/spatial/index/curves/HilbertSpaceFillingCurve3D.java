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

import static org.neo4j.gis.spatial.index.curves.HilbertSpaceFillingCurve3D.BinaryCoordinateRotationUtils3D.*;

public class HilbertSpaceFillingCurve3D extends SpaceFillingCurve
{

    /**
     * Utilities for rotating point values in binary about various axes
     */
    static class BinaryCoordinateRotationUtils3D
    {
        static int rotateNPointLeft( int value )
        {
            return (value << 1) & 0b111 | ((value & 0b100) >> 2);
        }

        static int rotateNPointRight( int value )
        {
            return (value >> 1) | ((value & 0b001) << 2);
        }

        static int xXOR( int value )
        {
            return value ^ 0b100;
        }

        static int rotateYZ( int value )
        {
            return value ^ 0b011;
        }
    }

    /**
     * Description of the space filling curve structure
     */
    static class HilbertCurve3D extends CurveRule
    {
        CurveRule[] children = null;

        private HilbertCurve3D( int... npointValues )
        {
            super( 3, npointValues );
            //debugNpoints();
            assert npointValues[0] == 0 || npointValues[0] == 3 || npointValues[0] == 5 || npointValues[0] == 6;
        }

        private void debugNpoints()
        {
            System.out.println( "\t" + name() );
            for ( int i = 0; i < length(); i++ )
            {
                System.out.println( "\t\t" + i + ": NPoint = " + npointValues[i] + "\t" + binaryString( npointValues[i] ) );
                if ( npointValues[i] >= length() )
                {
                    System.out.println( "Invalid npoint: " + npointValues[i] );
                }
            }
        }

        static String binaryString( int value )
        {
            String binary = "00" + Integer.toBinaryString( value );
            return binary.substring( binary.length() - 3, binary.length() );
        }

        private char direction( int start, int end )
        {
            end -= start;
            switch ( end )
            {
            case 1:
                return 'F'; // move up      000->001
            case 2:
                return 'U'; // move right   000->010
            case 4:
                return 'R'; // move right   000->100
            case -4:
                return 'L'; // move left    111->011
            case -2:
                return 'D'; // move down    111->101
            case -1:
                return 'B'; // move back    111->110
            default:
                return '-';
            }
        }

        public String name()
        {
            return String.valueOf( direction( npointValues[0], npointValues[1] ) ) + direction( npointValues[1], npointValues[2] ) +
                    direction( npointValues[0], npointValues[length() - 1] );
        }

        /**
         * Rotate about the normal diagonal (the 000->111 diagonal). This simply involves
         * rotating the bits of all npoint values either left or right depending on the
         * direction of rotation, normal or reversed (positive or negative).
         */
        private HilbertCurve3D rotateOneThirdDiagonalPos( boolean direction )
        {
            int[] newNpoints = new int[length()];
            for ( int i = 0; i < length(); i++ )
            {
                if ( direction )
                {
                    newNpoints[i] = rotateNPointRight( npointValues[i] );
                }
                else
                {
                    newNpoints[i] = rotateNPointLeft( npointValues[i] );
                }
            }
            return new HilbertCurve3D( newNpoints );
        }

        /**
         * Rotate about the neg-x diagonal (the 100->011 diagonal). This is similar to the
         * normal diagonal rotation, but with x-switched, so we XOR the x value before and after
         * the rotation, and rotate in the opposite direction to specified.
         */
        private HilbertCurve3D rotateOneThirdDiagonalNeg( boolean direction )
        {
            int[] newNpoints = new int[length()];
            for ( int i = 0; i < length(); i++ )
            {
                if ( direction )
                {
                    newNpoints[i] = xXOR( rotateNPointLeft( xXOR( npointValues[i] ) ) );
                }
                else
                {
                    newNpoints[i] = xXOR( rotateNPointRight( xXOR( npointValues[i] ) ) );
                }
            }
            return new HilbertCurve3D( newNpoints );
        }

        /**
         * Rotate about the x-axis. This involves leaving x values the same, but xOR'ing the rest.
         */
        private HilbertCurve3D rotateAboutX()
        {
            int[] newNpoints = new int[length()];
            for ( int i = 0; i < length(); i++ )
            {
                newNpoints[i] = rotateYZ( npointValues[i] );
            }
            return new HilbertCurve3D( newNpoints );
        }

        private HilbertCurve3D singleTon( HilbertCurve3D curve )
        {
            String name = curve.name();
            if ( curves.containsKey( name ) )
            {
                return curves.get( name );
            }
            else
            {
                curves.put( name, curve );
                return curve;
            }
        }

        private void makeChildren()
        {
            this.children = new HilbertCurve3D[length()];
            this.children[0] = singleTon( rotateOneThirdDiagonalPos( true ) );
            this.children[1] = singleTon( rotateOneThirdDiagonalPos( false ) );
            this.children[2] = singleTon( rotateOneThirdDiagonalPos( false ) );
            this.children[3] = singleTon( rotateAboutX() );
            this.children[4] = singleTon( rotateAboutX() );
            this.children[5] = singleTon( rotateOneThirdDiagonalNeg( true ) );
            this.children[6] = singleTon( rotateOneThirdDiagonalNeg( true ) );
            this.children[7] = singleTon( rotateOneThirdDiagonalNeg( false ) );
        }

        @Override
        public CurveRule childAt( int npoint )
        {
            if ( children == null )
            {
                makeChildren();
            }
            return children[npoint];
        }
    }

    static HashMap<String,HilbertCurve3D> curves = new LinkedHashMap<>();

    private static HilbertCurve3D addCurveRule( int... npointValues )
    {
        HilbertCurve3D curve = new HilbertCurve3D( npointValues );
        String name = curve.name();
        if ( !curves.containsKey( name ) )
        {
            curves.put( name, curve );
        }
        return curve;
    }

    private static final HilbertCurve3D curveUFR = addCurveRule( 0b000, 0b010, 0b011, 0b001, 0b101, 0b111, 0b110, 0b100 );

    public static final int MAX_LEVEL = 63 / 3 - 1;

    public HilbertSpaceFillingCurve3D( Envelope range )
    {
        this( range, MAX_LEVEL );
    }

    public HilbertSpaceFillingCurve3D( Envelope range, int maxLevel )
    {
        super( range, maxLevel );
        assert maxLevel <= MAX_LEVEL;
        assert range.getDimension() == 3;
    }

    @Override
    protected CurveRule rootCurve()
    {
        return curveUFR;
    }
}
