# CLAUDE.md — Instructions pour Claude Code

Ce fichier est lu automatiquement par Claude à chaque session. Il contient les conventions, commandes et règles spécifiques au projet Yomikata Z.

Pour la documentation technique complète, voir `ARCHITECTURE.md`.

> **Sources Kotlin** : les sources se trouvent dans `app/src/main/kotlin/` (et non `java/`). Toujours chercher les classes dans ce dossier.

---

## Build & Tests

### Java Home (obligatoire sur cette machine)
```powershell
$env:JAVA_HOME = "C:\Users\valen\AppData\Local\Programs\Android Studio\jbr"
```
Sans ça, `gradlew.bat` échoue avec "JAVA_HOME is set to an invalid directory".

### Commandes courantes
```powershell
# Build debug
.\gradlew.bat assembleDebug

# Tests unitaires JVM (rapide, ~15s)
.\gradlew.bat test

# Build + APK de test instrumentation (avant de commit un test androidTest)
.\gradlew.bat assembleDebug assembleAndroidTest

# Build avec schéma Room exporté (auto si DATABASE_VERSION a changé)
.\gradlew.bat kspDebugKotlin
```

### Version Catalog

Toutes les dépendances sont dans `gradle/libs.versions.toml`. Pour mettre à jour une version, modifier `[versions]` dans ce fichier — **ne jamais écrire de version inline** dans les `.gradle`.

Les repos sont centralisés dans `settings.gradle` via `dependencyResolutionManagement`. Ne pas ajouter de bloc `repositories {}` dans les sous-modules.

### SQLite3 (scripts de données)
```powershell
$sq = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"
$db = "C:\Users\valen\Repos\yomikata\app\src\main\assets\yomikataz.db"
```

**Piège encodage** : ne jamais piper du texte avec des caractères non-ASCII directement vers sqlite3 depuis PowerShell. Toujours écrire le SQL dans un fichier UTF-8 sans BOM, puis utiliser `.read fichier.sql` :
```powershell
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
$writer = [System.IO.StreamWriter]::new($sqlPath, $false, $utf8NoBom)
# ... écrire le SQL ...
$writer.Close()
& $sq $db ".read $sqlPath"
```

---

## Conventions de code

### Migrations Room
- Chaque migration est un `object : Migration(X, Y)` dans le companion object de `YomikataDatabase`
- Elles sont nommées `MIGRATION_X_Y` et ajoutées à `addMigrations(...)` dans `getDatabase()`
- Toujours incrémenter `DATABASE_VERSION` dans `YomikataDatabase.kt`
- Room exporte automatiquement le schéma JSON dans `app/schemas/` à la compilation (KSP) — committer ce fichier
- **Ne jamais utiliser** de DAOs, entités ou constantes externes dans les migrations — elles doivent être auto-contenues
- Après une migration qui modifie des données, toujours appliquer les mêmes changements à l'asset `yomikataz.db` et mettre à jour son `PRAGMA user_version`

### Entités Room (`DatabaseEntities.kt`)
- Chaque entité `RoomFoo` a deux méthodes :
  - `companion object fun from(model: Foo): RoomFoo` — conversion model → Room
  - `fun toFoo(): Foo` — conversion Room → model
- Les nouveaux champs avec `DEFAULT` doivent avoir `= valeurParDéfaut` dans le constructeur Kotlin pour ne pas casser les call sites existants

### Modèles
- Les champs de traduction suivent la convention de nommage par table :
  - `words` : `english`, `french`, `german`, `spanish`, `portuguese`, `chinese` (noms complets)
  - `sentences`, `kanji_solo`, `radicals` : `en`, `fr`, `de`, `es`, `pt`, `zh` (codes ISO)
  - `quiz` : `nameEn`, `nameFr`, `nameDe`, `nameEs`, `namePt`, `nameZh` (camelCase préfixé)

### `getTrad()` / `getName()`
Pattern uniforme dans tous les modèles — chaîne de fallback :
```kotlin
fun getTrad(): String {
    return when (LanguageManager.current) {
        AppLanguage.FRENCH     -> french
        AppLanguage.GERMAN     -> german.ifEmpty { english }
        AppLanguage.SPANISH    -> spanish.ifEmpty { english }
        AppLanguage.PORTUGUESE -> portuguese.ifEmpty { english }
        AppLanguage.CHINESE    -> chinese.ifEmpty { english }
        else                   -> english
    }.readableTranslationFormat()
}
```
Quand une nouvelle langue est ajoutée, mettre à jour les 6 modèles : `Word`, `Sentence`, `Quiz`, `KanjiSolo`, `KanjiSoloRadical`, `Radical`.

### SharedPreferences
Toujours passer par `Prefs` enum (dans `util/Prefs.kt`) pour les clés. Ne jamais utiliser de string literal directement. Injecté via Kodein — ne jamais appeler `PreferenceManager.getDefaultSharedPreferences()` dans les presenters ou helpers.

### Coroutines
Les presenters reçoivent leur `CoroutineScope` en paramètre constructeur (le `lifecycleScope` du fragment/activity hôte). Ne jamais créer `CoroutineScope(Dispatchers.Main)` manuellement dans un presenter ou helper — utiliser `lifecycleScope` dans les composants Android.

### Tutoriels au premier lancement (`util/TutoOverlay.kt`)
- Utiliser `showTutoOnce(prefs, TutoId.XXX, activity, targetView, title, message) { }` — affichage unique automatique.
- Utiliser `showTutoAlways()` uniquement pour l'écran de bienvenue (pas de mémorisation d'état).
- Pour ajouter un nouveau tutoriel : ajouter une entrée dans l'enum `TutoId` avec une clé snake_case stable (ex. `tuto_mon_truc`) — ne jamais utiliser un string localisé comme clé.
- `resetAllTutos(prefs)` est appelé depuis `PrefsActivity` ("reset_tuto") — toute nouvelle entrée `TutoId` est automatiquement réinitialisée.

### Dialogs (boîtes de dialogue)
- **Nouveau dialog** : utiliser `Context.yomikataAlert(...)` (`ui/components/YomikataDialogHost.kt`) depuis du code impératif, ou `YomikataDialog` directement depuis un composable. Ne pas créer de nouvel `alertDialog { }` Splitties brut.
- Un seul bouton `DialogButtonStyle.Primary` (pill orange) par dialog ; annulation/secondaire en `Muted` ; action irréversible en `Destructive` (rouge).
- Le thème global `YomikataAlertDialog` (`styles.xml`, `alertDialogTheme`) restyle automatiquement les dialogs Splitties/AppCompat non migrés — le conserver comme filet de sécurité.
- Voir `DESIGN.md` § Dialog pour les specs complètes.

---

## Injection de dépendances (Kodein)

### Bindings dans `applicationModule`
```kotlin
bind<Context>()          with instance(context)
bind<SharedPreferences>() with singleton { PreferenceManager.getDefaultSharedPreferences(instance()) }
bind<LanguageManager>()  with singleton { LanguageManager(instance()) }
```

### Initialisation au démarrage (`YomikataZKApplication.onCreate`)
```kotlin
LanguageManager.initFromPrefs(PreferenceManager.getDefaultSharedPreferences(this))
```
Doit être appelé AVANT tout accès au DI ou à `getTrad()`.

---

## Base de données

- **Version courante : 21** — migration consolidée `MIGRATION_16_21` (prod APK code 65 = DB v16 → v21 en un seul saut)
- Asset : `app/src/main/assets/yomikataz.db`
- `fallbackToDestructiveMigrationFrom(0..12)` est utilisé (PAS `fallbackToDestructiveMigration()`) → les utilisateurs sur version < 13 ont un reset propre sans crash. **Piège Room 2.6.1** : `fallbackToDestructiveMigration()` seul + `createFromAsset` efface la DB de TOUS les utilisateurs (isMigrationRequired retourne false → SQLiteCopyOpenHelper remplace la DB par l'asset pour toute version ≠ cible)
- **Piège Room 2.7+** : `room_table_modification_log` requis par `TriggerBasedInvalidationTracker` — créer explicitement dans les migrations ET dans `onOpen` (`CREATE TABLE IF NOT EXISTS`)
- Les migrations 1→12 ont été supprimées (code mort, no-op)
- Schémas exportés disponibles à partir de la v14 dans `app/schemas/`

### Règles pour une nouvelle migration
1. Écrire `MIGRATION_X_Y` dans le companion object de `YomikataDatabase`
2. Ajouter à `addMigrations(...)` dans `getDatabase()`
3. Incrémenter `DATABASE_VERSION`
4. Appliquer les mêmes SQL sur l'asset `yomikataz.db` + `PRAGMA user_version = Y`
5. Builder → Room génère `Y.json` dans `app/schemas/`
6. Ajouter `MIGRATION_X_Y` à `ALL_MIGRATIONS` dans `RoomMigrationTest`
7. Ajouter un test `migrateXtoY()` dans `RoomMigrationTest`

---

## Tests

### Règles
- Tests JVM purs dans `src/test/` — pas de Robolectric, pas d'Android runtime
- Tests d'instrumentation dans `src/androidTest/` pour Room, DAOs
- Utiliser `InstantTaskExecutorRule` pour tout test qui touche `LiveData` (dep : `androidx.arch.core:core-testing` dans `testImplementation`)
- `mockkStatic` n'est plus nécessaire pour `PreferenceManager` — `SharedPreferences` est injecté directement
- Pour mocker `Random` : utiliser `object : Random() { override fun nextInt(bound: Int) = 0 }` (pas `mockk<Random>()` — NPE JvmAutoHinter)

### Comptage actuel
**147 tests unitaires JVM — 100% verts** (13 classes).

Ajoutés lors du durcissement pré-prod (couche 1) :
- `GetTradMultiLanguageTest` — fallback `getTrad()`/`getName()` DE/ES/PT/ZH sur les 5 modèles (Word/Sentence/KanjiSolo/Radical/Quiz)
- `RandomAnswerGeneratorTest` — la bonne réponse QCM est toujours présente, à la position du RNG
- `WordStatisticsRecorderTest` — mapping résultat → StatAction/StatResult + délégation aux repos
- `CategoriesTest` — mappings catégorie → niveau/taille/version/url de pack de voix
- `FuriganaParsingTest` — `sentenceFuri`/`sentenceNoFuri`/`sentenceNoAnswerFuri` (`util/ActionsUtils.kt`)

Côté instrumentation : `DatabaseIntegrityTest` (10 tests) vérifie l'asset réel (versions, intégrité, couverture 6 langues, `0x3F`, `identity_hash`, POS) — voir § Tests d'intégrité.

`DialogFlowController` (logique auto-skip fin de quiz) reste non couvert en JVM (couplé `Fragment`/dialog) → à traiter en tests Compose/E2E (couche 4).

### Tests de screenshot (Roborazzi, couche 3)
Tests de régression visuelle sur la JVM (Robolectric, **sans émulateur**). Baselines committées dans `app/src/test/screenshots/`.
```powershell
.\gradlew.bat recordRoborazziDebug   # (ré)enregistre les baselines (1ère fois / changement voulu)
.\gradlew.bat verifyRoborazziDebug   # gate de régression : échoue à toute dérive pixel
```
**Pièges spécifiques à ce stack (AGP 9.2 / Kotlin 2.3 / compileSdk 37)** — voir `MasteryBarScreenshotTest` :
- Robolectric plafonne à **SDK 36** → annoter `@Config(sdk = [36])` (compileSdk 37 non supporté au runtime).
- Utiliser `application = android.app.Application::class` dans `@Config` → sinon `YomikataZKApplication.onCreate` lance Firebase/DI et crashe.
- Utiliser l'API **content-lambda `captureRoboImage("src/test/screenshots/x.png") { ... }`**, PAS `createComposeRule()` (qui exige une Activity launcher que Robolectric ne résout pas avec l'`applicationId` `.debug`).
- Qualifiers en ordre canonique Android (`night` AVANT la densité) : `"w411dp-h891dp-night-xxhdpi"`.
- Changer la langue de l'UI par capture : `RuntimeEnvironment.setQualifiers("+de")` (les strings résolvent via `values-de`, indépendant de `LanguageManager`).

---

## Langue de l'application

### Architecture langue unifiée
Depuis la dernière session, langue UI (strings Android) et langue des traductions (mots/quiz) sont **couplées** via `AppCompatDelegate.setApplicationLocales()` :

- `LanguageManager.initFromPrefs()` → appelle `AppCompatDelegate.setApplicationLocales(current.isoCode)` au démarrage
- `LanguageManager.setLanguage()` → appelle `AppCompatDelegate.setApplicationLocales(lang.isoCode)` + persiste en prefs
- `PrefsActivity` → appelle directement `AppCompatDelegate.setApplicationLocales()` en plus du changement de `LanguageManager.current`
- AppCompat recrée l'activité automatiquement → **plus de dialog "redémarrer l'app"**
- `res/xml/locale_config.xml` déclare les locales supportées (obligatoire Android 13+)

### Comportement de détection
- Pas de prefs sauvegardée → `fromSystemLocale()` (suit la locale du device)
- Prefs sauvegardée (choix explicite user) → utilise cette valeur
- **Ne pas persister la détection automatique** — sinon le changement de langue système ne se reflète plus

### Piège quiz_subtitle (glyphes CJK en locale non-japonaise)
Dans `QuizzesAdapter`, le subtitle affiche les caractères japonais des noms de quiz. **Toujours** :
1. Lire depuis `quiz.nameEn.split("%").getOrElse(1) { "" }` — source unique correcte, indépendante de la langue
2. Mettre `holder.quizSubtitle.textLocale = Locale.JAPANESE` — déclenche le fallback CJK dans les locales DE/ES/PT/etc.

### Piège encodage XML layouts (PowerShell Set-Content)
Quand on modifie un fichier XML de layout via PowerShell `Set-Content`, le fichier est écrit avec un BOM UTF-8. Android AAPT rejette les fichiers XML avec BOM. Toujours supprimer le BOM après :
```powershell
$bytes = [System.IO.File]::ReadAllBytes($f)
if ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    [System.IO.File]::WriteAllBytes($f, $bytes[3..($bytes.Length-1)])
}
```

### Piège scripts PowerShell quiz names
Tout script PS5.1 qui lit des données SQLite via pipe et les ré-écrit dans la DB **corrompt les caractères non-ASCII** (CJK et accents). Pattern obligatoire :
- Lire les données SQLite via `.read` d'un fichier UTF-8 NoBOM, pas via pipe
- Pour les accents dans les string literals (ö, ä, ü, etc.) : utiliser `[char]0x00F6` etc.
- Après exécution : vérifier avec hex check qu'aucun `0x3F` n'apparaît dans les colonnes localisées

### Piège accents dans les traductions QCM (`removeAnyJapanese`)
`String.removeAnyJapanese()` (`util/language/TranslationParser.kt`), appelée par `cleanForQCM(false)` pour la question et les propositions du quiz, doit retirer **uniquement le japonais** (hiragana, katakana, kanji/Han, ponctuation CJK). Ne **jamais** utiliser `\P{InBasicLatin}` : ce bloc se limite à l'ASCII pur, donc les lettres accentuées (é, è, à, ä, ö, ñ, ã, ç…) y sont considérées « non latines » et disparaissent → traductions FR/DE/ES/PT amputées dans le quiz. Cibler les plages Unicode japonaises via `\p{IsHiragana}\p{IsKatakana}\p{IsHan}` + blocs `\p{InCJKSymbolsAndPunctuation}`/`\p{InKatakanaPhoneticExtensions}`/`\p{InHalfwidthAndFullwidthForms}` (constante `japaneseChars`). **Limite connue** : pour le chinois (ZH) la traduction est en Han → toujours supprimée par cette fonction (pré-existant, à traiter séparément en rendant `cleanForQCM` conscient de la langue cible).

### Piège room_master_table
Après chaque modification de schéma qui crée ou modifie des tables, **mettre à jour `room_master_table`** dans l'asset :
```sql
UPDATE room_master_table SET identity_hash = '<hash_from_schemas/VERSION.json>' WHERE id = 42;
```
Le hash est dans `app/schemas/.../VERSION.json` champ `identityHash`. Sans ça, crash au démarrage ("Migration Error").

---

## Plan en cours — Audit BDD multi-langues

### Phases terminées
- ✅ **Phase 1** : Nettoyage migrations (suppression v1–12, fallbackToDestructiveMigration)
- ✅ **Phase 2** : Migration 17 — nettoyage données (fantôme id=3537, espaces doubles)
- ✅ **Phase 3a** : LanguageManager + refactoring getTrad() (6 langues)
- ✅ **Phase 3b** : Migration 18 — ajout colonnes DE/ES/PT/ZH dans 5 tables
- ✅ **Phase 3c** : Allemand — UI strings + quiz names + **7 503/7 503 mots (100%)** — JMdict (3 196) + traduction manuelle (4 307)
- ✅ **Phase 3d** : Espagnol — UI strings + quiz names + JMdict (1 932/7 503, 26%)
- ✅ **Phase 3d bis** : Espagnol mots complets — **7 503/7 503 mots (100%)** — traduction manuelle Claude (5 571 mots)
- ✅ **Phase 3e** : Portugais — UI strings + quiz names (mots = 0% — `por` absent de JMdict, déferré à 3g)
- ✅ **Phase 3g bis** : Portugais mots complets — **7 503/7 503 mots (100%)** — traduction manuelle Claude (7 503 mots)
- ✅ **Phase 3g ter** : Mandarin mots complets — **7 503/7 503 mots (100%)** — traduction manuelle Claude (7 503 mots)
- ✅ **Phase 3f** : Mandarin — UI strings (238 strings, values-zh/strings.xml) + noms de quiz (96/96)

### Phases suivantes
- ✅ **Phase 4** : Tags POS — colonne `pos` dans `words` (migration 19→20), extraction depuis `english`, chips localisés dans la fiche détail du mot
- ✅ **Migration phrases v16** : `populateTranslationsIfNeeded` étendue pour `sentences` (DE/ES/PT/ZH) depuis `yomikataz_translations.db` ; `needsTranslations` vérifie `words.german` ET `sentences.de` pour couvrir les utilisateurs déjà migrés avant l'ajout des traductions de phrases
- ✅ **Refactoring packages** : réorganisation complète des packages (dao→repository/database/dao, managers→audio, furigana→view/furigana, presenters/source→presenters/impl, util sous-packages language/quiz/backup/japanese, screens/prefs)
- ✅ **Session 3.4** : Suppression topbar KenBurns + intégration héros par page — `HomeHero` → `yomi_logo_home.png` ; `StudyHero` dynamique par catégorie (`ic_*_big`) ; `SelectionsFragment` → héro `ic_selections_big` ; `QuizzesActivity` layout simplifié (pas de Toolbar/AppBarLayout). Fix bug `openContent` (passait `quiz.id` au lieu de l'index dans la liste).
- ✅ **Session 3.4b** : Suppression double topbar dans `ContentActivity` — `activity_content.xml` réduit à `FrameLayout` + `FloatingActionsMenu` ; `ContentActivity` : plus de `setSupportActionBar` ; `ContentFragment` : suppression workaround `hide()`/`show()` de l'ActionBar. La `WordListScreen` Compose gère sa propre `TopAppBar`.
- ✅ **Session 3.5** : Topbar Quiz migrée en Compose — `activity_quiz.xml` réduit à `FrameLayout` ; `QuizActivity` : `WindowCompat.setDecorFitsSystemWindows(false)` pour edge-to-edge ; `QuizScreen` : `TopAppBar` Compose avec `statusBarsPadding()`, bouton ✕ (`AccentOrange`), titre (`TextPrimary`), icônes TTS/Answers (`TextMuted`) ; `QuizFragment` : suppression `setHasOptionsMenu`/`onCreateOptionsMenu`/`onOptionsItemSelected`, ajout `showQuitDialog()`.
- ✅ **Session 3.6** : Écran Answer review migré en Compose (`ui/answers/AnswerReviewScreen.kt`) — dernier écran XML/RecyclerView. `AnswersFragment` héberge un `ComposeView` ; `AnswersActivity` simplifiée (suppression Toolbar/`setSupportActionBar`/`onOptionsItemSelected`, edge-to-edge `WindowCompat`) ; `activity_answers.xml` → `FrameLayout`. Cartes teintées par résultat (vert `BackgroundCorrect`/`BorderCorrect`, rouge `BackgroundWrong`/`BorderWrong`), kanji + mot cible colorés `Correct`/`Wrong` (fin du rouge de maîtrise sur mot juste) ; en-tête de synthèse `結果 · Results` ; picker de sélection en `ModalBottomSheet`. `QuizPresenter.addCurrentWordToAnswers` : couleurs HTML `#77d228`/`#d22828` → `#4ADE80`/`#F87171`. Supprimés : `AnswersAdapter.kt`, `vh_answer.xml`.
- ✅ **Session 3.7** : Retouches Home — `HOME` est désormais l'onglet de démarrage (`QuizzesActivity` : `currentDestination`/`navigateTo` initial + retour arrière → `HOME`). Hero animé Ken Burns rétabli (`HomeHero` : crossfade entre `pic_*` toutes les 7 s + zoom lent 1→1.18 via `graphicsLayer`, scrim chaud). Fond d'écran cohérent : `color_bg` (#0a0e17) sur `activity_quizzes.xml` (était `colorPrimaryDark` = gris clair) + `HomeScreen` root `BackgroundPrimary`. Suppression du `FABBar` bas de Home ; action « Continuer » déplacée **dans** la `ContinueCard` et soutien GitHub **dans** la `SupportCard`, via `DiscreetActionButton` (OutlinedButton pill `RadiusXl`, bordure `BorderAccent`, texte `AccentOrange`). Signature `HomeScreen(onContinueClick, onSupportClick)` ; `onSupportClick` → `openGithubSponsors` (https://github.com/sponsors/jehutyno).
- ✅ **Session 3.8** : Hero Study avec photos de fond — extraction de `KenBurnsImage` (zoom lent en boucle) dans `ui/components/KenBurnsImage.kt`, partagé par Home et Study. `StudyHero` affiche désormais une photo `pic_*` distincte par niveau (`categoryHeroPhoto`) en arrière-plan, avec `Crossfade` sur `selectedCategory` (fondu au changement de chip), sous le scrim froid + icône `ic_*_big` + nom du niveau. `HomeScreen` refactoré pour réutiliser le `KenBurnsImage` partagé.
- ✅ **Session 3.9** : Réintégration des options de lancement sur Study — le `FABBar` ouvre un `LaunchOptionsSheet` (`ModalBottomSheet`) qui restaure deux fonctionnalités perdues : (1) sélection multi-types `QuizType` (chips icône+label, `TYPE_AUTO` exclusif, persistée à chaque toggle) et (2) mode de lancement `QuizStrategy` (Progressif/Tout/Aléatoire — taper un mode lance directement le quiz, dernier mode mémorisé via `last_launch_mode`). Logique de toggle extraite dans `util/quiz/QuizTypePrefs.kt` (`object` sans état) ; `QuizzesPresenter` y délègue → `QuizzesPresenterTest` reste vert sans modification. Nouveaux `QuizTypeChip`/`LaunchModeRow` dans `StudyScreen.kt`. **Piège** : les `ic_*_selector.xml` (state-lists) sont inopérants en Compose → utiliser les paires `ic_*_check`/`ic_*_uncheck`. Strings `quiz_type_*`/`quiz_types_header`/`launch_mode_header` ajoutées en 6 langues. Tests : `QuizTypePrefsTest` (10 cas). **Retouches UI** : sheet — AUTO en pleine largeur au-dessus + 5 types manuels en grille 2 colonnes (chips taille égale `weight(1f)`, 38 dp, centrés) ; liste Study — chaque Content devient une card séparée (`SurfacePrimary`/`BorderDefault`/`RadiusMd`, espacée 8 dp, surbrillance `SurfaceAccent`+`AccentOrange` si sélectionnée) avec dégradé de fondu en bas de la `LazyColumn` pour adoucir la coupure vers le `FABBar`.
- ✅ **Session 3.10** : Retouches écran Quiz (`ui/quiz/QuizScreen.kt` + `QuizComponents.kt`) — (1) **Card de question réduite** : `Spacer(weight)` souple sous la card pour qu'elle ne dévore plus tout l'espace. (2) **Bloc QCM centré verticalement** entre card et bouton : deux `Spacer(weight 0.2f)` symétriques (au-dessus + en dessous des réponses) uniquement en mode QCM ; en mode édition/audio, un seul `Spacer(0.4f)` au-dessus. (3) **Bouton « Suivant » remonté** du bord : `padding(bottom = 12.dp)` + inset bas. (4) **Champ d'édition mis en avant** : entouré d'un liseré `AccentOrange` (1.5 dp, `RadiusMd`) sur fond `SurfacePrimary`, hauteur 56 dp ; soulignement natif de l'`EditText` supprimé (`background = null` sur `HiraganaEditText`) — fin du « trait orange » qui coupait le hint. (5) **Gestion clavier edge-to-edge** : `QuizActivity` passe en `android:windowSoftInputMode="adjustResize"` (manifest) + root Column consomme `WindowInsets.navigationBars.union(WindowInsets.ime)` via `windowInsetsPadding` → à l'ouverture du clavier la Column se réduit (plus de pan qui masquait la top bar), la card s'adapte à la hauteur restante. (6) **Barre de progression** : compteur `x / total` dédupliqué (suppression de celui de droite, conservé seulement en mode infini sans segments) ; compteurs ✓/✗ agrandis `8.sp → 12.sp` + `FontWeight.W600`.
- ✅ **Session dialogs** : Harmonisation de toutes les boîtes de dialogue au Design System v2, en deux niveaux. **Niveau 1** — thème global `YomikataAlertDialog` (`styles.xml`) + fond `drawable/bg_dialog.xml` (surface bleu nuit, bordure, `radius_xl`), branché sur `AppTheme` via `alertDialogTheme` : restyle **les 22 dialogs Splitties/AppCompat** sans toucher au Kotlin (titre/message/items aux tokens texte, bouton principal orange, autres en sourdine). **Niveau 2** — composant Compose réutilisable `ui/components/YomikataDialog.kt` (`YomikataDialog`/`YomikataDialogContent` + `DialogButton`/`DialogButtonStyle` Primary/Muted/Destructive + `DialogIcon`) et bridge impératif `ui/components/YomikataDialogHost.kt` (`Context.yomikataAlert` héberge le Compose dans un `ComposeView`, owners ViewTree câblés depuis la `ComponentActivity`). 9 dialogs migrés vers le composant : fin de quiz/session (`DialogFlowController`, **écran du screenshot**, logique auto-skip préservée), confirmations Quitter (`QuizFragment`/`QuizActivity`/`QuizzesActivity`), confirmations destructives réinit/suppression voix (`PrefsFragment`, bouton rouge). Dialogs riches restants (EditText création sélection, `ProgressBar`, liste `setItems`, erreurs BDD) laissés sur le thème Niveau 1.
- ✅ **Session progression** : Refonte de l'affichage de la progression sur Study + liste de mots. Nouveau composant partagé `ui/components/MasteryBar.kt` — une **barre de maîtrise segmentée unique** (vert `Correct` maîtrisés / ambre `MasteryMedium4` à revoir, piste `BorderDefault`, pill `RadiusPill`) + « X % maîtrisé » + total + légende optionnelle (`showLegend`). **Study** : les 3 barres empilées (`StudyProgressBars`/`ProgressRow`, dont la barre « Total » toujours pleine et redondante car `good+wrong==total`) → `StudyProgress` = carte surface + `SectionHeader` (`progress_header`) + `MasteryBar`. **Liste de mots** : le `TabRow` Material → `MasteryBar` compacte + 3 **filtres-pilules chiffrés** `MasteryFilterChip` (calqués sur `LevelChip`, mêmes indices `selectedTab` 0/1/2 → **aucune modif présenteur/repo**). Nouvelles chaînes en 6 langues : `progress_header`, `mastery_all`, `mastery_to_review`, `mastered`, `mastery_percent_label`, `word_count_label`. 2 buckets seulement (maîtrisés HIGH+MASTER vs à revoir LOW+MEDIUM ; les mots jamais vus restent en « à revoir »).
- ✅ **Session sélections** : Remise en place de la fonctionnalité Sélections (listes de mots utilisateur), cassée depuis la migration Compose (l'onglet `SelectionsFragment` était un placeholder « Bientôt disponible »). **Couche données inchangée** : une sélection = un `Quiz` `category = CATEGORY_SELECTIONS` (8), mots liés via `quiz_word` ; toutes les méthodes repository existaient déjà (`getQuiz`/`saveQuiz`/`deleteQuiz`/`updateQuizName`/`updateQuizSelected`/`addWordToQuiz`/`deleteWordFromQuiz`/`countWordsForQuizzes`). **UI** : nouveau `ui/selections/SelectionsScreen.kt` calqué sur `StudyScreen` (hero `ic_selections_big`, `MasteryBar` agrégée, liste de cartes à cocher avec compteur de mots + icône édition `ic_tooltip_edit`, carte « créer », état vide, `FABBar` + `LaunchOptionsSheet`). `SelectionsFragment(di)` réécrit en state-holder à la `QuizzesFragment` (injecte `QuizRepository`/`StatsRepository`/`prefs`, observe `getQuiz(CATEGORY_SELECTIONS)`, `WordCountPresenter` pour la maîtrise ; create/rename/delete via `createNewSelectionDialog`, `openContent`/`launchQuiz` identiques à Study mais `category = 8`). DI branchée dans `QuizzesActivity.navigateTo` (`SelectionsFragment(di)`). **`LaunchOptionsSheet` extrait** de `StudyScreen.kt` vers `ui/study/LaunchOptionsSheet.kt` (public, partagé Study + Sélections) avec `QuizTypeChip`/`LaunchModeRow`. **Bouton ★ liste de mots réparé** : `ContentFragment.onFavoriteClick` câblé à `showSelectionPicker` (le `ContentContract.Presenter` implémentait déjà `SelectionsInterface` + `WordInQuizInterface`). Aucune nouvelle chaîne (réutilise `no_selections`/`new_selection`/`add_to_selections`/`selection_edit`/`drawer_your_selections`/`word_count_label`). **Indicateur d'appartenance (étoile orange)** : nouvelle requête `WordDao.getWordIdsInQuizzesOfCategory(category)` → `WordRepository.getWordIdsInSelections(): Flow<List<Long>>` (lecture seule, pas de migration). Liste de mots — `ContentPresenter.wordsInSelections` (LiveData) → `WordListUiState.selectedWordIds` → `WordListRow.isFavorite` orange. Quiz — `QuizUiState.isCurrentWordInSelection` + `QuizFragment.refreshSelectionState()` (au changement de mot et après toggle, via `isWordInQuizzes`). Détail mot — `WordDetailUiState.isFavorite` + `refreshFavorite()`, et **bug corrigé** : le `PopupMenu` (anchor ComposeView, inopérant) supprimé. **Sélecteur unifié** : helper partagé `Fragment.showWordSelectionDialog(wordId, selectionsInterface, wordInQuizInterface, onChanged)` (`util/SelectionCreation.kt`) — dialog `setMultiChoiceItems` (cases à cocher reflétant l'appartenance à chaque liste, toggle = add/remove immédiat) + bouton « nouvelle sélection » ; si aucune liste, va direct à la création. Utilisé par Quiz/Content/Détail (fin de la duplication `showSelectionMenu`/`showSelectionPicker`/`addSelection`).
- ✅ **Session nav-flottante** : Refonte de la navigation principale en **barre flottante Material 3** façon Telegram, mais en palette Yomikata. Nouveau composant `ui/components/YomikataFloatingNavBar.kt` (remplace `YomikataBottomBar`, conservée pour référence) : pill **large** détachée des bords (`SurfacePrimary` + bordure `BorderDefault`, `RadiusPill`, ombre), 4 items répartis en `weight(1f)` avec **libellés conservés**. Item actif → highlight pill orange translucide (`SurfaceAccent` + `BorderAccent`) **animé** (`animateColorAsState` sur fond/icône/label + `animateDpAsState` sur le padding). Le contenu passe **sous** la barre et s'**estompe derrière** via un `Brush.verticalGradient` (transparent → `BackgroundPrimary`). **Layout** : `activity_quizzes.xml` passe de `LinearLayout` empilé à `FrameLayout` plein écran où le `FragmentContainerView` remplit l'écran et le `ComposeView` de nav flotte en overlay (`layout_gravity=bottom`). **Helper partagé** : `FloatingNavBarHeight` (82dp) + `floatingNavBarBottomPadding()` (= hauteur + inset `navigationBars`) ; appliqué en bas de `StudyScreen`/`SelectionsScreen` (FABBar + état vide), `HomeScreen` (Spacer final) et de la `RecyclerView` de `PrefsFragment` (`clipToPadding=false`) pour que rien ne soit masqué. `QuizzesActivity` non edge-to-edge → inset système = 0, la pill flotte au-dessus de la barre OS. Réutilise l'enum `BottomNavDestination` → **zéro impact** sur `navigateTo()`.
- ✅ **Session fixes-quiz** : Deux bugs du Quiz corrigés. **(1) Accents manquants dans les QCM** : `removeAnyJapanese()` (`util/language/TranslationParser.kt`) supprimait tout caractère non-ASCII via `\P{InBasicLatin}` → les lettres accentuées (é, è, ä, ñ, ã, ç…) disparaissaient de la question et des propositions (`cleanForQCM(false)`). Remplacé par un ciblage des seules plages japonaises (`\p{IsHiragana}\p{IsKatakana}\p{IsHan}` + blocs CJK), constante `japaneseChars`. Corrige FR/DE/ES/PT (limite ZH connue, voir Piège). 5 cas de test ajoutés à `TranslationParserKtTest`. **(2) Barre de progression réinitialisée après revue d'erreurs** : `QuizContract.View.displayWords` prend désormais un `WordDisplayMode` (`Fresh`/`ErrorSession`/`ResumeNormal`). `onLaunchErrorSession` → `ErrorSession` (snapshot des segments principaux dans `QuizFragment.normalModeSegments`, segments neufs pour les erreurs) ; `onContinueQuizAfterErrorSession` → `ResumeNormal` (restaure le snapshot). Avant, `displayWords(quizWords)` repassait tous les segments à `Pending` → barre à 0 ✓ 0 ✗ malgré la position conservée.
- ✅ **Session voix** : Réintégration du point d'entrée de **téléchargement des packs de voix**, perdu à la migration Compose. Le backend était **intact** (lecture `audio/VoicesManager.kt` voix MP3→fallback TTS via `checkSpeechAvailability` ; téléchargement Firebase `launchVoicesDownload` ; suppression `PrefsFragment`) mais le seul accès était `speechNotSupportedAlert()`, jamais montré aux utilisateurs ayant un TTS. **Card sur Study** (`ui/study/VoiceDownloadCard.kt`, calquée sur `StudyProgress` : `SurfacePrimary`/`BorderDefault`/`RadiusMd`, `SectionHeader` `voices_card_title`) insérée sous la progression, **par catégorie sélectionnée** : icône haut-parleur + état (`DiscreetActionButton` « Télécharger (X Mo) » via `download_voices_action` / check `Correct` si présent / `LinearProgressIndicator` + % en cours). **Refactor** `util/ActionsUtils.kt` : extraction du cœur Firebase dans `downloadVoices(activity, level, onProgress, onComplete)` (sans UI) ; `launchVoicesDownload(...)` réécrit comme wrapper dialog Splitties qui y délègue (chemin fallback inchangé). **État** : 3 champs `StudyUiState` (`voicesDownloaded`/`voiceSizeMb`/`voiceDownloadProgress`), `onDownloadVoices` ajouté à `StudyScreen`. **Wiring** `QuizzesFragment` : `refreshVoiceState(category)` (via `anyVoicesDownloaded`/`getCategoryLevel`/`getLevelDownloadSize`) appelé dans `onCreate` + `selectCategory` ; `startVoiceDownload()` met à jour la progression dans l'état. Une fois les MP3 présents, toute l'app les lit sans autre modif. Strings (6 langues) : `voices_card_title`/`voices_card_subtitle`/`voices_downloaded`/`voices_downloading`. Pas de migration BDD. **Source des packs migrée Firebase → HTTPS** : le bucket Firebase `yomikataz.appspot.com` renvoyait `HTTP 402` (facturation Spark/Blaze). `downloadVoices` télécharge désormais via `HttpURLConnection` (thread de fond, callbacks reposés sur le main) depuis `R.string.url_voices_base` (`https://github.com/jehutyno/yomikata/releases/download/voices/`) + `Voices_level_{level}.zip`. HTTPS obligatoire (pas de `usesCleartextTraffic`). Dépendance `firebase-storage` plus utilisée nulle part (retirée du bundle `firebase` lors de la session de nettoyage de code mort). **Pré-requis hébergement** : créer la release GitHub tag `voices` avec les 7 assets `Voices_level_0.zip`..`Voices_level_6.zip` (mêmes zips qu'avant : mp3 `w_*`/`s_*` à la racine de l'archive).
- ✅ **Session hero edge-to-edge** : Correction du edge-to-edge des bandeaux (hero) sur Home/Study/Sélections + homogénéisation. **(1) Logo passant sous la status bar** : la `Column` de contenu (logo + titre) des trois hero utilisait `fillMaxSize()` + `Arrangement.Center` → centrage sur toute la hauteur du `Box`, status bar incluse, donc le logo remontait dessous. Ajout de `.statusBarsPadding()` **sur la seule Column de contenu** (le fond `KenBurnsImage` + scrim restent en `fillMaxSize()` → effet edge-to-edge conservé). **(2) Homogénéisation Home ← Study** : `HomeHero` aligné sur `StudyHero` — hauteur `240.dp → 180.dp`, fond `BackgroundHeroWarm → BackgroundHero`, scrim chaud → scrim froid `listOf(Color(0xA6000A14), Color(0xCC000A14))`, logo `96.dp → 80.dp`. Sous-titre `学ぶ · 読む · 理解する` (couleur `AccentWarm`) conservé. **(3) Hauteur commune** : les **trois** hero passent à `180.dp` (était 160) pour redonner de l'air au sous-titre de Home sans le coller au bord bas — règle : tous les bandeaux gardent la même hauteur.
- ✅ **Session fixes-langue+splash** : Deux correctifs. **(1) Home vide après changement de langue** : `AppCompatDelegate.setApplicationLocales()` recrée `QuizzesActivity` (config change) ; le système passe alors un `savedInstanceState` **non-null**, donc le garde `if (savedInstanceState == null) navigateTo(HOME)` était sauté et aucun fragment n'était ajouté. Comme `onSaveInstanceState` saute volontairement `super` (aucun état de fragment persisté), le FragmentManager ne restaurait rien non plus → page blanche. Remplacé par un garde fiable : `if (supportFragmentManager.findFragmentById(R.id.study_fragment_container) == null) navigateTo(HOME)` (robuste lancement à froid **et** recréation). **(2) Splash trop clair + disque blanc** : l'app est dark-only mais `SplashTheme` (`styles.xml`) utilisait `windowSplashScreenBackground = @color/colorPrimary` (= `#f6f6f6` en mode clair, sombre seulement en `values-night`) → splash blanc sur device en mode clair ; pointé sur `@color/color_bg` (`#0a0e17`, pas de variante night → cohérent). Le **disque blanc** autour du logo venait de l'icône de lanceur héritée (PNG plat `@mipmap/ic_launcher` avec cercle blanc incrusté) utilisée par défaut comme icône de splash Android 12+. Nouveau drawable `res/drawable/splash_icon.xml` (`<inset>` 18 % de `@drawable/yomi_logo`, logo spirale+読 sur **fond transparent**) branché via `windowSplashScreenAnimatedIcon` → logo posé directement sur le fond sombre, sans disque.
- ✅ **Session pos-noms-complets** : Les chips POS (catégorie grammaticale) de la liste de mots (`WordListRow.kt`, 1er token) et du détail (`WordDetailScreen.kt`, 3 tokens) affichaient des abréviations peu lisibles (« N », « V », « Adj-i »…) via deux mappings privés dupliqués. Centralisé dans un helper partagé `ui/word/PosChipStyle.kt` (`posChipColor` / `posChipLabelRes` / `posChipLabel`) qui mappe chaque token JMdict vers un **nom complet traduit** + une couleur par grande famille (bleu=nom, orange=verbe, vert=adjectif, violet=adverbe, gris=autre). **Granularité détaillée** : variantes de verbes (Intransitif/Transitif/Ichidan/Godan/Suru/Zuru/Auxiliaire) et d'adjectifs (Adj. -i/-na/-no/-taru) distinctes. Mapping aligné sur l'ancien `WordPagerAdapter` (XML, non modifié). **Strings** : les `R.string.pos_*` existaient déjà ; ajout des 8 sous-types manquants (`pos_verb_ichidan/godan/suru/zuru`, `pos_adj_i/na/no/t`) en FR/DE/ES/PT — termes japonais romanisés (`Ichidan`/`Godan`/`Suru`/`Zuru`) + format `Adj. -i/-na/-no/-taru`, identiques en écriture latine (EN/ZH les avaient déjà). Aucune migration BDD.
- ✅ **Session fr-pos-cleanup** : Nettoyage des POS résiduels dans le champ `french` de `words`. Les traductions françaises portaient les mêmes préfixes POS JMdict que l'anglais (ex. `(adj-na)(n) déplorable`, `(adv) particulièrement`) ; comme le POS est désormais déporté dans la colonne `pos`, ils étaient redondants. **Stratégie** (validée hors-ligne via perl, ASCII-only, byte-safe pour les accents) : retirer **uniquement les groupes en tête** composés **exclusivement** de tokens POS whitelistés (`^(?:\s*\(\s*(?:TOKEN)(?:\s*,\s*(?:TOKEN))*\s*\))+\s*`, `IGNORE_CASE`). Le leading-only + whitelist préserve le contenu légitime entre parenthèses : en tête (`(compteur de jours)`, `(ma) femme`, `(indique la nationalité)`) comme en milieu de chaîne (`(crayon)`, `(à quelqu'un)`, `(bureau de) réception`). 5990/7503 mots nettoyés, **aucun ne devient vide**. Whitelist plus large que l'anglais (le FR a `adj`, `uk`, `conj`, `vs-s`, `hon`, `adj-pn`, `vulg`, `pop`, `col`, `sl`, `gram`, `arch`… + `su`/`s` qui n'apparaissent que dans 2 groupes malformés `(su,ctr)`/`(vs,s,vi)`). **Deux entrées sources malformées** (`)` manquant : ids 6526 `(adj-na,nà…`, 6798 `(adj-na,n,adj-no …`) corrigées par UPDATE explicite (guardé par match exact, idempotent). **Intégration** : pas de nouvelle migration (aucune version publiée depuis v16 — prod APK code 65) → logique ajoutée à **`MIGRATION_16_21`** (utilisateurs v16) ET à son maillon granulaire **`MIGRATION_19_20`** (cohérence chaîne/tests), inline et auto-contenue (pattern de duplication existant). **Asset** `yomikataz.db` nettoyé du même coup (UPDATEs générés par perl, `.read` UTF-8 NoBOM, `PRAGMA user_version` reste 21, `integrity_check` ok). Pas de changement de schéma → `room_master_table`/identity_hash inchangés. Tests `RoomMigrationTest` (`migrate19To20`, `migrate16To21`) enrichis : strip FR en tête + préservation des parenthèses de contenu + cas malformé 6526.
- ✅ **Session sponsor-firebase** : La card « Soutenir le projet » de la Home est désormais pilotée à distance via deux entrées Firebase Realtime DB lues à la racine (même base que les news) : `sponsor_available` (booléen → la `SupportCard` n'est rendue que si `true`, masquée par défaut le temps que GitHub Sponsors soit validé) et `sponsor_url` (string → URL ouverte au clic, libre de service). `HomeUiState` enrichi de `sponsorAvailable`/`sponsorUrl` ; `HomeFragment.onViewCreated` ajoute deux `addListenerForSingleValueEvent` ; `SupportCard` enveloppée dans `if (state.sponsorAvailable)`. Texte du bouton « ♥ Soutenir sur GitHub » → **« Soutenir »** (neutre). Nouveau helper `openSponsorUrl(context, url)` (`util/ActionsUtils.kt`, no-op si vide) remplace `openGithubSponsors` dans `onSupportClick` (l'ancien helper est conservé mais plus appelé depuis la Home). Aucune migration BDD, aucune nouvelle chaîne (libellés Home hardcodés FR).
- ✅ **Session content-launch-harmonisé** : Le lancement de quiz depuis une liste de mots (`ContentActivity`) utilisait encore le FAB design V1 (`FloatingActionsMenu` getbase à 3 boutons progressif/normal/aléatoire). Harmonisé avec l'écran Study : bouton unique **« Lancer le quiz »** (`FABBar`, nouvel état `FABBarState.Launch`) qui ouvre la `LaunchOptionsSheet` partagée (types de quiz + mode de lancement). `activity_content.xml` : `FloatingActionsMenu` → `ComposeView` `launch_bar` (`layout_gravity=bottom`). `ContentActivity` : suppression du câblage V1 (3 `setOnClickListener`, `collapseOrQuit`, back `OnBackInvokedDispatcher`/`addCallback` — la `ModalBottomSheet` gère son propre back) ; nouveau composable privé `ContentLaunchBar` (FABBar + sheet) ; état Compose `launchTypes`/`lastMode` réactif, toggle via `QuizTypePrefs.toggle` (AUTO-exclusif comme Study), `launchQuiz` mémorise `LAST_LAUNCH_MODE`. `LaunchOptionsSheet` reçoit un param `showProgressive: Boolean = true` → Content passe `level == null` pour masquer le mode Progressif sur une revue par niveau (comportement V1 préservé). Fonctionne en mode ViewPager2 (le bouton suit le quiz de la page courante) comme en mode level. **Dépendance supprimée** : `libs.fab` (`com.github.jehutyno:android-floating-action-button`), seul usage de la lib getbase, retirée de `app/build.gradle` et du version catalog.
- ✅ **Session analytics-firebase** : Intégration de **Firebase Analytics + Crashlytics** pour le suivi d'usage produit (distinct des stats locales `StatsRepository`), avec **consentement RGPD opt-in strict**. **Deps** : `firebase-analytics`/`firebase-crashlytics` ajoutés au bundle `firebase` (BoM 34.0.0) + plugin `com.google.firebase.crashlytics` (3.0.2) dans le version catalog, `build.gradle` racine (apply false) et `app/build.gradle`. **Helper central** `util/analytics/Analytics.kt` (`object`) : encapsule `FirebaseAnalytics`/`FirebaseCrashlytics`, centralise tous les noms d'events/params (aucune string en dur dispersée), n'envoie **aucune donnée personnelle** (slugs de catégorie stables indépendants de la langue, enums, compteurs). **Piège tests JVM** : le `Bundle` (classe Android non mockée → RuntimeException) ne doit être construit que si Firebase est initialisé → `private inline fun log(event, build: Bundle.() -> Unit)` qui early-return sur `firebaseAnalytics == null` (sinon 12 tests `QuizPresenterTest` rouges). **Consentement** : 2 clés `Prefs` (`ANALYTICS_CONSENT` + `ANALYTICS_CONSENT_ASKED`), collecte **OFF par défaut** via 3 `meta-data` du manifeste (`firebase_analytics_collection_enabled`/`firebase_crashlytics_collection_enabled`/`google_analytics_default_allow_analytics_storage` = false), `Analytics.init` au démarrage applique l'état persisté, `setConsent` réactive dynamiquement les deux drapeaux Firebase. **UI** : dialogue opt-in au 1er lancement (chaîné après le tuto d'accueil dans `QuizzesActivity.tutos()`, via `yomikataAlert`), toggle `CheckBoxPreference analytics_consent` dans `preferences.xml`/`PrefsFragment` (route via `Analytics.setConsent`). **Events** posés à côté des `addStatEntry` existants : `quiz_launched` (Study + Selection : source/category-slug/level/strategy/types), `selection_created`/`selection_deleted`, `quiz_completed` (sessionLength/strategy/errorCount, dans `QuizPresenter` fin de session). Fréquence d'ouverture = auto (Firebase `first_open`/`session_start`/DAU). **Strings** consentement en 6 langues. **Secrets / google-services.json** : config client (pas un secret), mais rangé **par build-type** (le plugin google-services préfère `app/src/<buildType>/` à la racine) — `app/src/debug/google-services.json` (client `.debug`, SHA-1 debug `a4abf6f2…`, app id `…7215bd4b`) et `app/src/release/google-services.json` (client release `com.jehutyno.yomikata`, SHA-1 release `8158cbf6…`, app id `…2f355f1b`) ; l'ancien `app/google-services.json` racine **supprimé** (`git rm`). `.gitignore` durci contre les service-account keys (`*-firebase-adminsdk-*.json`). **Politique de confidentialité** : `PrivacyPolicyYomikataZ.html` (racine du repo) réécrit en version RGPD complète (Analytics/Crashlytics opt-in, base légale = consentement, sous-traitant Google, rétention, droits) ; URL externalisée dans `strings.xml` (`url_privacy_policy`, non localisée) → `https://jehutyno.github.io/yomikata/PrivacyPolicyYomikataZ.html` ; `PrefsFragment` clé `privacy` pointe désormais sur cette string (fin du lien rawgit mort). **Pré-requis hors code** : activer **GitHub Pages** sur `jehutyno/yomikata` (branche `main`, dossier `/`) pour servir la page ; côté console Firebase : vérifier le lien GA4 (`measurement_id`), activer Crashlytics, régler rétention/anonymisation IP. Pas de migration BDD.
- ✅ **Session nettoyage-code-mort** : Vérification des sources et suppression de code mort + imports inutilisés (aucun outil d'analyse statique configuré — sweep ponctuel via warnings du compilateur). **Orphelins supprimés** (0 référence) : composable `YomikataBottomBar` (remplacé par `YomikataFloatingNavBar` ; l'enum `BottomNavDestination` qu'il hébergeait a été **déplacée** dans `YomikataFloatingNavBar.kt`), `util/SeekBarsManager.kt`, adapters pré-Compose `QuizzesAdapter` (+ layouts `vh_quiz.xml`/`vh_new_selection.xml`) et `QuizItemPagerAdapter` (+ `vh_quiz_item.xml`), fonction `openGithubSponsors` (`ActionsUtils.kt`, remplacée par `openSponsorUrl`). **Dépendance** : `firebase-storage` retirée du bundle `firebase` et du version catalog (migrée HTTPS en session voix). **Sous-système `search` supprimé** (feature abandonnée, injoignable) : 4 fichiers `screens/search/*` + entrées manifest (`<activity SearchResultActivity>` et meta-data `default_searchable` cassée pointant vers une classe inexistante `SearchResultsActivity`) + layouts `activity_search.xml`/`activity_search_results.xml` + `menu/menu_search.xml`. **Imports inutilisés** : 20 retirés dans 9 fichiers (`ComposeView`, `TutoId`, `Parcelable`, `Color`, 7× `Mastery*` dans `MasteryDots.kt`, `width`/`HorizontalDivider`/`BorderSubtle` dans `QuizScreen.kt`, `BackgroundNav`, `StringRes`, `Box`, `Arrangement`/`TypeMicro` dans `WordListRow.kt`, `View`) — `kotlinc` ne signale pas les imports inutilisés, détectés par scan heuristique (exclusion des noms implicites : délégués/opérateurs). **Garde** : `R.menu.popup_copy` conservé (partagé avec le vivant `WordPagerAdapter`). `assembleDebug` + `test` verts. Pas de migration BDD.
- **Phase 5** : Tests intégrité BDD (`DatabaseIntegrityTest`)

### Couverture traductions (état actuel)
| Langue | Mots (7 503) | Phrases (7 425) | Source mots |
|---|---|---|---|
| Allemand (DE) | 100% | 100% | JMdict + traduction manuelle Claude |
| Espagnol (ES) | 100% | 100% | JMdict (1 932) + traduction manuelle Claude (5 571) |
| Portugais (PT) | 100% | 100% | Traduction manuelle Claude |
| Mandarin (ZH) | 100% | 100% | Traduction manuelle Claude |

### Scripts disponibles
- `extract_jmdict_de.ps1` / `extract_jmdict_es.ps1` / `extract_jmdict_pt.ps1` — extraction JMdict par langue
- `update_quiz_de.ps1` / `update_quiz_es.ps1` / `update_quiz_pt.ps1` — noms de quiz localisés
- `translate_sentences.ps1` — traduction des phrases via Haiku 4.5 (7 425 phrases × 4 langues, 100% complet)
- JMdict.xml mis en cache dans `$env:TEMP` — pas besoin de re-télécharger

---

## Règle systématique

**À chaque fin de session de refactoring ou d'ajout de fonctionnalité :**
1. Mettre à jour `ARCHITECTURE.md` (structure, version DB, tests, couverture)
2. Mettre à jour `CLAUDE.md` si une convention ou commande a changé
3. Puis `/compact`

## Design System

Le design system de Yomikata est documenté dans `DESIGN.md` à la racine.
**Toujours consulter DESIGN.md avant de créer ou modifier un composant UI.**

### Règles absolues
- Aucune couleur hardcodée. Toujours utiliser les tokens de `Color.kt`.
- Aucun radius arbitraire. Utiliser les valeurs de `Shape.kt` (RadiusXs→RadiusXl).
- Aucun rouge sur les kanji ou furigana (rouge = mauvaise réponse uniquement).
- Toujours inclure une `@Preview` dark mode sur les composants Compose.
- Les section headers sont bilingues : format `"JP · EN"`.

### Fichiers design
- `app/src/main/java/fr/yomisuite/yomikata/ui/theme/Color.kt`
- `app/src/main/java/fr/yomisuite/yomikata/ui/theme/Theme.kt`
- `app/src/main/java/fr/yomisuite/yomikata/ui/theme/Type.kt`
- `app/src/main/java/fr/yomisuite/yomikata/ui/theme/Shape.kt`