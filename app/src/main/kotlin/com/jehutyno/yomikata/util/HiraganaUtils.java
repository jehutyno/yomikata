package com.jehutyno.yomikata.util;

import java.util.ArrayList;

/**
 * Created by jehutyno on 20/02/16.
 */
public class HiraganaUtils {

    public static String toHiragana(String romajiText) {
        if (romajiText.matches("[ \t\n\f\r]*.*")) {
            romajiText = romajiText.replaceFirst("[ \t\n\f\r]*", "");
        }
        String remainingText = romajiText;
        ArrayList<String> syllabes = new ArrayList<String>();

        while (remainingText.length() > 0) {

            //vowel alone or hiragana or . or -
            if (remainingText.substring(0, 1).matches("[aeuio-[.]]") || remainingText.length() == 1
                || remainingText.substring(0, 1).matches(HiraganaTable.hiraganaRegex)) {
                syllabes.add(remainingText.substring(0, 1));
                if (remainingText.length() > 1) {
                    remainingText = remainingText.substring(1, remainingText.length());
                } else {
                    remainingText = "";
                }
                //n followed by not a vowel
            } else if (remainingText.substring(0, 1).matches("n") && !remainingText.substring(1, 2).matches("[aeyuion-[.]]")) {
                syllabes.add("nn");
                remainingText = remainingText.substring(1, remainingText.length());
                //two letters syllabes
            } else if (remainingText.substring(1, 2).matches("[aeuio]")
                || remainingText.substring(0, 2).matches("nn")
                || remainingText.length() == 2) {
                syllabes.add(remainingText.substring(0, 2));
                if (remainingText.length() > 2) {
                    remainingText = remainingText.substring(2, remainingText.length());
                } else {
                    remainingText = "";
                }
                //three letters syllabes
            } else if (remainingText.substring(2, 3).matches("[aeuio]") || remainingText.length() >= 3) {
                //little tsu case with two following
                if (remainingText.substring(0, 1).equals(remainingText.substring(1, 2))) {
                    syllabes.add("lttu");
                    remainingText = remainingText.substring(1, remainingText.length());

                    //normal 3 letters syllabes case
                } else {
                    syllabes.add(remainingText.substring(0, 3));
                    remainingText = remainingText.substring(3, remainingText.length());
                }
            }

        }

        String hiraganaText = "";
        for (String syllabe : syllabes) {
            if (HiraganaTable.hiraganaMap.containsKey(syllabe)) {
                hiraganaText = hiraganaText.concat(HiraganaTable.hiraganaMap
                    .get(syllabe));
            } else {
                hiraganaText = hiraganaText.concat(syllabe);
            }
        }

        return hiraganaText;
    }

}
