/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package rscminus.game.data;

import rscminus.game.constants.Game;

public class SaveInfo {
    public int inventoryCount;
    public int inventory[];
    public int inventoryStack[];
    public boolean inventoryEquipped[];

    public int statCurrent[];
    public int statMax[];
    public int statXP[];
    public int fatigue;

    // Appearance
    public int headType;
    public int top;
    public int bottom;
    public int hairColor;
    public int topColor;
    public int bottomColor;
    public int skinColor;

    // Location
    public int x;
    public int y;

    public SaveInfo() {
        // Initialize inventory
        inventoryCount = 0;
        inventory = new int[Game.INVENTORY_COUNT];
        inventoryStack = new int[Game.INVENTORY_COUNT];
        inventoryEquipped = new boolean[Game.INVENTORY_COUNT];

        // Initialize stats
        statCurrent = new int[Game.STAT_COUNT];
        statMax = new int[Game.STAT_COUNT];
        statXP = new int[Game.STAT_COUNT];

        // Initialize fatigue
        fatigue = 0;

        headType = -1;
        top = -1;
        bottom = -1;
        hairColor = 0;
        topColor = 0;
        bottomColor = 0;
        skinColor = 0;

        // Initialize position
        x = 279;
        y = 493;
        //x = 280;
        //y = 498;

        //x = 235;
        //y = 486;
        //x = 163;
        //y = 141;
        //x = 219;
        //y = 724;
        //x = 146;
        //y = 508;

        //x = 449;
        //y = 765;
    }
}
