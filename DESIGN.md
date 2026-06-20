# Yomikata — Design System v2

> Refonte complète — juin 2026.
> Basé sur l'analyse de l'existant (v1) et les maquettes produites sur les 5 écrans principaux.
> Stack cible : migration progressive vers **Jetpack Compose** (voir section 10).

---

## 1. Palette de couleurs

### Backgrounds

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_bg`               | `#0a0e17`   | Fond principal — noir bleuté profond                |
| `color_bg_hero`          | `#0d1520`   | Zone question quiz, header word detail              |
| `color_bg_hero_warm`     | `#130a02`   | Hero home avec photo de fond (warm overlay)         |
| `color_bg_nav`           | `#0d1218`   | Bottom navigation bar, footer zones                 |
| `color_bg_correct`       | `#0b1a10`   | Fond card état bonne réponse                        |
| `color_bg_wrong`         | `#200d0d`   | Fond card état mauvaise réponse                     |
| `color_bg_hero_wrong`    | `#180808`   | Fond card hero quiz après mauvaise réponse          |

### Surfaces (cards, listes)

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_surface`          | `#141c26`   | Cards, items de liste, inputs                       |
| `color_surface_accent`   | `#1a1508`   | Surface avec teinte orange (chip actif, highlight)  |
| `color_surface_correct`  | `#0d2318`   | Surface item correct                                |
| `color_surface_wrong`    | `#1a0808`   | Surface item incorrect                              |

### Bordures

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_border`           | `#1e2b3a`   | Bordure standard des cards et composants            |
| `color_border_subtle`    | `#0f1520`   | Dividers, séparateurs de sections                   |
| `color_border_accent`    | `#fb8c0040` | Bordure légère teintée orange (item en vedette)     |
| `color_border_correct`   | `#4ade80`   | Bordure état correct (bonne réponse)                |
| `color_border_correct_bg`| `#4ade8040` | Bordure correcte atténuée (card reveal)             |
| `color_border_hero_correct`| `#4ade8030` | Bordure subtile card hero quiz — bonne réponse    |
| `color_border_wrong`     | `#f87171`   | Bordure état incorrect                              |
| `color_border_hero_wrong`| `#f8717130` | Bordure subtile card hero quiz — mauvaise réponse   |

### Accent / Brand

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_accent`           | `#fb8c00`   | Orange signature — inchangé                         |
| `color_accent_on`        | `#0a0500`   | Texte/icône sur fond orange (FAB, bouton Suivant)   |
| `color_accent_warm`      | `#7a5030`   | Orange très sombre (tagline hero, métadonnées)      |

### Sémantiques

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_correct`          | `#4ade80`   | Bonne réponse, progression good                     |
| `color_correct_on`       | `#051a0a`   | Texte sur fond vert (bouton Suivant)                |
| `color_wrong`            | `#f87171`   | Mauvaise réponse, progression wrong                 |

### Texte

| Token                    | Valeur      | Usage                                               |
|--------------------------|-------------|-----------------------------------------------------|
| `color_text_primary`     | `#e8ecf0`   | Texte principal — blanc légèrement froid            |
| `color_text_secondary`   | `#c0ccd8`   | Traductions, phrases contextuelles                  |
| `color_text_muted`       | `#5a7090`   | Informations secondaires, exemples                  |
| `color_text_dim`         | `#3d4d5e`   | Labels uppercase, métadonnées                       |
| `color_text_ghost`       | `#2e3f52`   | Radicaux, infos très secondaires, empty states      |
| `color_text_warm`        | `#e8d8c0`   | Texte sur fond chaud (status bar hero)              |

### Chips "Partie du discours" (inchangé)

| Type          | Bg          | Texte     |
|---------------|-------------|-----------|
| Noun          | `#1565C0`   | `#FFFFFF` |
| Verb          | `#E65100`   | `#FFFFFF` |
| Adjective     | `#2E7D32`   | `#FFFFFF` |
| Adverb        | `#6A1B9A`   | `#FFFFFF` |
| Other         | `#546E7A`   | `#FFFFFF` |

### Échelle de maîtrise (inchangée — 16 niveaux)

Gradient vert → rouge, utilisé pour les dots de maîtrise et les segments de progress bar.

```
Master  #77d228 → #d2d228
High    #d2d228 → #d2b028
Medium  #d28e28 → #d26728
Low     #d26028 → #d22828
```

> **Règle** : la couleur rouge `#d22828` et ses variantes ne s'utilisent QUE pour l'échelle de maîtrise et l'état `wrong`. Les kanji et furigana sont toujours en blanc/gris dans la v2.

---

## 2. Typographie

| Rôle                     | Taille  | Poids | Couleur token          | Exemple d'usage                       |
|--------------------------|---------|-------|------------------------|---------------------------------------|
| `type_display`           | 52sp    | 300   | `color_text_primary`   | Kanji principal (word detail)         |
| `type_quiz_word`         | 46sp    | 300   | `color_text_primary`   | Mot mis en vedette (quiz)             |
| `type_hero_title`        | 22sp    | 600   | `color_accent`         | "Yomikata Z" (hero home)              |
| `type_screen_title`      | 16sp    | 600   | `color_text_primary`   | Titre d'écran (Study "学ぶ")          |
| `type_sentence`          | 17sp    | 400   | `color_text_secondary` | Phrases de contexte quiz              |
| `type_word_translation`  | 18sp    | 400   | `color_text_secondary` | Traduction principale (word detail)   |
| `type_list_title`        | 14sp    | 500   | `color_text_primary`   | Nom de quiz dans la liste             |
| `type_body`              | 13sp    | 400   | `color_text_muted`     | Contenu cards, descriptions           |
| `type_answer`            | 13sp    | 400   | `color_text_primary`   | Texte bouton réponse quiz             |
| `type_caption`           | 11sp    | 400   | `color_text_muted`     | Furigana, sous-titres                 |
| `type_label`             | 10sp    | 600   | `color_text_dim`       | Section headers uppercase             |
| `type_micro`             | 9sp     | 400   | `color_text_ghost`     | Radicaux, 画数, métadonnées           |
| `type_status_bar`        | 10sp    | 500   | `color_text_warm`      | Heure (zone hero)                     |

**Section header** : 10sp, weight 600, UPPERCASE, letter-spacing 0.14em, `color_text_dim`.
Toujours bilingue : `構成 · Composition`, `今日 · Today`, `例文 · Example`.

**Polices** : Roboto (système) + Noto Sans JP / Noto Serif JP pour les caractères japonais.

---

## 3. Espacements & Formes

### Corner radius — scale unifiée

| Token            | Valeur | Usage                                                    |
|------------------|--------|----------------------------------------------------------|
| `radius_xs`      | 6dp    | Highlight inline (mot dans phrase), petits badges        |
| `radius_sm`      | 10dp   | Barre de recherche, chips compactes                      |
| `radius_md`      | 14dp   | Cards standards, items de liste, kanji component cards   |
| `radius_lg`      | 16dp   | Cards principales, boutons réponses, section cards       |
| `radius_xl`      | 22dp   | FAB bar, pill buttons ("Suivant", "Continuer")           |
| `radius_pill`    | 50%    | Level chips, POS chips, dots, logo circle                |

> Règle : **un seul radius par type de composant**. Fini les 4dp/8dp/16dp non intentionnels.

### Espacements

| Usage                        | Valeur |
|------------------------------|--------|
| Padding écran (horizontal)   | 14dp   |
| Padding card interne         | 12–16dp|
| Gap entre cards              | 7–8dp  |
| Gap entre sections           | 18dp   |
| Gap grid stats (2×2)         | 8dp    |
| Gap items liste              | 0 (dividers) |

### Bordures

- Standard : `1dp solid color_border`
- Feedback correct : `1.5dp solid color_border_correct`
- Feedback wrong : `1.5dp solid color_border_wrong`
- Navigation bottom : `1dp top color_border`

---

## 4. Navigation

### Architecture v2

```
Bottom Navigation Bar (permanente, 4 onglets)
│
├── 🏠 Home       → Dashboard stats + News + Support + FAB "Continuer"
├── 📚 Study      → Level chips + Liste des quiz → Quiz
├── ★  Selections → Mots favoris
└── ⚙  Settings  → Night mode, réglages, liens sociaux, version
```

### Bottom Navigation Bar

- Fond : `color_bg_nav`
- Bordure top : `color_border`
- Icône active : `color_accent`, label `color_accent`
- Icône inactive : `color_text_ghost`, label `color_text_dim`
- Taille icône : 16–18dp, label : 9–10sp
- Padding bottom : 10dp (safe area)

### Level chips (écran Study)

- Scroll horizontal sans scrollbar
- Padding : `9px 0 9px 10px`, gap `6px`, margin-right `10px` dernier item
- **Chip inactif** : bg `color_surface`, border `color_border`, text `color_text_muted`
- **Chip actif** : bg `color_surface_accent`, border `color_accent`, text `color_accent`
- Mémorisation du niveau choisi via `SharedPreferences`
- Défaut premier lancement : Hiragana

---

## 5. Composants

### FABBar (action principale)
- Fond : `color_accent` (orange)
- Texte/icône : `color_accent_on`
- Radius : `radius_xl` (22dp)
- Padding : 9–10dp vertical, 14dp horizontal
- Position : juste au-dessus du bottom nav, padding `0 10–14dp 8dp`
- États : "Commencer" (aucune session), "Continuer — [niveau]" (session active), "Lancer la sélection" (quiz sélectionnés), "Suivant →" (post-réponse, fond `color_correct`)

### StatCard (grille 2×2 home)
- Fond : `color_surface`
- Border : `color_border`
- Radius : `radius_md` (14dp)
- Padding : 8–13dp
- Nombre : 20–30sp, weight 600, couleur sémantique (primary / correct / wrong)
- Label : 9–11sp, `color_text_dim`

### MasteryDots (word detail)
- 5 dots, 7×7dp, radius pill
- Non étudié : `color_border`
- Progression : couleurs de l'échelle de maîtrise (rouge → vert)
- Label texte à droite : 9sp, `color_text_ghost`

### ProgressSegmentBar (quiz)
- N segments = N questions du quiz
- Hauteur : 4dp, gap 2dp, radius 2dp
- Vert = correct, Rouge = wrong, Orange = current, Gris = à venir
- Légende : `2 ✓` (vert) et `1 ✗` (rouge) sous la barre

### AnswerButton (quiz — 3 états)
- Hauteur : 58dp
- Radius : `radius_lg` (16dp, soit 14dp dans le code)
- Gap intérieur : 10dp
- **Default** : bg `color_surface`, border `color_border`, letter bg `#1e2b3a`, letter text `color_text_dim`, texte `color_text_primary`
- **Correct** : bg `color_bg_correct`, border `color_border_correct`, letter bg `#1a3a1a`, letter text `color_correct`, texte `color_correct`, icône ✓
- **Wrong sélectionné** : bg `color_bg_wrong`, border `color_border_wrong`, texte `color_wrong`
- **Wrong non sélectionné (après révélation)** : opacity 0.4

### KanjiComponentCard (word detail)
- Fond : `color_surface`, border `color_border`, radius `radius_md`
- Padding : 12–14dp
- Layout horizontal : kanji 42dp + séparateur 1dp + info flex
- Kanji : 32sp, weight 300, `color_text_primary`
- 画数 : 8sp, `color_text_ghost`, margin-top 3dp
- Meaning : 12sp, weight 500, `color_text_primary`
- Readings row : label 9sp `color_text_dim` (min-width 20dp) + valeur 10sp `color_text_muted`
- Radical : 9sp, `color_text_ghost`

### WordListRow (liste des mots)
- Padding : 10dp 12dp
- Border bottom : `color_border_subtle`
- Layout : dot 8dp + info flex + actions
- **Dot maîtrise** : 8×8dp, radius pill, couleur échelle mastery
- Furigana : 9sp, `color_text_dim`, letter-spacing 0.06em
- Kanji : 18sp, weight 500, `color_text_primary`
- Traduction : 11sp, `color_text_muted`
- Chip POS : right-aligned, 8sp
- Icônes : ★ (favori) + 🔊 (audio), 13dp, `color_text_ghost` / `color_accent` si actif

### SectionHeader (bilingual label)
- 10sp, weight 600, UPPERCASE, letter-spacing 0.14em, `color_text_dim`
- Ligne horizontale après le texte : `color_border_subtle`
- Format : `[FR/JP] · [EN]` (ex: `今日 · Today`)

### ActionBar (word detail)
- Card horizontale avec séparateurs 1dp
- 4 boutons : Favori | Audio | Copier | Signaler
- Fond card : `color_surface`, border `color_border`, radius `radius_md`
- Icône : 18dp, `color_text_ghost` (inactif) / `color_accent` (audio, actif par défaut)
- Label : 8sp, `color_text_dim`

### Dialog (AlertDialog / YomikataDialog)

Deux niveaux d'harmonisation coexistent :

- **Niveau 1 — thème global** (`YomikataAlertDialog` dans `styles.xml`, branché via `alertDialogTheme`) : restyle automatiquement toutes les AlertDialog Splitties/AppCompat sans toucher au code. Fond `color_surface`, bordure `color_border`, coins `radius_xl` (`drawable/bg_dialog.xml`), titre `color_text_primary`, message `color_text_secondary`, bouton principal `color_accent`, autres `color_text_muted`.
- **Niveau 2 — composant Compose** (`ui/components/YomikataDialog.kt`) : look pixel-perfect pour les dialogs migrés.

Spécifications du composant :
- Surface : `SurfacePrimary`, border 1dp `BorderDefault`, radius `radius_xl`, largeur max 360dp, padding 20dp (12dp en bas)
- Titre : `type_screen_title` / `color_text_primary` + icône optionnelle `DialogIcon` (succès = `color_correct`, avertissement = `color_wrong`)
- Message : 14sp / `color_text_secondary`
- Boutons (`DialogButtonStyle`), radius `radius_md` :
  - **Primary** : pill orange plein (`color_accent` / `color_accent_on`) — une seule action principale par dialog
  - **Muted** : texte `color_text_muted` (annulation / action secondaire)
  - **Destructive** : contour rouge `color_wrong` (action irréversible : reset, suppression)
- Disposition : ≤ 2 boutons → ligne alignée à droite (principal à droite, mockup screenshot) ; ≥ 3 → principal pleine largeur en haut + autres en ligne dessous
- Appel impératif (Activity/Fragment) : `Context.yomikataAlert(title, message, icon, buttons, cancelable, onCancel, onBackKey, content)` (`ui/components/YomikataDialogHost.kt`) — héberge le composant dans un `ComposeView`

> Règle : préférer `yomikataAlert` / `YomikataDialog` pour tout nouveau dialog. Le thème global reste le filet de sécurité des dialogs non migrés.

---

## 6. Écrans — résumé des layouts

### Home
1. Status bar (warm bg)
2. Hero (photo + scrim + logo circle + titre + tagline bilingue)
3. Stats 今日 · Today (grille 2×2 StatCards)
4. 続ける · Continue (dernière session avec mini progress bars — masqué si vide)
5. ニュース · News (card simple)
6. Support (card subtile)
7. FABBar ("Continuer — [niveau]" ou "Commencer")
8. Bottom nav (Home actif)

### Study
1. Status bar
2. Header ("学ぶ" + nom niveau actif)
3. Level chips (scroll horizontal)
4. Progress bars compactes (3 lignes : good / wrong / total)
5. Liste des quiz (items avec checkbox + titre orange + preview kana + chevron)
6. FABBar ("Lancer tous les quiz" ou "Lancer la sélection [N]")
7. Bottom nav (Study actif)

### Liste des mots
1. AppBar (← + titre catégorie + compteur + toggle liste/grille)
2. Progress bars (good / wrong / total)
3. Tabs (Tous / À revoir / Maîtrisés)
4. Barre de recherche
5. Liste WordListRow
6. FABBar
7. Bottom nav

### Détail d'un mot
1. AppBar (← + contexte catégorie + ❮ 1/92 ❯)
2. Zone principale : MasteryDots + furigana + kanji display + POS chip + traduction + ActionBar
3. Section 構成 · Composition (KanjiComponentCards)
4. Section 例文 · Example (phrase + highlight + traduction + audio)
5. Bottom nav

### Quiz
1. AppBar (✕ + titre niveau + 🔊 + ⚙ + compteur)
2. ProgressSegmentBar (N segments)
3. Card question (fond `color_bg_hero` animé, `radius_xl`, marges 12dp h / 8dp v) :
   - Centrage vertical du bloc principal
   - **Mot en vedette** (kanji large 46sp, centré, couleur animée blanc→vert/rouge)
   - **Phrase d'exemple** (18sp, centrée horizontalement, mot cible surligné orange)
   - Traduction phrase (masquable via alpha, réserve toujours son espace)
   - Actions secondaires (あ / ★ / ⚠ / 📋)
4. Instruction (label uppercase, AccentOrange 75%, 11sp, letter-spacing 1.2sp)
5. Grille 2×2 AnswerButtons
6. FABBar → devient "Suivant →" (fond `color_correct`) après réponse
7. Bottom nav

---

## 7. Règles de couleur — anti-confusion

1. **Rouge ≠ kanji** : plus jamais de kanji en rouge dans les listes ou le détail.
2. **Rouge = mauvaise réponse ET bas de l'échelle maîtrise uniquement.**
3. **Orange = brand/accent** : titres, icônes actives, mots surlignés dans les phrases.
4. **Vert = bonne réponse uniquement** (pas de vert décoratif ailleurs).
5. **Blanc/gris** = texte neutre, kanji par défaut.
6. **Furigana** : `color_text_dim` (#3d4d5e) — ni rouge, ni orange.

---

## 8. Migration technique (résumé — voir section 10 pour le détail)

- Stack cible : **Jetpack Compose** + Material 3
- Approche : migration écran par écran (pas de réécriture globale)
- Ordre recommandé : tokens → theme → composants partagés → écrans
- Coexistence XML/Compose via `ComposeView` pendant la transition

---

## 9. Tokens Android (noms suggérés)

```kotlin
// Color.kt
val BackgroundPrimary = Color(0xFF0A0E17)
val BackgroundHero = Color(0xFF0D1520)
val BackgroundHeroWarm = Color(0xFF130A02)
val BackgroundNav = Color(0xFF0D1218)
val BackgroundCorrect = Color(0xFF0B1A10)
val BackgroundWrong = Color(0xFF200D0D)
val BackgroundHeroWrong = Color(0xFF180808)

val SurfacePrimary = Color(0xFF141C26)
val SurfaceAccent = Color(0xFF1A1508)
val SurfaceCorrect = Color(0xFF0D2318)

val BorderDefault = Color(0xFF1E2B3A)
val BorderSubtle = Color(0xFF0F1520)
val BorderAccent = Color(0x40FB8C00)
val BorderCorrect = Color(0xFF4ADE80)
val BorderCorrectBg = Color(0x404ADE80)
val BorderHeroCorrect = Color(0x304ADE80)
val BorderWrong = Color(0xFFF87171)
val BorderHeroWrong = Color(0x30F87171)

val AccentOrange = Color(0xFFFB8C00)
val AccentOnOrange = Color(0xFF0A0500)

val Correct = Color(0xFF4ADE80)
val CorrectOn = Color(0xFF051A0A)
val Wrong = Color(0xFFF87171)

val TextPrimary = Color(0xFFE8ECF0)
val TextSecondary = Color(0xFFC0CCD8)
val TextMuted = Color(0xFF5A7090)
val TextDim = Color(0xFF3D4D5E)
val TextGhost = Color(0xFF2E3F52)

// Mastery scale
val MasteryMaster4 = Color(0xFF77D228)
val MasteryLow1 = Color(0xFFD22828)
// ... (16 niveaux)

// Radius (Shape.kt)
val RadiusXs = 6.dp
val RadiusSm = 10.dp
val RadiusMd = 14.dp
val RadiusLg = 16.dp
val RadiusXl = 22.dp
```

---

## 10. Stratégie de transposition dans Claude Code

> Voir document séparé : `CLAUDE_CODE_STRATEGY.md`

---

*Généré juin 2026 — basé sur les maquettes Home v3, Study v2, Word List, Word Detail, Quiz.*
