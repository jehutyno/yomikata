package com.jehutyno.yomikata.util


/**
 * Used to parse English/French translation strings.
 * e.g. In order to remove info such as (v5t) that can give away the answer, or to remove
 * any japanese words that may be referred to in the translation.
 */


/** single character used to identify which ending a godan (五段) verb has.
 * e.g. yomu (読む) is 'm' */
private const val godanEndings = "utrnbmkgs"
/** all possible strings that appear in parts of speech (pop) info */
private val possiblePopTokens = arrayOf(
    "n", "n-suf", "n-adv",                  // types of noun
    "pn",                                   // pronoun
    "vs", "vi", "vt",                       // types of verb
    "adj-na", "adj-no", "adj-i", "adj-t",   // types of adjective
    "adv", "adv-to", "n-adv", "n-t",        // adverb, adverb taking と (to) particle
    "pref", "suf",                          // prefix, suffix
    "exp",                                  // expression
    "num", "ctr",                           // number, counter
    "pol", "hum",                           // polite speech, humble speech
    "abbr",                                 // abbreviation
    "int",                                  // ???
    "v1", "vz",                             // ichidan verb, zuru verb (ichidan ending in ずる (zuru))
    "aux-v", "aux-adj" ,                    // auxiliary verb, auxiliary adjective
    *godanEndings.map{ "v5$it" }.toTypedArray()    // godan verbs
)

private val concatenatedPopTokens = possiblePopTokens.joinToString("|")

/** finds the pop tokens in parentheses (), or comma separated, includes trailing whitespaces
 *  e.g. it will match (n)   or    (v1,vt)    or    (n,n-suf,adj-na)  */
private val regexPopTokens = Regex("""\(($concatenatedPopTokens)(,($concatenatedPopTokens))*\)\s*""")

/**
 * Remove parts of speech info
 *
 * Parts of speech specifies what 'type' a word is, such as v for verb or n for noun.
 *
 * @return New string with the parts of speech removed
 */
fun String.removePartsOfSpeechInfo(): String {
    return regexPopTokens.replace(this, "")   // remove all matched substrings
}


/**
 * Remove any japanese
 *
 * Removes hiragana, katakana, and kanji from a string by removing any non-latin
 * characters. Also removes any other characters within the same parentheses () as the
 * removed japanese text. e.g. `(hello 先輩)` will be remove completely since the hello
 * is also in the brackets.
 * Also removes any trailing whitespaces.
 *
 * WARNING: This will also remove other special characters that are not part of the standard
 * latin character set.
 *
 * @return New string with any non-latin characters removed
 */
fun String.removeAnyJapanese(): String {
    /** Finds any text inside of parentheses () that also contains non-latin characters, and
     * any trailing whitespaces */
    val regex = Regex("""\([^()]*\P{InBasicLatin}[^()]*\)\s*""")
    val newString = regex.replace(this, "")

    // also remove any stray japanese that is not in brackets
    return Regex("""\P{InBasicLatin}""").replace(newString, "")
}

/** Regex for indexes which mark different translations / meaning for words that have many */
private val indexRegex = Regex("""\(\d+\)""")

/**
 * Remove synonyms
 *
 * Simplifies large translations by removing synonyms / meanings past some point.
 * Tries to find numbers (1), (2), etc., and then removes any part of the string
 * past (maxLevel) (maxLevel itself is not removed).
 *
 * @param maxLevel Maximum value to still allow in the string
 * @return String with any translations marked by an integer (i) with i > maxLevel removed
 */
fun String.removeSynonyms(maxLevel: Int = 2): String {
    val matches = indexRegex.findAll(this)
    var smallestNumberBiggerThanMaxLevel: Pair<Int, MatchResult?> = Pair(Int.MAX_VALUE, null)
    matches.forEach { match ->
        val number = match.value.removePrefix("(").removeSuffix(")").toInt()
        if (number > maxLevel && number < smallestNumberBiggerThanMaxLevel.first) {
            smallestNumberBiggerThanMaxLevel = Pair(number, match)
        }
    }
    val match = smallestNumberBiggerThanMaxLevel.second ?: return this

    return this.removeRange(match.range.first, this.length)
}


/**
 * Ensure single space
 *
 * e.g. "Say Hello    World  " -> "Say Hello World ".
 *
 * Does not apply to tabs, newlines, etc.
 *
 * @return A new string where any consecutive spaces are replaced with exactly one space
 */
private fun String.ensureSingleSpace(): String {
    return Regex(" +").replace(this, " ")
}


/**
 * Clean for QCM
 *
 * Used to display translations in a simpler format which avoids giving away the answer
 * by hiding information that may be present in the full translation.
 *
 * @return A new string with Parts Of Speech info remove, and any Japanese removed.
 */
fun String.cleanForQCM(): String {
    return this.removePartsOfSpeechInfo().removeAnyJapanese().trim().ensureSingleSpace()
}


/**
 * Readable translation format
 *
 * Turns semicolons into commas with spaces.
 * Adds newline characters if many synonyms / meanings are present.
 *
 * @return A new string which is easier to read
 */
fun String.readableTranslationFormat(): String {
    // find all semicolons, including following index (i), and potentially pps info
    val regex = Regex(""";+\s*(?<popIndexGroup>($regexPopTokens)?($indexRegex)?)""")

    fun getReplacement(matchResult: MatchResult): String {
        // if the semicolon is followed by a `(i)` (with i a number), then turn it into a newline
        // otherwise make it a `, `
        return matchResult.groups["popIndexGroup"].let {
            // also check that it.value contains ")" to avoid unwanted match with only \\s* part
            if (it != null && it.value.contains(")"))
                "\n${it.value}"        // make sure to keep the popIndexGroup
            else
                ", "
        }
    }

    return regex.replace(this, ::getReplacement).ensureSingleSpace()
}
