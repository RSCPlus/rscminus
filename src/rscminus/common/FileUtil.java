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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

public class FileUtil {
    public static void mkdir(String path) {
        new File(path).mkdirs();
    }

    public static boolean writeFull(String fname, byte[] data) {
        try {
            DataOutputStream os = new DataOutputStream(new FileOutputStream(fname));
            os.write(data);
            os.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] readFull(File f) {
        try {
            return Files.readAllBytes(f.toPath());
        } catch (Exception e) {
            return null;
        }
    }

    public static String findDirectoryReverse(String name) {
        String ret = Settings.Dir.JAR;

        for (int i = 0; i < 8; i++) {
            File file = new File(ret + name);
            if (file.exists() && file.isDirectory()) return ret;
            ret += "/..";
        }

        return Settings.Dir.JAR;
    }

    public static void copyFile(File source, File dest) throws IOException {
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return;
        }
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            sourceChannel.close();
            destChannel.close();
        }
    }

}
