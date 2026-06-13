# Yomikata Z — Architecture

## Ce que fait l'application

Yomikata Z est une application Android d'apprentissage du japonais. Elle permet à l'utilisateur de :

- Parcourir des listes de mots classées par catégorie (Hiragana, Katakana, JLPT N1–N5, Kanjis, Compteurs) et par sélections personnelles.
- Lancer des sessions de quiz pour mémoriser les mots.
- Suivre sa progression via un système de niveaux et de points par mot.
- Écouter la prononciation de mots et de phrases d'exemple (voix pré-enregistrées ou TTS).
- Sauvegarder/restaurer sa base de données depuis un stockage externe.

L'interface et les traductions sont disponibles en **6 langues** : anglais (EN), français (FR), allemand (DE), espagnol (ES), portugais (PT), mandarin (ZH). La langue est sélectionnable dans les préférences, avec détection automatique de la locale système au premier lancement.

---

## Couches principales

```
┌─────────────────────────────────────────────────────┐
│  Screens  (Activity / Fragment / Presenter — MVP)    │
├─────────────────────────────────────────────────────┤
│  Presenters partagés  (SelectionsPresenter,          │
│                        WordInQuizPresenter,          │
│                        WordCountPresenter)           │
├─────────────────────────────────────────────────────┤
│  Repositories  (interfaces + implémentations Room)   │
├─────────────────────────────────────────────────────┤
│  DAOs  (Room — KotlinFlow / suspend)                 │
├─────────────────────────────────────────────────────┤
│  YomikataDatabase  (Room, SQLite, asset DB v19)      │
└─────────────────────────────────────────────────────┘
         Injection de dépendances : Kodein DI 7.x
```

---

## Pattern architectural : MVP

Chaque écran suit le pattern **Model–View–Presenter** avec un contrat d'interface :

```
FooContract.View      ← implémenté par Fragment/Activity
FooContract.Presenter ← implémenté par FooPresenter
```

Les `Fragment` et `Activity` ne contiennent que de la logique UI (binding, animations, navigation). Toute la logique métier est dans les `Presenter`, qui accèdent aux données via les repositories injectés par Kodein.

Les presenters reçoivent un `CoroutineScope` (le `lifecycleScope` du fragment hôte) à la construction — ils n'en créent jamais eux-mêmes, ce qui les rend lifecycle-aware.

---

## Écrans

| Package | Activité principale | Rôle |
|---|---|---|
| `quizzes` | `QuizzesActivity` | **Activité de démarrage** — chargement DB, navigation principale (ViewPager2 : Home + liste des catégories), gestion erreur DB |
| `home` | `HomeFragment` | Dashboard — stats globales, accès rapide, fil d'actualité Firebase, lien GitHub Sponsors |
| `content` | `ContentActivity` | Liste des mots d'une catégorie, graphique de progression |
| `content/word` | `WordDetailDialogFragment` | Détail d'un mot (kanji, radical, exemple de phrase) |
| `quiz` | `QuizActivity` / `QuizFragment` | Session de quiz complète |
| `answers` | `AnswersActivity` | Récapitulatif des réponses après une session |
| `search` | `SearchResultActivity` | Recherche de mots dans toute la base |
| `prefs` | `PrefsActivity` | Paramètres (thème, vitesse TTS, voix, langue de traduction) |

---

## Système de langues (`util/language/AppLanguage.kt`, `util/language/LanguageManager.kt`)

### `AppLanguage`

Enum des langues supportées :

```kotlin
enum class AppLanguage(val isoCode: String, val displayName: String) {
    ENGLISH("en", "English"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    CHINESE("zh", "中文")
}
```

### `LanguageManager`

Singleton Kodein. Gère la langue active de façon centralisée.

- **`LanguageManager.current`** — propriété companion `@Volatile` lue directement par les méthodes `getTrad()` des modèles (pas d'injection Android dans les data classes).
- **`initFromPrefs(prefs)`** — appelé dans `Application.onCreate()`, avant tout DI. Détecte la locale système au premier lancement et la persiste.
- **`setLanguage(lang)`** — met à jour `current` et écrit dans `SharedPreferences`.

### Langue UI et langue de traduction — couplées

Depuis la migration vers `AppCompatDelegate.setApplicationLocales()`, la langue UI (strings Android) et la langue des traductions (mots, quiz) sont **synchronisées** :

- `initFromPrefs()` appelle `AppCompatDelegate.setApplicationLocales(current.isoCode)` au démarrage de l'app
- `setLanguage()` fait pareil lors d'un changement en cours de session
- AppCompat recrée automatiquement l'activité courante → plus de dialog "redémarrer l'app"
- `res/xml/locale_config.xml` déclare les locales supportées (requis Android 13+)

**Comportement de détection** : si aucun choix explicite n'est sauvegardé, suit la locale système. Le choix explicite (via `PrefsActivity`) est persisté dans `SharedPreferences` (`app_language`).

### Chaîne de fallback dans `getTrad()`

Pour les traductions non encore remplies (colonnes vides) :
```
langue demandée → EN si colonne vide → EN par défaut
```

Les modèles `Word`, `Sentence`, `Quiz`, `KanjiSolo`, `KanjiSoloRadical` et `Radical` implémentent tous ce pattern via `when (LanguageManager.current)`.

---

## Modèles de données

### `Word`
Entité centrale. Champs notables :
- `japanese`, `reading` — contenu japonais.
- `english`, `french` — traductions originales (source JMdict).
- `german`, `spanish`, `portuguese`, `chinese` — traductions ajoutées en v18, vides jusqu'au remplissage (phases 3c–3f). Fallback vers `english` si vide.
- `points` (0–850) — mesure la maîtrise du mot, détermine le niveau.
- `level` — redondant avec `points`, maintenu pour compatibilité.
- `repetition` — délai avant la prochaine apparition en mode progressif.
- `baseCategory` — catégorie d'origine du mot.
- `sentenceId` — lien optionnel vers une phrase d'exemple.

### `Quiz`
Une liste de mots (ex : "JLPT N4 - verbes"). Champs :
- `nameEn`, `nameFr` — noms originaux.
- `nameDe`, `nameEs`, `namePt`, `nameZh` — noms traduits (v18, vides jusqu'au remplissage).
- `category` et `isSelected`.

### `Sentence`
Phrase d'exemple japonaise avec traductions `en`, `fr`, `de`, `es`, `pt`, `zh` et un niveau de difficulté.

### `KanjiSolo` / `Radical`
Données statiques sur les kanjis (tracés, kunyomi/onyomi, radical). Champs `en`, `fr`, `de`, `es`, `pt`, `zh`.

### `StatEntry`
Journal d'événements (action, résultat, date, id associé).

---

## Système de niveaux (`util/quiz/LevelSystem.kt`)

Les mots sont notés de 0 à 850 points. Le niveau (`Level`) en découle directement :

| Level | Points min | Répétition typique |
|---|---|---|
| LOW | 0 | 1 |
| MEDIUM | 200 | ~6 |
| HIGH | 400 | ~28 |
| MASTER | 600 | ~149 |

- `addPoints()` : ajoute ou retire des points selon le type de quiz et la vitesse choisie.
- `getRepetition()` : croissance exponentielle — les mots bien connus réapparaissent beaucoup moins fréquemment.
- `levelUp()` / `levelDown()` : saut de niveau forcé, en conservant le % de progression intra-niveau.

---

## Types de quiz (`QuizType`)

| Type | Description | Points bonus |
|---|---|---|
| `TYPE_PRONUNCIATION` | Saisie clavier de la lecture (hiragana/katakana) | +15 |
| `TYPE_EN_JAP` | Saisie du japonais à partir de la traduction | +5 |
| `TYPE_JAP_EN` | QCM ou saisie de la traduction | 0 |
| `TYPE_AUDIO` | Identifier le mot depuis l'audio | -5 |
| `TYPE_PRONUNCIATION_QCM` | QCM de lecture | -10 |
| `TYPE_AUTO` | Choix aléatoire | 0 |

La validation est centralisée dans `AnswerValidator.checkWord()` : le tiret ASCII `-` est accepté pour `ー`, et les lectures multiples séparées par `/` ou `;` sont toutes valides.

---

## Stratégies de quiz (`QuizStrategy`)

- `STRAIGHT` — tous les mots dans l'ordre.
- `SHUFFLE` — tous les mots dans un ordre aléatoire.
- `PROGRESSIVE` — seuls les mots dont la `repetition` est ≤ 0 sont proposés ; après chaque réponse, la valeur est recalculée et tous les autres mots sont décrémentés.

---

## Architecture du quiz (classes extraites de `QuizPresenter`)

`QuizPresenter` délègue à plusieurs helpers :

| Classe | Responsabilité |
|---|---|
| `QuizSessionState` | Machine d'état : index courant (mode normal vs. mode erreurs), liste des mots, liste des erreurs |
| `AnswerValidator` | Vérification de la réponse (pure, sans état) |
| `RandomAnswerGenerator` | Génère les options de QCM (3 distracteurs aléatoires) |
| `WordStatisticsRecorder` | Enregistre les stats (tries, success, fail) en base |
| `QCMUIController` | Prépare les données d'affichage QCM (textes, furigana, couleurs) |
| `SettingsUIManager` | Configure l'UI du quiz (volume TTS, vitesse) — reçoit `SharedPreferences` par injection |
| `DialogFlowController` | Gère les dialogues de fin de session / fin de quiz / mode erreurs — reçoit `SharedPreferences` par injection |

---

## Base de données (Room)

Fichier : `yomikataz.db` — livré dans `assets/` à la version 21, copié au premier lancement. Les traductions DE/ES/PT/ZH et les POS sont déjà dans l'asset ; pour les utilisateurs en upgrade, ils sont copiés depuis `yomikataz_translations.db` (second asset) via le callback `onOpen`.

Un second asset `yomikataz_translations.db` contient les données de référence (tables `words`, `quiz`, `sentences`) avec toutes les colonnes DE/ES/PT/ZH remplies. La fonction `populateTranslationsIfNeeded` l'attache via `ATTACH DATABASE` depuis `onOpen` et met à jour les tables manquantes. La garde `needsTranslations()` vérifie `words.german` **et** `sentences.de` pour couvrir les utilisateurs qui avaient migré avant l'ajout des traductions de phrases.

**Version courante : 21.** Toutes les migrations sont définies dans `YomikataDatabase`. `fallbackToDestructiveMigrationFrom(0..12)` est utilisé (PAS `fallbackToDestructiveMigration()`) pour les utilisateurs encore sur une version < 13 (reset propre sans crash).

> **Piège Room 2.6.1** : `fallbackToDestructiveMigration()` seul + `createFromAsset` provoque l'effacement de la DB de TOUS les utilisateurs dont la version ≠ cible, car `isMigrationRequired` retourne toujours `false` → `SQLiteCopyOpenHelper` remplace la DB par l'asset même quand les migrations existent.

> **Piège Room 2.7+** : `room_table_modification_log` est requis par `TriggerBasedInvalidationTracker` mais n'est créé par Room que lors d'un `onCreate` (installation fraîche). Pour les migrations depuis d'anciennes versions, il faut le créer explicitement — dans `MIGRATION_16_21` ET dans le callback `onOpen` (garde-fou pour les devices déjà à v21 sans la table).

### Tables

| Table | Entité Room | Notes |
|---|---|---|
| `words` | `RoomWords` | `german`, `spanish`, `portuguese`, `chinese` (v18) ; `pos` (v20) |
| `quiz` | `RoomQuiz` | `name_de`, `name_es`, `name_pt`, `name_zh` (v18) |
| `quiz_word` | `RoomQuizWord` | Clé composite `quiz_id` + `word_id` |
| `sentences` | `RoomSentences` | `de`, `es`, `pt`, `zh` (v18) |
| `kanji_solo` | `RoomKanjiSolo` | `de`, `es`, `pt`, `zh` (v18) |
| `radicals` | `RoomRadicals` | `de`, `es`, `pt`, `zh` (v18) |
| `stat_entry` | `RoomStatEntry` | |

### Historique des migrations

| Version | Contenu |
|---|---|
| 1→12 | Supprimées (migrations obsolètes, no-op). Les utilisateurs v≤12 reçoivent un reset. |
| 13→14 | Refactoring Room : DROP/CREATE de toutes les tables pour uniformiser les types. |
| 14→15 | Nouveau système de points (0–850 cumulatifs, ancienne remise à zéro supprimée). |
| 15→16 | Nettoyage des traductions EN : suppression du suffixe `;(P)` issu de JMdict. |
| **16→21** | **Migration consolidée** (prod APK code 65 = DB v16) : nettoyage données (fantôme id=3537, espaces doubles), ADD COLUMN DE/ES/PT/ZH sur 5 tables, ADD COLUMN `pos` + extraction POS du champ `english`, CREATE `room_table_modification_log`. Traductions peuplées via `onOpen`. |

Les schémas Room sont exportés à partir de la version 14 dans `app/schemas/`.

### Colonne `pos` (v20)

La colonne `pos` dans `words` stocke les Part-of-Speech extraits du champ `english` (format JMdict : `(n)`, `(v1,vt)`, etc.), sous forme de tokens séparés par virgule (`n`, `v1,vt`, etc.). L'extraction se fait par regex lors de `MIGRATION_16_21` ; les mots manquants (JLPT4/5 sans préfixe JMdict) sont complétés via `populatePosIfNeeded` dans `onOpen` depuis l'asset.

Les POS sont affichés sous forme de chips colorés dans la fiche détail d'un mot (`vh_word_detail.xml`), avec labels localisés en 6 langues et couleur par catégorie (bleu=nom, orange=verbe, vert=adjectif, violet=adverbe, gris=autre).

### Contenu de la base (v21)

| Table | Lignes |
|---|---|
| `words` | 7 503 |
| `sentences` | 7 425 |
| `kanji_solo` | 1 993 |
| `radicals` | 320 |
| `quiz` | 96 |

**Couverture des traductions par langue :**

| Langue | Mots (7 503) | Phrases (7 425) | Source mots |
|---|---|---|---|
| Anglais (EN) | 100% | 100% | Original JMdict |
| Français (FR) | 100% | 100% | Original |
| Allemand (DE) | 100% | 100% | JMdict (3 196) + traduction manuelle Claude (4 307) |
| Espagnol (ES) | 100% | 100% | JMdict (1 932) + traduction manuelle Claude (5 571) |
| Portugais (PT) | 100% | 100% | Traduction manuelle Claude (7 503) |
| Mandarin (ZH) | 100% | 100% | Traduction manuelle Claude (7 503) |

Les traductions de phrases (DE/ES/PT/ZH) ont été générées via `translate_sentences.ps1` (Haiku 4.5, batches de 1–80) et stockées dans l'asset principal `yomikataz.db` ainsi que dans `yomikataz_translations.db` pour les migrations.

**Langues d'interface disponibles :** EN, FR, DE, ES, PT, ZH (strings.xml complets pour toutes les langues).

---

## Injection de dépendances (Kodein)

L'arbre DI est construit dans `YomikataZKApplication` à partir de 5 modules :

```
applicationModule   ← Context, SharedPreferences (singleton), LanguageManager (singleton)
databaseModule      ← YomikataDatabase (singleton)
daoModule           ← les 5 DAOs
repositoryModule    ← les 5 sources locales (implémentent les interfaces repository)
presenterModule     ← SelectionsPresenter, WordInQuizPresenter, WordCountPresenter
```

**`SharedPreferences`** est lié comme singleton Kodein dans `applicationModule` et injecté dans :
- `QuizPresenter` (paramètre constructeur)
- `QuizzesPresenter` (paramètre constructeur, remplace l'ancien `Context`)
- `QuizItemPagerAdapter`, `SettingsUIManager`, `DialogFlowController` (via `QuizFragment`)

**`LanguageManager`** est lié comme singleton Kodein. Il est également initialisé statiquement via `LanguageManager.initFromPrefs()` dans `Application.onCreate()` pour que `getTrad()` soit correct dès le premier appel.

Les `Activity` et `Fragment` étendent `DIAware` et récupèrent leurs dépendances via `by di.newInstance { ... }` ou `by instance()`. `QuizActivity` crée un sous-DI (`subDI`) local pour fournir le `QuizPresenter` avec les paramètres de la session courante.

---

## Préférences utilisateur (`util/Prefs.kt`, `util/Extras.kt`)

| Clé | Type | Rôle |
|---|---|---|
| `app_language` | String (ISO 639-1) | Langue de traduction choisie |
| `pref_day_night_mode` | Int | Thème jour/nuit (AppCompatDelegate) |
| `prefs_selected_category` | Int | Dernière catégorie sélectionnée |
| `selected_quiz_types` | String (CSV) | Types de quiz actifs |
| `was_selected_quiz_types` | String (CSV) | Sauvegarde avant passage en mode AUTO |
| `length` | String | Longueur de session (nombre de mots) |
| `speed` | String | Vitesse de progression (facteur de points) |
| `play_start` / `play_end` | Boolean | Audio au début/fin d'un mot |
| `font_size` | String | Taille de police des phrases |
| `tts_rate` | Int | Vitesse TTS |
| `latest_category_1/2` | Int | Historique des catégories récentes |
| `db_restore_ongoing` | Boolean | Flag de sécurité (restauration en cours) |
| `voice_downloaded_level_V` | Boolean | Voix téléchargées par catégorie |

---

## Audio

Deux systèmes coexistent :

1. **Voix pré-enregistrées** (`ExoPlayer`) — fichiers MP3 téléchargeables par niveau depuis Firebase Storage. Chaque phrase a un fichier `s_<id>.mp3`. La disponibilité est vérifiée par `SpeechAvailability`.
2. **TTS Android** (`TextToSpeech`) — fallback si les voix ne sont pas téléchargées.

La logique de choix est dans `VoicesManager` ; la coordination avec l'UI est dans `QuizFragment`.

---

## Firebase

| Service | Usage |
|---|---|
| Realtime Database | Fil d'actualité affiché dans `HomeFragment` |
| Cloud Messaging | Notifications push |
| Storage | Téléchargement des packs de voix par niveau |

### Realtime Database — nœuds de news

Le fil d'actualité est un `String` stocké à la racine de la RTDB, avec un nœud par langue :

| Nœud | Langue |
|---|---|
| `news_en` | Anglais (fallback par défaut) |
| `news_fr` | Français |
| `news_de` | Allemand |
| `news_es` | Espagnol |
| `news_pt` | Portugais |
| `news_zh` | Mandarin |

`HomeFragment` sélectionne le nœud via `LanguageManager.current` (mapping `when` → `AppLanguage`). Le fallback est `news_en` pour toute langue non listée.

---

## Tests

| Suite | Outil | Localisation |
|---|---|---|
| Tests unitaires JVM (purs) | JUnit 4 + MockK | `src/test/` |
| Tests d'instrumentation Room | JUnit + Room Testing | `src/androidTest/` |

### Tests unitaires (117 tests)

| Fichier | Tests | Couverture |
|---|---|---|
| `TranslationParserKtTest` | 3 | Parsing `readableTranslationFormat()` |
| `AnswerValidatorTest` | 15 | Validation de toutes les combinaisons type/format |
| `QuizSessionStateTest` | 16 | Machine d'état de session (modes normal/erreurs) |
| `LevelSystemTest` | 24 | Frontières de niveaux, levelUp/Down, addPoints, getRepetition |
| `QuizPresenterTest` | 25 | Flow complet d'une session (5 combinaisons longueur × type) |
| `QuizzesPresenterTest` | 14 | Sélection de types de quiz, persistance prefs |
| `LanguageManagerTest` | 20 | AppLanguage, initFromPrefs, setLanguage, getTrad() sur 5 modèles |

### Tests d'instrumentation (androidTest)

| Fichier | Couverture |
|---|---|
| `RoomMigrationTest` | Migrations 14→15, 15→16, 16→21 (consolidée), 19→20, 20→21, chemin complet 14→21 |
| `OldMigrationTest` | Migration 13→14 (depuis schéma SQLite v13) |
| `WordDaoTest`, `QuizDaoTest`, `SentenceDaoTest`, `KanjiSoloDaoTest`, `StatsDaoTest` | DAOs Room |

---

## Build & gestion des dépendances

### Version Catalog (`gradle/libs.versions.toml`)

Depuis la migration (juin 2026), toutes les versions et dépendances sont centralisées dans `gradle/libs.versions.toml`, conformément aux recommandations Android Developer. Les fichiers de build (`build.gradle`, `app/build.gradle`) utilisent exclusivement des références `libs.*`.

**Structure du catalog :**

| Section | Contenu |
|---|---|
| `[versions]` | Toutes les versions (kotlin, agp, room, lifecycle, media3, firebase-bom, …) |
| `[libraries]` | 40+ entrées groupées par domaine (AndroidX, Room, Firebase, UI libs, tests…) |
| `[bundles]` | `media3` (exoplayer + ui + common), `firebase` (database + messaging + storage) |
| `[plugins]` | android-application, kotlin-android, kotlin-compose, ksp, google-services, gradle-versions |

**Repos centralisés dans `settings.gradle`** via `dependencyResolutionManagement` (mode `FAIL_ON_PROJECT_REPOS`) — plus de bloc `repositories {}` dans les sous-modules.

---

## Dépendances tierces notables

| Bibliothèque | Usage |
|---|---|
| Kodein DI 7.x | Injection de dépendances |
| Room + KSP | ORM SQLite |
| Kotlin Coroutines + Flow | Asynchronisme, streams réactifs |
| ExoPlayer (Media3) | Lecture audio |
| Calligraphy + ViewPump | Police personnalisée (Roboto) |
| Splitties | DSL dialogs AlertDialog |
| KenBurnsView | Animations fond (diaporama photos dans QuizzesActivity) |
| androidx.core:core-splashscreen | Contrôle du splash screen système (Android 12+) |
| HiraganaEditText | Saisie IME hiragana |
| Compose BOM 2025.05 + Material 3 | UI Compose (migration en cours — Phase 0, Session 0.3 terminée) |
| Firebase BOM 33.x | RTDB, FCM, Storage |
| MockK 1.13.x | Mocking pour les tests unitaires |
| androidx.arch.core:core-testing | `InstantTaskExecutorRule` pour les tests LiveData |

---

## Tutoriels au premier lancement (`util/TutoOverlay.kt`)

---

## Structure des packages

```
com.jehutyno.yomikata/
├── YomikataZKApplication.kt
├── ApplicationModule.kt
├── audio/                       ← ExoPlayerAudio, VoicesManager, VoicesManagerModule
├── filechooser/                 ← FileChooserDialog (Java legacy)
├── model/                       ← Word, Quiz, Sentence, KanjiSolo, Radical, Answer, StatEntry
├── presenters/                  ← BasePresenter + interfaces (SelectionsInterface, etc.)
│   └── impl/                   ← SelectionsPresenter, WordInQuizPresenter, WordCountPresenter, PresenterModule
├── repository/                  ← interfaces XxxRepository (5 fichiers)
│   ├── database/               ← YomikataDatabase, DatabaseEntities, DatabaseModule
│   │   └── dao/                ← les 5 DAOs Room + DaoModule
│   ├── local/                  ← implémentations XxxSource + RepositoryModule
│   └── migration/              ← SqliteTestHelper
├── screens/                     ← un sous-package par écran
│   ├── answers/
│   ├── content/
│   │   └── word/
│   ├── home/
│   ├── prefs/                  ← PrefsActivity
│   ├── quiz/
│   ├── quizzes/
│   └── search/
├── util/                        ← utilitaires généraux (Prefs, Extras, TutoOverlay, etc.)
│   ├── backup/                 ← BackupAndRestore, CopyUtils, LocalPersistence, InsecureSHA1PRNGKeyDerivator
│   ├── japanese/               ← HiraganaTable, HiraganaUtils
│   ├── language/               ← AppLanguage, LanguageManager, TranslationParser
│   └── quiz/                   ← Categories, QuizStrategy, QuizType, LevelSystem
├── ui/
│   ├── theme/                  ← Color.kt, Shape.kt, Type.kt, Theme.kt (design tokens + YomikataTheme)
│   └── components/             ← SectionHeader, MasteryDots, FABBar (+ FABBarState), LevelChip (+ StudyLevel, LevelChipRow)
└── view/                        ← vues custom Android
    ├── furigana/               ← FuriganaView, QuadraticOptimizer
    │   └── utils/              ← FuriganaUtils
    ├── AppBarStateChangeListener.java
    ├── SwipeDirection.java
    ├── SwipeDirectionViewPager.java
    └── WordSelectorActionModeCallback.kt
```

---

## Tutoriels au premier lancement (`util/TutoOverlay.kt`)

Les tutoriels contextuels (overlay qui met en valeur un élément d'interface au premier lancement d'un écran) sont implémentés en natif — sans dépendance tierce.

### Composants

| Élément | Rôle |
|---|---|
| `TutoId` | Enum des 11 tutoriels, chacun avec une clé SharedPreferences stable (ex. `tuto_quiz_type`) |
| `showTutoAlways()` | Affiche l'overlay sans vérifier l'état — utilisé pour l'écran de bienvenue |
| `showTutoOnce()` | Affiche l'overlay une seule fois (clé mémorisée dans `SharedPreferences`) ; no-op si déjà vu |
| `resetAllTutos()` | Efface les 11 clés `TutoId` des `SharedPreferences` (appelé depuis `PrefsActivity`) |
| `TutoOverlayView` | `FrameLayout` custom ajouté au `DecorView` ; dessine fond semi-transparent + découpe circulaire (`PorterDuff.CLEAR`) + anneau `colorAccent` autour de la cible ; texte repositionné automatiquement au-dessus ou en-dessous selon la position à l'écran ; se retire et appelle `onDismiss` au premier touch |

### Points d'utilisation

| Écran | Tutoriels |
|---|---|
| `QuizzesActivity` | Bienvenue (anchor) → bouton navigation catégories |
| `QuizzesFragment` | Type de quiz → barre de progression → checkbox sélection partielle (séquence chainée) |
| `QuizzesFragment` (onClick) | Un tutoriel par bouton de type de quiz (6 boutons, affiché une seule fois chacun) |

L'état est stocké dans les `SharedPreferences` de l'app (même instance Kodein que le reste). Les clés sont des constantes stables indépendantes de la locale (contrairement à l'ancienne lib Spotlight qui utilisait le texte localisé du titre comme clé, créant un bug potentiel au changement de langue).
