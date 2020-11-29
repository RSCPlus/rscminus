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

package rscminus.game.constants;

public class Game {
    // World
    public static final int WORLD_WIDTH = 4608; // Unsure
    public static final int WORLD_HEIGHT = 3776;
    public static final int WORLD_PLANE_X = 2304;
    public static final int WORLD_PLANE_Y = 1776;
    public static final int WORLD_Y_OFFSET = 944;
    public static final int REGION_WIDTH = 48;
    public static final int REGION_HEIGHT = 48;
    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT;
    public static final int REGION_FLOORS = 4;

    // Collision
    public static final int COLLISION_NONE = 0x00;
    public static final int COLLISION_EASTWEST = 0x01;
    public static final int COLLISION_NORTHSOUTH = 0x02;
    public static final int COLLISION_TILE = 0x04;

    // Scenery Direction
    public static final int DIRECTION_NORTH = 0;
    public static final int DIRECTION_NORTHWEST = 1;
    public static final int DIRECTION_WEST = 2;
    public static final int DIRECTION_SOUTHWEST = 3;
    public static final int DIRECTION_SOUTH = 4;
    public static final int DIRECTION_SOUTHEAST = 5;
    public static final int DIRECTION_EAST = 6;
    public static final int DIRECTION_NORTHEAST = 7;

    // Boundary direction
    public static final int DIRECTION_WALLOBJECT_NORTHSOUTH = 0;
    public static final int DIRECTION_WALLOBJECT_EASTWEST = 1;

    // Quests
    public static final int QUEST_BLACK_KNIGHTS_FORTRESS = 0;
    public static final int QUEST_COOKS_ASSISTANT = 1;
    public static final int QUEST_DEMON_SLAYER = 2;
    public static final int QUEST_DORICS_QUEST = 3;
    public static final int QUEST_THE_RESTLESS_GHOST = 4;
    public static final int QUEST_GOBLIN_DIPLOMACY = 5;
    public static final int QUEST_ERNEST_THE_CHICKEN = 6;
    public static final int QUEST_IMP_CATCHER = 7;
    public static final int QUEST_PIRATES_TREASURE = 8;
    public static final int QUEST_PRINCE_ALI_RESCUE = 9;
    public static final int QUEST_ROMEO_AND_JULIET = 10;
    public static final int QUEST_SHEEP_SHEARER = 11;
    public static final int QUEST_SHIELD_OF_ARRAV = 12;
    public static final int QUEST_THE_KNIGHTS_SWORD = 13;
    public static final int QUEST_VAMPIRE_SLAYER = 14;
    public static final int QUEST_WITCHS_POTION = 15;
    public static final int QUEST_DRAGON_SLAYER = 16;
    public static final int QUEST_WITCHS_HOUSE = 17;
    public static final int QUEST_LOST_CITY = 18;
    public static final int QUEST_HEROS_QUEST = 19;
    public static final int QUEST_DRUIDIC_RITUAL = 20;
    public static final int QUEST_MERLINS_CRYSTAL = 21;
    public static final int QUEST_SCORPION_CATCHER = 22;
    public static final int QUEST_FAMILY_CREST = 23;
    public static final int QUEST_TRIBAL_TOTEM = 24;
    public static final int QUEST_FISHING_CONTEST = 25;
    public static final int QUEST_MONKS_FRIEND = 26;
    public static final int QUEST_TEMPLE_OF_IKOV = 27;
    public static final int QUEST_CLOCK_TOWER = 28;
    public static final int QUEST_THE_HOLY_GRAIL = 29;
    public static final int QUEST_FIGHT_ARENA = 30;
    public static final int QUEST_TREE_GNOME_VILLAGE = 31;
    public static final int QUEST_THE_HAZEEL_CULT = 32;
    public static final int QUEST_SHEEP_HERDER = 33;
    public static final int QUEST_PLAGUE_CITY = 34;
    public static final int QUEST_SEA_SLUG = 35;
    public static final int QUEST_WATERFALL_QUEST = 36;
    public static final int QUEST_BIOHAZARD = 37;
    public static final int QUEST_JUNGLE_POTION = 38;
    public static final int QUEST_GRAND_TREE = 39;
    public static final int QUEST_SHILO_VILLAGE = 40;
    public static final int QUEST_UNDERGROUND_PASS = 41;
    public static final int QUEST_OBSERVATORY_QUEST = 42;
    public static final int QUEST_TOURIST_TRAP = 43;
    public static final int QUEST_WATCHTOWER = 44;
    public static final int QUEST_DWARF_CANNON = 45;
    public static final int QUEST_MURDER_MYSTERY = 46;
    public static final int QUEST_DIGSITE = 47;
    public static final int QUEST_GERTRUDES_CAT = 48;
    public static final int QUEST_LEGENDS_QUEST = 49;
    public static final int QUEST_COUNT = 50;

    // Welcome screen constants
    public static final int WELCOME_IP_HIDE = 0;
    public static final int WELCOME_MESSAGES_HIDE = 0;
    public static final int WELCOME_MESSAGES_SHOW = 1;
    public static final int WELCOME_RECOVERY_UNSET = 200;
    public static final int WELCOME_RECOVERY_HIDE = 201;

    // Inventory
    public static final int INVENTORY_COUNT = 35;

    // Equipment stats
    public static final int EQUIP_STAT_ARMOUR = 0;
    public static final int EQUIP_STAT_WEAPONAIM = 1;
    public static final int EQUIP_STAT_WEAPONPOWER = 2;
    public static final int EQUIP_STAT_MAGIC = 3;
    public static final int EQUIP_STAT_PRAYER = 4;
    public static final int EQUIP_STAT_COUNT = 5;

    // Prayers
    public static final int PRAYER_COUNT = 14;

    // Stats
    public static final int STAT_ATTACK = 0;
    public static final int STAT_DEFENSE = 1;
    public static final int STAT_STRENGTH = 2;
    public static final int STAT_HITS = 3;
    public static final int STAT_RANGED = 4;
    public static final int STAT_PRAYER = 5;
    public static final int STAT_MAGIC = 6;
    public static final int STAT_COUNT = 18;
    public static final String[] STAT_NAMES = { "attack", "defense", "strength", "hitpoints", "ranged", "prayer", "magic" }; // TODO research to make sure all spellings are correct for all skills as typed in the levelup messages

    // Chat Types
    public static final int CHAT_NONE = 0;
    public static final int CHAT_QUEST = 3;
    public static final int CHAT_CHAT = 4;

    // Precomputed XP Table
    public static final int[] xpLevelTable = { 0, 332, 696, 1104, 1552, 2048, 2600, 3204, 3876, 4616, 5432, 6336, 7332, 8428, 9644, 10984, 12460, 14092, 15892, 17880, 20072, 22496, 25164, 28112, 31368, 34960, 38920, 43296, 48124, 53452, 59332, 65824, 72988, 80896, 89624, 99260, 109892, 121632, 134592, 148896, 164684, 182116, 201356, 222596, 246048, 271932, 300508, 332056, 366884, 405332, 447780, 494640, 546376, 603488, 666544, 736160, 813016, 897864, 991544, 1094968, 1209152, 1335216, 1474396, 1628060, 1797712, 1985016, 2191812, 2420128, 2672204, 2950508, 3257780, 3597028, 3971580, 4385112, 4841684, 5345772, 5902324, 6516800, 7195232, 7944272, 8771272, 9684348, 10692456, 11805492, 13034376, 14391168, 15889176, 17543104, 19369180, 21385328, 23611324, 26069012, 28782516, 31778456, 35086232, 38738308, 42770516, 47222424, 52137724 };

}
