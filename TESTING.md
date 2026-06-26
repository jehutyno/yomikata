# TESTING.md — Stratégie de test & passe de pré-production

Ce document décrit **comment Yomikata Z est testé** et **comment dérouler une passe de tests avant chaque mise en production**. Il est versionné exprès : la [checklist de pré-release](#checklist-de-pré-release) est à rejouer à chaque version.

> Pièges techniques détaillés (Room, Roborazzi, encodage) : voir `CLAUDE.md` § Tests.
> Inventaire des classes de test : voir `ARCHITECTURE.md` § Tests.

---

## TL;DR — lancer toute la suite

```powershell
$env:JAVA_HOME = "C:\Users\valen\AppData\Local\Programs\Android Studio\jbr"

# Couche 1 — unitaires JVM (~15 s, sans device)
.\gradlew.bat test

# Couche 3 — screenshots (JVM, sans device) : gate de régression visuelle
.\gradlew.bat verifyRoborazziDebug

# Couches 2 & 4 — instrumentation + Compose UI + E2E (émulateur lancé)
.\gradlew.bat connectedDebugAndroidTest

# Build de release (gate final)
.\gradlew.bat assembleRelease
```

Ordre recommandé : du plus rapide au plus lent. **S'arrêter au premier rouge.**

---

## Stratégie — pyramide en 5 couches

On maximise la logique testée sur la JVM (rapide, déterministe), on réserve l'émulateur aux comportements vraiment intégrés, et on garde le test manuel au strict minimum irréductible (audio, réseau réel, services Google, pickers système).

| Couche | Quoi | Outil | Device ? | Statut |
|---|---|---|---|---|
| **1** | Logique pure (presenters, validateurs, mappings, fallback traductions) | JUnit 4 + MockK | non | ✅ 147 tests, 13 classes |
| **2** | Room : DAOs, migrations, **intégrité de l'asset** | Room Testing + AndroidJUnit | émulateur | ✅ DAOs + migrations + `DatabaseIntegrityTest` |
| **3** | Régression visuelle des écrans Compose | Roborazzi + Robolectric | non (JVM) | ✅ 20 baselines (8 écrans + composants) × EN/DE |
| **4** | Interactions Compose + smoke E2E bout-en-bout | Compose UI Test + Espresso | émulateur | 🟡 démarré (nav, quiz, smoke) |
| **5** | Irréductible manuel (audio, réseau, GA4, SAF, 1er lancement) | checklist IA-assistée | device réel | 🔁 à chaque release |

---

## Couche 1 — Tests unitaires JVM

- **Où** : `app/src/test/kotlin/com/jehutyno/yomikata/…`
- **Lancer** : `.\gradlew.bat test` (résultats : `app/build/test-results/testDebugUnitTest/*.xml`)
- **Couvre** : `QuizPresenter`, `AnswerValidator`, `QuizSessionState`, `LevelSystem`, `LanguageManager`, `QuizTypePrefs`, `TranslationParser`, fallback `getTrad()`/`getName()` 6 langues, `RandomAnswerGenerator`, `WordStatisticsRecorder`, furigana (`ActionsUtils`), `Categories`.

### Ajouter un test (couche 1)
1. La cible doit être de la **logique pure** (pas de `Context`/`View`/Compose). Si c'est couplé Android, ça relève de la couche 2 ou 4.
2. Mocker les repos / `SharedPreferences` avec **MockK** ; `InstantTaskExecutorRule` pour tout `LiveData` ; `runBlocking`/`UnconfinedTestDispatcher` pour les `suspend`.
3. Mocker `Random` via `object : Random() { override fun nextInt(bound: Int) = … }` (pas `mockk<Random>()`).
4. **Pièges JVM** : pas de Robolectric ici → `android.util.Log` et la construction de `Bundle` lèvent « not mocked » ; ne tester que les chemins qui n'y touchent pas. Les noms de méthodes en backticks ne peuvent pas contenir `.`/`:` (`0..6` interdit).

---

## Couche 2 — Room (DAOs, migrations, intégrité asset)

- **Où** : `app/src/androidTest/kotlin/com/jehutyno/yomikata/…`
- **Lancer** : émulateur up, puis `.\gradlew.bat connectedDebugAndroidTest`
- **Couvre** : 5 DAOs (DB in-memory), `RoomMigrationTest` (14→21, chemin prod 16→21), `OldMigrationTest` (13→14), et **`DatabaseIntegrityTest`** qui ouvre l'**asset réel** `yomikataz.db` et vérifie versions, `integrity_check`, couverture 6 langues (0 colonne vide), absence de `0x3F`, `identity_hash` == `schemas/21.json`, colonne `pos`.

### Ajouter une migration → tests obligatoires
À chaque nouvelle migration (voir `CLAUDE.md` § Règles migration) :
1. Ajouter `MIGRATION_X_Y` à `ALL_MIGRATIONS` dans `RoomMigrationTest`.
2. Ajouter un test `migrateXtoY()` (données synthétiques en v X → assertions en v Y).
3. Mettre à jour les seuils/colonnes attendus dans `DatabaseIntegrityTest` si le schéma ou les données changent (et le hash `EXPECTED_IDENTITY_HASH`).
4. **Piège Room 2.7** : une migration qui crée `room_table_modification_log` doit passer `validateDroppedTables = false` à `runMigrationsAndValidate` (sinon faux positif « Unexpected table »).

---

## Couche 3 — Screenshots Roborazzi (JVM, sans émulateur)

- **Où** : tests dans `app/src/test/kotlin/com/jehutyno/yomikata/screenshot/`, baselines PNG committées dans `app/src/test/screenshots/`.
- **Enregistrer / vérifier** :
  ```powershell
  .\gradlew.bat recordRoborazziDebug   # (ré)enregistre les baselines (1ère fois / changement voulu)
  .\gradlew.bat verifyRoborazziDebug   # gate de régression : échoue à toute dérive pixel
  ```
- **Couvre** : Home, Study, Quiz (QCM avant/bonne réponse), Selections, WordList, WordDetail, AnswerReview, + composant `MasteryBar` — chacun en **EN et DE**.

### Ajouter un écran/état (couche 3)
Patron : voir `MainScreensScreenshotTest` / `RemainingScreensScreenshotTest`.
1. Construire une **fixture `UiState`** (pas de BDD/réseau) et appeler le composable d'écran.
2. Utiliser le helper `capture(name) { … }` : il enveloppe dans `CompositionLocalProvider(LocalInspectionMode provides true)` + `YomikataTheme` + `Box(Modifier.size(411.dp, 891.dp))`.
3. Variante de langue : `RuntimeEnvironment.setQualifiers("+de")` (resp. `+es`, `+pt`, `+zh`) avant `capture`.
4. `recordRoborazziDebug`, **relire les PNG** (revue visuelle : troncatures, accents amputés, rouge interdit sur kanji/furigana), puis committer les baselines.

**Pièges de ce stack** (détaillés dans `CLAUDE.md` § Tests de screenshot) :
- `@Config(sdk = [36], application = android.app.Application::class)` (Robolectric plafonne à SDK 36 ; éviter le bootstrap Firebase/DI).
- API content-lambda `captureRoboImage("…") { }`, **pas** `createComposeRule()`.
- **Geler les animations infinies** sous `LocalInspectionMode` (sinon un seul écran prend ~36 min au lieu de ~5 s).
- **Borner** la capture (`Box.size(411.dp,891.dp)`) sinon OOM bitmap sur les écrans `fillMaxSize`/`LazyColumn`.

---

## Couche 4 — Interactions Compose + smoke E2E

- **Où** : `app/src/androidTest/kotlin/com/jehutyno/yomikata/compose/` (émulateur).
- **Lancer** : `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.package=com.jehutyno.yomikata.compose"`
- **Fait** :
  - `FloatingNavBarInteractionTest` — taper une destination invoque le callback avec le bon enum ; 4 destinations rendues.
  - `QuizScreenInteractionTest` — taper une option QCM invoque `onOptionClick(index)` ; 4 options affichées.
  - `AppLaunchSmokeTest` — la vraie `QuizzesActivity` atteint RESUMED sans crash (intègre DI + chargement asset DB + Firebase + host Compose).
- **Reste** (backlog) : saisie édition + IME, barre de progression conservée après revue d'erreurs, CRUD sélections, changement de langue (recréation d'activité), `DialogFlowController` (auto-skip), E2E complet Study → quiz → Answers.

### Ajouter un test d'interaction (couche 4)
1. `@get:Rule val rule = createComposeRule()` ; `rule.setContent { YomikataTheme { … } }` avec une fixture `UiState` + des callbacks-espions.
2. Sélecteurs **stables et indépendants de la locale** : préférer les libellés non localisés (kana des options QCM, libellés nav « Home/Study/… »). Éviter les chaînes `stringResource` (varient selon la langue de l'émulateur).
3. `rule.onNodeWithText("…").performClick()` puis asserter l'effet (callback reçu, état affiché). Compose fusionne le texte descendant dans le nœud cliquable parent → cliquer le texte déclenche le `onClick`.
4. **Deps** : `androidTestImplementation` `platform(compose-bom)` + `compose-ui-test-junit4`, `debugImplementation compose-ui-test-manifest` (fournit l'Activity de `createComposeRule`).

---

## Couche 5 — Checklist manuelle IA-assistée

Ce qui dépend de réseau réel, audio audible, services Google ou pickers système — non fiable à automatiser. À piloter via `adb` + MCP computer-use + skill `/verify` (captures asserties par l'IA ; humain dans la boucle pour le son). Voir la [checklist de pré-release](#checklist-de-pré-release) ci-dessous.

---

## Checklist de pré-release

> **À dérouler intégralement avant chaque publication.** Cocher au fur et à mesure.

### A. Gates automatisés (s'arrêter au premier rouge)
- [ ] `.\gradlew.bat test` — unitaires JVM verts
- [ ] `.\gradlew.bat verifyRoborazziDebug` — aucune dérive visuelle (sinon : régression, ou `record` + revue si le changement est voulu)
- [ ] `.\gradlew.bat connectedDebugAndroidTest` — DAOs, migrations **et `DatabaseIntegrityTest`** verts (émulateur)
- [ ] `.\gradlew.bat assembleRelease` — build R8/ProGuard réussit

### B. Cohérence données & config (en partie couvert par `DatabaseIntegrityTest`)
- [ ] `DATABASE_VERSION` (`YomikataDatabase.kt`) == `PRAGMA user_version` de l'asset == dernier `app/schemas/*.json`
- [ ] `versionCode` / `versionName` incrémentés (`app/build.gradle`)
- [ ] `res/xml/locale_config.xml` liste EN/FR/DE/ES/PT/ZH
- [ ] `google-services.json` présents en `app/src/debug/` **et** `app/src/release/`
- [ ] Release GitHub tag `voices` : 7 assets `Voices_level_0.zip`..`6.zip` accessibles en HTTPS
- [ ] Page de confidentialité servie (GitHub Pages) à l'URL `url_privacy_policy`

### C. Manuel sur device réel (couche 5)
- [ ] **1er lancement** : splash sombre (pas de disque blanc) → tutos welcome+catégories → dialog consentement analytics (1×)
- [ ] **Voix — téléchargement** : card Study par niveau → DL réel → barre de progression → check vert
- [ ] **Voix — lecture** : audio **audible** (mot et phrase) ; **TTS** parle en l'absence de pack
- [ ] **Firebase** : Home charge `news_xx` ; `sponsor_available`/`sponsor_url` pilotent la SupportCard
- [ ] **Analytics réel** : consentir → events `quiz_launched`/`quiz_completed` arrivent dans GA4 ; refuser → rien
- [ ] **Backup / Restore** : backup `.db` via SAF ; restore → recharge + migration + validité
- [ ] **Réglages** : furigana on/off, longueur quiz (5–50), vitesse, taille police impactent le quiz
- [ ] **Clavier edge-to-edge** en quiz édition : la Column se réduit, la top bar reste visible
- [ ] **Changement de langue** (Settings) : activité recréée, Home non vide, strings traduites
- [ ] **Build release signé** (AAB) : démarre, quiz tourne, pas de crash d'obfuscation

---

## Bonnes pratiques — test IA-assisté

1. **Pyramide, pas pyramide inversée** : logique pure d'abord (couche 1), émulateur pour l'intégré (2/4), manuel au minimum (5).
2. **Le screenshot comme oracle de régression** : l'IA valide le baseline une fois (lecture des PNG), la machine garde le pixel ensuite. Idéal pour une app multi-langues dark-only.
3. **Fixtures de state plutôt que BDD/réseau** : les écrans prennent un `UiState` → générer des cas représentatifs (vides, longues traductions DE, CJK).
4. **Générer les cas depuis les contrats** : pointer l'IA sur `QuizContract`/presenters/validateurs pour dériver les cas limites.
5. **Piloter le device avec l'IA** (couche 5) : `adb` + computer-use + `/verify`, captures asserties par l'IA ; humain seulement pour le son.
6. **Matrice systématique** `{langues} × {dark} × {écrans}` : ne pas tester une langue et supposer les autres.
7. **Tout bug trouvé → un test de régression** dans la couche adéquate **avant** le fix.

---

## Lacunes connues (backlog de test)

- **Couche 4** : socle posé (nav, quiz, smoke) ; reste édition/IME, progression post-erreurs, CRUD sélections, changement de langue, `DialogFlowController`, E2E complet.
- Matrice screenshot limitée à **EN/DE** : étendre à FR/ES/PT/ZH (1 ligne par variante).
- `DialogFlowController` non couvert (couplé `Fragment`/dialog) → couche 4.
- Stubs vides dans `WordDaoTest` (`updateWord`, `addWord`, …) à compléter.
- Pas de CI : les gates sont locaux. Candidats idéaux pour un `on: push` : couches 1 et 3 (JVM, sans émulateur).
