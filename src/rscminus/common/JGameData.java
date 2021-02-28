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

import rscminus.game.constants.Game;

public class JGameData {
    public static int itemCount;
    public static String itemName[];
    public static String itemExamine[];
    public static String itemCommand[];
    public static int itemPrice[];
    public static boolean itemStackable[];
    public static int itemWearable[];
    public static boolean itemTradable[];
    public static boolean itemMembers[];
    public static int npcCount;
    public static String npcName[];
    public static String npcExamine[];
    public static int npcAttack[];
    public static int npcStrength[];
    public static int npcHits[];
    public static int npcDefense[];
    public static int npcAttackable[];
    public static String npcCommand[];
    public static int animationCount;
    public static String animationName[];
    public static int animationIndex[];
    public static int sceneryCount;
    public static int sceneryWidth[];
    public static int sceneryHeight[];
    public static int boundaryCount;
    public static boolean boundaryAdjacent[];
    public static boolean boundaryPassable[];
    public static int tileCount;
    public static int tileDecoration[];
    public static int tileType[];
    public static int tileAdjacent[];

    public static byte regionCollisionMask[][][][];
    public static byte regionDirection[][][][];
    public static int regionMap[][][][];

    public static short terrainWallNorthSouth[][][][];
    public static short terrainWallEastWest[][][][];
    public static short terrainWallDiagonal[][][][];
    public static short terrainHeight[][][][];
    public static short terrainColor[][][][];
    public static short terrainDecoration[][][][];
    public static short terrainDirection[][][][];
    public static int terrainColorPalette[];
    public static int terrainDecorationPalette[];

    public static int method307(int var0, int var2, int var3) {
        //return -(var0 / 8) + -(var2 / 8 * 1024) + (-1 - var3 / 8 * 32);
        return (var2) << 24 | (var3) << 16 | (var0) << 8 | 0xFF;
    }

    public static boolean init(boolean member) {
        JContent content = new JContent();
        JContent contentMembers = new JContent();
        JContent landscapeContent = new JContent();
        JContent landscapeContentMembers = new JContent();

        // Generate landscape color palette
        terrainColorPalette = new int[256];
        for (int var3 = 0; var3 < 64; ++var3) {
            terrainColorPalette[var3] = method307(-(4 * var3) + 255, 255 - var3 * 4, -((int) ((double) var3 * 1.75D)) + 255);
        }

        for (int var4 = 0; var4 < 64; ++var4) {
            terrainColorPalette[var4 + 64] = method307(0, var4 * 3, 144);
        }

        for (int var5 = 0; var5 < 64; ++var5) {
            terrainColorPalette[128 + var5] = method307(0, 192 - (int) ((double) var5 * 1.5D), 144 - (int) ((double) var5 * 1.5D));
        }

        for (int var6 = 0; var6 < 64; ++var6) {
            terrainColorPalette[192 + var6] = method307(0, -((int) (1.5D * (double) var6)) + 96, 48 + (int) (1.5D * (double) var6));
        }

        // Read content0 (Configuration)
        if(!content.open("content0_229aa476"))
            return false;
        JContentFile string = content.unpack("string.dat");
        if (string == null)
            return false;
        JContentFile integer = content.unpack("integer.dat");
        if (integer == null)
            return false;
        content.close();

        // Read item data
        itemCount = integer.readUnsignedShort();
        itemName = new String[itemCount];
        itemExamine = new String[itemCount];
        itemCommand = new String[itemCount];
        itemPrice = new int[itemCount];
        itemStackable = new boolean[itemCount];
        itemWearable = new int[itemCount];
        itemTradable = new boolean[itemCount];
        itemMembers = new boolean[itemCount];
        for (int i = 0; i < itemCount; i++)
            itemName[i] = string.readString();
        for (int i = 0; i < itemCount; i++)
            itemExamine[i] = string.readString();
        for (int i = 0; i < itemCount; i++)
            itemCommand[i] = string.readString();
        for (int i = 0; i < itemCount; i++)
            integer.skip(2); // Sprite
        for (int i = 0; i < itemCount; i++)
            itemPrice[i] = integer.readUnsignedInt();
        for (int i = 0; i < itemCount; i++)
            itemStackable[i] = (integer.readUnsignedByte() == 0);
        for (int i = 0; i < itemCount; i++)
            integer.skip(1); // Unused
        for (int i = 0; i < itemCount; i++)
            itemWearable[i] = integer.readUnsignedShort();
        for (int i = 0; i < itemCount; i++)
            integer.skip(4); // Mask
        for (int i = 0; i < itemCount; i++)
            itemTradable[i] = (integer.readUnsignedByte() == 0);
        for (int i = 0; i < itemCount; i++)
            itemMembers[i] = (integer.readUnsignedByte() == 1);

        // Read npc data
        npcCount = integer.readUnsignedShort();
        npcName = new String[npcCount];
        npcExamine = new String[npcCount];
        npcAttack = new int[npcCount];
        npcStrength = new int[npcCount];
        npcHits = new int[npcCount];
        npcDefense = new int[npcCount];
        npcAttackable = new int[npcCount];
        npcCommand = new String[npcCount];
        System.out.println("NPC Names:");
        for (int i = 0; i < npcCount; i++) {
            npcName[i] = string.readString();
            System.out.print(npcName[i]);
            System.out.print(",");
        }
        System.out.println();
        for (int i = 0; i < npcCount; i++)
            npcExamine[i] = string.readString();
        for (int i = 0; i < npcCount; i++)
            npcAttack[i] = integer.readUnsignedByte();
        for (int i = 0; i < npcCount; i++)
            npcStrength[i] = integer.readUnsignedByte();
        for (int i = 0; i < npcCount; i++)
            npcHits[i] = integer.readUnsignedByte();
        for (int i = 0; i < npcCount; i++)
            npcDefense[i] = integer.readUnsignedByte();
        for (int i = 0; i < npcCount; i++)
            npcAttackable[i] = integer.readUnsignedByte();
        for (int i = 0; i < npcCount; i++)
            for (int i2 = 0; i2 < 12; i2++)
                integer.skip(1); // Sprite
        for (int i = 0; i < npcCount; i++)
            integer.skip(4); // Hair color
        for (int i = 0; i < npcCount; i++)
            integer.skip(4); // Top color
        for (int i = 0; i < npcCount; i++)
            integer.skip(4); // Bottom color
        for (int i = 0; i < npcCount; i++)
            integer.skip(4); // Skin color
        for (int i = 0; i < npcCount; i++)
            integer.skip(2); // Width
        for (int i = 0; i < npcCount; i++)
            integer.skip(2); // Height
        for (int i = 0; i < npcCount; i++)
            integer.skip(1); // Walk model (?)
        for (int i = 0; i < npcCount; i++)
            integer.skip(1); // Combat model (?)
        for (int i = 0; i < npcCount; i++)
            integer.skip(1); // Combat animation (?)
        for (int i = 0; i < npcCount; i++)
            npcCommand[i] = string.readString();

        // Read texture data
        int textureCount = integer.readUnsignedShort();
        System.out.println("TextureNames:");
        for (int i = 0; i < textureCount; i++) {
            String textureName = string.readString(); // Name
            System.out.print(textureName);
            System.out.print(",");
        }
        System.out.println();

        for (int i = 0; i < textureCount; i++)
            string.readString(); // Subtype name

        // Read animation data
        animationCount = integer.readUnsignedShort();
        animationName = new String[animationCount];
        animationIndex = new int[animationCount];
        System.out.println("Animation Names:");
        for (int i = 0; i < animationCount; i++) {
            animationName[i] = string.readString();
            System.out.print(animationName[i]);
            System.out.print(",");
        }
        System.out.println();

        for (int i = 0; i < animationCount; i++)
            integer.skip(4); // CharacterColour
        for (int i = 0; i < animationCount; i++)
            integer.skip(1); // Unknown
        for (int i = 0; i < animationCount; i++)
            integer.skip(1); // animationHasA
        for (int i = 0; i < animationCount; i++)
            integer.skip(1); // animationHasF
        for (int i = 0; i < animationCount; i++)
            animationIndex[i] = integer.readUnsignedByte(); //animation number

        sceneryCount = integer.readUnsignedShort();
        sceneryWidth = new int[sceneryCount];
        sceneryHeight = new int[sceneryCount];
        Logger.Info("Scenery Names:");
        for (int i = 0; i < sceneryCount; i++) {
            String sceneryName = string.readString(); // Name
            System.out.print(sceneryName);
            System.out.print(",");
        }
        System.out.println();

        for (int i = 0; i < sceneryCount; i++)
            string.readString(); // Examine
        for (int i = 0; i < sceneryCount; i++)
            string.readString(); // Command 1
        for (int i = 0; i < sceneryCount; i++)
            string.readString(); // Command 2
        for (int i = 0; i < sceneryCount; i++)
            string.readString(); // Unknown
        for (int i = 0; i < sceneryCount; i++)
            sceneryWidth[i] = integer.readUnsignedByte();
        for (int i = 0; i < sceneryCount; i++)
            sceneryHeight[i] = integer.readUnsignedByte();
        for (int i = 0; i < sceneryCount; i++)
            integer.skip(1); // Type
        for (int i = 0; i < sceneryCount; i++)
            integer.skip(1); // Elevation

        boundaryCount = integer.readUnsignedShort();
        boundaryAdjacent = new boolean[boundaryCount];
        boundaryPassable = new boolean[boundaryCount];
        System.out.println("Boundary Names:");
        for (int i = 0; i < boundaryCount; i++) {
            String boundaryName = string.readString(); // Name
            System.out.print(boundaryName);
            System.out.print(",");
        }
        System.out.println();

        for (int i = 0; i < boundaryCount; i++)
            string.readString(); // Examine
        for (int i = 0; i < boundaryCount; i++)
            string.readString(); // Command 1
        for (int i = 0; i < boundaryCount; i++)
            string.readString(); // Command 2
        for (int i = 0; i < boundaryCount; i++)
            integer.skip(2); // Unknown
        for (int i = 0; i < boundaryCount; i++)
            integer.skip(4); // Texture 1
        for (int i = 0; i < boundaryCount; i++)
            integer.skip(4); // Texture 2
        for (int i = 0; i < boundaryCount; i++)
            boundaryAdjacent[i] = (integer.readUnsignedByte() != 0); // Adjacent
        for (int i = 0; i < boundaryCount; i++)
            boundaryPassable[i] = (integer.readUnsignedByte() == 0); // Collidable

        int roofCount = integer.readUnsignedShort();
        for (int i = 0; i < roofCount; i++)
            integer.skip(1); // Height
        for (int i = 0; i < roofCount; i++)
            integer.skip(1); // Vertices count

        tileCount = integer.readUnsignedShort();
        tileDecoration = new int[tileCount];
        tileType = new int[tileCount];
        tileAdjacent = new int[tileCount];

        for (int i = 0; i < tileCount; i++)
            tileDecoration[i] = integer.readUnsignedInt();
        for (int i = 0; i < tileCount; i++)
            tileType[i] = integer.readUnsignedByte();
        for (int i = 0; i < tileCount; i++)
            tileAdjacent[i] = integer.readUnsignedByte();

        // Populate tile decoration palette
        terrainDecorationPalette = new int[tileCount];
        terrainDecorationPalette[0] = 0x808080FF; // Gray paths
        terrainDecorationPalette[1] = 0x5483DAFF; // Water
        terrainDecorationPalette[2] = 0xE0651BFF; // Indoors
        terrainDecorationPalette[3] = 0xE0651BFF; // Docks
        terrainDecorationPalette[4] = 0x808080FF; // Castles
        terrainDecorationPalette[5] = 0xE82D31FF; // Red Indoors
        terrainDecorationPalette[6] = 0x366074FF; // Dirty Water
        terrainDecorationPalette[7] = 0x000000FF; // Entrances
        terrainDecorationPalette[8] = 0xC1C1C1FF; // Cliffs
        terrainDecorationPalette[9] = 0x000000FF; // More Entrances
        terrainDecorationPalette[10] = 0xD76417FF; // Lava
        terrainDecorationPalette[11] = 0xD76417FF; // More Bridges
        terrainDecorationPalette[12] = 0x5483DAFF; // Blue Indoors
        terrainDecorationPalette[13] = 0x808080FF; // Even More Entrances
        terrainDecorationPalette[14] = 0x502F16FF; // Brown Indoors
        terrainDecorationPalette[15] = 0x000000FF; // Black Indoors
        terrainDecorationPalette[16] = 0xF2F2F2FF; // White Indoors
        terrainDecorationPalette[17] = 0x000000FF; // Black Underground
        terrainDecorationPalette[18] = 0x000000FF; // More Black Underground
        terrainDecorationPalette[19] = 0x000000FF; // Even Even More Entrances
        terrainDecorationPalette[20] = 0x000000FF; // Unknown, some agility logs
        terrainDecorationPalette[21] = 0x000000FF; // TODO: Find this!
        terrainDecorationPalette[22] = 0x8E460BFF; // Dirt
        terrainDecorationPalette[23] = 0x8E460BFF; // Brown Cliffs
        terrainDecorationPalette[24] = 0x8E460BFF; // Ship Floors

        string.close();
        integer.close();

        // Convert member items
        if (!member) {
            for (int i = 0; i < itemCount; i++) {
                if (itemMembers[i]) {
                    itemName[i] = "Members object";
                    itemExamine[i] = "You need to be a member to use this object";
                    itemCommand[i] = "";
                    itemPrice[i] = 0;
                    itemWearable[i] = 0;
                    itemTradable[i] = false;
                }
            }
        }

        for (int i = 0; i < itemCount; i++)
            System.out.println("id: " + i + ", name: '" + itemName[i] + "', examine: '" + itemExamine[i] + "', command: '" + itemCommand[i] + "', price: '" + itemPrice[i] + "gp', stackable: " + itemStackable[i] + ", wearable: " + itemWearable[i] + ", tradable: " + itemTradable[i] + ", members: " + itemMembers[i]);
        for (int i = 0; i < npcCount; i++) {
            System.out.println("id: " + i + ", name: '" + npcName[i] + "', examine: '" + npcExamine[i] + "', attack: " + npcAttack[i] + ", strength: " + npcStrength[i] + ", hits: " + npcHits[i] + ", defense: " + npcDefense[i] + ", attackable: " + npcAttackable[i] + ", command: '" + npcCommand[i] + "'");
        }
        for (int i = 0; i < animationCount; i++) {
            System.out.println("id: " + i + ", name: '" + animationName[i] + "', index: " + animationIndex[i]);
        }

        int maxRegionWidth = Game.WORLD_WIDTH / Game.REGION_WIDTH;
        int maxRegionHeight = Game.WORLD_HEIGHT / Game.REGION_HEIGHT;
        regionCollisionMask = new byte[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        regionDirection = new byte[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        regionMap = new int[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];

        // Initialize terrain data
        terrainWallNorthSouth = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainWallEastWest = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainWallDiagonal = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainHeight = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainColor = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainDecoration = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];
        terrainDirection = new short[maxRegionWidth][maxRegionHeight][Game.REGION_FLOORS][Game.REGION_SIZE];

        // Read content6 (landscape)
        if (!landscapeContent.open("content6_ffffffffe997514b"))
            return false;
        if (!landscapeContentMembers.open("content7_3fc5d9e3"))
            return false;
        if (!content.open("content4_ffffffffaaca2b0d"))
            return false;
        if (!contentMembers.open("content5_6a1d6b00"))
            return false;
        for (int x = 0; x < maxRegionWidth; x++) {
            for (int y = 0; y < maxRegionHeight; y++) {
                for (int floor = 0; floor < Game.REGION_FLOORS; floor++) {
                    if(!loadLandscape(content, landscapeContent, x, y, floor) && member)
                        loadLandscape(contentMembers, landscapeContentMembers, x, y, floor);
                }
            }
        }
        content.close();
        contentMembers.close();

        return true;
    }

    private static boolean loadLandscape(JContent content, JContent landscape, int x, int y, int floor) {
        // Initialize collisions to collidable
        for (int i = 0; i < Game.REGION_SIZE; i++)
            regionCollisionMask[x][y][floor][i] = Game.COLLISION_TILE;
        regionMap[x][y][floor] = null;

        String mapName = "m" + floor + x / 10 + x % 10 + y / 10 + y % 10;

        JContentFile map = landscape.unpack(mapName + ".hei");

        if (map != null) {
            // Load height
            int prevHeight = 0;
            int index = 0;
            while (index < Game.REGION_SIZE) {
                int data = map.readUnsignedByte();
                if (data < 128) {
                    prevHeight = data;
                    terrainHeight[x][y][floor][index++] = (short) data;
                }

                if (data >= 128) {
                    for (int j = 0; j < data - 128; ++j)
                        terrainHeight[x][y][floor][index++] = (short) prevHeight;
                }
            }

            prevHeight = 64;

            for (int posX = 0; posX < Game.REGION_WIDTH; posX++) {
                for (int posY = 0; posY < Game.REGION_HEIGHT; posY++) {
                    int posIndex = Game.REGION_WIDTH * posY + posX;
                    prevHeight = 127 & terrainHeight[x][y][floor][posIndex] + prevHeight;
                    terrainHeight[x][y][floor][posIndex] = (short) (prevHeight * 2);
                }
            }

            // Load color
            prevHeight = 0;
            index = 0;
            while (index < Game.REGION_SIZE) {
                int data = map.readUnsignedByte();
                if (data < 128) {
                    prevHeight = data;
                    terrainColor[x][y][floor][index++] = (short) data;
                }

                if (data >= 128) {
                    for (int j = 0; j < data - 128; ++j)
                        terrainColor[x][y][floor][index++] = (short) prevHeight;
                }
            }

            prevHeight = 35;

            for (int posX = 0; posX < Game.REGION_WIDTH; posX++) {
                for (int posY = 0; posY < Game.REGION_HEIGHT; posY++) {
                    int posIndex = Game.REGION_WIDTH * posY + posX;
                    prevHeight = 127 & terrainColor[x][y][floor][posIndex] + prevHeight;
                    terrainColor[x][y][floor][posIndex] = (short) (prevHeight * 2);
                }
            }

            map.close();
        }

        map = content.unpack(mapName + ".dat");
        if (map == null)
            return false;

        // Clear collisions
        for (int i = 0; i < Game.REGION_SIZE; i++)
            regionCollisionMask[x][y][floor][i] = Game.COLLISION_NONE;

        // North/South Walls
        for (int i = 0; i < Game.REGION_SIZE; i++) {
            int id = map.readUnsignedByte();
            terrainWallNorthSouth[x][y][floor][i] = (short)id;
            regionCollisionMask[x][y][floor][i] |= (id > 0 && JGameData.boundaryPassable[id - 1] && JGameData.boundaryAdjacent[id - 1]) ? Game.COLLISION_EASTWEST : Game.COLLISION_NONE;
        }
        for (int i = 0; i < Game.REGION_SIZE; i++) {
            int id = map.readUnsignedByte();
            terrainWallEastWest[x][y][floor][i] = (short)id;
            regionCollisionMask[x][y][floor][i] |= (id > 0 && JGameData.boundaryPassable[id - 1] && JGameData.boundaryAdjacent[id - 1]) ? Game.COLLISION_NORTHSOUTH : Game.COLLISION_NONE;
        }

        int data[] = new int[Game.REGION_SIZE];
        for (int i = 0; i < Game.REGION_SIZE; i++)
            data[i] = map.readUnsignedByte();
        for (int i = 0; i < Game.REGION_SIZE; i++) {
            int val = map.readUnsignedByte();
            if (val > 0)
                data[i] = 12000 + val;
            terrainWallDiagonal[x][y][floor][i] = (short)data[i];

            int id = data[i];
            regionCollisionMask[x][y][floor][i] |= (id > 0 && id < 12000 && JGameData.boundaryPassable[id - 1] && JGameData.boundaryAdjacent[id - 1]) ? Game.COLLISION_TILE : Game.COLLISION_NONE;
            regionCollisionMask[x][y][floor][i] |= (id >= 12000 && JGameData.boundaryPassable[id - 12001] && JGameData.boundaryAdjacent[id - 12001]) ? Game.COLLISION_TILE : Game.COLLISION_NONE;
        }

        // Unknown
        int prevValue = 0;
        for (int i = 0; i < Game.REGION_SIZE; i++) {
            int roof = map.readUnsignedByte();
            if (roof >= 128) {
                for (int i2 = 0; i2 < roof - 128; i2++)
                    i++;
                i--;
            } else {
                prevValue = roof;
            }
        }

        prevValue = 0;
        int index = 0;
        while (index < Game.REGION_SIZE) {
            int tileDecoration = map.readUnsignedByte();
            if (tileDecoration >= 128) {
                for (int i2 = 0; i2 < tileDecoration - 128; ++i2) {
                    terrainDecoration[x][y][floor][index++] = (short)prevValue;
                }
            } else {
                terrainDecoration[x][y][floor][index++] = (short)tileDecoration;
                prevValue = tileDecoration;
            }
        }

        index = 0;
        while (index < Game.REGION_SIZE) {
            int tileDirection = map.readUnsignedByte();
            if (tileDirection >= 128) {
                for (int i2 = 0; i2 < tileDirection - 128; ++i2)
                    terrainDirection[x][y][floor][index++] = 0;
            } else {
                terrainDirection[x][y][floor][index++] = (short)tileDirection;
            }
        }

        map.close();

        // Create map tile
        regionMap[x][y][floor] = new int[Game.REGION_SIZE];
        for (int i = 0; i < Game.REGION_SIZE; i++) {
            regionMap[x][y][floor][i] = 0xFFFFFFFF;
        }

        return true;
    }
}
