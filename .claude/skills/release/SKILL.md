---
name: release
description: Publier une nouvelle version de Yomikata Z de bout en bout — tests, notes de version 6 langues, bump semver, tag git (déclenche le CI GitHub → AAB draft sur Play), puis push des notes et rollout progressif via le CLI tools/play. À déclencher quand l'utilisateur dit « publie une nouvelle version », « nouvelle release », « sortir la 2.x », « /release ».
---

# Publier une nouvelle version

Enchaîne toute la chaîne de release Yomikata Z. **Le CI GitHub (tag `v*`) ne fait qu'une partie du travail** : il teste, build l'AAB et l'uploade en **brouillon** (`status: draft`) sur Play — **il ne publie rien aux utilisateurs et ne lit pas les notes de version**. Les notes et le rollout se font **après**, en local via `tools/play/play.mjs`.

## Paramètres à établir en début de run

1. **Version cible** (semver `X.Y.Z`). Par défaut, proposer le repli courant de `app/build.gradle` (`versionName`). Choisir selon la nature des changements depuis le dernier tag (`git tag --list 'v*'`) : PATCH = correctifs seuls, MINOR = nouvelles fonctionnalités, MAJOR = ruptures. Rester `< 21.x`, ne jamais régresser.
2. **`versionCode` dérivé** = `MAJ*10000 + MIN*100 + PAT` (ex. `2.1.0` → `20100`). Sert de `--version` au CLI Play.
3. **Stratégie de rollout** : demander (progressif 10 % recommandé / 100 % direct / draft seulement). Confirmer explicitement — c'est une publication production irréversible.

## Prérequis (vérifier, sinon s'arrêter et demander)

- **`JAVA_HOME`** valide pour la gate locale : `$env:JAVA_HOME = "<jbr Android Studio>"`. ⚠️ Le chemin du `CLAUDE.md` (`C:\Users\valen\...`) peut ne pas correspondre à la machine courante — vérifier `Test-Path $env:JAVA_HOME`.
- Pour les étapes notes + rollout : **clé de compte de service** présente (`tools/play/service-account.json` OU `$PLAY_SERVICE_ACCOUNT_JSON`) **et** `npm install` fait dans `tools/play` (`node_modules` présent). Si absent → le dire et s'arrêter avant l'étape 8 ; l'utilisateur fournit la clé (ne jamais la committer, elle est gitignorée).
- `gh` authentifié (`gh auth status`) pour surveiller le CI.
- Arbre git propre / sur `main`.

## Étapes (dans l'ordre, ne pas en sauter)

### 1. Gate tests en local
```powershell
.\gradlew.bat test verifyRoborazziDebug
```
Convention : **vert obligatoire avant de tagger** (sinon le tag part et le CI échoue → il faut supprimer le tag). C'est aussi la gate exacte du CI. Si `JAVA_HOME` indisponible et impossible à corriger, prévenir l'utilisateur que la gate reposera uniquement sur le CI (risque de tag à supprimer si rouge) et demander s'il accepte.

### 2. Écrire les notes de version — 6 langues
Créer `tools/play/notes/<version>/<lang>.txt` pour **chaque** langue de `tools/play/play.config.json` : `fr-FR`, `en-GB`, `de-DE`, `es-ES`, `pt-PT`, `zh-CN`. S'inspirer du ton de `tools/play/notes/2.0.2/` (court, orienté utilisateur, pas de jargon technique). Rédiger fr-FR d'abord, puis traduire fidèlement dans les 5 autres. Un fichier = le texte brut de la note, encodage UTF-8 sans BOM.

### 3. Bump du repli de version
Dans `app/build.gradle`, mettre `versionCode`/`versionName` (les valeurs de repli après `?:`) à la version cible. Le CI dérive du tag, mais on garde le repli local synchrone (convention).

### 4. Documentation
- `ARCHITECTURE.md` : § CI/version → repli à jour.
- `CLAUDE.md` : nouvelle entrée « Session release-X.Y.Z » dans le Plan en cours (résumé des changements embarqués).

### 5. Commit
Tout d'un bloc, message `build: passe la version à X.Y.Z (...)` ou `chore(release): X.Y.Z`. Committer sur `main`.

### 6. Tag + push → déclenche le CI
```powershell
git push origin main
git tag vX.Y.Z
git push origin vX.Y.Z
```

### 7. Attendre le CI
```powershell
gh run watch --exit-status
```
(ou `gh run list --workflow release.yml -L 1` puis `gh run watch <id> --exit-status`). Attendre la fin des deux jobs (`test` puis `build-publish`). À la fin : l'AAB est en **brouillon** sur le track production. **Rien n'est encore publié.**

### 8. Pousser les notes de version (le CI ne le fait pas)
Depuis `tools/play`, pour chaque langue :
```powershell
node play.mjs notes set --lang fr-FR --file notes/<version>/fr-FR.txt --version <versionCode> --yes
# … idem en-GB, de-DE, es-ES, pt-PT, zh-CN
```
Sans `--yes` = validation à blanc (aperçu, rien publié). Le CLI cible automatiquement la release draft.

### 9. Rollout — CONFIRMATION EXPLICITE OBLIGATOIRE
Publication production irréversible → **s'arrêter et demander le feu vert** avant d'exécuter, même si la stratégie a été choisie en début de run.
```powershell
node play.mjs rollout status                              # état avant
node play.mjs rollout start --fraction 0.1 --version <versionCode> --yes   # 10 %
# montées ultérieures : rollout bump --fraction 0.5 --yes ; rollout complete --yes
# en cas de souci : rollout halt --yes
```
Pour « draft seulement » : ne rien exécuter ici, indiquer à l'utilisateur de déclencher le rollout dans la console.

### 10. Notifier
Récapituler : version publiée, code, fraction de rollout, liens (run CI, console Play). Signaler tout avertissement Play attendu et **bénin** (AD_ID « faux positif SDK », symboles natifs — voir checklist Play Console du `CLAUDE.md`) pour ne pas inquiéter.

## Pièges à garder en tête
- **Notes ≠ committées suffit.** Le tag n'uploade pas les notes ; l'étape 8 est indispensable.
- **Draft ≠ publié.** Sans l'étape 9, aucun utilisateur ne reçoit la version.
- Notes d'abord (8), rollout ensuite (9) : le rollout préserve les `releaseNotes` déjà posées.
- Ne jamais régresser le semver ; `versionCode` monotone.
- Clé de compte de service **jamais committée**.
