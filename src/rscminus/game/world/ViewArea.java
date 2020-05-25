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
import rscminus.game.entity.Scenery;
import rscminus.game.entity.Player;
import rscminus.game.entity.Boundary;

import java.util.HashSet;
import java.util.LinkedList;

public class ViewArea {
    private LinkedList<Player> m_players;
    private LinkedList<Scenery> m_scenery;
    private LinkedList<Boundary> m_boundaries;
    private HashSet<Player> m_playerUpdates;
    private HashSet<Scenery> m_sceneryUpdates;
    private HashSet<Boundary> m_boundaryUpdates;

    public ViewArea() {
        m_players = new LinkedList<Player>();
        m_scenery = new LinkedList<Scenery>();
        m_boundaries = new LinkedList<Boundary>();
        m_playerUpdates = new HashSet<Player>();
        m_sceneryUpdates = new HashSet<Scenery>();
        m_boundaryUpdates = new HashSet<Boundary>();
    }

    public LinkedList<Player> getPlayers() {
        return m_players;
    }

    public void add(Scenery scenery) {
        m_scenery.add(scenery);
    }

    public void add(Boundary boundary) {
        m_boundaries.add(boundary);
    }

    public void add(Player player) {
        m_players.add(player);
    }

    public void remove(Player player) {
        m_players.remove(player);
    }

    public void update(Boundary boundary) {
        m_boundaryUpdates.add(boundary);
    }

    public void update(Scenery scenery) {
        m_sceneryUpdates.add(scenery);
    }

    public void clearUpdates() {
        m_playerUpdates.clear();
        m_sceneryUpdates.clear();
        m_boundaryUpdates.clear();
    }

    public void writeScenery(Player player) {
        for (Scenery scenery : m_scenery)
            PacketBuilder.addSceneryUpdate(player, scenery, player.getNetworkStream(), player.getISAACCipher());
    }

    public void writeBoundaries(Player player) {
        for (Boundary boundary : m_boundaries)
            PacketBuilder.addBoundaryUpdate(player, boundary, player.getNetworkStream(), player.getISAACCipher());
    }

    public void updateScenery(Player player) {
        for (Scenery scenery : m_sceneryUpdates)
            PacketBuilder.addSceneryUpdate(player, scenery, player.getNetworkStream(), player.getISAACCipher());
    }

    public void updateBoundaries(Player player) {
        for (Boundary boundary : m_boundaryUpdates)
            PacketBuilder.addBoundaryUpdate(player, boundary, player.getNetworkStream(), player.getISAACCipher());
    }

    public Boundary getBoundary(int x, int y) {
        for (Boundary boundary : m_boundaries)
            if (boundary.getX() == x && boundary.getY() == y)
                return boundary;
        return null;
    }

    public Boundary getBoundary(int x, int y, int direction) {
        for (Boundary boundary : m_boundaries)
            if (boundary.getX() == x && boundary.getY() == y && boundary.getDirection() == direction)
                return boundary;
        return null;
    }

    public Scenery getScenery(int x, int y) {
        for (Scenery scenery : m_scenery)
            if (scenery.getX() == x && scenery.getY() == y)
                return scenery;
        return null;
    }
}
