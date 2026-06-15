# Yomikata — Document de migration UI

> Ce document décrit **fonctionnellement et techniquement** les changements à apporter
> à l'application Yomikata dans le cadre de la refonte visuelle v2.
> Il doit être lu conjointement avec `DESIGN.md` qui contient tous les tokens,
> composants et règles visuelles.
>
> **Public cible** : Claude Code, qui a accès direct au code source et doit
> produire un plan de sessions adapté à l'état réel du projet.

---

## 1. Contexte

L'application Yomikata est une app Android d'apprentissage du japonais (JLPT).
Elle est **en production**, avec des utilisateurs actifs. La migration doit être
progressive et non destructrice : aucune fonctionnalité existante ne doit régresser.

**Stack actuelle** : XML Views + AppCompat DayNight + Navigation Drawer.
**Stack cible** : Jetpack Compose (Material 3) introduit progressivement, les
ViewModels et la couche données restent inchangés.

---

## 2. Décisions techniques arrêtées

### 2.1 Migration hybride progressive
- Chaque écran est migré individuellement vers Compose.
- Pendant la transition, les nouveaux écrans Compose coexistent avec les anciens
  via `ComposeView` intégré dans les Fragments existants, ou remplacement direct
  du Fragment par un `Fragment` contenant un `setContent { }`.
- L'order de migration est défini à la section 5.
- **Aucune réécriture globale** : on migre un écran, on valide, on passe au suivant.

### 2.2 ViewModels conservés
- Les ViewModels existants ne sont **pas réécrits**.
- S'ils utilisent `LiveData`, les wrappers Compose (`observeAsState()`) sont utilisés.
- Si la migration est l'occasion d'ajouter un `UiState` data class manquant,
  le faire dans une extension du ViewModel existant ou un wrapper, sans toucher
  la logique métier.

### 2.3 Système de tokens
- Créer `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` en Compose.
- Les `colors.xml` et `styles.xml` existants sont maintenus pour les vues XML
  non encore migrées, mais mis à jour avec les nouvelles valeurs de couleur
  (section 9 de DESIGN.md) dès la première session.
- **Aucune couleur hardcodée** dans le code Compose. Tout passe par les tokens.

### 2.4 Material 3
- Le thème Compose utilise Material 3 (`MaterialTheme` M3).
- Le mapping des rôles M3 vers nos tokens est dans DESIGN.md section 9.
- On n'utilise pas les composants M3 tels quels quand ils s'éloignent trop
  du design cible (ex: les boutons réponses quiz sont des composants custom).

### 2.5 Navigation
- **Changement majeur** : suppression du `NavigationDrawer` (hamburger menu).
- Remplacement par une `NavigationBar` (Bottom Navigation) permanente avec
  4 destinations : Home, Study, Selections, Settings.
- La navigation entre écrans utilise le `NavGraph` existant si disponible,
  sinon le mettre en place avec Compose Navigation.
- Night Mode, liens sociaux (Discord, Facebook, Play Store, Share) et
  numéro de version migrent vers l'écran Settings.

### 2.6 Mémorisation du niveau Study
- Le niveau choisi dans l'écran Study est persisté dans `SharedPreferences`.
- Clé suggérée : `"last_selected_level"`.
- Valeur par défaut au premier lancement : Hiragana (ou le premier niveau disponible).
- La lecture se fait dans le ViewModel ou un `Repository` dédié aux préférences.

---

## 3. Changement architectural majeur : la navigation

### Avant
```
NavigationDrawer (hamburger)
  ├── Home
  ├── Your Selections
  ├── Hiragana
  ├── Katakana
  ├── Kanji Beginner
  ├── Counters
  ├── JLPT 5
  ├── JLPT 4
  ├── JLPT 3
  ├── JLPT 2
  ├── JLPT 1
  ├── Night Mode (toggle)
  └── Settings
```

### Après
```
BottomNavigationBar (permanente)
  ├── 🏠 Home        → Écran Home (dashboard)
  ├── 📚 Study       → Écran Study (level chips + liste quiz)
  ├── ★  Selections  → Écran Selections (favoris)
  └── ⚙  Settings   → Écran Settings (night mode, liens, version)

Écran Study contient :
  └── Level chips (scroll horizontal)
        あ Hiragana · ア Katakana · 漢 Kanji · 数 Counters
        N5 · N4 · N3 · N2 · N1
      → Sélection d'un chip charge la liste des quiz du niveau
```

**Impact** : le Drawer et son Activity/Fragment associé peuvent être supprimés
après migration complète. Pendant la transition, il peut coexister.

---

## 4. Changements par écran

### 4.1 Écran Home

**Ce qui change visuellement :**
- Hero : garder la photo de fond existante avec un scrim sombre
  (`rgba(10, 5, 0, 0.65)`) par-dessus. Le logo (drawable existant) reste centré.
  Ajouter le tagline bilingue sous le titre : "学ぶ · 読む · 理解する".
- **Supprimer** la rangée de boutons sociaux (Facebook, Discord, Play Store, Share).
- **Supprimer** la section "Latest categories" avec son empty state.
- **Remplacer** Today/This week/This month (3 sections de stats textuelles) par
  une grille 2×2 de `StatCard` pour "Today" uniquement. Les stats semaine/mois
  ne disparaissent pas : elles sont accessibles via un futur écran Stats
  (hors scope de cette migration) ou cachées temporairement.
- **Ajouter** la section "続ける · Continue" : affiche le dernier quiz consulté
  avec ses mini progress bars (good/wrong). **Masquée si aucune session.**
- Conserver la section News (simplifiée visuellement).
- Conserver le Support banner (moins visible, en bas).
- **Ajouter** une `FABBar` orange au-dessus de la BottomNav :
  - Aucune session : "Commencer"
  - Session existante : "Continuer — [nom du niveau]"

**Ce qui ne change pas fonctionnellement :**
- Récupération des stats depuis le ViewModel/repository existant.
- Affichage des news (même source de données).

---

### 4.2 Écran Study (anciennement "List of Quizzes")

Cet écran existait par niveau (un écran Hiragana, un écran JLPT5, etc.).
Dans la v2, il n'y a **qu'un seul écran Study** avec un sélecteur de niveau.

**Ce qui change fonctionnellement :**
- Un seul écran pour tous les niveaux (au lieu d'un écran par niveau).
- Chips de niveau en haut (scroll horizontal) : sélection charge la liste du niveau.
- Le niveau sélectionné est mémorisé (`SharedPreferences`).
- Au premier lancement (ou si aucun niveau mémorisé) : charger Hiragana.

**Ce qui change visuellement :**
- Suppression du header avec image de fond (propre à chaque niveau actuel).
- Header simple : titre "学ぶ" + nom du niveau actif en sous-titre orange.
- Level chips scrollables (voir DESIGN.md section 5 — `LevelChip`).
- Progress bars compactes (3 lignes : good / wrong / total) sous les chips.
- Items de liste modernisés : radius 12dp, fond `color_surface`, titre orange,
  preview kana en `color_text_dim`, checkbox.
- `FABBar` : "Lancer tous les quiz" (aucun coché) ou "Lancer la sélection (N)"
  (N quiz cochés).

**Ce qui ne change pas :**
- Logique de sélection des quiz (checkboxes).
- Lancement du quiz depuis la sélection.
- Données (quiz par catégorie depuis la source existante).

---

### 4.3 Liste des mots

**Ce qui change fonctionnellement :**
- Passage de la grille 2 colonnes (kanji only) à une **vue liste** par défaut.
- Un toggle liste/grille en haut à droite permet de revenir à la grille.
- Trois onglets : **Tous** / **À revoir** (dot rouge/orange) / **Maîtrisés** (dot vert).
- Barre de recherche filtrante (kanji, furigana, traduction).

**Ce qui change visuellement :**
- **Supprimer** le rouge sur les kanji (voir règle DESIGN.md section 7).
- Chaque item de liste : dot de maîtrise (couleur échelle) + kanji (blanc) +
  furigana (muted) + traduction + chip POS + icônes ★ et 🔊.
- Progress bars (good/wrong/total) conservées mais redessinées (3 lignes,
  labels textuels, hauteur 3dp, `color_correct` / `color_wrong`).
- AppBar : titre + compteur "92 mots · 0 maîtrisés" + icônes toggle.

**Ce qui ne change pas :**
- Source de données des mots.
- Ouverture du détail au tap sur un mot.
- Logique de maîtrise (calcul du niveau depuis les stats existantes).

---

### 4.4 Détail d'un mot

**Ce qui change fonctionnellement :**
- **Modal → écran plein** : le dialog/bottom sheet actuel devient un écran
  de navigation à part entière avec back button.
- Navigation prev/next intégrée dans l'AppBar (❮ 1/92 ❯) au lieu des flèches
  flottantes latérales.

**Ce qui change visuellement :**
- **Supprimer** le rouge sur le kanji principal et les furigana.
  Kanji : 52sp weight 300, `color_text_primary`.
  Furigana : 12sp, `color_text_dim`.
- **Ajouter** les `MasteryDots` (5 dots, voir DESIGN.md section 5).
- Section Composition : chaque kanji composant dans sa propre `KanjiComponentCard`
  (layout horizontal : kanji 32sp + séparateur + readings). Voir DESIGN.md.
- `WordActionBar` : barre d'actions horizontale en card (Favori / Audio / Copier
  / Signaler), remplace les icônes éparpillées.
- Section Example : phrase avec mot cible surligné en `color_accent` + audio.

**Ce qui ne change pas :**
- Données affichées (composition, lectures, exemple).
- Fonctionnalité favori, audio, signaler, copier.

---

### 4.5 Écran Quiz

**Ce qui change fonctionnellement :**
- Rien ne change fonctionnellement. La mécanique du quiz (question, 4 réponses,
  bonne/mauvaise détection, progression) reste identique.

**Ce qui change visuellement (et c'est l'écran avec le plus gros gain) :**

- **Supprimer** l'espace vide excessif (40% de l'écran inutilisé actuellement).
- Remplacer les 4 progress bars empilées par une `ProgressSegmentBar`
  (N segments colorés : vert=correct, rouge=wrong, orange=current, gris=restants).
- La "zone question" occupe la moitié supérieure de l'écran :
  - Phrase de contexte avec le mot cible surligné en `color_accent` (orange).
  - Séparateur.
  - **Mot vedette** : furigana 11sp + kanji 46sp weight 300, centré.
  - Icônes secondaires (furigana toggle, favori, signaler, copier).
- Grille 2×2 de `AnswerButton` avec labels A/B/C/D.
- **États des boutons après réponse :**
  - Correct sélectionné : fond `color_bg_correct`, bordure `color_border_correct`,
    texte et icône ✓ en `color_correct`.
  - Wrong sélectionné : fond `color_bg_wrong`, bordure `color_border_wrong`.
  - Autres boutons (non sélectionnés) : opacité 0.4.
  - Mot dans la phrase : passe de orange → vert après bonne réponse.
  - Traduction révélée sous le mot vedette.
- `FABBar` "Suivant →" apparaît après la réponse (fond `color_correct`,
  texte `color_correct_on`). Avant la réponse : masquée ou absente.

---

### 4.6 Écran Settings (nouveau ou existant à enrichir)

Cet écran **reçoit** les éléments retirés des autres écrans :
- Toggle Night Mode (était dans le Drawer).
- Liens sociaux : Discord, Facebook, Play Store, Share (étaient sur la Home).
- Numéro de version (était dans le Drawer).
- Les réglages existants (audio, paramètres quiz, etc.) restent.

Si un écran Settings existe déjà, l'enrichir.
Si non, créer un écran minimal Compose avec ces éléments.

---

## 5. Ordre de migration recommandé

L'ordre tient compte de trois critères :
1. **Impact visuel** (gain perçu par l'utilisateur)
2. **Dépendances** (les tokens doivent exister avant les composants)
3. **Risque** (les écrans les plus critiques passent après validation des fondations)

```
Phase 0 — Fondations (pas d'UI utilisateur modifiée)
  0.1  Mise à jour colors.xml (nouvelles valeurs, impact immédiat sur toute l'app)
  0.2  Color.kt + Shape.kt + Type.kt + Theme.kt (tokens Compose)
  0.3  Composants atomiques partagés (SectionHeader, MasteryDots, FABBar, LevelChip)
  0.4  BottomNavigationBar (composant partagé, utilisé par tous les écrans migrés)

Phase 1 — Écrans à fort gain visuel, faible risque fonctionnel
  1.1  Composants Quiz (AnswerButton, ProgressSegmentBar)
  1.2  Écran Quiz (remplace le Fragment existant)
  1.3  Composants mots (WordListRow, KanjiComponentCard, WordActionBar)
  1.4  Écran Word Detail (modale → plein écran)

Phase 2 — Écrans à changement fonctionnel modéré
  2.1  Écran Word List (grille → liste, tabs, recherche)
  2.2  Écran Study (fusion des N écrans de niveau en 1 + level chips)
  2.3  Navigation (suppression Drawer, mise en place BottomNav)

Phase 3 — Home et Settings
  3.1  Écran Settings (consolidation des éléments déplacés)
  3.2  Écran Home (hero + stats + FABBar)
  3.3  Nettoyage : suppression du Drawer, des fichiers XML orphelins
```

---

## 6. Ce qu'il ne faut pas toucher

- **Couche données** : Room database, DAOs, repositories, modèles de données.
- **Logique quiz** : algorithme de sélection des questions, calcul good/wrong.
- **Logique de maîtrise** : calcul du niveau de maîtrise par mot.
- **Audio** : moteur de lecture, téléchargement des voix.
- **Download** : téléchargement des packs de données.
- **ViewModels** (logique métier) : les adapter aux nouveaux états UI si besoin,
  mais ne pas réécrire leur logique.
- **Contenu** : les données JLPT, kana, kanji restent inchangées.

---

## 7. Règles de code à respecter dans tout le code produit

1. **Aucune couleur hardcodée** dans les fichiers `.kt` Compose.
   Toujours utiliser les tokens de `Color.kt`.

2. **Aucune taille hardcodée** pour les radius. Utiliser `Shape.kt`.

3. **Aucun rouge** (`color_wrong` ou `MasteryLow1`) sur les kanji ou furigana
   en dehors du contexte quiz (réponse incorrecte) ou mastery (dot de niveau bas).

4. **Previews obligatoires** sur chaque composant Compose, avec dark theme
   (le thème de l'app est toujours dark).

5. **Un composant = un seul fichier** si possible. Ne pas créer de fichiers
   "fourre-tout". Exceptions : les composants très liés (ex: `AnswerButton` +
   `AnswerState` dans `QuizComponents.kt`).

6. **Labels bilingues** sur tous les section headers :
   format `"JP · EN"` (ex: `"今日 · Today"`, `"構成 · Composition"`).

7. **Interop** : quand un écran Compose est inséré dans un Fragment existant,
   utiliser `ComposeView` avec `setViewCompositionStrategy(
   ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)`.

8. **Tests** : vérifier après chaque session que l'APK compile et que l'écran
   migré est navigable sans crash sur un émulateur API 30+.

---

## 8. Référence croisée avec DESIGN.md

| Besoin                        | Section DESIGN.md |
|-------------------------------|-------------------|
| Tokens de couleur             | Section 1 et 9    |
| Échelle typographique         | Section 2         |
| Radius et espacements         | Section 3         |
| Architecture de navigation    | Section 4         |
| Description des composants    | Section 5         |
| Layout de chaque écran        | Section 6         |
| Règles anti-confusion couleur | Section 7         |
| Noms des tokens Kotlin        | Section 9         |

