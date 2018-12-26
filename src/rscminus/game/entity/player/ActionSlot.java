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

public class ActionSlot {
    private int m_action;
    private int m_inventorySlot;
    private WalkingQueue m_walkingQueue;
    private int m_interactX;
    private int m_interactY;
    private int m_interactDirection;
    private int m_interactOption;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_INVENTORY_DROP = 1;
    public static final int ACTION_INVENTORY_EQUIP = 2;
    public static final int ACTION_INVENTORY_UNEQUIP = 3;
    public static final int ACTION_WALK = 4;
    public static final int ACTION_INTERACT_WALLOBJECT = 5;
    public static final int ACTION_INTERACT_OBJECT = 6;

    public ActionSlot() {
        m_walkingQueue = new WalkingQueue();
    }

    public int getAction() {
        return m_action;
    }

    public int getInventorySlot() {
        return m_inventorySlot;
    }

    public WalkingQueue getWalkingQueue() {
        return m_walkingQueue;
    }

    public int getInteractX() {
        return m_interactX;
    }

    public int getInteractY() {
        return m_interactY;
    }

    public int getInteractDirection() {
        return m_interactDirection;
    }

    public int getInteractOption() {
        return m_interactOption;
    }

    public void setInteraction(int x, int y, int direction, int option) {
        m_interactX = x;
        m_interactY = y;
        m_interactDirection = direction;
        m_interactOption = option;
    }

    public void setInteraction(int x, int y, int option) {
        setInteraction(x, y, 0, option);
    }

    public void setAction(int action) {
        m_action = action;
    }

    public void setInventorySlot(int slot) {
        m_inventorySlot = slot;
    }

    public void clear() {
        m_action = ACTION_NONE;
        m_walkingQueue.clear();
    }
}
