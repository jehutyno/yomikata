# État

Dernière mise à jour : 2026-10-30

Ce fichier décrit où on en est, jamais l'historique (voir `JOURNAL.md`). Il se réécrit à chaque fin de session. Première version reprise de l'ancien `CLAUDE.md`, à confirmer à la prochaine reprise.

## En cours

- Audit BDD multi-langues : phases 1 à 4 faites ; phase 5 (tests d'intégrité, `DatabaseIntegrityTest`) présente côté instrumentation, statut à confirmer
- Version en production : 2.1.0 (corrections de plantages remontés par Crashlytics, voir `JOURNAL.md`)

## Points ouverts

- Unifier le menu `LevelMenu.kt` : il n'est utilisé que dans `QuizScreen` et `AnswerReviewScreen`, mais pas `WordDetailScreen` (où il reste encore les deux boutons *demote/promote*).
- En mode quiz, quand on touche la phrase pendant un quiz pour afficher les informations sur le mot en vedette : le rectangle gris qui pop-up utilise l'ancien style graphique de l'application. Unifier avec `WordDetailScreen`.
- Restaurer la possibilité de **recherche** des mots dans tous les niveaux et pas seulement la Partie qui s'affiche à l'écran.
- Certaines `strings` s'affichent toujours au pluriel (ex : `mastery_mastered`, `quiz_launched`, `words_seen`, `good_answers`, `wrong_answers`).
- Quiz Prononciation : actuellement le **champ de saisie** est de type `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, cela **désactive la touche dakuten (゛)** sur les claviers japonais (12キー). Il serait préférable d'utiliser le type `InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS`, mais cela perturbe la bibliothèque de conversion romaji-kana (testé avec Gboard, Microsoft SwiftKey et HeliBoard).

### Confusions possibles pour les développeurs

- La string `action_favorite` a les mêmes valeurs que `bottomnav_selections`, ça fait doublon.
- Il faudrait renommer `onFavoriteClick` en `onSelectionClick` partout.
- Confusion possible entre `isReveal` et `tap_to_reveal` : le premier indique si l'utilisateur a répondu à la question (la réponse est révélée), le deuxième correspond à la préférence qui masque initialement les réponses des QCM.

### Mots liés à des phrases d'exemples où le mot n'apparaît pas strictement (issue #4)

Cela cause un problème d'affichage (le premier mot de la phrase est mis en surbrillance par défaut).
Exemples : "左" introuvable dans "ひだりへまがりなさい。".

Cas les plus fréquents :
- kanji écrit en kana
- kanji isolé par furigana ({英;えい}{語;ご} au lieu de {英語;えいご})
- mot orthographié différemment (okurigana, ヶ écrit か...)
- mot absent (ex : w679 二ヶ月 mais s7356 contient 三ヶ月)

La requête suivante permet de les lister :

```sql
select w._id, w.sentence_id, w.japanese, s.jap from words w
inner join sentences s on w.sentence_id = s._id
where s.jap not like '%' || w.japanese || '%'
```

### Problèmes d'affichage

- **Boutons par-dessus le texte en mode quiz** : les boutons du *"GROUPE 3 (ou bas pour audio/EN-JAP) : boutons d'action"* s'affichent très souvent au-dessus de la phrase d'exemple en mode "Prononciation" (écriture) car le clavier comprime tout verticalement.
- **Chevauchement de furigana** : parfois le furigana d'un kanji déborde sur le furigana du kanji juste à droite.
- **Sélections affichées en mode "Grille"** : le mot a très peu d'espace pour s'afficher *à gauche* des icônes "(Godan) ⭐ 🔉", ce qui donne des "boîtes" très étirées verticalement.

## Couverture traductions (état actuel)

| Langue         | Mots (7 503) | Phrases (7 425) | Source mots                                         |
|----------------|--------------|-----------------|-----------------------------------------------------|
| Allemand (DE)  | 100%         | 100%            | JMdict + traduction manuelle Claude                 |
| Espagnol (ES)  | 100%         | 100%            | JMdict (1 932) + traduction manuelle Claude (5 571) |
| Portugais (PT) | 100%         | 100%            | Traduction manuelle Claude                          |
| Mandarin (ZH)  | 100%         | 100%            | Traduction manuelle Claude                          |

## Comptage des tests

**155 tests unitaires JVM — 100% verts** (13 classes), soit **184 tests JVM** avec les 29 screenshots Roborazzi.

Ajoutés lors du durcissement pré-prod (couche 1) :
- `GetTradMultiLanguageTest` — fallback `getTrad()`/`getName()` DE/ES/PT/ZH sur les 5 modèles (Word/Sentence/KanjiSolo/Radical/Quiz)
- `RandomAnswerGeneratorTest` — la bonne réponse QCM est toujours présente, à la position du RNG
- `WordStatisticsRecorderTest` — mapping résultat → StatAction/StatResult + délégation aux repos
- `CategoriesTest` — mappings catégorie → niveau/taille/version/url de pack de voix
- `FuriganaParsingTest` — `sentenceFuri`/`sentenceNoFuri`/`sentenceNoAnswerFuri` (`util/ActionsUtils.kt`)

Côté instrumentation : `DatabaseIntegrityTest` (10 tests) vérifie l'asset réel (versions, intégrité, couverture 6 langues, `0x3F`, `identity_hash`, POS) — voir § Tests d'intégrité.

`DialogFlowController` (logique auto-skip fin de quiz) reste non couvert en JVM (couplé `Fragment`/dialog) → à traiter en tests Compose/E2E (couche 4).
