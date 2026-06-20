# Yomikata — Plan de migration UI v2

> Fichier de suivi des sessions Claude Code pour la refonte visuelle Compose.
> Lire conjointement avec `DESIGN.md` (tokens, composants) et `MIGRATION.md` (changements fonctionnels).
> **Mettre à jour le statut de chaque session dès qu'elle est terminée.**

---

## Décisions techniques arrêtées

- **Stack cible** : Jetpack Compose + Material 3 (dark-only, `darkColorScheme`)
- **Approche** : migration écran par écran via `ComposeView` dans les Fragments existants
- **Architecture** : MVP conservée (pas de ViewModels). Les Presenters appellent des callbacks View → le Fragment met à jour un `MutableState` Compose.
- **Navigation** : `QuizzesActivity` devient le host unique avec `YomikataBottomBar` permanente. Pas de Navigation Component — Fragment transactions classiques.
- **FuriganaView** : conservée via `AndroidView` dans tous les composants Compose. Réécriture Canvas Compose hors scope.
- **Package ui/theme** : `com.jehutyno.yomikata.ui.theme`
- **Niveau Study** : persisté via clé `LAST_SELECTED_LEVEL` dans l'enum `Prefs.kt`
- **DrawerLayout** : désactivé (pas supprimé) pendant la transition. Supprimé en Session 3.3.
- **Noto fonts** : différé post-migration. Roboto système en Phase 0.

---

## Statuts

| Symbole | Signification |
|---|---|
| `[ ]` | À faire |
| `[~]` | En cours |
| `[x]` | Terminé |
| `[!]` | Bloqué — voir note |

---

## Phase 0 — Fondations

> Aucun changement visible pour l'utilisateur. L'app compile et fonctionne identiquement après chaque session.

---

### Session 0.0 — Gradle : ajout de Compose et Material 3
**Statut** : `[x]`

**Objectif** : configurer le build pour accepter les fichiers Compose.

**Prompt à lancer** :
> Nous démarrons la Session 0.0 du plan de migration UI vers Compose (voir `MIGRATION_PLAN.md`).
> Objectif : ajouter toutes les dépendances Compose et Material 3 dans `gradle/libs.versions.toml` et `app/build.gradle`, puis vérifier que le build passe.
> Contraintes : build.gradle est en Groovy DSL (pas .kts). Ne toucher à aucun fichier Kotlin ou XML de l'app.
> Après succès, mettre `[x]` sur la Session 0.0 dans `MIGRATION_PLAN.md`.

**Dépendances à ajouter** :
- `androidx.compose:compose-bom` (2025.05.x ou latest stable)
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-tooling-preview` + `ui-tooling` (debugImplementation)
- `androidx.compose.material3:material3`
- `androidx.activity:activity-compose`
- `androidx.compose.runtime:runtime-livedata` (hors BOM — déclaration séparée)
- `androidx.lifecycle:lifecycle-runtime-compose`
- Plugin `org.jetbrains.kotlin.plugin.compose` (requis Kotlin 2.x + Compose)

**Fichiers modifiés** :
- `gradle/libs.versions.toml`
- `app/build.gradle`

**Points de vigilance** :
- `kotlinCompilerExtensionVersion` doit être compatible avec Kotlin 2.3.21 du projet
- `buildFeatures { compose = true }` en Groovy : syntaxe `buildFeatures { compose true }` (sans `=`)
- `runtime-livedata` n'est PAS dans le BOM → version explicite

**Critère de done** : `.\gradlew.bat assembleDebug` passe. Aucune modification visible dans l'app.

---

### Session 0.1 — Tokens : colors.xml + Color.kt / Shape.kt / Type.kt / Theme.kt
**Statut** : `[x]`

**Objectif** : créer tous les tokens de design en Compose ET étendre `colors.xml` pour les vues XML restantes.

**Prompt à lancer** :
> Nous démarrons la Session 0.1 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : créer `ui/theme/Color.kt`, `Shape.kt`, `Type.kt`, `Theme.kt` avec tous les tokens de `DESIGN.md` sections 1, 2, 3, 9. Étendre `res/values/colors.xml` avec les nouveaux tokens sans modifier les existants.
> Contraintes : aucune couleur hardcodée dans les fichiers Kotlin. `YomikataTheme` = dark-only (`darkColorScheme` M3). Package : `com.jehutyno.yomikata.ui.theme`.
> Après succès, mettre `[x]` sur la Session 0.1 dans `MIGRATION_PLAN.md`.

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/theme/Color.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/theme/Shape.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/theme/Type.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/theme/Theme.kt`

**Fichiers modifiés** :
- `app/src/main/res/values/colors.xml` — ajout des tokens `color_bg`, `color_surface`, `color_border`, `color_accent`, `color_text_*`… Les tokens existants (`colorPrimary`, mastery levels, POS chips) sont **conservés intacts**.

**Points de vigilance** :
- Ne pas renommer les tokens XML existants — ils alimentent les vues XML non encore migrées
- Les couleurs POS chips et mastery levels dans `colors.xml` sont déjà correctes (DESIGN.md §1) — les référencer depuis `Color.kt`, ne pas dupliquer les valeurs
- `YomikataTheme` utilise uniquement `darkColorScheme()` — pas de `lightColorScheme`
- Roboto système pour la typo (Noto JP différé post-migration)

**Critère de done** : `YomikataTheme { }` importable. Build vert. Aucun changement visuel dans l'app.

---

### Session 0.2 — Composants atomiques partagés
**Statut** : `[x]`

**Objectif** : créer les 4 briques réutilisables utilisées par plusieurs écrans.

**Prompt à lancer** :
> Nous démarrons la Session 0.2 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : créer 4 composants Compose atomiques dans `ui/components/` : `SectionHeader`, `MasteryDots`, `FABBar`, `LevelChip`. Référence visuelle dans `DESIGN.md` section 5.
> Contraintes : preview dark mode obligatoire sur chaque composant. Aucune couleur hardcodée. Un fichier par composant.
> Après succès, mettre `[x]` sur la Session 0.2 dans `MIGRATION_PLAN.md`.

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/components/SectionHeader.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/components/MasteryDots.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/components/FABBar.kt` — `sealed class FABBarState` (Commencer / Continuer / Suivant)
- `app/src/main/java/com/jehutyno/yomikata/ui/components/LevelChip.kt`

**Points de vigilance** :
- `FABBar` : état "Suivant" = fond `color_correct` (vert), pas orange. Modéliser avec `sealed class FABBarState`.
- `MasteryDots` : les 16 couleurs mastery viennent de `Color.kt` (liste ordonnée). Mapping `score → index` à aligner avec `LevelSystem.kt` existant.
- `LevelChip` : labels (Hiragana, Katakana, 漢, 数, N5…N1) → définir un enum ou lire depuis `Categories.kt` existant.
- `SectionHeader` : format bilingue `"JP · EN"`, 10sp weight 600 UPPERCASE, ligne horizontale après.

**Critère de done** : Previews visibles dans Android Studio pour chaque composant. Build vert.

---

### Session 0.3 — Composant BottomNavigationBar
**Statut** : `[x]`

**Objectif** : créer `YomikataBottomBar` Compose (4 onglets) sans encore l'intégrer dans l'app.

**Prompt à lancer** :
> Nous démarrons la Session 0.3 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : créer `ui/components/YomikataBottomBar.kt` — `NavigationBar` M3 avec 4 destinations (Home, Study, Selections, Settings), fond `color_bg_nav`, bordure top `color_border`, icônes actives `color_accent`, inactives `color_text_ghost`. Voir `DESIGN.md` section 4.
> Ce composant n'est pas encore intégré dans l'app — seulement créé avec ses previews.
> Après succès, mettre `[x]` sur la Session 0.3 dans `MIGRATION_PLAN.md`.

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/components/YomikataBottomBar.kt`

**Points de vigilance** :
- M3 `NavigationBar` a un `containerColor` par défaut (surfaceColorAtElevation) → forcer à `BackgroundNav`
- Les 4 destinations = enum `BottomNavDestination` avec label + icône drawable
- Vérifier que les drawables des 4 icônes existent dans `res/drawable/` avant de les référencer

**Critère de done** : Preview visible. Build vert. Aucun changement dans l'app.

---

## Phase 1 — Écrans à fort gain visuel

> Chaque session produit un écran ou composant navigable et testable.

---

### Session 1.1 — Composants Quiz : AnswerButton + ProgressSegmentBar
**Statut** : `[x]`

**Objectif** : créer les deux composants spécifiques à l'écran Quiz.

**Prompt à lancer** :
> Nous démarrons la Session 1.1 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : créer `ui/quiz/QuizComponents.kt` avec `AnswerButton` (3 états : Default/Correct/Wrong) et `ProgressSegmentBar` (N segments colorés). Référence dans `DESIGN.md` section 5.
> `AnswerButton` reçoit : `label: String`, `indexLetter: Char`, `state: AnswerButtonState`, `isRevealed: Boolean`, `isSelected: Boolean`, `onClick: () -> Unit`.
> Previews obligatoires pour les 3 états de `AnswerButton` et une barre à 10 segments.
> Après succès, mettre `[x]` sur la Session 1.1 dans `MIGRATION_PLAN.md`.

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/quiz/QuizComponents.kt`

**Points de vigilance** :
- `AnswerButton` : le "index letter box" (A/B/C/D) est un `Box` 20×20dp interne, fond `#1e2b3a`, texte `color_text_dim`
- État "Wrong non sélectionné" = les 3 autres boutons passent à `alpha = 0.4f` après révélation
- `ProgressSegmentBar` : N segments avec `weight(1f)` partagé dans une `Row`. Hauteur 4dp, gap 2dp, radius 2dp. Légende ("`2 ✓`" vert / "`1 ✗`" rouge) dans une `Row` séparée sous la barre.

**Critère de done** : Previews des 3 états et de la barre. Build vert.

---

### Session 1.2 — Écran Quiz
**Statut** : `[x]`

**Objectif** : remplacer le contenu visuel de `QuizFragment` par un `ComposeView` utilisant `QuizScreen`.

**Prompt à lancer** :
> Nous démarrons la Session 1.2 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : migrer `QuizFragment` vers Compose. Le Fragment garde son shell MVP (implémente `QuizContract.View`), mais son layout devient un `ComposeView` unique qui affiche `QuizScreen`. Créer `ui/quiz/QuizScreen.kt`.
> Stratégie : créer un `QuizUiState` data class dans `QuizFragment`, alimenté par les callbacks du Presenter existant via `mutableStateOf`. `QuizScreen` est un composable stateless qui reçoit ce state + les callbacks.
> `FuriganaView` existante à intégrer via `AndroidView` dans les zones furigana.
> Référence layout dans `DESIGN.md` section 6 (écran Quiz).
> Après succès, mettre `[x]` sur la Session 1.2 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/res/layout/fragment_quiz.xml` → `<ComposeView android:id="@+id/compose_view" />`
- `app/src/main/java/com/jehutyno/yomikata/screens/quiz/QuizFragment.kt`

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/quiz/QuizScreen.kt`

**Points de vigilance** :
- `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` obligatoire sur le `ComposeView`
- `QuizUiState` à modéliser : `currentWord`, `sentence`, `answers: List<String>`, `selectedAnswer: Int?`, `correctAnswer: Int`, `isRevealed: Boolean`, `segmentStates`, `questionIndex: Pair<Int,Int>`, `showFurigana: Boolean`
- `QCMUIController` et `DialogFlowController` gèrent animations/états de boutons — leur logique passe dans le state Compose
- Zone question : fond `color_bg_hero` (#0D1520). Phrase avec mot surligné orange. Séparateur. Mot vedette 46sp weight 300.
- `FABBar` "Suivant →" : visible seulement après réponse (`isRevealed = true`), fond `color_correct`

**Critère de done** : Lancer un quiz, répondre à 3 questions, voir états correct/wrong, appuyer Suivant. Stats correctement enregistrées.

---

### Session 1.3 — Composants Mot : WordListRow, KanjiComponentCard, WordActionBar
**Statut** : `[x]`

**Objectif** : créer les 3 composants réutilisables de la vue mot.

**Prompt à lancer** :
> Nous démarrons la Session 1.3 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : créer 3 composants dans `ui/word/` : `WordListRow`, `KanjiComponentCard`, `WordActionBar`. Référence dans `DESIGN.md` section 5.
> Un fichier par composant. Previews dark mode obligatoires avec données fictives.
> Après succès, mettre `[x]` sur la Session 1.3 dans `MIGRATION_PLAN.md`.

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/word/WordListRow.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/word/KanjiComponentCard.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/word/WordActionBar.kt`

**Points de vigilance** :
- `WordListRow` : **aucun rouge sur les kanjis** (règle absolue). Kanji = `color_text_primary`. Dot = couleur mastery.
- Chip POS : couleurs depuis `Color.kt` (referencer les valeurs POS existantes dans `colors.xml`)
- `KanjiComponentCard` : prend un `KanjiSolo` en paramètre. Séparateur vertical 1dp entre kanji et readings.
- `WordActionBar` : séparateurs verticaux 1dp entre les 4 boutons = `Box(Modifier.width(1.dp).fillMaxHeight().background(BorderDefault))`

**Critère de done** : Previews des 3 composants. Build vert.

---

### Session 1.4 — Écran Word Detail (Dialog → plein écran)
**Statut** : `[x]`

**Objectif** : convertir `WordDetailDialogFragment` (Dialog) en Fragment plein écran avec `ComposeView`.

**Prompt à lancer** :
> Nous démarrons la Session 1.4 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : convertir `WordDetailDialogFragment` en `WordDetailFragment` (Fragment standard, pas Dialog). Son layout devient un `ComposeView` affichant `WordDetailScreen`.
> Dans `ContentFragment`, remplacer `WordDetailDialogFragment.show()` par une transaction Fragment avec back stack.
> Navigation prev/next via boutons ❮/❯ dans l'AppBar (pas de swipe pour l'instant).
> `FuriganaView` via `AndroidView`. Kanji principal 52sp weight 300 `color_text_primary`. Aucun rouge.
> Référence layout dans `DESIGN.md` section 6 (Word Detail).
> Après succès, mettre `[x]` sur la Session 1.4 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/java/com/jehutyno/yomikata/screens/content/word/WordDetailDialogFragment.kt` → `WordDetailFragment.kt` (DialogFragment → Fragment)
- `app/src/main/java/com/jehutyno/yomikata/screens/content/ContentFragment.kt`
- `app/src/main/res/layout/dialog_word_detail.xml` → remplacé par `ComposeView` ou supprimé

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/word/WordDetailScreen.kt`

**Points de vigilance** :
- `WordDetailUiState` : `word: Word?`, `kanjiComponents: List<KanjiSolo>`, `currentIndex: Int`, `totalCount: Int`, `isFavorite: Boolean`, `sentence: Sentence?`
- Les `KanjiSolo` sont chargés async via `WordContract.View.showKanjiSolo()` — le state peut être vide initialement puis mis à jour
- `WordPagerAdapter` (swipe entre mots) devient inutile pour cette session — garder le fichier sans le supprimer
- Section Example : phrase avec mot cible surligné `color_accent` + bouton audio

**Critère de done** : Tap sur un mot → écran plein avec back button. Prev/next fonctionne. Audio, favori, copier fonctionnent.

---

## Phase 2 — Changements fonctionnels

---

### Session 2.1 — Écran Word List
**Statut** : `[x]`

**Objectif** : migrer `ContentFragment` vers Compose avec liste par défaut, 3 tabs et barre de recherche.

**Prompt à lancer** :
> Nous démarrons la Session 2.1 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : migrer `ContentFragment` vers Compose. Layout → `ComposeView`. Créer `ui/wordlist/WordListScreen.kt` avec : AppBar (← + titre + compteur + toggle liste/grille) + TabRow 3 onglets (Tous / À revoir / Maîtrisés) + SearchBar filtrante + `LazyColumn` de `WordListRow` (ou `LazyVerticalGrid` en mode grille) + progress bars compactes (good/wrong/total, 3dp).
> `FuriganaView` via `AndroidView` pour les furigana dans `WordListRow` si nécessaire.
> Référence dans `DESIGN.md` section 6 (Word List) et `MIGRATION.md` section 4.3.
> Après succès, mettre `[x]` sur la Session 2.1 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/res/layout/fragment_content.xml` → `ComposeView`
- `app/src/main/java/com/jehutyno/yomikata/screens/content/ContentFragment.kt`

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/wordlist/WordListScreen.kt`

**Points de vigilance** :
- `ContentPagerAdapter` (onglets existants) devient inutile — ne pas supprimer avant validation
- Filtrage tabs : "À revoir" = mastery score bas (rouge/orange), "Maîtrisés" = score master/high. Utiliser `LevelSystem.kt` pour le mapping score → niveau.
- Recherche : filtre sur la liste déjà chargée en mémoire (pas de requête DB supplémentaire). Vérifier si le Presenter charge tous les mots d'un coup.
- Toggle liste/grille : état local `var isGrid by remember { mutableStateOf(false) }`. Icône dans l'AppBar.
- **Supprimer le rouge** sur les kanjis : `WordListRow` Compose respecte automatiquement la règle.

**Critère de done** : Liste mots avec tabs, recherche filtrante, toggle grille/liste. Tap → Word Detail. Stats correctes.

---

### Session 2.2 — Écran Study
**Statut** : `[x]`

**Objectif** : remplacer le ViewPager2 multi-niveaux de `QuizzesActivity` par un seul écran Study avec level chips.

**Prompt à lancer** :
> Nous démarrons la Session 2.2 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : migrer `QuizzesFragment` vers Compose. Créer `ui/study/StudyScreen.kt` avec : header "学ぶ" + nom niveau actif en orange + `LevelChip` en scroll horizontal + progress bars compactes + `LazyColumn` des quiz du niveau sélectionné + `FABBar` ("Lancer tous" ou "Lancer la sélection (N)").
> Le niveau sélectionné est persisté via la clé `LAST_SELECTED_LEVEL` dans l'enum `Prefs.kt`.
> Le `ViewPager2` multi-niveaux dans `QuizzesActivity` est supprimé (ou désactivé).
> Référence dans `DESIGN.md` section 6 (Study) et `MIGRATION.md` section 4.2.
> Après succès, mettre `[x]` sur la Session 2.2 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/res/layout/fragment_quizzes.xml` → `ComposeView`
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesFragment.kt`
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesActivity.kt` — simplification (retrait ViewPager2 multi-niveaux)
- `app/src/main/java/com/jehutyno/yomikata/util/Prefs.kt` — ajout `LAST_SELECTED_LEVEL`

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/study/StudyScreen.kt`

**Points de vigilance** :
- `QuizzesPresenter` charge les quiz d'une catégorie — vérifier s'il accepte un paramètre catégorie dynamique ou s'il est instancié une fois par catégorie fixe. Adapter si besoin (sans toucher la logique).
- `QuizzesPagerAdapter` devient inutile — conserver le fichier sans supprimer pour rollback.
- Items quiz en Compose : checkbox + titre orange + preview kana (`color_text_dim`) + chevron. Référencer `QuizzesAdapter.kt` pour les données affichées.
- Premier lancement (aucun niveau mémorisé) : charger Hiragana (catégorie 0 dans `Categories.kt`).

**Critère de done** : Écran Study avec chips. Sélectionner N4 → liste N4. Cocher des quiz → FABBar "Lancer la sélection (N)". Niveau mémorisé après redémarrage.

---

### Session 2.3 — Navigation : BottomNav permanente
**Statut** : `[x]`

**Objectif** : intégrer `YomikataBottomBar` dans `QuizzesActivity` et câbler les 4 destinations.

**Prompt à lancer** :
> Nous démarrons la Session 2.3 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : intégrer `YomikataBottomBar` dans `QuizzesActivity` comme navigation permanente. Les 4 onglets (Home, Study, Selections, Settings) switchent entre Fragments via Fragment Manager (pas de Navigation Component).
> Le `DrawerLayout` est désactivé (hamburger icon supprimé) mais le code XML reste pour rollback.
> Créer un Fragment placeholder pour Selections si l'écran n'existe pas encore.
> Gérer le back press : Back depuis Home = quitter l'app. Back depuis autres onglets = revenir à Home.
> Référence dans `DESIGN.md` section 4 et `MIGRATION.md` section 3.
> Après succès, mettre `[x]` sur la Session 2.3 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/res/layout/activity_quizzes.xml` — ajout `ComposeView` en bas pour la BottomNav, désactivation visuelle du Drawer
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesActivity.kt` — câblage BottomNav + Fragment transactions

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/screens/selections/SelectionsFragment.kt` (placeholder si absent)

**Points de vigilance** :
- `YomikataBottomBar` dans un `ComposeView` en bas d'une Activity XML → utiliser `WindowInsets` pour le padding safe area
- Le fragment Home est actuellement dans un `ViewPager2` avec `QuizzesFragment`. Après cette session, ils sont des destinations BottomNav indépendantes.
- Vérifier que `back stack` du Fragment Manager ne crée pas de double entrée lors du switch d'onglets (utiliser `popBackStack` + `replace` avec `addToBackStack(null)` uniquement pour Home).

**Critère de done** : BottomNav visible sur tous les écrans. 4 onglets naviguent correctement. Hamburger disparu. Back fonctionne.

---

## Phase 3 — Home et Settings

---

### Session 3.1 — Écran Settings
**Statut** : `[x]`

**Objectif** : intégrer `PrefsActivity` comme Fragment dans la BottomNav et ajouter les éléments migrés (Night Mode, liens sociaux, version).

**Prompt à lancer** :
> Nous démarrons la Session 3.1 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : transformer `PrefsActivity` en `PrefsFragment` intégré dans l'onglet Settings de `QuizzesActivity`. Créer `ui/settings/SettingsScreen.kt` pour les nouveaux éléments : toggle Night Mode + liens sociaux (Discord, Facebook, Play Store, Share) + numéro de version. Les préférences existantes (`PreferenceFragmentCompat`) peuvent être embarquées via `AndroidView` ou converties progressivement.
> Référence dans `MIGRATION.md` section 4.6.
> Après succès, mettre `[x]` sur la Session 3.1 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/java/com/jehutyno/yomikata/screens/prefs/PrefsActivity.kt` → extraction en `PrefsFragment`
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesActivity.kt` — câblage onglet Settings

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/settings/SettingsScreen.kt`

**Points de vigilance** :
- Night Mode toggle : appelle `AppCompatDelegate.setDefaultNightMode()` — identique à ce qui existe dans le Drawer
- Les liens sociaux étaient dans `HomeFragment` — les supprimer de Home après les avoir ajoutés ici
- `PrefsActivity` pouvait être lancée depuis le Drawer (`startActivity`) — ce point d'entrée disparaît

**Critère de done** : Onglet Settings accessible via BottomNav. Night Mode fonctionne. Liens sociaux ouvrent la bonne URL. Version affichée.

---

### Session 3.2 — Écran Home
**Statut** : `[x]`

**Objectif** : migrer `HomeFragment` vers Compose avec hero, StatCards 2×2, section Continue, News, FABBar.

**Prompt à lancer** :
> Nous démarrons la Session 3.2 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : migrer `HomeFragment` vers Compose. Layout → `ComposeView`. Créer `ui/home/HomeScreen.kt` et `ui/home/StatCard.kt`.
> Layout : Hero (photo existante + scrim `rgba(0x0A0500, 0.65)` + logo circle + titre "Yomikata Z" + tagline "学ぶ · 読む · 理解する") + grille 2×2 StatCards "今日 · Today" + section "続ける · Continue" (masquée si aucune session) + section News + Support + FABBar.
> Supprimer les boutons sociaux (déjà dans Settings) et la section "Latest categories".
> Image hero via `AndroidView` si `KenBurnsView` est conservée, sinon `Image` Compose avec fond statique.
> Référence dans `DESIGN.md` section 6 (Home) et `MIGRATION.md` section 4.1.
> Après succès, mettre `[x]` sur la Session 3.2 dans `MIGRATION_PLAN.md`.

**Fichiers modifiés** :
- `app/src/main/res/layout/fragment_home.xml` → `ComposeView`
- `app/src/main/java/com/jehutyno/yomikata/screens/home/HomeFragment.kt`

**Fichiers créés** :
- `app/src/main/java/com/jehutyno/yomikata/ui/home/HomeScreen.kt`
- `app/src/main/java/com/jehutyno/yomikata/ui/home/StatCard.kt`

**Points de vigilance** :
- `HomePresenter` expose les stats via callbacks View — les mapper dans `HomeUiState`
- Section "Continue" : nécessite une query "dernière session" depuis `StatsRepository`. Vérifier si le Presenter l'expose déjà. Si non, l'ajouter dans le Presenter (pas dans le Repository).
- Scrim hero : `Box` avec `Brush.verticalGradient` par-dessus l'image
- `KenBurnsView` : conserver via `AndroidView` pour l'animation. Alternative : `Image` statique si l'animation n'est pas prioritaire.
- `FABBar` : état "Commencer" (aucune session) ou "Continuer — [nom niveau]" (session existante)

**Critère de done** : Home affiche hero, 4 StatCards avec vraies données, FABBar. Section Continue visible si session existante. Boutons sociaux absents.

---

### Session 3.3 — Nettoyage
**Statut** : `[x]`

**Objectif** : supprimer le code mort post-migration, vérifier la cohérence globale.

**Prompt à lancer** :
> Nous démarrons la Session 3.3 du plan de migration UI (voir `MIGRATION_PLAN.md`).
> Objectif : supprimer le code mort accumulé pendant la migration. Vérifier que le build est propre et que les tests passent.
> Éléments à supprimer (après vérification qu'ils ne sont plus référencés) :
> - Code DrawerLayout dans `QuizzesActivity` et `activity_quizzes.xml`
> - `nav_header.xml`
> - `QuizzesPagerAdapter.kt` (si inutilisé)
> - `ContentPagerAdapter.kt` (si inutilisé)
> - `WordPagerAdapter.kt` (si inutilisé)
> - `dialog_word_detail.xml` (si remplacé)
> - Imports inutilisés dans les fichiers modifiés
> Après succès, mettre `[x]` sur la Session 3.3 dans `MIGRATION_PLAN.md` et vérifier que TOUTES les sessions sont `[x]`.

**Fichiers modifiés** :
- `app/src/main/res/layout/activity_quizzes.xml` — retrait `DrawerLayout` / `NavigationView`
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesActivity.kt` — retrait code Drawer

**Fichiers supprimés** (après vérification) :
- `app/src/main/res/layout/nav_header.xml`
- `app/src/main/java/com/jehutyno/yomikata/screens/quizzes/QuizzesPagerAdapter.kt` (si inutilisé)
- `app/src/main/java/com/jehutyno/yomikata/screens/content/ContentPagerAdapter.kt` (si inutilisé)
- `app/src/main/java/com/jehutyno/yomikata/screens/content/word/WordPagerAdapter.kt` (si inutilisé)

**Points de vigilance** :
- Vérifier chaque fichier avec `grep` avant de supprimer (un fichier "inutilisé" peut être référencé depuis un endroit non évident)
- `.\gradlew.bat test` doit passer (117 tests — 116 verts, 1 rouge préexistant `LanguageManagerTest`)
- Vérifier l'APK avec un émulateur API 30+ avant de valider

**Critère de done** : `.\gradlew.bat assembleDebug` et `.\gradlew.bat test` passent. Aucun `DrawerLayout` dans le code actif. Toutes les sessions du plan sont `[x]`.

---

## Notes de session

> Espace pour noter les décisions prises et les écarts par rapport au plan.

<!-- Ajouter les notes ici au fil des sessions -->

### Session 3.6 — Écran Answer review migré en Compose
**Statut** : `[x]`

Dernier écran XML/RecyclerView migré. `AnswersFragment` héberge désormais un `ComposeView`
unique affichant `ui/answers/AnswerReviewScreen.kt`.
- **Cartes teintées par résultat** (vert/`BackgroundCorrect`+`BorderCorrect` si correct,
  rouge/`BackgroundWrong`+`BorderWrong` si faux) — reprend les états « reveal » du Quiz. Kanji
  et mot cible de la phrase colorés `Correct`/`Wrong` (plus de `getWordColor` → fin du rouge de
  maîtrise sur les mots justes, conforme `DESIGN.md` §7).
- **En-tête de synthèse** : `結果 · Results` + barre proportionnelle verte/rouge + « N ✓ / M ✗ ».
- **Picker de sélection** : l'ancien `PopupMenu` (ancré sur la row) remplacé par un
  `ModalBottomSheet` Compose piloté par `AnswerReviewUiState.selectionSheet`.
- `AnswersActivity` : suppression Toolbar XML + `setSupportActionBar`/`onOptionsItemSelected`,
  edge-to-edge `WindowCompat.setDecorFitsSystemWindows`. `activity_answers.xml` → `FrameLayout`.
- `QuizPresenter.addCurrentWordToAnswers` : couleurs HTML de la réponse harmonisées
  `#77d228`/`#d22828` → `#4ADE80`/`#F87171` (tokens Correct/Wrong).
- Supprimés : `AnswersAdapter.kt`, `res/layout/vh_answer.xml`.
- Build `assembleDebug` + `test` verts.
