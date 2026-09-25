# Networked Minimal Build Dependencies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove machine-specific dependency JAR lookups from all ten public mod builds, consume the complete KineticCore mod JAR from its public GitHub Release, convert the three private projects to public artifacts wherever an exact compatible artifact exists, and add manually triggered GitHub Release build workflows to the ten public repositories.

**Architecture:** Each independent Gradle repository declares exact Minecraft 1.20.1 Forge-compatible coordinates from the dependency author's public Maven or immutable CurseMaven/Modrinth file source. KineticCore attaches its complete mod JAR to a versioned GitHub Release; consumers resolve that asset for compilation and development runtime. Ten public repositories get `workflow_dispatch` release workflows; private repositories get no public release workflow and keep private-only first-party inputs local.

**Tech Stack:** Gradle, ForgeGradle, Maven/Ivy repositories, complete KineticCore mod JAR, GitHub Actions, PowerShell verification scripts.

**Spec:** `D:/IDEAWork/KineticCore/docs/superpowers/specs/2026-09-25-networked-minimal-build-dependencies-design.md`

## Global Constraints

- Public projects: `AdventureSystems`, `ContentStudio`, `EnchantWorks`, `EntityControl`, `ItemControl`, `KineticArmory`, `KineticCore`, `RealmControl`, `TACZWorkshop`, and `TextStudio`.
- Private projects: `CombatSystems`, `MobAscension`, and `ModRefinery` stay private; do not add publishing workflows or expose their source/artifacts.
- Preserve Minecraft 1.20.1 / Forge compatibility, each project's runtime `mods.toml` requirements, and exact pinned dependency versions.
- Do not use `D:/IDEA_Caches/libs`, `flatDir`, a project-local mod JAR, floating versions, unaudited mirrors, or `latest` for a dependency being migrated.
- Use the complete KineticCore mod JAR as the consumer dependency; do not create or publish a separate API-only artifact.
- FTB Library, FTB Quests, and FTB Teams must use the public `https://maven.ftb.dev/releases` repository with the verified Forge coordinates and versions `2001.2.12`, `2001.4.22`, and `2001.3.2` respectively.
- Release workflows are manually triggered, ask for an explicit `YY.M.D` version, build and upload GitHub Release assets, and never publish to CurseForge. Do not execute a release workflow during implementation.
- Push the validated source changes to all thirteen existing repository origins as previously requested. Do not dispatch the manual release workflows or publish release assets during implementation.

## Review Focus

- Each dependency coordinate must resolve to the exact intended 1.20.1 Forge artifact, not a Fabric/NeoForge or different Minecraft build.
- The full KineticCore release asset must be the regular game mod JAR and remain unshaded in consumer artifacts.
- Full KineticCore dependencies must support consumer compilation and development runtime; runtime metadata stays accurate.
- A clean-cache build must not silently read from a developer machine's shared library directory.
- All ten workflows must be manual-only and must fail before release creation on invalid/duplicate version input or failed build.
- The three private builds must not gain public release automation or lose dependencies that are private-only or required by compatibility code.

---

## Task 1: Finalize the dependency source and version matrix

**Files:**
- Modify: `D:/IDEAWork/KineticCore/docs/superpowers/specs/2026-09-25-networked-minimal-build-dependencies-design.md`
- Create: `D:/IDEAWork/KineticCore/docs/superpowers/plans/2026-09-25-networked-minimal-build-dependencies.md`
- Inspect all thirteen repositories' `build.gradle`, `settings.gradle`, `gradle.properties`, `src/main/java`, mixins, and `META-INF/mods.toml`.

- [ ] For every `localMod(...)`, `flatDir`, direct local `files(...)`, and `D:/IDEA_Caches/libs` reference, record project, mod/version, public artifact coordinate, authoritative repository URL, Forge/Minecraft variant, and dependency scope.
- [ ] Check source imports, mixin targets, reflective lookups, generated sources, and run configurations before deciding between `compileOnly`, `localRuntime`, `runtimeOnly`, or an implementation dependency.
- [ ] Verify every versioned artifact using the upstream repository metadata/POM/module or official project build. For CurseMaven and Modrinth, record the immutable file/version identifier and confirm the downloaded file metadata.
- [ ] Confirm FTB dependencies against their versioned Maven POM/module and keep the three Forge artifacts above; declare the FTB repository with a `dev.ftb.mods` content filter.
- [ ] Record dependencies unavailable publicly or lacking an exact compatible build as explicit local exceptions only in the private project matrix.
- [ ] Self-review the completed matrix for floating versions, wrong loader variants, missing repositories, and unclassified local dependencies before editing Gradle declarations.

**Verification:** a source-controlled dependency matrix is sufficient to reproduce each chosen declaration; each selected public URL returns the corresponding immutable artifact/metadata.

## Task 2: Prepare the full KineticCore release artifact for consumers

**Files:**
- Inspect/modify as needed: `D:/IDEAWork/KineticCore/build.gradle`
- Inspect: `D:/IDEAWork/KineticCore/gradle.properties`

- [ ] Inspect the existing Gradle archive name and version override behavior; keep the normal mod JAR filename predictable for dependency resolution and release upload.
- [ ] Build KineticCore with the exact version override intended for a release and confirm the complete, non-empty mod JAR is emitted to the workflow's output directory.
- [ ] Ensure the release workflow uploads only the regular KineticCore mod JAR; do not add an API-specific Gradle task or asset.

**Verification:** `gradlew clean build` succeeds; archive listing confirms the expected complete mod archive and release filename.

## Task 3: Migrate the ten public repositories off local JAR dependencies

**Files:**
- Modify `build.gradle`, `settings.gradle`, and `gradle.properties` as needed in each of:
  - `D:/IDEAWork/AdventureSystems`
  - `D:/IDEAWork/ContentStudio`
  - `D:/IDEAWork/EnchantWorks`
  - `D:/IDEAWork/EntityControl`
  - `D:/IDEAWork/ItemControl`
  - `D:/IDEAWork/KineticArmory`
  - `D:/IDEAWork/KineticCore`
  - `D:/IDEAWork/RealmControl`
  - `D:/IDEAWork/TACZWorkshop`
  - `D:/IDEAWork/TextStudio`

- [ ] Add only the authoritative public repositories needed by that project's verified dependencies, with group/content filters where supported.
- [ ] Replace every local dependency with its exact coordinate. In AdventureSystems migrate FTB Library, FTB Quests, and FTB Teams to the verified FTB Forge Maven artifacts alongside its other audited third-party dependencies.
- [ ] Replace each KineticCore local dependency with the pinned complete mod release asset. Use the complete artifact for compilation and development runtime; do not create a separate API dependency.
- [ ] Remove local library path properties, local JAR discovery closures, `flatDir`, and local dependency references when no longer used.
- [ ] Keep upstream mod JARs out of the produced mod (no shading/embedding).
- [ ] Re-run IntelliJ/Gradle import or dependency insight where available and adjust scopes if annotation processors, mixins, or dev launches require an additional configuration.

**Verification:** in each of the ten public roots, search finds no `localMod`, `flatDir`, `D:/IDEA_Caches/libs`, or direct local mod-JAR dependency; dependency reports show the chosen pinned artifact and correct scope.

## Task 4: Move public equivalents in the private projects without publishing them

**Files:**
- Modify as necessary: `D:/IDEAWork/CombatSystems/build.gradle`, `settings.gradle`, `gradle.properties`
- Modify as necessary: `D:/IDEAWork/MobAscension/build.gradle`, `settings.gradle`, `gradle.properties`
- Modify as necessary: `D:/IDEAWork/ModRefinery/build.gradle`, `settings.gradle`, `gradle.properties`

- [ ] Migrate public third-party dependencies using the verified source matrix; use public ContentStudio and AdventureSystems release assets only where their required first-party classes are consumed.
- [ ] Keep MobAscension's ModRefinery dependency local because ModRefinery is private; do not upload it or add credentials.
- [ ] Retain any private/unavailable local dependency with a concise build comment/property explaining the exception; do not retain public dependencies locally when an exact artifact is available.
- [ ] Preserve all private repository visibility and do not create `.github/workflows` release publishing files for these projects.

**Verification:** each private project's online-resolvable dependencies resolve from public repositories; remaining local dependencies exactly match the explicitly documented private/unavailable exceptions; git status shows no release workflow additions.

## Task 5: Add manual GitHub Release build workflows to the public ten

**Files:** create `.github/workflows/manual-release.yml` in each of the ten public repositories listed in Task 3.

- [ ] Use only `workflow_dispatch` with a required explicit version input validated against `YY.M.D`; pass it to Gradle with automatic versioning disabled.
- [ ] Check out the repository, set up the required JDK/Gradle, build the mod JAR to a runner-local output path, and fail before publishing if the expected artifact is missing or empty.
- [ ] Reject a version whose release/tag already exists, then create a GitHub Release and attach that project's mod JAR with the repository's `GITHUB_TOKEN`.
- [ ] In KineticCore, build and attach only the normal complete mod JAR.
- [ ] Keep workflows manually triggered only: no `push`, tag, schedule, or CurseForge upload trigger/action.
- [ ] Use explicit permissions with minimum release-write access; ensure the workflow does not print secrets.
- [ ] Confirm artifact filenames align with each project's `archivesName` and version behavior, including any version override flags needed by that project.

**Verification:** statically inspect all ten YAML files for a sole `workflow_dispatch` trigger, version validation, build-before-release ordering, duplicate protection, expected artifacts, and absence of CurseForge publishing.

## Task 6: Verify builds from an isolated Gradle cache

**Files:** no source changes unless verification finds a migration defect; fix the owning project's Gradle files if needed.

- [ ] Use a fresh task-specific `GRADLE_USER_HOME` and run each public project's `gradlew build` after the required full KineticCore Release asset exists, without consulting the shared local libs folder. Repeat the Core and consumer builds after Task 7 changes its archive pipeline and pinyin implementation.
- [ ] Verify every project produces its expected mod JAR in the configured build/release location and no dependencies are shaded into it.
- [ ] Run private project Gradle builds against online public dependencies plus only the retained private local artifacts.
- [ ] Run repository-wide scans across all thirteen roots for local dependency path literals, `flatDir`, and unused resolver helper methods.
- [ ] Check all ten workflow version/file patterns against the actual built artifact names and verify KineticCore release consumers resolve its complete mod JAR.
- [ ] Record any blocked checks explicitly if an upstream artifact or prerequisite GitHub Release is not yet available; do not fake a passing fresh-cache build.

**Verification:** all builds that have their documented prerequisites pass with a clean Gradle user home; remaining blockers name the missing public release/artifact and exact dependent project. No release workflow is dispatched; source changes are pushed to their existing origins.

## Task 7: Own KineticCore pinyin search and remove fat-JAR packaging

**Files:**
- Modify: `D:/IDEAWork/KineticCore/build.gradle`
- Modify: `D:/IDEAWork/KineticCore/gradle/kinetic-api-verification.gradle`
- Modify: `D:/IDEAWork/KineticCore/src/main/java/dev/xyat/kineticcore/internal/client/search/KineticSearchRuntime.java`
- Create: `D:/IDEAWork/KineticCore/src/main/java/dev/xyat/kineticcore/internal/client/search/KineticPinyin.java`
- Create: `D:/IDEAWork/KineticCore/src/main/resources/assets/kineticcore/pinyin/han-readings.tsv`
- Create: `D:/IDEAWork/KineticCore/src/main/resources/assets/kineticcore/pinyin/NOTICE.txt` and `UNICODE-LICENSE.txt`
- Create: `D:/IDEAWork/KineticCore/tools/generate_pinyin_data.ps1`
- Create: `D:/IDEAWork/KineticCore/src/test/java/dev/xyat/kineticcore/internal/client/search/PinyinTransliterationRegression.java`
- Update the source matrix, design spec, and this plan.

- [x] Write and run a failing regression proving supplementary Han characters with a Mandarin reading must match their pinyin before changing the implementation.
- [x] Add a self-contained code-point transliterator backed by the checked-in Unicode 18.0.0 Unihan `kMandarin` data. Select the documented first zh-Hans reading, strip tone marks, normalize u with diaeresis to v, and cover common, traditional, Extension A, and supplementary Han.
- [x] Preserve the existing Kinetic search API, full-pinyin, initial, mixed-token matching, and match ranking; test unknown Han literal matching.
- [x] Remove TinyPinyin's repository, Gradle dependency, package relocation, Shadow plugin, and embedded/fat-JAR output. Produce the same full KineticCore release artifact name through the regular ForgeGradle `jar`/`reobfJar` path.
- [x] Include the Unicode data notice/license and a reproducible generator; ensure no third-party pinyin implementation or jar is present in KineticCore's output.
- [x] Add the pinyin regression to KineticCore's normal `check` task and rerun the complete Core build plus all public consumers that can resolve from the local first-release mirror.

**Verification:** the regression fails against the previous implementation and passes after the replacement; `check` includes it; KineticCore builds its normal release JAR; the JAR contains the owned reading table and no TinyPinyin classes; repository search finds no TinyPinyin or Shadow/fat-JAR configuration; public consumer builds still pass.

## Plan Self-Review

- Scope explicitly separates the public ten from the private three.
- FTB Library, Quests, and Teams are included with verified Forge Maven coordinates and an upstream repository.
- KineticCore consumers use the complete mod artifact for compile and development runtime; no API-only package is produced.
- KineticCore pinyin search is implemented internally using bundled Unicode reading data, without an external library or fat-JAR plugin.
- Dependency selection is audited before Gradle edits, and clean-cache verification is specified.
- Release automation is manual, date-versioned, GitHub-only, and covered by static validation.
