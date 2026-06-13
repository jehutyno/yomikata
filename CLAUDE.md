# CLAUDE.md — Instructions pour Claude Code

Ce fichier est lu automatiquement par Claude à chaque session. Il contient les conventions, commandes et règles spécifiques au projet Yomikata Z.

Pour la documentation technique complète, voir `ARCHITECTURE.md`.

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
**117 tests unitaires JVM** — 116 verts, 1 rouge préexistant (`LanguageManagerTest > initFromPrefs saves detected locale to prefs on first launch` : le test vérifie que la locale détectée est persistée, mais le code ne le fait PAS exprès — voir CLAUDE.md "Ne pas persister la détection automatique").

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