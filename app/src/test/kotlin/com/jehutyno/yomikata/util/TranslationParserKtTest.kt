package com.jehutyno.yomikata.util

import org.junit.Test


class TranslationParserKtTest {

    @Test
    fun removePartsOfSpeechInfo() {
        // unaffected strings
        assert ( "".removePartsOfSpeechInfo() == "" )
        assert ( "fig (fruit/tree)".removePartsOfSpeechInfo() == "fig (fruit/tree)" )
        assert ( "metal; Friday".removePartsOfSpeechInfo() == "metal; Friday" )
        assert ( "9".removePartsOfSpeechInfo() == "9" )
        assert ( "ON".removePartsOfSpeechInfo() == "ON" )
        // simple pop at beginning
        assert (
            "(n) state;condition;circumstances;the way things are or should be;truth;(P)"
                .removePartsOfSpeechInfo()
            == "state;condition;circumstances;the way things are or should be;truth;(P)"
        )
        // complex pop at beginning
        assert (
            "(v1,vt) to lay in stock;to replenish stock;to procure;to learn something that might be useful later;(P)"
                .removePartsOfSpeechInfo()
            == "to lay in stock;to replenish stock;to procure;to learn something that might be useful later;(P)"
        )
        assert (
            "(adj-na,n) (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)"
                .removePartsOfSpeechInfo()
            == "(1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)"
        )
        // pop at different places in string
        assert (
            "(n,n-suf,pref) (1) super-;ultra-;hyper-;very;really;(n,n-suf) (2) over (after a number or counter);more than;(P)"
                .removePartsOfSpeechInfo()
            == "(1) super-;ultra-;hyper-;very;really;(2) over (after a number or counter);more than;(P)"
        )
        // similar to pop, but should not be remove
        assert ("(n parts)".removePartsOfSpeechInfo() == "(n parts)")
        assert ("(adverb)".removePartsOfSpeechInfo() == "(adverb)")
    }

    @Test
    fun removeAnyJapanese() {
        // unaffected string
        assert ( "".removeAnyJapanese() == "" )
        assert ( "hello world".removeAnyJapanese() == "hello world" )
        assert ( "141;:#@&|^[](){}".removeAnyJapanese() == "141;:#@&|^[](){}" )
        // stand-alone japanese
        assert (
            "寿司 sushi".removeAnyJapanese() == " sushi"
        )
        // japanese in parentheses
        assert (
            "(晩御飯 シ) this is a test".removeAnyJapanese() == "this is a test"
        )
        // japanese in parentheses, together with other text
        assert (
            "(n,adj-no) (1) physiology;(2) (See 月経) menstruation;one's period;menses;(P)"
                .removeAnyJapanese()
            == "(n,adj-no) (1) physiology;(2) menstruation;one's period;menses;(P)"
        )
        // non-japanese in parentheses -> should not be removed
        assert (
            "(testing 1234) finish".removeAnyJapanese() == "(testing 1234) finish"
        )
    }

    @Test
    fun removeSynonyms() {
        val fullTestInts = -1..20
        fun String.unchangedTest() {
            this.let {
                for (fullTestInt in fullTestInts) {
                    assert ( it.removeSynonyms(fullTestInt) == it )
                }
            }
        }
        // unaffected strings
        "".unchangedTest()
        "fig (fruit/tree)".unchangedTest()
        "metal; Friday".unchangedTest()
        "9".unchangedTest()
        "ON".unchangedTest()
        "(9v)".unchangedTest()
        "(v5s)".unchangedTest()
        // simple remove one
        assert (
            "(n) (1) state;condition;circumstances;the way things are or should be;truth;(P)"
                .removeSynonyms(0)
            == "(n) "
        )
        // complex remove one
        assert (
            "(adj-na,n) (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)"
                .removeSynonyms(1)
                    == "(adj-na,n) (1) deep blue;bright blue;"
        )
        // remove multiple
        assert (
            "(adj-na,n) (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)"
                .removeSynonyms(0)
            == "(adj-na,n) "
        )
    }
}
