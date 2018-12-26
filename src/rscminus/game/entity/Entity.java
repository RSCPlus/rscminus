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

public class Entity {
    private boolean m_active;

    public Entity() {
        m_active = false;
    }

    public void setActive(boolean active) {
        m_active = active;
    }

    public boolean isActive() {
        return m_active;
    }

    public int getX() {
        return 0;
    }

    public int getY() {
        return 0;
    }

    public int getDistance(Entity otherEntity) {
        int xDist = getX() - otherEntity.getX();
        int yDist = getY() - otherEntity.getY();
        return Math.max(Math.abs(xDist), Math.abs(yDist));
    }
}
