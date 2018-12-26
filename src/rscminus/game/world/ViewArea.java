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

package rscminus.game.world;

import rscminus.game.PacketBuilder;
import rscminus.game.entity.GameObject;
import rscminus.game.entity.Player;
import rscminus.game.entity.WallObject;

import java.util.HashSet;
import java.util.LinkedList;

public class ViewArea {
    private LinkedList<Player> m_players;
    private LinkedList<GameObject> m_objects;
    private LinkedList<WallObject> m_wallObjects;
    private HashSet<Player> m_playerUpdates;
    private HashSet<GameObject> m_objectUpdates;
    private HashSet<WallObject> m_wallObjectUpdates;

    public ViewArea() {
        m_players = new LinkedList<Player>();
        m_objects = new LinkedList<GameObject>();
        m_wallObjects = new LinkedList<WallObject>();
        m_playerUpdates = new HashSet<Player>();
        m_objectUpdates = new HashSet<GameObject>();
        m_wallObjectUpdates = new HashSet<WallObject>();
    }

    public LinkedList<Player> getPlayers() {
        return m_players;
    }

    public void add(GameObject gameObject) {
        m_objects.add(gameObject);
    }

    public void add(WallObject wallObject) {
        m_wallObjects.add(wallObject);
    }

    public void add(Player player) {
        m_players.add(player);
    }

    public void remove(Player player) {
        m_players.remove(player);
    }

    public void update(WallObject wallObject) {
        m_wallObjectUpdates.add(wallObject);
    }

    public void update(GameObject obj) {
        m_objectUpdates.add(obj);
    }

    public void clearUpdates() {
        m_playerUpdates.clear();
        m_objectUpdates.clear();
        m_wallObjectUpdates.clear();
    }

    public void writeObjects(Player player) {
        for (GameObject obj : m_objects)
            PacketBuilder.addObjectUpdate(player, obj, player.getNetworkStream(), player.getISAACCipher());
    }

    public void writeWallObjects(Player player) {
        for (WallObject wallObj : m_wallObjects)
            PacketBuilder.addWallObjectUpdate(player, wallObj, player.getNetworkStream(), player.getISAACCipher());
    }

    public void updateObjects(Player player) {
        for (GameObject obj : m_objectUpdates)
            PacketBuilder.addObjectUpdate(player, obj, player.getNetworkStream(), player.getISAACCipher());
    }

    public void updateWallObjects(Player player) {
        for (WallObject wallObj : m_wallObjectUpdates)
            PacketBuilder.addWallObjectUpdate(player, wallObj, player.getNetworkStream(), player.getISAACCipher());
    }

    public WallObject getWallObject(int x, int y) {
        for (WallObject wallObj : m_wallObjects)
            if (wallObj.getX() == x && wallObj.getY() == y)
                return wallObj;
        return null;
    }

    public WallObject getWallObject(int x, int y, int direction) {
        for (WallObject wallObj : m_wallObjects)
            if (wallObj.getX() == x && wallObj.getY() == y && wallObj.getDirection() == direction)
                return wallObj;
        return null;
    }

    public GameObject getObject(int x, int y) {
        for (GameObject obj : m_objects)
            if (obj.getX() == x && obj.getY() == y)
                return obj;
        return null;
    }
}
