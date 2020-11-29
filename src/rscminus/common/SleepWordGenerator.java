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
package rscminus.common;

import java.io.File;
import java.util.HashMap;

public class SleepWordGenerator {

    public static int notSoRandomCounter = 1778;
    private static int loadedCheaterCaptchasSize = 0;
    private static HashMap<Integer, SleepWord> allCheaterCaptchas = new HashMap<>();

    public static boolean init() {
        return initializeCheaterCaptchas();
    }
    private static boolean initializeCheaterCaptchas() {
        try {
            File cheaterDataDir = new File("cheaterSleepwords");
            File[] cheaterDataPaths = cheaterDataDir.listFiles();

            int size = -1;
            for (File cheater : cheaterDataPaths) {
                SleepWord a = new SleepWord();
                a.populateCheaterData(cheater);
                allCheaterCaptchas.put(++size, a);
            }
            loadedCheaterCaptchasSize = size;
            Logger.Info(String.format("Loaded %d captchas!", size));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static SleepWord getRandomSleepword() {
        while (true) {
            SleepWord candidate = allCheaterCaptchas.get(notSoRandomCounter);
            if (candidate.filename.contains("!INCORRECT!") ||
                candidate.filename.contains("!SUDDENLY-AWOKE!")) {
                Logger.Info(String.format("Picking %d out of %d available captchas", notSoRandomCounter, allCheaterCaptchas.size()));
                incrementCounter();
                return candidate;
            }
            incrementCounter();
        }
    }

    private static void incrementCounter() {
        ++notSoRandomCounter;
        if (notSoRandomCounter > loadedCheaterCaptchasSize) {
            notSoRandomCounter = 0;
            Logger.Warn("Finished all captchas!");
        }
    }
}
