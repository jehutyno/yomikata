package com.jehutyno.yomikata.ui.word

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.ui.theme.PosAdjective
import com.jehutyno.yomikata.ui.theme.PosAdverb
import com.jehutyno.yomikata.ui.theme.PosNoun
import com.jehutyno.yomikata.ui.theme.PosOther
import com.jehutyno.yomikata.ui.theme.PosVerb

/**
 * Mapping partagé entre la liste de mots ([WordListRow]) et le détail ([WordDetailScreen]) :
 * un token POS JMdict → couleur de chip + nom complet localisé.
 *
 * Granularité détaillée : les variantes de verbes (transitif/intransitif, ichidan, godan, suru,
 * zuru, auxiliaire) et d'adjectifs (-i, -na, -no, -taru) ont chacune leur propre nom. La couleur
 * reste celle de la grande famille grammaticale (nom = bleu, verbe = orange, adjectif = vert,
 * adverbe = violet, divers = gris). Mapping identique à celui de l'ancien `WordPagerAdapter`.
 */

/** Couleur du chip POS pour un token JMdict (par grande famille grammaticale). */
fun posChipColor(token: String): Color = when (val t = token.lowercase()) {
    "n", "n-t", "n-suf", "pn" -> PosNoun
    "vi", "vt", "v1", "vz", "vs", "aux-v", "aux-adj" -> PosVerb
    "adj-i", "adj-na", "adj-no", "adj-t" -> PosAdjective
    "n-adv", "adv", "adv-to" -> PosAdverb
    else -> if (t.startsWith("v5")) PosVerb else PosOther
}

/** Ressource de chaîne (nom complet localisé), ou 0 si inconnu. */
@StringRes
fun posChipLabelRes(token: String): Int = when (val t = token.lowercase()) {
    "n", "n-t"        -> R.string.pos_noun
    "n-suf"           -> R.string.pos_suffix
    "n-adv"           -> R.string.pos_adverb
    "pn"              -> R.string.pos_pronoun
    "vi"              -> R.string.pos_intransitive
    "vt"              -> R.string.pos_transitive
    "v1"              -> R.string.pos_verb_ichidan
    "vz"              -> R.string.pos_verb_zuru
    "vs"              -> R.string.pos_verb_suru
    "aux-v", "aux-adj" -> R.string.pos_auxiliary
    "adj-i"           -> R.string.pos_adj_i
    "adj-na"          -> R.string.pos_adj_na
    "adj-no"          -> R.string.pos_adj_no
    "adj-t"           -> R.string.pos_adj_t
    "adv", "adv-to"   -> R.string.pos_adverb
    "pref"            -> R.string.pos_prefix
    "suf"             -> R.string.pos_suffix
    "exp"             -> R.string.pos_expression
    "num"             -> R.string.pos_number
    "ctr"             -> R.string.pos_counter
    "abbr"            -> R.string.pos_abbreviation
    "pol"             -> R.string.pos_polite
    "hum"             -> R.string.pos_humble
    "int"             -> R.string.pos_interjection
    else              -> if (t.startsWith("v5")) R.string.pos_verb_godan else 0
}

/** Texte du chip : nom complet localisé, ou token brut en majuscules si inconnu. */
@Composable
fun posChipLabel(token: String): String {
    val res = posChipLabelRes(token)
    return if (res != 0) stringResource(res) else token.uppercase()
}
