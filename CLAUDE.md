# CLAUDE.md : instructions pour Claude Code

Ce fichier est lu automatiquement par Claude à chaque session. Il contient les conventions, commandes et règles spécifiques au projet Yomikata Z.

Pour la documentation technique complète, voir `ARCHITECTURE.md` ; l'état du projet est dans `ETAT.md`, l'historique des sessions dans `JOURNAL.md`.

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

# APK release signé minifié (R8 + shrinkResources), teste le build prod localement
.\gradlew.bat assembleRelease
```

### Minification (R8) : build release uniquement
- `minifyEnabled true` + `shrinkResources true` sur le build-type `release`. Les effets de R8 (obfuscation, shrink ressources) **ne se voient qu'en release**, toujours valider sur un `assembleRelease` réel, pas en debug.
- Règles dans `app/proguard-rules.pro`. La plupart des libs (Room, Compose, Firebase, Media3, Kodein 7.x, AndroidX) embarquent leurs consumer rules → garder ce fichier minimal. Pièges déjà couverts : vues custom XML des libs jitpack (règle `extends android.view.View`), Parcelable/enums, modèles (`com.jehutyno.yomikata.model.**`).
- **Crashlytics** : `-keepattributes SourceFile,LineNumberTable` conservé + le plugin `firebase-crashlytics` uploade le `mapping.txt` automatiquement → traces déobfusquées.
- **Piège signature locale** : `keystore.properties` est un fichier `.properties` Java → le `storeFile` doit utiliser des slashes `/` (jamais des `\` Windows, traités comme échappements) et **pas de guillemets**.

### CI / Release (GitHub Actions → Google Play)

> **Process complet automatisé** : le skill **`/release`** (`.claude/skills/release/SKILL.md`) enchaîne les 10 étapes (gate tests → notes 6 langues → bump → tag → attente CI → push notes → rollout 10 % → notification). Dire « publie une nouvelle version » le déclenche. Le tag CI ne produit qu'un **brouillon sans notes ni rollout**, les notes de version et le déploiement se poussent **après** via `tools/play/play.mjs` (voir § outil-play-cli). Ne pas croire que committer les notes ou pousser le tag suffit à publier.

Workflow `.github/workflows/release.yml`, déclenché en poussant un tag `v*` :
```powershell
# Incrémenter le semver, committer, puis tagger
git tag v2.0.1
git push origin v2.0.1
```
1. **Gate** : job `test` → `./gradlew test verifyRoborazziDebug` (152 tests JVM + régression visuelle Roborazzi). Bloque la suite si rouge.
2. **Build** : `./gradlew bundleRelease` → AAB signé (aussi conservé en artifact GitHub).
3. **Publication** : action `r0adkll/upload-google-play` → track `production`, `status: draft` (= « modifications prêtes à être publiées », **aucun rollout**, validation manuelle dans la console).

**Versioning dérivé du tag** (`app/build.gradle`, repli `20100`/`2.1.0` en local) :
- `versionName` = tag sans le `v` (`v2.0.1` → `2.0.1`)
- `versionCode` = `MAJ*10000 + MIN*100 + PAT` (`2.0.1` → `20001`). Monotone, déterministe, toujours `> 67` (ancien code prod). **Ne jamais régresser le semver** ; rester `< 21.x` par palier.

**Signature** : `signingConfigs.release` lit les variables d'env en CI, avec repli sur `keystore.properties` (gitignoré, modèle `keystore.properties.example`) pour un release **local**. Aucun secret committé.

**Secrets GitHub requis** (Settings → Secrets and variables → Actions) : `KEYSTORE_BASE64` (`base64 -w0 cle.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON` (JSON brut du compte de service Play). Le tout premier upload d'un `applicationId` doit se faire manuellement via la console (déjà fait : prod existante).

### Checklist Play Console (avertissements à connaître)

Deux avertissements récurrents de la Play Console, tous deux **non bloquants**, à traiter/ignorer une fois pour toutes :

- **Identifiant publicitaire (AD_ID)** : le manifeste **retire volontairement** les deux permissions ad-id (`com.google.android.gms.permission.AD_ID` **et** `android.permission.ACCESS_ADSERVICES_AD_ID`, `tools:node="remove"`, `AndroidManifest.xml`), pas de pub, Analytics opt-in RGPD sur app-instance ID. L'app ne collecte donc **réellement pas** l'ad-id (vérifié : manifeste fusionné de l'AAB exempt de toute permission AD_ID). **MAIS** la déclaration Play Console (Contenu de l'application → Identifiant publicitaire) doit rester sur **« Oui, mon application utilise un identifiant publicitaire »** : Google Play fait une **détection au niveau SDK** et voit `play-services-measurement` (transitif de firebase-analytics), qu'il traite comme usage de l'ad-id **quel que soit** le retrait des permissions. Déclarer « Non » → l'upload est **rejeté** (« this release includes the AD_ID permission but your declaration says No », message trompeur, la permission est bel et bien absente). Avec « Oui » + permissions retirées, l'upload passe (avec un avertissement doux « ad-id réinitialisé », **non bloquant**, sans conséquence car on n'utilise pas l'ad-id). Seule alternative pour un « Non » propre : **retirer firebase-analytics**. Faux positif SDK connu, ne pas re-tenter « Non » sans supprimer le SDK.
- **Symboles de débogage natifs** : `debugSymbolLevel = 'FULL'` est activé sur le build-type release (`app/build.gradle`), mais **aucun symbole n'est produit**, le seul code natif de l'AAB vient de libs AndroidX précompilées **déjà strippées** (`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`) ; il n'y a aucun code natif first-party. L'avertissement « code natif sans symboles » **persiste et est bénin** (recommandation, pas erreur ; les traces Kotlin sont déobfusquées via le `mapping.txt` Crashlytics). À **ignorer** tant qu'aucun code natif à nous n'est ajouté.

### Version Catalog

Toutes les dépendances sont dans `gradle/libs.versions.toml`. Pour mettre à jour une version, modifier `[versions]` dans ce fichier, **ne jamais écrire de version inline** dans les `.gradle`.

Les repos sont centralisés dans `settings.gradle` via `dependencyResolutionManagement`. Ne pas ajouter de bloc `repositories {}` dans les sous-modules.

### SQLite3 (scripts de données)
```powershell
$sq = "C:\Users\valen\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe"
$db = "C:\Users\valen\Repos\dev\yomikata\app\src\main\assets\yomikataz.db"
```

**Piège encodage** : ne jamais piper du texte non ASCII vers sqlite3 depuis PowerShell ; écrire le SQL dans un fichier UTF-8 sans BOM, puis `& $sq $db ".read $sqlPath"` (détail : skill `socle:pieges-windows`).

---

## Conventions de code

### Migrations Room
- Chaque migration est un `object : Migration(X, Y)` dans le companion object de `YomikataDatabase`
- Elles sont nommées `MIGRATION_X_Y` et ajoutées à `addMigrations(...)` dans `getDatabase()`
- Toujours incrémenter `DATABASE_VERSION` dans `YomikataDatabase.kt`
- Room exporte automatiquement le schéma JSON dans `app/schemas/` à la compilation (KSP), committer ce fichier
- **Ne jamais utiliser** de DAOs, entités ou constantes externes dans les migrations, elles doivent être auto-contenues
- Après une migration qui modifie des données, toujours appliquer les mêmes changements à l'asset `yomikataz.db` et mettre à jour son `PRAGMA user_version`

### Entités Room (`DatabaseEntities.kt`)
- Chaque entité `RoomFoo` a deux méthodes :
  - `companion object fun from(model: Foo): RoomFoo`, conversion model → Room
  - `fun toFoo(): Foo`, conversion Room → model
- Les nouveaux champs avec `DEFAULT` doivent avoir `= valeurParDéfaut` dans le constructeur Kotlin pour ne pas casser les call sites existants

### Modèles
- Les champs de traduction suivent la convention de nommage par table :
  - `words` : `english`, `french`, `german`, `spanish`, `portuguese`, `chinese` (noms complets)
  - `sentences`, `kanji_solo`, `radicals` : `en`, `fr`, `de`, `es`, `pt`, `zh` (codes ISO)
  - `quiz` : `nameEn`, `nameFr`, `nameDe`, `nameEs`, `namePt`, `nameZh` (camelCase préfixé)

### `getTrad()` / `getName()`
Pattern uniforme dans tous les modèles, chaîne de fallback :
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
Toujours passer par `Prefs` enum (dans `util/Prefs.kt`) pour les clés. Ne jamais utiliser de string literal directement. Injecté via Kodein, ne jamais appeler `PreferenceManager.getDefaultSharedPreferences()` dans les presenters ou helpers.

### Coroutines
Les presenters reçoivent leur `CoroutineScope` en paramètre constructeur (le `lifecycleScope` du fragment/activity hôte). Ne jamais créer `CoroutineScope(Dispatchers.Main)` manuellement dans un presenter ou helper, utiliser `lifecycleScope` dans les composants Android.

### Tutoriels au premier lancement (`util/TutoOverlay.kt`)
- Utiliser `showTutoOnce(prefs, TutoId.XXX, activity, targetView, title, message) { }`, affichage unique automatique.
- Utiliser `showTutoAlways()` uniquement pour l'écran de bienvenue (pas de mémorisation d'état).
- Pour ajouter un nouveau tutoriel : ajouter une entrée dans l'enum `TutoId` avec une clé snake_case stable (ex. `tuto_mon_truc`), ne jamais utiliser un string localisé comme clé.
- `resetAllTutos(prefs)` est appelé depuis `PrefsActivity` ("reset_tuto"), toute nouvelle entrée `TutoId` est automatiquement réinitialisée.

### Dialogs (boîtes de dialogue)
- **Nouveau dialog** : utiliser `Context.yomikataAlert(...)` (`ui/components/YomikataDialogHost.kt`) depuis du code impératif, ou `YomikataDialog` directement depuis un composable. Ne pas créer de nouvel `alertDialog { }` Splitties brut.
- Un seul bouton `DialogButtonStyle.Primary` (pill orange) par dialog ; annulation/secondaire en `Muted` ; action irréversible en `Destructive` (rouge).
- Le thème global `YomikataAlertDialog` (`styles.xml`, `alertDialogTheme`) restyle automatiquement les dialogs Splitties/AppCompat non migrés, le conserver comme filet de sécurité.
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

- **Version courante : 21**, migration consolidée `MIGRATION_16_21` (prod APK code 65 = DB v16 → v21 en un seul saut)
- Asset : `app/src/main/assets/yomikataz.db`
- `fallbackToDestructiveMigrationFrom(0..12)` est utilisé (PAS `fallbackToDestructiveMigration()`) → les utilisateurs sur version < 13 ont un reset propre sans crash. **Piège Room 2.6.1** : `fallbackToDestructiveMigration()` seul + `createFromAsset` efface la DB de TOUS les utilisateurs (isMigrationRequired retourne false → SQLiteCopyOpenHelper remplace la DB par l'asset pour toute version ≠ cible)
- **Piège Room 2.7+** : `room_table_modification_log` requis par `TriggerBasedInvalidationTracker`, créer explicitement dans les migrations ET dans `onOpen` (`CREATE TABLE IF NOT EXISTS`)
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

> **Processus complet de test + checklist de pré-release : voir `TESTING.md` à la racine.**
> Cette section ne garde que les règles et pièges techniques. La stratégie en 5 couches, comment
> ajouter un test dans chaque couche et la passe de pré-production vivent dans `TESTING.md`.

### Règles
- Tests JVM purs dans `src/test/`, pas de Robolectric, pas d'Android runtime
- Tests d'instrumentation dans `src/androidTest/` pour Room, DAOs
- Utiliser `InstantTaskExecutorRule` pour tout test qui touche `LiveData` (dep : `androidx.arch.core:core-testing` dans `testImplementation`)
- `mockkStatic` n'est plus nécessaire pour `PreferenceManager`, `SharedPreferences` est injecté directement
- Pour mocker `Random` : utiliser `object : Random() { override fun nextInt(bound: Int) = 0 }` (pas `mockk<Random>()`, NPE JvmAutoHinter)

### Tests de screenshot (Roborazzi, couche 3)
Tests de régression visuelle sur la JVM (Robolectric, **sans émulateur**). Baselines committées dans `app/src/test/screenshots/`.
```powershell
.\gradlew.bat recordRoborazziDebug   # (ré)enregistre les baselines (1ère fois / changement voulu)
.\gradlew.bat verifyRoborazziDebug   # gate de régression : échoue à toute dérive pixel
```
**Pièges spécifiques à ce stack (AGP 9.2 / Kotlin 2.3 / compileSdk 37)**, voir `MasteryBarScreenshotTest` :
- Robolectric plafonne à **SDK 36** → annoter `@Config(sdk = [36])` (compileSdk 37 non supporté au runtime).
- Utiliser `application = android.app.Application::class` dans `@Config` → sinon `YomikataZKApplication.onCreate` lance Firebase/DI et crashe.
- Utiliser l'API **content-lambda `captureRoboImage("src/test/screenshots/x.png") { ... }`**, PAS `createComposeRule()` (qui exige une Activity launcher que Robolectric ne résout pas avec l'`applicationId` `.debug`).
- Qualifiers en ordre canonique Android (`night` AVANT la densité) : `"w411dp-h891dp-night-xxhdpi"`.
- Changer la langue de l'UI par capture : `RuntimeEnvironment.setQualifiers("+de")` (les strings résolvent via `values-de`, indépendant de `LanguageManager`).
- **Geler les animations infinies** : envelopper la capture dans `CompositionLocalProvider(LocalInspectionMode provides true)` ET faire que les composables à animation infinie (`KenBurnsImage`, cycle photo de `HomeHero`) sautent l'animation sous `LocalInspectionMode.current`. Sinon Roborazzi fait tourner l'horloge à l'infini → un seul écran (Study/Home) prenait **36 min**, ramené à **~30 s** pour 7 captures.
- **Borner la taille** : envelopper le contenu dans `Box(Modifier.size(411.dp, 891.dp))`, les écrans `fillMaxSize` avec `LazyColumn`/`weight` mesurent une hauteur non bornée sinon → `OutOfMemoryError` à la conversion bitmap (cas Study). Heap de test relevé à 2 Go (`testOptions.unitTests.all { it.maxHeapSize }`).
- Voir `MainScreensScreenshotTest` (Home/Study/Quiz × EN/DE) pour le patron d'écran complet avec fixtures `UiState`.

---

## Langue de l'application

### Architecture langue unifiée
La langue UI (strings Android) et langue des traductions (mots/quiz) sont **couplées** via `AppCompatDelegate.setApplicationLocales()` :

- `LanguageManager.initFromPrefs()` → appelle `AppCompatDelegate.setApplicationLocales(current.isoCode)` au démarrage
- `LanguageManager.setLanguage()` → appelle `AppCompatDelegate.setApplicationLocales(lang.isoCode)` + persiste en prefs
- `PrefsActivity` → appelle directement `AppCompatDelegate.setApplicationLocales()` en plus du changement de `LanguageManager.current`
- AppCompat recrée l'activité automatiquement → **plus de dialog "redémarrer l'app"**
- `res/xml/locale_config.xml` déclare les locales supportées (obligatoire Android 13+)

### Comportement de détection
- Pas de prefs sauvegardée → `fromSystemLocale()` (suit la locale du device)
- Prefs sauvegardée (choix explicite user) → utilise cette valeur
- **Ne pas persister la détection automatique**, sinon le changement de langue système ne se reflète plus

### Piège quiz_subtitle (glyphes CJK en locale non-japonaise)
Dans `QuizzesAdapter`, le subtitle affiche les caractères japonais des noms de quiz. **Toujours** :
1. Lire depuis `quiz.nameEn.split("%").getOrElse(1) { "" }`, source unique correcte, indépendante de la langue
2. Mettre `holder.quizSubtitle.textLocale = Locale.JAPANESE`, déclenche le fallback CJK dans les locales DE/ES/PT/etc.

### Piège encodage XML layouts (BOM)
Android AAPT rejette les fichiers XML avec BOM : ne jamais écrire un layout avec `Set-Content` ou `Out-File` de PowerShell 5.1. Méthode d'écriture sans BOM : skill `socle:pieges-windows`.

### Piège scripts PowerShell quiz names
Tout script PS5.1 qui lit des données SQLite via pipe et les ré-écrit dans la DB **corrompt les caractères non-ASCII** (CJK et accents). Pattern obligatoire :
- Lire les données SQLite via `.read` d'un fichier UTF-8 NoBOM, pas via pipe
- Pour les accents dans les string literals (ö, ä, ü, etc.) : utiliser `[char]0x00F6` etc.
- Après exécution : vérifier avec hex check qu'aucun `0x3F` n'apparaît dans les colonnes localisées

### Piège accents dans les traductions QCM (`removeAnyJapanese`)
`String.removeAnyJapanese()` (`util/language/TranslationParser.kt`), appelée par `cleanForQCM(false)` pour la question et les propositions du quiz, doit retirer **uniquement le japonais** (hiragana, katakana, kanji/Han, ponctuation CJK). Ne **jamais** utiliser `\P{InBasicLatin}` : ce bloc se limite à l'ASCII pur, donc les lettres accentuées (é, è, à, ä, ö, ñ, ã, ç…) y sont considérées « non latines » et disparaissent → traductions FR/DE/ES/PT amputées dans le quiz. Cibler les plages Unicode japonaises (constante `japaneseChars`).

**Corollaire, jamais de `\p{In…}`/`\p{Is…}` dans une regex** : ces noms de blocs/scripts sont une **extension propre au JDK**. Le moteur regex adossé à **ICU** de certains builds Android les refuse **à la compilation du motif** (`PatternSyntaxException: U_ILLEGAL_ARGUMENT_ERROR`) → crash en production. Le piège est vicieux : le JDK les accepte, donc **le code compile et les tests JVM passent**. Toujours écrire des **plages `\uXXXX` explicites** dans les classes de caractères (cf. session `fix-regex-icu+lateinit-selections`).

**Limite connue** : pour le chinois (ZH) la traduction est en Han → toujours supprimée par cette fonction (pré-existant, à traiter séparément en rendant `cleanForQCM` conscient de la langue cible).

### Piège room_master_table
Après chaque modification de schéma qui crée ou modifie des tables, **mettre à jour `room_master_table`** dans l'asset :
```sql
UPDATE room_master_table SET identity_hash = '<hash_from_schemas/VERSION.json>' WHERE id = 42;
```
Le hash est dans `app/schemas/.../VERSION.json` champ `identityHash`. Sans ça, crash au démarrage ("Migration Error").

---

## Scripts de traduction
- `extract_jmdict_de.ps1` / `extract_jmdict_es.ps1` / `extract_jmdict_pt.ps1`, extraction JMdict par langue
- `update_quiz_de.ps1` / `update_quiz_es.ps1` / `update_quiz_pt.ps1`, noms de quiz localisés
- `translate_sentences.ps1`, traduction des phrases via Haiku 4.5 (7 425 phrases × 4 langues, 100% complet)
- JMdict.xml mis en cache dans `$env:TEMP`, pas besoin de re-télécharger

---

## Fin de développement

À chaque fin de correctif, refactoring ou fonctionnalité :
1. Committer le travail terminé (français, préfixe conventionnel `fix:`, `feat:`, `refactor:`, `chore:`)
2. Mettre à jour `ARCHITECTURE.md` si la structure, la version de base, les tests ou la couverture changent, et `CLAUDE.md` seulement si une convention ou une commande change
3. Consigner la session par la skill `fin-de-session` : entrée dans `JOURNAL.md`, état dans `ETAT.md` ; jamais d'entrée de session dans ce fichier

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
- `app/src/main/kotlin/com/jehutyno/yomikata/ui/theme/Color.kt`
- `app/src/main/kotlin/com/jehutyno/yomikata/ui/theme/Theme.kt`
- `app/src/main/kotlin/com/jehutyno/yomikata/ui/theme/Type.kt`
- `app/src/main/kotlin/com/jehutyno/yomikata/ui/theme/Shape.kt`