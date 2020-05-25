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

package rscminus.game.entity;

import rscminus.game.world.ViewArea;

public class Scenery extends Entity {
    private int m_x;
    private int m_y;
    private int m_id;
    private int m_direction;
    private ViewArea m_viewArea;

    public Scenery(ViewArea view, int x, int y, int id, int direction) {
        m_x = x;
        m_y = y;
        m_id = id;
        m_direction = direction;
        m_viewArea = view;
    }

    public int getX() {
        return m_x;
    }

    public int getY() {
        return m_y;
    }

    public int getID() {
        return m_id;
    }

    public int getDirection() {
        return m_direction;
    }

    public void setID(int id) {
        m_id = id;
        m_viewArea.update(this);
    }
}
