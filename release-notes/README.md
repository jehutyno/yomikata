# Notes de version (release notes)

Notes de version destinées à la fiche Google Play, versionnées par release.

## Structure

```
release-notes/
  v<version>/
    whatsnew-fr-FR
    whatsnew-en-US
    whatsnew-de-DE
    whatsnew-es-ES
    whatsnew-pt-PT
    whatsnew-zh-CN
```

- Un dossier par version (`v2.0.1`, `v2.0.2`, …).
- Un fichier par langue, nommé `whatsnew-<locale>` (codes BCP-47 Play : `fr-FR`,
  `en-US`, `de-DE`, `es-ES`, `pt-PT`, `zh-CN`). Ce nommage est **exactement**
  celui attendu par l'action `r0adkll/upload-google-play` (paramètre
  `whatsNewDirectory`), donc le dossier est directement réutilisable pour
  l'auto-upload des notes avec l'AAB.

## Contraintes Play Store

- **500 caractères maximum** par fichier (par langue).
- Émojis acceptés.

## Brancher l'auto-upload (optionnel)

Pour pousser automatiquement ces notes avec le bundle, ajouter à l'étape
`Upload to Google Play` de `.github/workflows/release.yml` :

```yaml
        with:
          ...
          whatsNewDirectory: release-notes/${{ github.ref_name }}
```

(`github.ref_name` = le tag, ex. `v2.0.1` → `release-notes/v2.0.1`). Tant que
ce paramètre n'est pas branché, les notes restent saisies manuellement dans la
Play Console et ce dossier sert de source de vérité versionnée.

## Langues

Langues couvertes = celles supportées par l'app (FR/EN/DE/ES/PT/ZH).
Le FR est la version source validée ; les autres en sont la traduction.
