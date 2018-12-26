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

package rscminus.game.entity.player;

import rscminus.common.MathUtil;
import rscminus.game.constants.Game;

public class WalkingQueue {
    public static final int MAX_SIZE = 200;

    private int m_x[];
    private int m_y[];
    private int m_count;
    private int m_index;
    private int m_nextX;
    private int m_nextY;
    private int m_nextDirection;

    public WalkingQueue() {
        m_x = new int[MAX_SIZE];
        m_y = new int[MAX_SIZE];
        clear();
    }

    public int getNextX() {
        return m_nextX;
    }

    public int getNextY() {
        return m_nextY;
    }

    public int getNextDirection() {
        return m_nextDirection;
    }

    public boolean isFinished() {
        return (m_index == m_count);
    }

    public void clear() {
        m_count = 0;
        m_index = 0;
        m_nextX = 0;
        m_nextY = 0;
    }

    public void copyTo(WalkingQueue walkingQueue) {
        walkingQueue.clear();
        for (int i = 0; i < m_count; i++)
            walkingQueue.add(m_x[i], m_y[i]);
    }

    public boolean update(int x, int y) {
        // Nothing in queue
        if (m_count == 0)
            return false;

        if (x == m_x[m_index] && y == m_y[m_index])
            m_index++;

        // Queue is finished
        if (m_index == m_count) {
            clear();
            return false;
        }

        m_nextX = MathUtil.sign(m_x[m_index] - x);
        m_nextY = MathUtil.sign(m_y[m_index] - y);
        m_nextDirection = calculateDirection();

        return true;
    }

    private int calculateDirection() {
        // Handle North, East, South, West
        if (m_nextX == 0) {
            if (m_nextY == -1)
                return Game.DIRECTION_NORTH;
            else if (m_nextY == 1)
                return Game.DIRECTION_SOUTH;
        } else if (m_nextY == 0) {
            if (m_nextX == -1)
                return Game.DIRECTION_EAST;
            else if (m_nextX == 1)
                return Game.DIRECTION_WEST;
        }

        // Handle diagonals
        if (m_nextX == 1) {
            if (m_nextY == -1)
                return Game.DIRECTION_NORTHWEST;
            else if (m_nextY == 1)
                return Game.DIRECTION_SOUTHWEST;
        } else if (m_nextX == -1) {
            if (m_nextY == -1)
                return Game.DIRECTION_NORTHEAST;
            else if (m_nextY == 1)
                return Game.DIRECTION_SOUTHEAST;
        }

        return Game.DIRECTION_NORTH;
    }

    public void add(int x, int y) {
        if (m_count == MAX_SIZE)
            return;
        // TODO: Confirm server drops waypoints on the same tile
        if (m_count >= 1 && x == m_x[m_count - 1] && y == m_y[m_count - 1])
            return;
        m_x[m_count] = x;
        m_y[m_count] = y;
        m_count++;
    }
}
