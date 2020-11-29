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

import rscminus.common.Logger;
import rscminus.common.Settings;
import rscminus.game.constants.Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class SaveInfo {
    public boolean firstLogin;
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
    public int hairColour;
    public int topColour;
    public int bottomColour;
    public int skinColour;

    // Location
    public int x;
    public int y;

    public SaveInfo() {
        firstLogin = true;

        // Initialize inventory
        inventoryCount = 0;
        inventory = new int[Game.INVENTORY_COUNT];
        inventoryStack = new int[Game.INVENTORY_COUNT];
        inventoryEquipped = new boolean[Game.INVENTORY_COUNT];

        // Initialize stats
        statCurrent = new int[Game.STAT_COUNT];
        statMax = new int[Game.STAT_COUNT];
        statXP = new int[Game.STAT_COUNT];

        for (int i = 0; i < Game.STAT_COUNT; i++) {
            statCurrent[i] = 1;
            statMax[i] = 1;
        }
        statCurrent[Game.STAT_HITS] = 10;
        statMax[Game.STAT_HITS] = 10;
        statXP[Game.STAT_HITS] = 1000 * 4;

        // Initialize fatigue
        fatigue = 0;

        headType = -1;
        top = -1;
        bottom = -1;
        hairColour = 0;
        topColour = 0;
        bottomColour = 0;
        skinColour = 0;

        // Initialize position
        // TODO: this should be tutorial island
        x = 61;
        y = 440;
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
    public int load(LoginInfo loginInfo) {
        Logger.Info(String.format("Loading Save Info for %s from ", loginInfo.username));

        String playerSaveFileName =
            String.format("%s%s", loginInfo.username, ".ini");

        Properties saveInfo = new Properties();
        try {
            FileInputStream in = new FileInputStream(playerSaveFileName);
            saveInfo.load(in);
            in.close();

            this.firstLogin = false;

            this.statXP[Game.STAT_MAGIC] = Integer.parseInt(saveInfo.getProperty("magicXP"));
            this.statCurrent[Game.STAT_MAGIC] = Integer.parseInt(saveInfo.getProperty("magicLevel"));
            this.statMax[Game.STAT_MAGIC] = Integer.parseInt(saveInfo.getProperty("magicLevel"));

            this.headType = Integer.parseInt(saveInfo.getProperty("headType"));
            this.top = Integer.parseInt(saveInfo.getProperty("top"));
            this.bottom = Integer.parseInt(saveInfo.getProperty("bottom"));
            this.hairColour = Integer.parseInt(saveInfo.getProperty("hairColour"));
            this.topColour = Integer.parseInt(saveInfo.getProperty("topColour"));
            this.bottomColour = Integer.parseInt(saveInfo.getProperty("bottomColour"));
            this.skinColour = Integer.parseInt(saveInfo.getProperty("skinColour"));
        } catch (Exception e) {
            Logger.Warn("Error loading " + playerSaveFileName);
            return 1;
        }
        return 0;
    }

    public int save(LoginInfo loginInfo) {
        Logger.Info(String.format("Saving Save Info for %s", loginInfo.username));

        String playerSaveFileName =
            String.format("%s%s", loginInfo.username, ".ini");

        Properties saveInfo = new Properties();
        saveInfo.setProperty("magicXP", String.format("%d", this.statXP[Game.STAT_MAGIC]));
        saveInfo.setProperty("magicLevel", String.format("%d", this.statMax[Game.STAT_MAGIC]));
        saveInfo.setProperty("headType", String.format("%d", this.headType));
        saveInfo.setProperty("top", String.format("%d", this.top));
        saveInfo.setProperty("bottom", String.format("%d", this.bottom));
        saveInfo.setProperty("hairColour", String.format("%d", this.hairColour));
        saveInfo.setProperty("topColour", String.format("%d", this.topColour));
        saveInfo.setProperty("bottomColour", String.format("%d", this.bottomColour));
        saveInfo.setProperty("skinColour", String.format("%d", this.skinColour));

        try {
            FileOutputStream out = new FileOutputStream(new File(Settings.Dir.SAVES, playerSaveFileName));
            saveInfo.store(out, "---rscminus player save data---");
            out.close();
        } catch (Exception e) {
            Logger.Warn("Error saving save data for " + playerSaveFileName);
        }


        return 0;
    }
}
