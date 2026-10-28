# État

Dernière mise à jour : 2026-10-28

Ce fichier décrit où on en est, jamais l'historique (voir `JOURNAL.md`). Il se réécrit à chaque fin de session. Première version reprise de l'ancien `CLAUDE.md`, à confirmer à la prochaine reprise.

## En cours

- Audit BDD multi-langues : phases 1 à 4 faites ; phase 5 (tests d'intégrité, `DatabaseIntegrityTest`) présente côté instrumentation, statut à confirmer
- Version en production : 2.1.0 (corrections de plantages remontés par Crashlytics, voir `JOURNAL.md`)

## Points ouverts

- Chinois (ZH) : `removeAnyJapanese` supprime aussi la traduction en Han ; rendre `cleanForQCM` conscient de la langue cible

## Couverture traductions (état actuel)
| Langue | Mots (7 503) | Phrases (7 425) | Source mots |
|---|---|---|---|
| Allemand (DE) | 100% | 100% | JMdict + traduction manuelle Claude |
| Espagnol (ES) | 100% | 100% | JMdict (1 932) + traduction manuelle Claude (5 571) |
| Portugais (PT) | 100% | 100% | Traduction manuelle Claude |
| Mandarin (ZH) | 100% | 100% | Traduction manuelle Claude |

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
