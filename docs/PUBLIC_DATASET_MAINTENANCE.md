# Yalda Public Dataset Maintenance

This is the authoritative application-side contract for maintaining
[`yalda-public-dataset`](https://github.com/shayan4shayan/yalda-public-dataset). The dataset
repository may add contributor guidance, but it must not publish files that conflict with this
document.

## Repository layout

```text
yalda-public-dataset/
├── catalog.v2.json
├── files.index.json
├── README.md
├── LICENSE
├── build_index.py
├── packs/
│   └── en-fa/
│       ├── daily-routines.v1.json
│       ├── food-and-dining.v1.json
│       └── travel-basics.v2.json
├── .github/workflows/validate-dataset.yml
└── *.json                         # legacy root pack files; retain for old apps
```

Use lowercase BCP-47/ISO language codes supported by Yalda: `en`, `es`, `fr`, `de`, `ja`, `fa`,
`it`, and `pt`. Pair directories and IDs are directed: `en-fa` and `fa-en` are different.

## ID and version conventions

- Pack IDs are permanent, lowercase, dot-separated identifiers in the form
  `<source>-<target>.<topic>`, for example `en-fa.travel-basics`. Renaming a title or moving a file
  must not change its pack ID.
- Category IDs are permanent and normally equal the pack ID. A pack represents one practical
  category.
- Entry IDs are permanent, lowercase identifiers prefixed by the pack ID, for example
  `en-fa.travel-basics.airport`. An entry ID must never be reused for a different concept.
- Catalog versions and pack versions are positive decimal strings (`"1"`, `"2"`, and so on).
  Increment the pack version for every published content change. Increment `catalogVersion` for
  every published catalog change.
- File names are descriptive, lowercase kebab case and include the current version, such as
  `travel-basics.v2.json`. A URL can change when the version changes; IDs do not.
- SHA-256 values are lowercase 64-character hexadecimal digests of the exact UTF-8 pack bytes in
  the repository. Normalize and format the file before calculating the digest.

## Exact `catalog.v2.json` schema

The catalog must conform to this JSON Schema. No additional properties are allowed.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/main/schemas/catalog.v2.schema.json",
  "title": "Yalda dataset catalog V2",
  "type": "object",
  "additionalProperties": false,
  "required": ["schemaVersion", "catalogVersion", "packs"],
  "properties": {
    "schemaVersion": { "const": 2 },
    "catalogVersion": { "type": "string", "pattern": "^[1-9][0-9]*$" },
    "packs": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": [
          "id", "version", "title", "sourceLanguage", "targetLanguage", "url",
          "wordCount", "sha256", "license", "recommendedDefault"
        ],
        "properties": {
          "id": { "type": "string", "pattern": "^[a-z]{2,3}-[a-z]{2,3}\\.[a-z0-9]+(?:-[a-z0-9]+)*$" },
          "version": { "type": "string", "pattern": "^[1-9][0-9]*$" },
          "title": { "type": "string", "minLength": 1 },
          "sourceLanguage": { "enum": ["en", "es", "fr", "de", "ja", "fa", "it", "pt"] },
          "targetLanguage": { "enum": ["en", "es", "fr", "de", "ja", "fa", "it", "pt"] },
          "url": { "type": "string", "pattern": "^https://" },
          "wordCount": { "type": "integer", "minimum": 0 },
          "sha256": { "type": "string", "pattern": "^[0-9a-f]{64}$" },
          "license": { "const": "CC BY 4.0" },
          "recommendedDefault": { "type": "boolean" }
        }
      }
    }
  }
}
```

In addition to JSON Schema validation, `id` values must be unique, source and target must differ,
the pair prefix in `id` must equal `sourceLanguage-targetLanguage`, and every URL must be a raw
GitHub URL for this repository.

Example:

```json
{
  "schemaVersion": 2,
  "catalogVersion": "12",
  "packs": [
    {
      "id": "en-fa.travel-basics",
      "version": "2",
      "title": "Travel basics",
      "sourceLanguage": "en",
      "targetLanguage": "fa",
      "url": "https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/refs/heads/main/packs/en-fa/travel-basics.v2.json",
      "wordCount": 180,
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "license": "CC BY 4.0",
      "recommendedDefault": true
    }
  ]
}
```

The sample checksum illustrates the required shape only; generated catalogs must contain the real
digest.

## Exact pack schema

Every file under `packs/` must conform to this schema. No additional properties are allowed.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://raw.githubusercontent.com/shayan4shayan/yalda-public-dataset/main/schemas/pack.v2.schema.json",
  "title": "Yalda dataset pack V2",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "schemaVersion", "packId", "version", "sourceLanguage", "targetLanguage",
    "category", "entries"
  ],
  "properties": {
    "schemaVersion": { "const": 2 },
    "packId": { "type": "string", "pattern": "^[a-z]{2,3}-[a-z]{2,3}\\.[a-z0-9]+(?:-[a-z0-9]+)*$" },
    "version": { "type": "string", "pattern": "^[1-9][0-9]*$" },
    "sourceLanguage": { "enum": ["en", "es", "fr", "de", "ja", "fa", "it", "pt"] },
    "targetLanguage": { "enum": ["en", "es", "fr", "de", "ja", "fa", "it", "pt"] },
    "category": {
      "type": "object",
      "additionalProperties": false,
      "required": ["id", "title"],
      "properties": {
        "id": { "type": "string", "minLength": 1 },
        "title": { "type": "string", "minLength": 1 }
      }
    },
    "entries": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["id", "word", "meaning", "description"],
        "properties": {
          "id": { "type": "string", "minLength": 1 },
          "word": { "type": "string", "minLength": 1 },
          "meaning": { "type": "string", "minLength": 1 },
          "description": { "type": "string", "minLength": 1 }
        }
      }
    }
  }
}
```

The app also requires `packId`, version, source language, and target language to exactly match the
catalog entry; `entries.length` to equal `wordCount`; and entry IDs to be unique within the pack.
The app verifies the checksum before writing anything to its database.

English-to-Persian example:

```json
{
  "schemaVersion": 2,
  "packId": "en-fa.travel-basics",
  "version": "2",
  "sourceLanguage": "en",
  "targetLanguage": "fa",
  "category": {
    "id": "en-fa.travel-basics",
    "title": "Travel basics"
  },
  "entries": [
    {
      "id": "en-fa.travel-basics.airport",
      "word": "airport",
      "meaning": "فرودگاه",
      "description": "A place where aircraft take off, land, and serve passengers."
    }
  ]
}
```

## Writing and review rules

- `word` is written in the source language.
- `meaning` is written in the target language.
- `description` explains the term in the source language; it is not a second translation field.
- Store JSON as UTF-8, normalize all text to Unicode NFC, use consistent source-language casing,
  trim surrounding whitespace, and use one final newline.
- Do not copy definitions from proprietary dictionaries, courses, or other copyrighted databases.
  Write original descriptions or use compatible, documented sources.
- Record data-source provenance in the repository README or a dedicated provenance file, including
  source URL, author/publisher, license, retrieval date, transformations, and affected pack IDs.
- A bilingual reviewer must check meaning, register, spelling, naturalness, description accuracy,
  and category fit before release. Machine translation can assist drafting but cannot replace this
  review.

The initial English-to-Persian V2 release target is at least 5,000 reviewed entries across at least
30 practical categories. Aim for 100–250 entries per category. Useful categories include daily
routines, family, food, dining, shopping, home, work, school, health, body, emotions, weather,
time, numbers, travel, transport, directions, accommodation, technology, communication, finance,
government services, emergencies, nature, hobbies, sports, clothing, social phrases, common verbs,
and common adjectives.

## Licensing and attribution

Dataset content is released under **Creative Commons Attribution 4.0 International (CC BY 4.0)**.
The repository must contain:

- a `LICENSE` file containing or linking to the CC BY 4.0 legal terms;
- a README statement identifying the dataset copyright holder(s) and CC BY 4.0 license;
- the preferred attribution text for downstream users;
- complete provenance and attribution for incorporated compatible sources.

Do not add content with a license that prevents redistribution, modification, or CC BY 4.0
publication. Do not remove required upstream attribution.

## Backward compatibility

Until all app versions using the legacy index are retired:

1. Retain legacy category JSON files at their existing root URLs.
2. Regenerate `files.index.json` after every compatible legacy-file change.
3. Keep its existing array shape: objects with `name` and absolute raw-GitHub `address` fields.
4. Do not redirect or repurpose a legacy URL for a different category or language pair.
5. V2 files and `catalog.v2.json` are authoritative. Legacy output is a compatibility artifact and
   is adapted only as English-to-Persian by current Yalda clients.

## Validation and catalog automation

Replace or extend `build_index.py` so one deterministic command formats and validates packs,
regenerates `catalog.v2.json`, calculates checksums, and regenerates `files.index.json`. It must fail
with a non-zero exit code for:

- JSON Schema errors, unknown fields, or unsupported schema versions;
- missing/invalid stable IDs, language codes, directed-pair prefixes, or versions;
- metadata that differs between the pack and catalog;
- checksum or entry-count mismatches;
- empty IDs, words, meanings, descriptions, category titles, licenses, or URLs;
- duplicate pack IDs or duplicate entry IDs within a pack;
- duplicate normalized source words within one pack.

The command must separately report normalized source-word duplicates across categories for manual
bilingual review. Cross-category duplicates are not automatically invalid because a term can have
different domain meanings.

Add a GitHub Actions workflow that runs this command on every pull request and on pushes to the
default branch. CI should validate that regeneration produces no diff; contributors must commit
the generated catalog, index, counts, and checksums.

## Release workflow

1. Edit one or more pack files without changing existing pack or entry IDs.
2. Normalize UTF-8/NFC text and complete bilingual review.
3. Increment every changed pack version and its versioned file name.
4. Run the validation/catalog command to update counts, URLs, SHA-256 digests,
   `catalog.v2.json`, and `files.index.json`.
5. Review the full diff. Pay special attention to removed entries: current apps never delete them
   automatically, but removal still requires an editorial reason.
6. Merge only after GitHub Actions passes.
7. Fetch `catalog.v2.json`, each changed V2 pack URL, `files.index.json`, and changed legacy URLs
   through `raw.githubusercontent.com`; verify HTTP success, byte-for-byte checksums, and counts.

The app safely adds new entries, updates unedited imported entries, preserves edited or user-created
content, and does not automatically delete entries removed upstream. Stable IDs and correct
versions are therefore essential to predictable updates.
