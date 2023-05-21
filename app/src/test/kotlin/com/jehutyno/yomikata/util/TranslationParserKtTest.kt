package com.jehutyno.yomikata.util

import org.junit.Test


class TranslationParserKtTest {

    private val samples = listOf(
        "(adj-na,n) (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)",
        "(n,adj-no) (1) physiology;(2) (See 月経) menstruation;one's period;menses;(P)",
        "(v1,vt) to lay in stock;to replenish stock;to procure;to learn something that might be useful later;(P)",
        "(n) state;condition;circumstances;the way things are or should be;truth;(P)",
        "(n,n-suf,pref) (1) super-;ultra-;hyper-;very;really;(n,n-suf) (2) over (after a number or counter);more than;(P)",
        "(adj-i) rough;rude;(P)",
        "(adj-na,n) (1) strange;odd;peculiar;weird;curious;queer;eccentric;funny;suspicious;fishy;(2) unexpected;(3) change;(4) incident;disturbance;disaster;accident;(n-pref) (5) {music} (See 変ロ短調) flat;(P)",
        "ON",
        "9",
        "metal; Friday",
        "fig (fruit/tree)"
    )

    @Test
    fun removePartsOfSpeechInfo() {
        assert( removePartsOfSpeechInfo(samples[0]) ==
                " (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)" )
        assert( removePartsOfSpeechInfo(samples[1]) ==
                " (1) physiology;(2) (See 月経) menstruation;one's period;menses;(P)" )
        assert( removePartsOfSpeechInfo(samples[2]) ==
                " to lay in stock;to replenish stock;to procure;to learn something that might be useful later;(P)" )
        assert( removePartsOfSpeechInfo(samples[7]) == samples[7] )
        assert( removePartsOfSpeechInfo(samples[10]) == samples[10] )

        assert( removePartsOfSpeechInfo("(v5s,vt) (1) to roll;(2) to turn over;to tip over;to throw down;(3) to leave;(4) to buy and sell (quickly for a profit)")
        == " (1) to roll;(2) to turn over;to tip over;to throw down;(3) to leave;(4) to buy and sell (quickly for a profit)")
    }

    @Test
    fun removeAnyJapanese() {
        assert( removeAnyJapanese(samples[0]) == samples[0] )
        assert( removeAnyJapanese(samples[1]) ==
                "(n,adj-no) (1) physiology;(2)  menstruation;one's period;menses;(P)" )
        assert( removeAnyJapanese(samples[10]) == samples[10] )
    }

    @Test
    fun removeSynonyms() {
        assert( removeSynonyms(samples[0], 2) == samples[0] )
        assert( removeSynonyms(samples[0], 1) == "(adj-na,n) (1) deep blue;bright blue;" )
        assert( removeSynonyms(samples[0], 0) == "(adj-na,n) " )
    }
}
