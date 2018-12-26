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

package rscminus.game;

import rscminus.common.JGameData;
import rscminus.game.constants.Game;
import rscminus.game.entity.GameObject;
import rscminus.game.entity.Player;
import rscminus.game.entity.WallObject;
import rscminus.game.world.ViewArea;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class WorldManager {
    private ViewArea m_viewArea[][];
    private int m_width;
    private int m_height;

    public WorldManager() {
        m_width = Game.WORLD_WIDTH >> 3;
        m_height = Game.WORLD_HEIGHT >> 3;
        m_viewArea = new ViewArea[m_width][m_height];
        for (int x = 0; x < m_width; x++)
            for (int y = 0; y < m_height; y++)
                m_viewArea[x][y] = new ViewArea();
    }

    public void clearUpdates() {
        for (int x = 0; x < m_width; x++)
            for (int y = 0; y < m_height; y++)
                m_viewArea[x][y].clearUpdates();
    }

    public ViewArea getViewArea(int x, int y) {
        if (x < 0 || y < 0)
            return null;
        return m_viewArea[x][y];
    }

    public ViewArea getViewAreaCoordinate(int x, int y) {
        return m_viewArea[x >> 3][y >> 3];
    }

    public void addObject(int x, int y, int id) {
        ViewArea view = getViewAreaCoordinate(x, y);
        view.add(new GameObject(view, x, y, id, getTileDirection(x, y)));
    }

    public void addWallObject(int x, int y, int id, int direction) {
        ViewArea view = getViewAreaCoordinate(x, y);
        view.add(new WallObject(view, x, y, id, direction));
    }

    public void removePlayer(Player player) {
        ViewArea view = getViewAreaCoordinate(player.getX(), player.getY());
        view.remove(player);
    }

    public void interactWallObject(Player player, int x, int y, int direction, int option) {
        int directionX = x - player.getX();
        int directionY = y - player.getY();
        ViewArea view = getViewAreaCoordinate(x, y);
        WallObject wallObj = view.getWallObject(x, y, direction);
        if (wallObj != null && wallObj.getInteractable(directionX, directionY))
            player.interactWallObject(wallObj, option);
    }

    public void interactObject(Player player, int x, int y, int option) {
        int directionX = x - player.getX();
        int directionY = y - player.getY();
        ViewArea view = getViewAreaCoordinate(x, y);
        GameObject obj = view.getObject(x, y);
        if (obj != null)
            player.interactObject(obj, option);
    }

    public int getCollisionMask(int x, int y) {
        int floor = y / Game.WORLD_Y_OFFSET;
        int floorOffset = floor * Game.WORLD_Y_OFFSET;
        int worldX = Game.WORLD_PLANE_X + x;
        int worldY = Game.WORLD_PLANE_Y - floorOffset + y;
        int regionX = worldX / 48;
        int regionY = worldY / 48;
        worldX = worldX - (regionX * 48);
        worldY = worldY - (regionY * 48);
        int index = (worldX * Game.REGION_HEIGHT) + worldY;
        int regionCollisionMask = JGameData.regionCollisionMask[regionX][regionY][floor][index];
        int objectCollisionMask = Game.COLLISION_NONE;

        ViewArea view = getViewAreaCoordinate(x, y);
        WallObject wallobj = view.getWallObject(x, y);

        if (wallobj != null) {
            objectCollisionMask = wallobj.getCollisionMask();
            regionCollisionMask &= ~(wallobj.getExpectedCollisionMask());
        }

        return regionCollisionMask | objectCollisionMask;
    }

    public int getTileDirection(int x, int y) {
        int floor = y / Game.WORLD_Y_OFFSET;
        int floorOffset = floor * Game.WORLD_Y_OFFSET;
        int worldX = Game.WORLD_PLANE_X + x;
        int worldY = Game.WORLD_PLANE_Y - floorOffset + y;
        int regionX = worldX / 48;
        int regionY = worldY / 48;
        worldX = worldX - (regionX * 48);
        worldY = worldY - (regionY * 48);
        int index = (worldX * Game.REGION_HEIGHT) + worldY;
        return JGameData.regionDirection[regionX][regionY][floor][index];
    }

    public boolean checkCollisionTile(int x, int y, int directionX, int directionY) {
        return ((getCollisionMask(x + directionX, y + directionY) & Game.COLLISION_TILE) != 0);
    }

    public boolean checkCollisionCardinal(int x, int y, int directionX, int directionY) {
        if (checkCollisionTile(x, y, directionX, directionY))
            return true;

        if (directionX == 0) {
            if (directionY == 1)
                return ((getCollisionMask(x, y + directionY) & Game.COLLISION_NORTHSOUTH) != 0);
            else if (directionY == -1)
                return ((getCollisionMask(x, y) & Game.COLLISION_NORTHSOUTH) != 0);
        } else if (directionY == 0) {
            if (directionX == 1)
                return ((getCollisionMask(x + directionX, y) & Game.COLLISION_EASTWEST) != 0);
            else if (directionX == -1)
                return ((getCollisionMask(x, y) & Game.COLLISION_EASTWEST) != 0);
        }
        return false;
    }

    public boolean checkCollisionIntermediate(int x, int y, int directionX, int directionY) {
        if (checkCollisionTile(x, y, 0, directionY))
            return true;
        else if (checkCollisionTile(x, y, directionX, 0))
            return true;
        else if (checkCollisionTile(x, y, directionX, directionY))
            return true;

        if (directionX == 1) {
            if (directionY == 1)
                return ((getCollisionMask(x + directionX, y + directionY) & (Game.COLLISION_NORTHSOUTH | Game.COLLISION_EASTWEST)) != 0) ||
                        ((getCollisionMask(x, y + directionY) & Game.COLLISION_NORTHSOUTH) != 0);
            else if (directionY == -1)
                return ((getCollisionMask(x + directionX, y + directionY) & Game.COLLISION_EASTWEST) != 0) ||
                        ((getCollisionMask(x + directionX, y) & (Game.COLLISION_NORTHSOUTH | Game.COLLISION_EASTWEST)) != 0);
        } else if (directionX == -1) {
            if (directionY == 1)
                return ((getCollisionMask(x, y) & Game.COLLISION_EASTWEST) != 0) ||
                        ((getCollisionMask(x, y + directionY) & (Game.COLLISION_EASTWEST | Game.COLLISION_NORTHSOUTH)) != 0);
            else if (directionY == -1)
                return ((getCollisionMask(x, y) & (Game.COLLISION_EASTWEST | Game.COLLISION_NORTHSOUTH)) != 0) ||
                        ((getCollisionMask(x + directionX, y) & Game.COLLISION_NORTHSOUTH) != 0);
        }
        return false;
    }

    public boolean checkCollision(int x, int y, int directionX, int directionY) {
        System.out.println(getCollisionMask(x + directionX, y + directionY));

        if (checkCollisionCardinal(x, y, directionX, directionY))
            return true;

        if (checkCollisionIntermediate(x, y, directionX, directionY))
            return true;

        return false;
    }

    public boolean init() {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File("objects.bin"))));
            int count = in.readInt();
            System.out.println("count: " + count);
            for (int i = 0; i < count; i++) {
                int x = in.readUnsignedShort();
                int y = in.readUnsignedShort();
                int id = in.readUnsignedShort();
                addObject(x, y, id);
            }
            in.close();
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File("wallObjects.bin"))));
            count = in.readInt();
            System.out.println("count: " + count);
            for (int i = 0; i < count; i++) {
                int x = in.readUnsignedShort();
                int y = in.readUnsignedShort();
                int id = in.readUnsignedShort();
                int direction = in.readUnsignedByte();
                addWallObject(x, y, id, direction);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
