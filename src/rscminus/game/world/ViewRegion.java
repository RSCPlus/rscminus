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
import rscminus.game.WorldManager;
import rscminus.game.entity.Player;

import java.util.Iterator;
import java.util.LinkedList;

public class ViewRegion {
    public static final int VIEW_DISTANCE = 5;

    private LinkedList<Player> m_localPlayers;
    private WorldManager m_worldManager;
    private ViewArea m_viewArea[][];
    private boolean m_update;

    public ViewRegion(WorldManager worldManager) {
        m_worldManager = worldManager;
        m_viewArea = new ViewArea[VIEW_DISTANCE][VIEW_DISTANCE];
        m_localPlayers = new LinkedList<Player>();
    }

    public void clear() {
        for (int x = 0; x < VIEW_DISTANCE; x++)
            for (int y = 0; y < VIEW_DISTANCE; y++)
                m_viewArea[x][y] = null;
        m_update = false;
        m_localPlayers.clear();
    }

    public void update(Player player) {
        int viewX = player.getX() >> 3;
        int viewY = player.getY() >> 3;
        int index = VIEW_DISTANCE / 2;

        ViewArea view = m_worldManager.getViewArea(viewX, viewY);
        if (m_viewArea[index][index] != view) {
            if (m_viewArea[index][index] != null)
                m_viewArea[index][index].remove(player);
            for (int x = 0; x < VIEW_DISTANCE; x++) {
                for (int y = 0; y < VIEW_DISTANCE; y++) {
                    int updateX = viewX + (x - index);
                    int updateY = viewY + (y - index);
                    m_viewArea[x][y] = m_worldManager.getViewArea(updateX, updateY);
                }
            }
            m_viewArea[index][index].add(player);
            m_update = true;
        }
    }

    public void sendUpdate(Player player) {
        for (Iterator<Player> otherPlayer = m_localPlayers.iterator(); otherPlayer.hasNext();) {
            if (otherPlayer.next().getDistance(player) > 15)
                otherPlayer.remove();
        }

        PacketBuilder.startCreatePlayers(player, player.getNetworkStream(), player.getISAACCipher());
        for (int x = 0; x < VIEW_DISTANCE; x++) {
            for (int y = 0; y < VIEW_DISTANCE; y++) {
                if (m_viewArea[x][y] != null) {
                    for (Player otherPlayer : m_viewArea[x][y].getPlayers()) {
                        if (otherPlayer == player)
                            continue;

                        if (otherPlayer.getDistance(player) <= 15)
                            PacketBuilder.addCreatePlayers(player, otherPlayer, player.getNetworkStream());
                    }
                }
            }
        }
        PacketBuilder.endCreatePlayers(player.getNetworkStream());

        PacketBuilder.startPlayerUpdate(player.getNetworkStream(), player.getISAACCipher());
        for (int x = 0; x < VIEW_DISTANCE; x++) {
            for (int y = 0; y < VIEW_DISTANCE; y++) {
                if (m_viewArea[x][y] != null) {
                    for (Player otherPlayer : m_viewArea[x][y].getPlayers()) {
                        if (m_localPlayers.contains(otherPlayer) && otherPlayer.getDistance(player) <= 15) {
                            otherPlayer.processPlayerUpdate(player.getNetworkStream());
                        } else {
                            PacketBuilder.addPlayerUpdateAppearance(otherPlayer, player.getNetworkStream());
                            m_localPlayers.add(otherPlayer);
                        }
                    }
                }
            }
        }
        PacketBuilder.endPlayerUpdate(player.getNetworkStream());

        if (m_update) {
            PacketBuilder.startObjectUpdate(player.getNetworkStream(), player.getISAACCipher());
            for (int x = 0; x < VIEW_DISTANCE; x++) {
                for (int y = 0; y < VIEW_DISTANCE; y++) {
                    if (m_viewArea[x][y] != null)
                        m_viewArea[x][y].writeObjects(player);
                }
            }
            PacketBuilder.endObjectUpdate(player.getNetworkStream());
            PacketBuilder.startWallObjectUpdate(player.getNetworkStream(), player.getISAACCipher());
            for (int x = 0; x < VIEW_DISTANCE; x++) {
                for (int y = 0; y < VIEW_DISTANCE; y++) {
                    if (m_viewArea[x][y] != null)
                        m_viewArea[x][y].writeWallObjects(player);
                }
            }
            PacketBuilder.endWallObjectUpdate(player.getNetworkStream());
            m_update = false;
        } else {
            PacketBuilder.startObjectUpdate(player.getNetworkStream(), player.getISAACCipher());
            for (int x = 0; x < VIEW_DISTANCE; x++) {
                for (int y = 0; y < VIEW_DISTANCE; y++) {
                    if (m_viewArea[x][y] != null)
                        m_viewArea[x][y].updateObjects(player);
                }
            }
            PacketBuilder.endObjectUpdate(player.getNetworkStream());
            PacketBuilder.startWallObjectUpdate(player.getNetworkStream(), player.getISAACCipher());
            for (int x = 0; x < VIEW_DISTANCE; x++) {
                for (int y = 0; y < VIEW_DISTANCE; y++) {
                    if (m_viewArea[x][y] != null)
                        m_viewArea[x][y].updateWallObjects(player);
                }
            }
            PacketBuilder.endWallObjectUpdate(player.getNetworkStream());
        }
    }
}
