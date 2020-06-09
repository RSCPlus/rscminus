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

/**
 * Credit to Isaac Exemplar for the majority of this files' refactoring
 */

package rscminus.common;

public class ChatFilter {
    /**
     * Censor data loaded from the filter file
     */
    private static int[] hashFragments;
    private static char[][] badList;
    private static byte[][][] badCharIds;
    private static char[][] hostList;
    private static byte[][][] hostCharIds;
    private static char[][] tldList;
    private static int[] tldType;

    /**
     * Words that should circumvent the censor
     */
    private static final String[] ignoreList = {
            "cook", "cook's", "cooks", "seeks", "sheet"
    };

    /**
     * Letters that appear directly after these chars are forced to be capitalized
     */
    private static final char[] forceUpperChars = new char[]{'.', '!', '?'} ;

    /**
     * Used for the URL filter
     */
    private static final char[] dot = { 'd', 'o', 't' };
    private static final char[] slash = {'s', 'l', 'a', 's', 'h' };

    /**
     * Dictates if the censor should operate. Even if it is off,
     * capitalization will still be checked
     */
    private static boolean censorOn = false;

    /**
     * Controls the censor flag
     */
    public static void enable() { censorOn = true; }
    public static void disable() { censorOn = false; }

    /**
     * Loads the censor list from file
     * @return boolean depending on success of loading
     */
    public static boolean init() {
        JContent filterFile = new JContent();
        if (!filterFile.open("filter2.jag"))
            return false;
        JContentFile buffFragments = filterFile.unpack("fragmentsenc.txt");
        JContentFile buffBadEnc = filterFile.unpack("badenc.txt");
        JContentFile buffHostEnc = filterFile.unpack("hostenc.txt");
        JContentFile buffTLDList = filterFile.unpack("tldlist.txt");

        if (buffFragments == null ||
            buffBadEnc == null ||
            buffHostEnc == null ||
            buffTLDList == null)
            return false;

        loadFilters(buffFragments, buffBadEnc, buffHostEnc, buffTLDList);
        enable();
        return true;
    }

    /**
     * Main filter function. Applies censoring, if enabled, and capitalization formatting
     * @param input message typed by the user
     * @return string with filtered message
     */
    public static String filter(String input) {
        char[] inputChars = input.toLowerCase().toCharArray();
        if (censorOn) {
            applyDotSlashFilter(inputChars);
            applyBadwordFilter(inputChars);
            applyHostFilter(inputChars);
            applyDigitFilter(inputChars);
            for (int ignoreIdx = 0; ignoreIdx < ignoreList.length; ignoreIdx++) {
                for (int inputIgnoreIdx = -1; (inputIgnoreIdx = input.indexOf(ignoreList[ignoreIdx], inputIgnoreIdx + 1)) != -1; ) {
                    char[] ignorewordChars = ignoreList[ignoreIdx].toCharArray();
                    for (int ignorewordIdx = 0; ignorewordIdx < ignorewordChars.length; ignorewordIdx++)
                        inputChars[ignorewordIdx + inputIgnoreIdx] = ignorewordChars[ignorewordIdx];

                }
            }
        }
        stripLowercase(input.toCharArray(), inputChars);
        toLowercase(inputChars);

        return new String(inputChars);
    }

    /**
     * Populates class arrays from file
     */
    private static void loadFilters(JContentFile fragments, JContentFile bad, JContentFile host, JContentFile tld) {
        loadBad(bad);
        loadHost(host);
        loadFragments(fragments);
        loadTld(tld);
    }

    /**
     * Populates tldList, tldType
     * Top level domains (com, org, gov etc)
     */
    private static void loadTld(JContentFile buffer) {
        int wordcount = buffer.readUnsignedInt();
        tldList = new char[wordcount][];
        tldType = new int[wordcount];
        for (int idx = 0; idx < wordcount; idx++) {
            tldType[idx] = buffer.readUnsignedByte();
            char[] ac = new char[buffer.readUnsignedByte()];
            for (int k = 0; k < ac.length; k++)
                ac[k] = (char) buffer.readUnsignedByte();

            tldList[idx] = ac;
        }

    }

    /**
     * Populates badList, badCharIds
     * Curse words
     */
    private static void loadBad(JContentFile buffer) {
        int wordcount = buffer.readUnsignedInt();
        badList = new char[wordcount][];
        badCharIds = new byte[wordcount][][];
        readJContentFile(buffer, badList, badCharIds);
    }

    /**
     * Populates hostList, hostCharIds
     * Host names
     */
    private static void loadHost(JContentFile buffer) {
        int wordcount = buffer.readUnsignedInt();
        hostList = new char[wordcount][];
        hostCharIds = new byte[wordcount][][];
        readJContentFile(buffer, hostList, hostCharIds);
    }

    /**
     * Populates hashFragments
     */
    private static void loadFragments(JContentFile buffer) {
        hashFragments = new int[buffer.readUnsignedInt()];
        for (int i = 0; i < hashFragments.length; i++) {
            hashFragments[i] = buffer.readUnsignedShort();
        }
    }

    /**
     * Parses the sub-files within the filter file
     */
    private static void readJContentFile(JContentFile buffer, char[][] wordList, byte[][][] charIds) {
        for (int i = 0; i < wordList.length; i++) {
            char[] currentWord = new char[buffer.readUnsignedByte()];
            for (int j = 0; j < currentWord.length; j++)
                currentWord[j] = (char) buffer.readUnsignedByte();

            wordList[i] = currentWord;
            byte[][] ids = new byte[buffer.readUnsignedInt()][2];
            for (int j = 0; j < ids.length; j++) {
                ids[j][0] = (byte) buffer.readUnsignedByte();
                ids[j][1] = (byte) buffer.readUnsignedByte();
            }

            if (ids.length > 0)
                charIds[i] = ids;
        }
    }

    /**
     * Sets all output chars to uppercase if they are uppercase in the input string
     * @param input original message
     * @param output filtered message
     */
    private static void stripLowercase(char[] input, char[] output) {
        for (int i = 0; i < input.length; i++)
            if (output[i] != '*' && isUppercase(input[i]))
                output[i] = input[i];
    }

    /**
     * Applies the capitalization filter
     * @param input message to censor
     */
    private static void toLowercase(char[] input) {
        boolean force = true;
        boolean optional = false;
        for (int i = 0; i < input.length; i++) {
            char current = input[i];
            if (isColorCodeFormat(input, i)) {
                i += 4;
                continue;
            }
            if (current == ' ') {
                optional = true;
                continue;
            }
            if (force) {
                force = false;
                optional = false;
                if (isLowercase(current))
                    input[i] -= 32;
            } else if (optional) {
                optional = false;
            } else if(isUppercase(current)) {
                input[i] += 32;
            }

            if (!isLetter(current)){
                optional = true;
                for (char forceChar : forceUpperChars) {
                    if (forceChar == current) {
                        force = true;
                        optional = false;
                        break;
                    }
                }
            }
        }

    }

    /**
     * Removes profanity from a message. The censored word list is
     * loaded in ChatFilter.init()
     * @param input message to censor
     */
    private static void applyBadwordFilter(char[] input) {
        for (int i = 0; i < 2; i++) {
            for (int j = badList.length - 1; j >= 0; j--)
                applyWordFilter(input, badList[j], badCharIds[j]);

        }
    }

    /**
     * Removes specific host names from a message. The URL list is
     * loaded in ChatFilter.init()
     * @param input message to censor
     */
    private static void applyHostFilter(char[] input) {
        for (int i = hostList.length - 1; i >= 0; i--)
            applyWordFilter(input, hostList[i], hostCharIds[i]);
    }

    /**
     * Removes specific top level domains from a message. The TLD list is
     * loaded in ChatFilter.init()
     * @param input Message to search
     * @param input1 input after ran through the dot filter
     * @param input2 input after ran through the slash filter
     * @param tld Top level domain
     * @param type tld type as specified in the filter file
     */
    private static void applyTldFilter(char[] input, char[] input1, char[] input2, char[] tld, int type) {
        if (tld.length > input.length)
            return;
        for (int charIndex = 0; charIndex <= input.length - tld.length; charIndex++) {
            int inputCharCount = charIndex;
            int l = 0;
            while (inputCharCount < input.length) {
                int i1 = 0;
                char current = input[inputCharCount];
                char next = '\0';
                if (inputCharCount + 1 < input.length)
                    next = input[inputCharCount + 1];
                if (l < tld.length && (i1 = compareLettersNumbers(tld[l], current, next)) > 0) {
                    inputCharCount += i1;
                    l++;
                    continue;
                }
                if (l == 0)
                    break;
                if ((i1 = compareLettersNumbers(tld[l - 1], current, next)) > 0) {
                    inputCharCount += i1;
                    continue;
                }
                if (l >= tld.length || !isSpecial(current))
                    break;
                inputCharCount++;
            }
            if (l >= tld.length) {
                boolean flag = false;
                int dotUsage = getDotUsage(input, input1, charIndex);
                int slashUsage = getSlashUsage(input, input2, inputCharCount - 1);
                if (type == 1 && dotUsage > 0 && slashUsage > 0)
                    flag = true;
                if (type == 2 && (dotUsage > 2 && slashUsage > 0 || dotUsage > 0 && slashUsage > 2))
                    flag = true;
                if (type == 3 && dotUsage > 0 && slashUsage > 2)
                    flag = true;
                if (flag) {
                    int l1 = charIndex;
                    int i2 = inputCharCount - 1;
                    if (dotUsage > 2) {
                        if (dotUsage == 4) {
                            boolean flag1 = false;
                            for (int k2 = l1 - 1; k2 >= 0; k2--)
                                if (flag1) {
                                    if (input1[k2] != '*')
                                        break;
                                    l1 = k2;
                                } else if (input1[k2] == '*') {
                                    l1 = k2;
                                    flag1 = true;
                                }

                        }
                        boolean flag2 = false;
                        for (int l2 = l1 - 1; l2 >= 0; l2--)
                            if (flag2) {
                                if (isSpecial(input[l2]))
                                    break;
                                l1 = l2;
                            } else if (!isSpecial(input[l2])) {
                                flag2 = true;
                                l1 = l2;
                            }

                    }
                    if (slashUsage > 2) {
                        if (slashUsage == 4) {
                            boolean flag3 = false;
                            for (int i3 = i2 + 1; i3 < input.length; i3++)
                                if (flag3) {
                                    if (input2[i3] != '*')
                                        break;
                                    i2 = i3;
                                } else if (input2[i3] == '*') {
                                    i2 = i3;
                                    flag3 = true;
                                }

                        }
                        boolean flag4 = false;
                        for (int j3 = i2 + 1; j3 < input.length; j3++)
                            if (flag4) {
                                if (isSpecial(input[j3]))
                                    break;
                                i2 = j3;
                            } else if (!isSpecial(input[j3])) {
                                flag4 = true;
                                i2 = j3;
                            }

                    }
                    for (int j2 = l1; j2 <= i2; j2++)
                        input[j2] = '*';

                }
            }
        }
    }

    /**
     * Censors "dot" & "slash" , which are typically attempts to get around
     * URL filtering. Also censors top level domains
     * @param input message to censor
     */
    private static void applyDotSlashFilter(char[] input) {
        char[] input1 = input.clone();
        applyWordFilter(input1, dot, null);
        char[] input2 = input.clone();
        applyWordFilter(input2, slash, null);
        for (int i = 0; i < tldList.length; i++)
            applyTldFilter(input, input1, input2, tldList[i], tldType[i]);

    }

    /**
     * Determines if a dot is at the given position
     * @param input raw message to search
     * @param input1 input after ran through the dot filter
     * @param position position in message to search
     * @return 0: no, 1: special char, 2: end of message,
     *         3: yes, 4: yes (censored)
     */
    private static int getDotUsage(char[] input, char[] input1, int position) {
        if (position == 0)
            return 2;
        for (int j = position - 1; j >= 0; j--) {
            if (!isSpecial(input[j]))
                break;
            if (input[j] == ',' || input[j] == '.')
                return 3;
        }

        int filtered = 0;
        for (int l = position - 1; l >= 0; l--) {
            if (!isSpecial(input1[l]))
                break;
            if (input1[l] == '*')
                filtered++;
        }

        if (filtered >= 3)
            return 4;
        return isSpecial(input[position - 1]) ? 1 : 0;
    }

    /**
     * Determines if a slash is at the given position
     * @param input raw message to search
     * @param input1 input after ran through slash filter
     * @param len position to search
     * @return 0: no, 1: special char, 2: end of message,
     *         3: yes, 4: yes (censored)
     */
    private static int getSlashUsage(char[] input, char[] input1, int len) {
        if (len + 1 == input.length)
            return 2;
        for (int j = len + 1; j < input.length; j++) {
            if (!isSpecial(input[j]))
                break;
            if (input[j] == '\\' || input[j] == '/')
                return 3;
        }

        int filtered = 0;
        for (int l = len + 1; l < input.length; l++) {
            if (!isSpecial(input1[l]))
                break;
            if (input1[l] == '*')
                filtered++;
        }

        if (filtered >= 5)
            return 4;
        return isSpecial(input[len + 1]) ? 1 : 0;
    }

    /**
     * Censors a message using the supplied filter
     * @param input message to search
     * @param wordlist list of unacceptable words
     * @param charIds a group of characters that can disable the filter,
     *                loaded with the word list
     */
    private static void applyWordFilter(char[] input, char[] wordlist, byte[][] charIds) {
        if (wordlist.length > input.length)
            return;
        for (int charIndex = 0; charIndex <= input.length - wordlist.length; charIndex++) {
            int inputCharCount = charIndex;
            int k = 0;
            boolean specialChar = false;
            while (inputCharCount < input.length) {
                int l = 0;
                char inputChar = input[inputCharCount];
                char nextChar = '\0';
                if (inputCharCount + 1 < input.length)
                    nextChar = input[inputCharCount + 1];
                if (k < wordlist.length && (l = compareLettersSymbols(wordlist[k], inputChar, nextChar)) > 0) {
                    inputCharCount += l;
                    k++;
                    continue;
                }
                if (k == 0)
                    break;
                if ((l = compareLettersSymbols(wordlist[k - 1], inputChar, nextChar)) > 0) {
                    inputCharCount += l;
                    continue;
                }
                if (k >= wordlist.length || !isNotLowercase(inputChar))
                    break;
                if (isSpecial(inputChar) && inputChar != '\'')
                    specialChar = true;
                inputCharCount++;
            }
            if (k >= wordlist.length) {
                boolean filter = true;
                if (!specialChar) {
                    char prevChar = ' ';
                    if (charIndex - 1 >= 0)
                        prevChar = input[charIndex - 1];
                    char curChar = ' ';
                    if (inputCharCount < input.length)
                        curChar = input[inputCharCount];
                    byte prevId = getCharId(prevChar);
                    byte curId = getCharId(curChar);
                    if (charIds != null && compareCharIds(charIds, prevId, curId))
                        filter = false;
                } else {
                    boolean flag2 = false;
                    boolean flag3 = false;
                    if (charIndex - 1 < 0 || isSpecial(input[charIndex - 1]) && input[charIndex - 1] != '\'')
                        flag2 = true;
                    if (inputCharCount >= input.length || isSpecial(input[inputCharCount]) && input[inputCharCount] != '\'')
                        flag3 = true;
                    if (!flag2 || !flag3) {
                        boolean flag4 = false;
                        int j1 = charIndex - 2;
                        if (flag2)
                            j1 = charIndex;
                        for (; !flag4 && j1 < inputCharCount; j1++)
                            if (j1 >= 0 && (!isSpecial(input[j1]) || input[j1] == '\'')) {
                                char[] ac2 = new char[3];
                                int k1;
                                for (k1 = 0; k1 < 3; k1++) {
                                    if (j1 + k1 >= input.length || isSpecial(input[j1 + k1]) && input[j1 + k1] != '\'')
                                        break;
                                    ac2[k1] = input[j1 + k1];
                                }

                                boolean flag5 = true;
                                if (k1 == 0)
                                    flag5 = false;
                                if (k1 < 3 && j1 - 1 >= 0 && (!isSpecial(input[j1 - 1]) || input[j1 - 1] == '\''))
                                    flag5 = false;
                                if (flag5 && !containsFragmentHashes(ac2))
                                    flag4 = true;
                            }

                        if (!flag4)
                            filter = false;
                    }
                }
                if (filter) {
                    for (int i1 = charIndex; i1 < inputCharCount; i1++)
                        input[i1] = '*';

                }
            }
        }

    }

    /**
     * Compares two consecutive chars in a message
     * @param charIdData character values specific to an unacceptable word
     * @param prevCharId first char
     * @param curCharId second char
     * @return True/false
     */
    private static boolean compareCharIds(byte[][] charIdData, byte prevCharId, byte curCharId) {
        int first = 0;
        if (charIdData[first][0] == prevCharId && charIdData[first][1] == curCharId)
            return true;
        int last = charIdData.length - 1;
        if (charIdData[last][0] == prevCharId && charIdData[last][1] == curCharId)
            return true;
        while (first != last && first + 1 != last) {
            int middle = (first + last) / 2;
            if (charIdData[middle][0] == prevCharId && charIdData[middle][1] == curCharId)
                return true;
            if (prevCharId < charIdData[middle][0] || prevCharId == charIdData[middle][0] && curCharId < charIdData[middle][1])
                last = middle;
            else
                first = middle;
        }
        return false;
    }

    /**
     * Compares letters with symbols that are commonly used in their place
     * @param filterChar char to filter
     * @param currentChar char that mimics filterChar
     * @param nextChar secondary char, used only for ph->f
     * @return 0 for no match, 1 for match, 2 for ph->f
     */
    private static int compareLettersNumbers(char filterChar, char currentChar, char nextChar) {
        if (filterChar == currentChar)
            return 1;
        if (filterChar == 'e' && currentChar == '3')
            return 1;
        if (filterChar == 't' && (currentChar == '7' || currentChar == '+'))
            return 1;
        if (filterChar == 'a' && (currentChar == '4' || currentChar == '@'))
            return 1;
        if (filterChar == 'o' && currentChar == '0')
            return 1;
        if (filterChar == 'i' && currentChar == '1')
            return 1;
        if (filterChar == 's' && currentChar == '5')
            return 1;
        if (filterChar == 'f' && currentChar == 'p' && nextChar == 'h')
            return 2;
        return filterChar == 'g' && currentChar == '9' ? 1 : 0;
    }

    /**
     * Compares letters with symbols that are commonly used in their place
     * @param filterChar  character to compare against
     * @param currentChar current character
     * @param nextChar    next character
     * @return 0 for no match, 1 for currentChar matches, 2 for both currentChar and nextChar matching
     */
    private static int compareLettersSymbols(char filterChar, char currentChar, char nextChar) {
        if (filterChar == '*')
            return 0;
        if (filterChar == currentChar)
            return 1;
        if (filterChar >= 'a' && filterChar <= 'z') {
            if (filterChar == 'e')
                return currentChar == '3' ? 1 : 0;
            if (filterChar == 't')
                return currentChar == '7' ? 1 : 0;
            if (filterChar == 'a')
                return currentChar == '4' || currentChar == '@' ? 1 : 0;
            if (filterChar == 'o') {
                if (currentChar == '0' || currentChar == '*')
                    return 1;
                return currentChar == '(' && nextChar == ')' ? 2 : 0;
            }
            if (filterChar == 'i')
                return currentChar == 'y' || currentChar == 'l' || currentChar == 'j' || currentChar == 'l' || currentChar == '!' || currentChar == ':' || currentChar == ';' ? 1 : 0;
            if (filterChar == 'n')
                return 0;
            if (filterChar == 's')
                return currentChar == '5' || currentChar == 'z' || currentChar == '$' ? 1 : 0;
            if (filterChar == 'r')
                return 0;
            if (filterChar == 'h')
                return 0;
            if (filterChar == 'l')
                return currentChar == '1' ? 1 : 0;
            if (filterChar == 'd')
                return 0;
            if (filterChar == 'c')
                return currentChar == '(' ? 1 : 0;
            if (filterChar == 'u')
                return currentChar == 'v' ? 1 : 0;
            if (filterChar == 'm')
                return 0;
            if (filterChar == 'f')
                return currentChar == 'p' && nextChar == 'h' ? 2 : 0;
            if (filterChar == 'p')
                return 0;
            if (filterChar == 'g')
                return currentChar == '9' || currentChar == '6' ? 1 : 0;
            if (filterChar == 'w')
                return currentChar == 'v' && nextChar == 'v' ? 2 : 0;
            if (filterChar == 'y')
                return 0;
            if (filterChar == 'b')
                return currentChar == '1' && nextChar == '3' ? 2 : 0;
            if (filterChar == 'v')
                return 0;
            if (filterChar == 'k')
                return 0;
            if (filterChar == 'x')
                return currentChar == ')' && nextChar == '(' ? 2 : 0;
            if (filterChar == 'j')
                return 0;
            if (filterChar == 'q')
                return 0;
            if (filterChar == 'z')
                return 0;
        }
        if (filterChar >= '0' && filterChar <= '9') {
            if (filterChar == '0') {
                if (currentChar == 'o' || currentChar == 'O')
                    return 1;
                return currentChar == '(' && nextChar == ')' ? 2 : 0;
            }
            if (filterChar == '1')
                return currentChar != 'l' ? 0 : 1;
            if (filterChar == '2')
                return 0;
            if (filterChar == '3')
                return 0;
            if (filterChar == '4')
                return 0;
            if (filterChar == '5')
                return 0;
            if (filterChar == '6')
                return 0;
            if (filterChar == '7')
                return 0;
            if (filterChar == '8')
                return 0;
            if (filterChar == '9')
                return 0;
        }
        if (filterChar == '-')
            return 0;
        if (filterChar == ',')
            return currentChar == '.' ? 1 : 0;
        if (filterChar == '.')
            return currentChar == ',' ? 1 : 0;
        if (filterChar == '(')
            return 0;
        if (filterChar == ')')
            return 0;
        if (filterChar == '!')
            return currentChar == 'i' ? 1 : 0;
        if (filterChar == '\'')
            return 0;
        return 0;
    }

    /**
     * Returns the id for the given char, ranging from {@code 1} to {@code 38}.
     * <p>
     * <pre>
     * id     range
     * 1-26   a-z
     * 27     unknown
     * 28     apostrophe
     * 29-38  0-9
     * </pre>
     *
     * @param c char to check
     * @return id for char {@code c}
     */
    private static byte getCharId(char c) {
        if (c >= 'a' && c <= 'z')
            return (byte) (c - 97 + 1);
        if (c == '\'')
            return 28;
        if (c >= '0' && c <= '9')
            return (byte) (c - 48 + 29);
        else
            return 27;
    }

    /**
     * Censors groups of 4 digits separated by special characters
     * EX: 19.5 43.75 14.5
     *     ********** 14.5
     * @param input message to filter
     */
    private static void applyDigitFilter(char[] input) {
        int digitIndex = 0;
        int fromIndex = 0;
        int k = 0;
        int l = 0;
        while ((digitIndex = indexOfDigit(input, fromIndex)) != -1) {
            boolean flag = false;
            for (int i = fromIndex; i >= 0 && i < digitIndex && !flag; i++)
                if (!isSpecial(input[i]) && !isNotLowercase(input[i]))
                    flag = true;

            if (flag)
                k = 0;
            if (k == 0)
                l = digitIndex;
            fromIndex = indexOfNonDigit(input, digitIndex);
            int j1 = 0;
            for (int k1 = digitIndex; k1 < fromIndex; k1++)
                j1 = (j1 * 10 + input[k1]) - 48;

            if (j1 > 255 || fromIndex - digitIndex > 8)
                k = 0;
            else
                k++;
            if (k == 4) {
                for (int i = l; i < fromIndex; i++)
                    input[i] = '*';
                k = 0;
            }
        }
    }

    /**
     * Finds the index of the first digit in char array
     * @param input Char array to search
     * @param offset Index to begin searching from
     * @return int index. = -1 if not found
     */
    private static int indexOfDigit(char[] input, int offset) {
        for (int i = offset; i < input.length && i >= 0; i++)
            if (input[i] >= '0' && input[i] <= '9')
                return i;

        return -1;
    }

    /**
     * Finds the index of the first non-digit in a char array
     * @param input Char array to search
     * @param offset Index to begin searching from
     * @return int index. = input.length if not found
     */
    private static int indexOfNonDigit(char[] input, int offset) {
        for (int i = offset; i < input.length && i >= 0; i++)
            if (input[i] < '0' || input[i] > '9')
                return i;

        return input.length;
    }

    /**
     * Determines if a char is not a digit or English letter
     * @param c char to check
     * @return True/false
     */
    private static boolean isSpecial(char c) {
        return !isLetter(c) && !isDigit(c);
    }

    /**
     * Determines if a char is NOT a lowercase English letter
     * v,x,j,q,z are considered not lowercase as well
     * @param c char to check
     * @return True/false
     */
    private static boolean isNotLowercase(char c) {
        if (c < 'a' || c > 'z')
            return true;
        return c == 'v' || c == 'x' || c == 'j' || c == 'q' || c == 'z';
    }

    /**
     * Determines if a char is an English letter
     * @param c char to check
     * @return True/false
     */
    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    /**
     * Determines if a char is a digit
     * @param c char to check
     * @return True/false
     */
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Determines if a char is a lowercase English letter
     * @param c char to check
     * @return True/false
     */
    private static boolean isLowercase(char c) {
        return c >= 'a' && c <= 'z';
    }

    /**
     * Determines if a char is a capital English letter
     * @param c char to check
     * @return True/false
     */
    private static boolean isUppercase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    /**
     * Searches a message for message fragments (hashed)
     * List of fragments is loaded in ChatFilter.init()
     * @param input message to search
     * @return True/false
     */
    private static boolean containsFragmentHashes(char[] input) {
        boolean notNum = true;
        for (int i = 0; i < input.length; i++)
            if (!isDigit(input[i]) && input[i] != 0)
                notNum = false;

        if (notNum)
            return true;
        int inputHash = word2hash(input);
        int first = 0;
        int last = hashFragments.length - 1;
        if (inputHash == hashFragments[first] || inputHash == hashFragments[last])
            return true;
        while (first != last && first + 1 != last) {
            int middle = (first + last) / 2;
            if (inputHash == hashFragments[middle])
                return true;
            if (inputHash < hashFragments[middle])
                last = middle;
            else
                first = middle;
        }
        return false;
    }

    /** Converts a word to its hash value
     * @param word word to hash
     * @return int hash
     */
    private static int word2hash(char[] word) {
        if (word.length > 6)
            return 0;
        int hash = 0;
        for (int i = 0; i < word.length; i++) {
            char c = word[word.length - i - 1];
            if (c >= 'a' && c <= 'z')
                hash = hash * 38 + c - 97 + 1;
            else if (c == '\'')
                hash = hash * 38 + 27;
            else if (c >= '0' && c <= '9')
                hash = hash * 38 + c - 48 + 28;
            else if (c != 0) {
                return 0;
            }
        }

        return hash;
    }

    /**
     * Determines if there is a color code at the current position
     * @param message Message to search
     * @param pos Position of the message
     * @return True/false
     */
    private static boolean isColorCodeFormat(char[] message, int pos) {
        if (pos + 4 >= message.length)
            return false;

        if (message[pos] == '@') {
            return message[pos + 4] == '@';
        }
        return false;
    }
}
