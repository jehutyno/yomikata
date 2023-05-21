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
        assert( samples[0].removePartsOfSpeechInfo() ==
                " (1) deep blue;bright blue;(2) ghastly pale;pallid;white as a sheet;(P)" )
        assert( samples[1].removePartsOfSpeechInfo() ==
                " (1) physiology;(2) (See 月経) menstruation;one's period;menses;(P)" )
        assert( samples[2].removePartsOfSpeechInfo() ==
                " to lay in stock;to replenish stock;to procure;to learn something that might be useful later;(P)" )
        assert( samples[7].removePartsOfSpeechInfo() == samples[7] )
        assert( samples[10].removePartsOfSpeechInfo() == samples[10] )

        assert( "(v5s,vt) (1) to roll;(2) to turn over;to tip over;to throw down;(3) to leave;(4) to buy and sell (quickly for a profit)"
            .removePartsOfSpeechInfo()
        == " (1) to roll;(2) to turn over;to tip over;to throw down;(3) to leave;(4) to buy and sell (quickly for a profit)")
    }

    @Test
    fun removeAnyJapanese() {
        assert( samples[0].removeAnyJapanese() == samples[0] )
        assert( samples[1].removeAnyJapanese() ==
                "(n,adj-no) (1) physiology;(2)  menstruation;one's period;menses;(P)" )
        assert( samples[10].removeAnyJapanese() == samples[10] )
    }

    @Test
    fun removeSynonyms() {
        assert( samples[0].removeSynonyms(2) == samples[0] )
        assert( samples[0].removeSynonyms(1) == "(adj-na,n) (1) deep blue;bright blue;" )
        assert( samples[0].removeSynonyms(0) == "(adj-na,n) " )
    }
}
