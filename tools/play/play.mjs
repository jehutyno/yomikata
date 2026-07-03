#!/usr/bin/env node
/**
 * play.mjs — CLI de gestion Google Play (fiche, images, notes de version, rollout).
 *
 * S'appuie sur la Google Play Developer Publishing API v3 et le compte de service
 * déjà utilisé par le CI (secret GitHub PLAY_SERVICE_ACCOUNT_JSON).
 *
 * Toutes les modifications passent par une « edit » (transaction) : on ouvre un edit,
 * on empile les changements, puis on `commit`. RIEN n'est publié tant que le commit
 * n'a pas eu lieu. Par sécurité, les commandes mutantes n'effectuent le commit que si
 * --yes est passé ; sinon elles valident puis abandonnent l'edit (aucun effet public).
 *
 * Auth : la clé de service account est lue, dans l'ordre :
 *   1. $PLAY_SERVICE_ACCOUNT_JSON  — chemin vers un fichier .json OU JSON inline
 *   2. $GOOGLE_APPLICATION_CREDENTIALS — chemin vers un fichier .json
 *   3. tools/play/service-account.json  (gitignoré)
 *
 * Exemples :
 *   node play.mjs auth check
 *   node play.mjs listing pull --all
 *   node play.mjs listing push --lang fr-FR --yes
 *   node play.mjs images push --type phoneScreenshots --lang fr-FR --yes
 *   node play.mjs notes set --track production --version 20003 --lang fr-FR --yes
 *   node play.mjs rollout status
 *   node play.mjs rollout start --fraction 0.1 --yes
 *   node play.mjs rollout bump --fraction 0.5 --yes
 *   node play.mjs rollout complete --yes
 *   node play.mjs rollout halt --yes
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { google } from 'googleapis';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONFIG = JSON.parse(fs.readFileSync(path.join(__dirname, 'play.config.json'), 'utf8'));
const SCOPES = ['https://www.googleapis.com/auth/androidpublisher'];

const IMAGE_TYPES = [
  'phoneScreenshots', 'sevenInchScreenshots', 'tenInchScreenshots',
  'tvScreenshots', 'wearScreenshots', 'icon', 'featureGraphic', 'tvBanner',
];

// ── petites aides ──────────────────────────────────────────────────────────
const log = (...a) => console.log(...a);
const info = (...a) => console.log('  ', ...a);
const die = (msg) => { console.error('\n✖', msg, '\n'); process.exit(1); };

function parseArgs(argv) {
  const positional = [];
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const next = argv[i + 1];
      if (next === undefined || next.startsWith('--')) { flags[key] = true; }
      else { flags[key] = next; i++; }
    } else { positional.push(a); }
  }
  return { positional, flags };
}

function mimeFor(file) {
  const ext = path.extname(file).toLowerCase();
  if (ext === '.png') return 'image/png';
  if (ext === '.jpg' || ext === '.jpeg') return 'image/jpeg';
  die(`Format d'image non supporté : ${file} (png ou jpg attendu)`);
}

// ── authentification ───────────────────────────────────────────────────────
function resolveCredentials() {
  const inline = process.env.PLAY_SERVICE_ACCOUNT_JSON;
  if (inline) {
    const trimmed = inline.trim();
    if (trimmed.startsWith('{')) return { credentials: JSON.parse(trimmed), source: '$PLAY_SERVICE_ACCOUNT_JSON (inline)' };
    if (fs.existsSync(trimmed)) return { keyFile: trimmed, source: `$PLAY_SERVICE_ACCOUNT_JSON (${trimmed})` };
  }
  const gac = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (gac && fs.existsSync(gac)) return { keyFile: gac, source: `$GOOGLE_APPLICATION_CREDENTIALS (${gac})` };

  const local = path.join(__dirname, 'service-account.json');
  if (fs.existsSync(local)) return { keyFile: local, source: 'tools/play/service-account.json' };

  die(
    'Aucune clé de compte de service trouvée.\n' +
    '  Renseigne l\'une de ces sources :\n' +
    '    • $PLAY_SERVICE_ACCOUNT_JSON  (chemin ou JSON inline)\n' +
    '    • $GOOGLE_APPLICATION_CREDENTIALS  (chemin)\n' +
    '    • tools/play/service-account.json  (fichier local, gitignoré)'
  );
}

async function getPublisher() {
  const cred = resolveCredentials();
  const auth = new google.auth.GoogleAuth({ ...cred, scopes: SCOPES });
  const publisher = google.androidpublisher({ version: 'v3', auth });
  return { publisher, credSource: cred.source };
}

// ── enveloppe transactionnelle ─────────────────────────────────────────────
/**
 * Ouvre un edit, exécute `work(ctx)`, puis committe (si opts.commit) ou abandonne.
 * `work` peut retourner un résumé (string) affiché avant le commit.
 */
async function withEdit(publisher, { commit }, work) {
  const packageName = CONFIG.packageName;
  const { data: edit } = await publisher.edits.insert({ packageName });
  const editId = edit.id;
  const ctx = { publisher, packageName, editId };
  try {
    const summary = await work(ctx);
    if (summary) log(summary);
    // Validation serveur avant tout commit
    await publisher.edits.validate({ packageName, editId });
    if (commit) {
      await publisher.edits.commit({ packageName, editId });
      log('\n✔ Modifications COMMITÉES et publiées sur Play.');
    } else {
      await publisher.edits.delete({ packageName, editId });
      log('\n· Aperçu validé, edit abandonné (aucune publication). Relance avec --yes pour publier.');
    }
  } catch (e) {
    try { await publisher.edits.delete({ packageName, editId }); } catch { /* edit déjà mort */ }
    throw e;
  }
}

// ── LISTING (textes de la fiche) ───────────────────────────────────────────
function listingDir(lang) { return path.join(__dirname, 'listing', lang); }

function readListingFiles(lang) {
  const dir = listingDir(lang);
  if (!fs.existsSync(dir)) die(`Pas de dossier de fiche pour ${lang} : ${dir}\n  (lance d'abord: play listing pull --lang ${lang})`);
  const read = (name) => {
    const f = path.join(dir, name);
    return fs.existsSync(f) ? fs.readFileSync(f, 'utf8').replace(/\r\n/g, '\n').trimEnd() : '';
  };
  return {
    language: lang,
    title: read('title.txt'),
    shortDescription: read('short.txt'),
    fullDescription: read('full.txt'),
    video: read('video.txt') || undefined,
  };
}

function writeListingFiles(lang, listing) {
  const dir = listingDir(lang);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, 'title.txt'), (listing.title ?? '') + '\n');
  fs.writeFileSync(path.join(dir, 'short.txt'), (listing.shortDescription ?? '') + '\n');
  fs.writeFileSync(path.join(dir, 'full.txt'), (listing.fullDescription ?? '') + '\n');
  fs.writeFileSync(path.join(dir, 'video.txt'), (listing.video ?? '') + '\n');
}

async function listingPull(publisher, flags) {
  const packageName = CONFIG.packageName;
  const { data: edit } = await publisher.edits.insert({ packageName });
  const editId = edit.id;
  try {
    let langs;
    if (flags.all) {
      const { data } = await publisher.edits.listings.list({ packageName, editId });
      langs = (data.listings ?? []).map((l) => l.language);
      if (!langs.length) die('Aucune fiche existante sur ce compte.');
    } else {
      langs = [flags.lang || CONFIG.languages[0]];
    }
    for (const lang of langs) {
      const { data } = await publisher.edits.listings.get({ packageName, editId, language: lang });
      writeListingFiles(lang, data);
      log(`↓ ${lang}  →  tools/play/listing/${lang}/{title,short,full,video}.txt`);
    }
  } finally {
    try { await publisher.edits.delete({ packageName, editId }); } catch { /* ignore */ }
  }
}

async function listingPush(publisher, flags) {
  const langs = flags.all ? CONFIG.languages : [flags.lang || CONFIG.languages[0]];
  await withEdit(publisher, { commit: !!flags.yes }, async ({ publisher, packageName, editId }) => {
    const lines = ['Fiche Play Store — modifications proposées :'];
    for (const lang of langs) {
      const l = readListingFiles(lang);
      // Garde-fous de longueur (limites Play)
      if (l.title.length > 30) die(`[${lang}] title > 30 caractères (${l.title.length})`);
      if (l.shortDescription.length > 80) die(`[${lang}] short > 80 caractères (${l.shortDescription.length})`);
      if (l.fullDescription.length > 4000) die(`[${lang}] full > 4000 caractères (${l.fullDescription.length})`);
      await publisher.edits.listings.update({ packageName, editId, language: lang, requestBody: l });
      lines.push(`  • ${lang}: titre="${l.title}" | short=${l.shortDescription.length}c | full=${l.fullDescription.length}c`);
    }
    return lines.join('\n');
  });
}

// ── IMAGES (screenshots, icône, feature graphic) ───────────────────────────
function mediaDir(lang, type) { return path.join(__dirname, 'media', lang, type); }

async function imagesPush(publisher, flags) {
  const type = flags.type;
  const lang = flags.lang || CONFIG.languages[0];
  if (!type) die('--type requis. Valeurs : ' + IMAGE_TYPES.join(', '));
  if (!IMAGE_TYPES.includes(type)) die(`--type invalide : ${type}\n  Valeurs : ${IMAGE_TYPES.join(', ')}`);

  const dir = mediaDir(lang, type);
  if (!fs.existsSync(dir)) die(`Dossier introuvable : ${dir}`);
  const files = fs.readdirSync(dir)
    .filter((f) => /\.(png|jpe?g)$/i.test(f))
    .sort();               // ordre = ordre d'affichage sur la fiche → nomme 01.png, 02.png…
  if (!files.length) die(`Aucune image (png/jpg) dans ${dir}`);

  await withEdit(publisher, { commit: !!flags.yes }, async ({ publisher, packageName, editId }) => {
    // Remplacement complet : on efface le type puis on ré-uploade dans l'ordre.
    await publisher.edits.images.deleteall({ packageName, editId, language: lang, imageType: type });
    for (const f of files) {
      const full = path.join(dir, f);
      await publisher.edits.images.upload({
        packageName, editId, language: lang, imageType: type,
        media: { mimeType: mimeFor(full), body: fs.createReadStream(full) },
      });
    }
    return `Images ${type} (${lang}) — remplacement par ${files.length} fichier(s) :\n  ${files.join('\n  ')}`;
  });
}

async function imagesList(publisher, flags) {
  const type = flags.type;
  const lang = flags.lang || CONFIG.languages[0];
  if (!type || !IMAGE_TYPES.includes(type)) die('--type requis. Valeurs : ' + IMAGE_TYPES.join(', '));
  const packageName = CONFIG.packageName;
  const { data: edit } = await publisher.edits.insert({ packageName });
  try {
    const { data } = await publisher.edits.images.list({ packageName, editId: edit.id, language: lang, imageType: type });
    const imgs = data.images ?? [];
    log(`${type} (${lang}) : ${imgs.length} image(s)`);
    imgs.forEach((im, i) => info(`${i + 1}. id=${im.id} sha256=${(im.sha256 || '').slice(0, 12)}… ${im.url}`));
  } finally {
    try { await publisher.edits.delete({ packageName, editId: edit.id }); } catch { /* ignore */ }
  }
}

// ── TRACKS (notes de version + rollout) ────────────────────────────────────
function pickRelease(track, { version } = {}) {
  const releases = track.releases ?? [];
  if (!releases.length) die(`Aucune release sur le track "${track.track}".`);
  if (version) {
    const r = releases.find((rel) => (rel.versionCodes ?? []).map(String).includes(String(version)));
    if (!r) die(`Aucune release ne contient le versionCode ${version} sur "${track.track}".`);
    return r;
  }
  // Sinon : la release "active" la plus pertinente (draft ou inProgress d'abord, sinon la 1ère).
  return releases.find((r) => r.status === 'inProgress')
    || releases.find((r) => r.status === 'draft')
    || releases[0];
}

async function tracksGet(publisher, editId, track) {
  const { data } = await publisher.edits.tracks.get({ packageName: CONFIG.packageName, editId, track });
  return data;
}

async function rolloutStatus(publisher, flags) {
  const track = flags.track || CONFIG.defaultTrack;
  const packageName = CONFIG.packageName;
  const { data: edit } = await publisher.edits.insert({ packageName });
  try {
    const data = await tracksGet(publisher, edit.id, track);
    log(`Track "${track}" :`);
    for (const r of data.releases ?? []) {
      const frac = r.userFraction != null ? `  rollout=${(r.userFraction * 100).toFixed(1)}%` : '';
      log(`  • ${r.name || '(sans nom)'}  status=${r.status}  versionCodes=[${(r.versionCodes || []).join(', ')}]${frac}`);
      for (const n of r.releaseNotes ?? []) info(`notes[${n.language}]: ${n.text.replace(/\n/g, ' ⏎ ').slice(0, 100)}`);
    }
  } finally {
    try { await publisher.edits.delete({ packageName, editId: edit.id }); } catch { /* ignore */ }
  }
}

async function notesSet(publisher, flags) {
  const track = flags.track || CONFIG.defaultTrack;
  const lang = flags.lang || CONFIG.languages[0];
  let text = flags.text;
  if (flags.file) text = fs.readFileSync(flags.file, 'utf8').replace(/\r\n/g, '\n').trimEnd();
  if (!text || text === true) die('Fournis --text "…" ou --file chemin.txt');

  await withEdit(publisher, { commit: !!flags.yes }, async ({ publisher, packageName, editId }) => {
    const trackData = await tracksGet(publisher, editId, track);
    const release = pickRelease(trackData, { version: flags.version });
    const notes = release.releaseNotes ?? [];
    const existing = notes.find((n) => n.language === lang);
    if (existing) existing.text = text; else notes.push({ language: lang, text });
    release.releaseNotes = notes;
    await publisher.edits.tracks.update({ packageName, editId, track, requestBody: { track, releases: trackData.releases } });
    return `Notes de version — track "${track}", versionCodes=[${(release.versionCodes || []).join(', ')}], langue ${lang} :\n  ${text.replace(/\n/g, '\n  ')}`;
  });
}

async function rolloutMutate(publisher, flags, action) {
  const track = flags.track || CONFIG.defaultTrack;
  await withEdit(publisher, { commit: !!flags.yes }, async ({ publisher, packageName, editId }) => {
    const trackData = await tracksGet(publisher, editId, track);
    const release = pickRelease(trackData, { version: flags.version });
    let summary;
    switch (action) {
      case 'start': {
        const frac = parseFloat(flags.fraction);
        if (!(frac > 0 && frac < 1)) die('--fraction doit être dans ]0,1[ (ex. 0.1 pour 10%).');
        release.status = 'inProgress';
        release.userFraction = frac;
        summary = `Démarrage rollout track "${track}" → ${(frac * 100).toFixed(1)}%`;
        break;
      }
      case 'bump': {
        const frac = parseFloat(flags.fraction);
        if (!(frac > 0 && frac < 1)) die('--fraction doit être dans ]0,1[ (ex. 0.5 pour 50%).');
        if (release.status !== 'inProgress') die(`La release n'est pas en cours de rollout (status=${release.status}).`);
        const prev = release.userFraction;
        if (prev != null && frac < prev) die(`Régression interdite : ${(frac * 100).toFixed(1)}% < ${(prev * 100).toFixed(1)}% actuel.`);
        release.userFraction = frac;
        summary = `Augmentation rollout track "${track}" → ${(frac * 100).toFixed(1)}%`;
        break;
      }
      case 'complete': {
        release.status = 'completed';
        delete release.userFraction;   // 100% = plus de fraction partielle
        summary = `Finalisation rollout track "${track}" → 100% (completed)`;
        break;
      }
      case 'halt': {
        release.status = 'halted';
        summary = `ARRÊT (halt) du rollout track "${track}"`;
        break;
      }
      default: die(`Action rollout inconnue : ${action}`);
    }
    await publisher.edits.tracks.update({ packageName, editId, track, requestBody: { track, releases: trackData.releases } });
    return summary + `  [versionCodes=${(release.versionCodes || []).join(', ')}]`;
  });
}

// ── auth check ─────────────────────────────────────────────────────────────
async function authCheck(publisher, credSource) {
  log(`Compte de service : ${credSource}`);
  log(`Package          : ${CONFIG.packageName}`);
  const packageName = CONFIG.packageName;
  const { data: edit } = await publisher.edits.insert({ packageName });
  try {
    const { data } = await publisher.edits.tracks.list({ packageName, editId: edit.id });
    const tracks = (data.tracks ?? []).map((t) => t.track);
    log('✔ Accès OK. Tracks visibles : ' + (tracks.join(', ') || '(aucun)'));
  } finally {
    try { await publisher.edits.delete({ packageName, editId: edit.id }); } catch { /* ignore */ }
  }
}

// ── routeur ────────────────────────────────────────────────────────────────
const HELP = `
play — gestion Google Play (fiche, images, notes, rollout)

  auth check                                       vérifie la clé et les droits

  listing pull [--lang fr-FR | --all]              télécharge les textes de la fiche
  listing push [--lang fr-FR | --all] [--yes]      pousse les textes (title/short/full/video)

  images list  --type <t> --lang fr-FR             liste les images d'un type
  images push  --type <t> --lang fr-FR [--yes]     remplace les images depuis media/<lang>/<type>/

  notes set --lang fr-FR (--text "…" | --file f) [--track production] [--version N] [--yes]

  rollout status [--track production]
  rollout start    --fraction 0.1 [--track production] [--version N] [--yes]
  rollout bump     --fraction 0.5 [--track production] [--version N] [--yes]
  rollout complete [--track production] [--version N] [--yes]
  rollout halt     [--track production] [--version N] [--yes]

Types d'image : ${IMAGE_TYPES.join(', ')}

Sécurité : sans --yes, la commande VALIDE puis abandonne l'edit (aucune publication).
           Ajoute --yes pour committer réellement sur Play.
`;

async function main() {
  const { positional, flags } = parseArgs(process.argv.slice(2));
  const [cmd, sub] = positional;
  if (!cmd || cmd === 'help' || flags.help) { log(HELP); return; }

  const { publisher, credSource } = await getPublisher();

  try {
    switch (`${cmd} ${sub ?? ''}`.trim()) {
      case 'auth check': await authCheck(publisher, credSource); break;

      case 'listing pull': await listingPull(publisher, flags); break;
      case 'listing push': await listingPush(publisher, flags); break;

      case 'images list': await imagesList(publisher, flags); break;
      case 'images push': await imagesPush(publisher, flags); break;

      case 'notes set': await notesSet(publisher, flags); break;

      case 'rollout status':   await rolloutStatus(publisher, flags); break;
      case 'rollout start':    await rolloutMutate(publisher, flags, 'start'); break;
      case 'rollout bump':     await rolloutMutate(publisher, flags, 'bump'); break;
      case 'rollout complete': await rolloutMutate(publisher, flags, 'complete'); break;
      case 'rollout halt':     await rolloutMutate(publisher, flags, 'halt'); break;

      default: die(`Commande inconnue : "${cmd} ${sub ?? ''}"\n${HELP}`);
    }
  } catch (e) {
    const api = e?.response?.data?.error;
    if (api) die(`API Play : ${api.code} ${api.status || ''} — ${api.message}`);
    die(e.message || String(e));
  }
}

main();
