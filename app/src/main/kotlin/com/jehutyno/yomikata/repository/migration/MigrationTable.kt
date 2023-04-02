package com.jehutyno.yomikata.repository.migration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Created by valentin on 13/10/2016.
 */
interface MigrationTable {
    val column: String
    val tableName: String

    companion object {
        fun <T: MigrationTable> allColumns(columns: Array<T>): Array<String> {
            return columns.flatMap { arrayOf(it.column).asIterable() }.toTypedArray()
        }
        fun <T: MigrationTable> allTables(tables: Array<T>): Array<String> {
            return tables.flatMap { arrayOf(it.tableName).asIterable() }.toTypedArray()
        }
    }
}

enum class MigrationTables(override val column: String, override val tableName: String) : MigrationTable {
    H_00_Hiragana_collumn_vowels("", "H_00_Hiragana_collumn_vowels"),
    H_01_Hiragana_collumn_k("", "H_01_Hiragana_collumn_k"),
    H_02_Hiragana_collumn_s("", "H_02_Hiragana_collumn_s"),
    H_03_Hiragana_collumn_t("", "H_03_Hiragana_collumn_t"),
    H_04_Hiragana_collumn_n("", "H_04_Hiragana_collumn_n"),
    H_05_Hiragana_collumn_h("", "H_05_Hiragana_collumn_h"),
    H_06_Hiragana_collumn_yw("", "H_06_Hiragana_collumn_yw"),
    H_07_Hiragana_collumn_r("", "H_07_Hiragana_collumn_r"),
    H_08_Hiragana_single_modified("", "H_08_Hiragana_single_modified"),
    H_09_Hiragana_collumn_m("", "H_09_Hiragana_collumn_m"),
    hiraganas_composed("", "hiraganas_composed"),
    hiraganas_word("", "hiraganas_word"),
    K_00_Katakana_collumn_vowels("", "K_00_Katakana_collumn_vowels"),
    K_01_Katakana_collumn_k("", "K_01_Katakana_collumn_k"),
    K_02_Katakana_collumn_s("", "K_02_Katakana_collumn_s"),
    K_03_Katakana_collumn_t("", "K_03_Katakana_collumn_t"),
    K_04_Katakana_collumn_n("", "K_04_Katakana_collumn_n"),
    K_05_Katakana_collumn_h("", "K_05_Katakana_collumn_h"),
    K_06_Katakana_collumn_yw("", "K_06_Katakana_collumn_yw"),
    K_07_Katakana_collumn_r("", "K_07_Katakana_collumn_r"),
    K_08_Katakana_single_modified("", "K_08_Katakana_single_modified"),
    K_09_Katakana_collumn_m("", "K_09_Katakana_collumn_m"),
    katakanas_composed("", "katakanas_composed"),
    katakanas_countries("", "katakanas_countries"),
    Kanji_level_01_basic_numbers("", "Kanji_level_01_basic_numbers"),
    Kanji_level_02_days("", "Kanji_level_02_days"),
    Kanji_level_03_very_easy("", "Kanji_level_03_very_easy"),
    Kanji_level_04_easy("", "Kanji_level_04_easy"),
    Kanji_level_05_medium("", "Kanji_level_05_medium"),
    Kanji_level_06_hard("", "Kanji_level_06_hard"),
    Kanji_level_07_adjectives("", "Kanji_level_07_adjectives"),
    Kanji_level_08_basic_verbs("", "Kanji_level_08_basic_verbs"),
    Kanji_level_09_double("", "Kanji_level_09_double"),
    N1_Words_01("", "N1_Words_01"),
    N1_Words_02("", "N1_Words_02"),
    N1_Words_03("", "N1_Words_03"),
    N1_Words_04("", "N1_Words_04"),
    N1_Words_05("", "N1_Words_05"),
    N1_Words_06("", "N1_Words_06"),
    N1_Words_07("", "N1_Words_07"),
    N1_Words_08("", "N1_Words_08"),
    N1_Words_09("", "N1_Words_09"),
    N1_Words_10("", "N1_Words_10"),
    N1_Words_11("", "N1_Words_11"),
    N1_Words_12("", "N1_Words_12"),
    N2_Words_01("", "N2_Words_01"),
    N2_Words_02("", "N2_Words_02"),
    N2_Words_03("", "N2_Words_03"),
    N2_Words_04("", "N2_Words_04"),
    N2_Words_05("", "N2_Words_05"),
    N2_Words_06("", "N2_Words_06"),
    N2_Words_07("", "N2_Words_07"),
    N2_Words_08("", "N2_Words_08"),
    N2_Words_09("", "N2_Words_09"),
    N3_Words_01("", "N3_Words_01"),
    N3_Words_02("", "N3_Words_02"),
    N3_Words_03("", "N3_Words_03"),
    N3_Words_04("", "N3_Words_04"),
    N3_Words_05("", "N3_Words_05"),
    N3_Words_06("", "N3_Words_06"),
    N3_Words_07("", "N3_Words_07"),
    N3_Words_08("", "N3_Words_08"),
    N3_Words_09("", "N3_Words_09"),
    N3_Words_10("", "N3_Words_10"),
    N4_Adjectives("", "N4_Adjectives"),
    N4_Verbs_01("", "N4_Verbs_01"),
    N4_Verbs_02("", "N4_Verbs_02"),
    N4_Words_01("", "N4_Words_01"),
    N4_Words_02("", "N4_Words_02"),
    N4_Words_03("", "N4_Words_03"),
    N4_Words_04("", "N4_Words_04"),
    N5_Adjectives("", "N5_Adjectives"),
    N5_Counters("", "N5_Counters"),
    N5_Counters_00_age("", "N5_Counters_00_age"),
    N5_Counters_01_animal("", "N5_Counters_01_animal"),
    N5_Counters_02_articles("", "N5_Counters_02_articles"),
    N5_Counters_03_books("", "N5_Counters_03_books"),
    N5_Counters_04_cycle("", "N5_Counters_04_cycle"),
    N5_Counters_05_days("", "N5_Counters_05_days"),
    N5_Counters_06_flat("", "N5_Counters_06_flat"),
    N5_Counters_07_hours("", "N5_Counters_07_hours"),
    N5_Counters_08_minutes("", "N5_Counters_08_minutes"),
    N5_Counters_09_months("", "N5_Counters_09_months"),
    N5_Counters_10_months2("", "N5_Counters_10_months2"),
    N5_Counters_11_number("", "N5_Counters_11_number"),
    N5_Counters_12_people("", "N5_Counters_12_people"),
    N5_Counters_13_times("", "N5_Counters_13_times"),
    N5_Counters_14_tsu("", "N5_Counters_14_tsu"),
    N5_Counters_15_weekdays("", "N5_Counters_15_weekdays"),
    N5_Counters_16_year("", "N5_Counters_16_year"),
    N5_Verbs("", "N5_Verbs"),
    N5_Words_01("", "N5_Words_01"),
    N5_Words_02("", "N5_Words_02"),
    N5_Words_03("", "N5_Words_03"),
    N5_Words_04("", "N5_Words_04")
}

enum class MigrationWordTable(override val column: String, override val tableName: String) : MigrationTable {
    ID("_id", ""),
    word("word", ""),
    prononciation("prononciation", ""),
    counter_try("counter_try", ""),
    counter_success("counter_success", ""),
    counter_fail("counter_fail", ""),
    priority("priority", "")
}

@Entity(tableName = "migrationTable")
data class RoomMigrationWordTable (
    @PrimaryKey val _id: Int,
    @ColumnInfo val word: String,
    @Suppress("SpellCheckingInspection")
    @ColumnInfo val prononciation: String,
    @ColumnInfo val counter_try: Int,
    @ColumnInfo val counter_success: Int,
    @ColumnInfo val counter_Fail: Int,
    @ColumnInfo val priority: Int
) {
    companion object {
        fun from(wordTable: WordTable): RoomMigrationWordTable {
            return RoomMigrationWordTable(wordTable.id, wordTable.word, wordTable.pronunciation,
                                          wordTable.counterTry, wordTable.counterSuccess,
                                          wordTable.counterFail, wordTable.priority)
        }
    }
    fun toWordTable(): WordTable {
        return WordTable(_id, word, prononciation, counter_try, counter_success, counter_Fail, priority)
    }
}
