# Outil CLI Play Store (`tools/play`)

Gestion de la **fiche Play Store**, des **images**, des **notes de version** et du **rollout**
via la [Google Play Developer Publishing API v3](https://developers.google.com/android-publisher),
avec le compte de service déjà utilisé par le CI (secret `PLAY_SERVICE_ACCOUNT_JSON`).

> **Modèle « edit »** : chaque modification passe par une transaction. Rien n'est publié tant
> qu'on ne committe pas. **Sans `--yes`**, chaque commande valide puis **abandonne** l'edit
> (aucune publication). Ajoute `--yes` pour committer réellement.

## Installation

```powershell
cd tools/play
npm install
```

## Authentification (clé de compte de service)

L'outil cherche la clé dans cet ordre :

1. `$PLAY_SERVICE_ACCOUNT_JSON` — chemin vers un `.json` **ou** le JSON inline (même secret que le CI)
2. `$GOOGLE_APPLICATION_CREDENTIALS` — chemin vers un `.json`
3. `tools/play/service-account.json` — fichier local (**gitignoré**)

Le plus simple en local : place la clé JSON dans `tools/play/service-account.json`.

### ⚠️ Droits requis côté Play Console (à faire une fois, manuellement)

Le compte de service du CI n'a probablement que le droit **« Releases »**. Pour la fiche et les
images, accorde-lui dans **Play Console → Utilisateurs et autorisations** :

- **Modifier les fiches Play Store, l'apparence et les images du Store**
- **Gérer les versions de production** (pour le rollout)

Vérifie l'accès :

```powershell
node play.mjs auth check
```

## Commandes

```
auth check                                       vérifie la clé et les droits

listing pull [--lang fr-FR | --all]              télécharge les textes de la fiche
listing push [--lang fr-FR | --all] [--yes]      pousse les textes

images list  --type <t> --lang fr-FR             liste les images d'un type
images push  --type <t> --lang fr-FR [--yes]     remplace les images

notes set --lang fr-FR (--text "…" | --file f) [--track production] [--version N] [--yes]

rollout status [--track production]
rollout start    --fraction 0.1 [--track production] [--version N] [--yes]
rollout bump     --fraction 0.5 [--track production] [--version N] [--yes]
rollout complete [--track production] [--version N] [--yes]
rollout halt     [--track production] [--version N] [--yes]
```

Types d'image : `phoneScreenshots`, `sevenInchScreenshots`, `tenInchScreenshots`,
`tvScreenshots`, `wearScreenshots`, `icon`, `featureGraphic`, `tvBanner`.

## Fichiers versionnés

### Textes de la fiche — `listing/<lang>/`

```
listing/fr-FR/title.txt    (≤ 30 caractères)
listing/fr-FR/short.txt    (≤ 80 caractères)
listing/fr-FR/full.txt     (≤ 4000 caractères)
listing/fr-FR/video.txt    (URL YouTube, optionnel)
```

`listing pull` les remplit depuis Play ; édite-les puis `listing push`.

### Images — `media/<lang>/<type>/`

```
media/fr-FR/phoneScreenshots/01.png
media/fr-FR/phoneScreenshots/02.png
media/fr-FR/featureGraphic/01.png
media/fr-FR/icon/01.png
```

L'**ordre d'affichage = ordre alphabétique** des noms de fichier → nomme `01.png`, `02.png`…
`images push` **remplace tout** le type ciblé (deleteall puis ré-upload dans l'ordre).

## Exemples de flux

```powershell
# 1. Récupérer la fiche actuelle pour l'éditer
node play.mjs listing pull --all

# 2. Modifier tools/play/listing/fr-FR/full.txt puis prévisualiser
node play.mjs listing push --lang fr-FR         # valide sans publier
node play.mjs listing push --lang fr-FR --yes   # publie

# 3. Notes de version pour la release en préparation
node play.mjs notes set --lang fr-FR --file notes-2.0.3-fr.txt --yes

# 4. Rollout progressif
node play.mjs rollout status
node play.mjs rollout start --fraction 0.1 --yes   # 10 %
node play.mjs rollout bump  --fraction 0.5 --yes   # 50 %
node play.mjs rollout complete --yes               # 100 %
# en cas de souci :
node play.mjs rollout halt --yes                   # gèle le déploiement
```

## Rapport avec le CI existant

`.github/workflows/release.yml` uploade l'AAB en **brouillon** sur le track `production`
(via `r0adkll/upload-google-play`). Cet outil complète ce pipeline pour tout ce que le CI
ne fait pas : textes de fiche, images, notes de version fines et **pilotage du rollout**.
