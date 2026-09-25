# Networked Minimal Build Dependencies

**Date:** 2026-09-25

**Status:** Draft for review

## Goal

Replace every dependency currently resolved from `D:/IDEA_Caches/libs`, a `flatDir`, or a project-local mod JAR with a version-pinned artifact downloaded from an online repository. Keep the dependency set as small as the source and development workflow allow. In particular, KineticCore consumers compile against a separate JAR containing only `dev/xyat/kineticcore/api/**`; they do not use the complete KineticCore mod JAR as their compile dependency.

Preserve the existing runtime mod requirements in each mod's metadata. A compile API artifact is not a replacement for installing KineticCore in a game instance.

## Scope

The release automation and complete public-dependency migration cover the ten public projects: AdventureSystems, ContentStudio, EnchantWorks, EntityControl, ItemControl, KineticArmory, KineticCore, RealmControl, TACZWorkshop, and TextStudio. The three private projects—CombatSystems, MobAscension, and ModRefinery—will not be made public and will not receive public release workflows. In those private builds, use public online artifacts when an exact, compatible public artifact exists; keep private first-party artifacts and dependencies with no equivalent public source local.

It includes repository declarations, pinned dependency versions, dependency scopes, build output locations, and publishing needed to make public first-party artifacts consumable. It does not change gameplay or UI behavior, publish CurseForge files automatically, or put private mod source or artifacts in public repositories. The private projects' local dependency paths are not removed when the only available source is private or local.

## Current local dependency inventory

This records the present `localMod(...)` dependencies so each can be classified during implementation. Versions are the versions of the local JARs audited in `D:/IDEA_Caches/libs`; the final coordinate and compatible Forge artifact will be checked against each upstream repository before editing its build. For the three private projects, a local entry may remain only when no exact public artifact is available or when it points to a private first-party mod.

| Gradle project | Current local dependencies |
| --- | --- |
| AdventureSystems | KineticCore, Curios, Refined Storage, Sophisticated Core, Sophisticated Backpacks, Architectury, FTB Library, FTB Quests, FTB Teams, JEI |
| CombatSystems | ContentStudio, TaCZ |
| ContentStudio | KineticCore, JEI |
| EnchantWorks | KineticCore |
| EntityControl | KineticCore, Jade, Curios |
| ItemControl | KineticCore, Architectury, KubeJS, Rhino, JEI |
| KineticArmory | KineticCore, Curios, KubeJS, Rhino, Architectury |
| KineticCore | Curios, JEI |
| MobAscension | KineticCore, ModRefinery, L2Hostility, L2Library |
| ModRefinery | Alex's Mobs, Enhanced AI, Legendary Tooltips, Sol Carrot, Curios, Explorer's Compass (edited), Nature's Compass, Waystones, Balm, L_Ender's Cataclysm, Registrate, Architectury, KubeJS, Rhino, Goety, Iron's Spells 'n Spellbooks, L2Hostility, L2Library, Refined Storage, CTM, JEI, BOMD, GeckoLib, CerbonsAPI, L2Complements, KineticCore, AdventureSystems, Twilight Forest |
| RealmControl | KineticCore, Architectury, Jade, KubeJS, Rhino, Explorer's Compass (edited), Nature's Compass |
| TACZWorkshop | KineticCore, TaCZ, Sophisticated Backpacks, Sophisticated Core |
| TextStudio | KineticCore |

The initial source versions observed locally include first-party KineticCore `26.9.25`, AdventureSystems `26.9.12`, ContentStudio `26.9.12`, and ModRefinery `26.9.23`; and third-party Curios `5.14.1`, Architectury `9.2.14`, JEI `15.20.0.130`, KubeJS `2001.6.5-build.16`, Rhino `2001.2.12`, TaCZ `1.1.8`, FTB Library `2001.2.12`, FTB Quests `2001.4.22`, FTB Teams `2001.3.2`, Jade `11.13.2`, Refined Storage `1.12.4`, Sophisticated Backpacks `3.24.53.1877`, Sophisticated Core `1.3.50.2005`, Alex's Mobs `1.22.9`, Enhanced AI `3.3.6`, Legendary Tooltips `1.4.5`, Sol Carrot `1.15.1`, Explorer's Compass Edited `1.4.0`, Nature's Compass `1.11.2`, Waystones `14.1.18`, Balm `7.3.38`, L_Ender's Cataclysm `3.27`, Registrate `1.3.3`, Goety `2.5.52.2`, Iron's Spells 'n Spellbooks `3.15.2`, L2Hostility `2.5.10`, L2Library `2.5.3`, CTM `1.1.10`, BOMD `1.1.2`, GeckoLib `4.8.3`, CerbonsAPI `1.1.0`, and L2Complements `2.6.1`. The local Explorer's Compass Edited filename says `1.3.3`, but its manifest identifies the `1.4.0` build; its SHA-256 matches CurseForge file `7175635`, so it has a verified public source. The Twilight Forest JAR has no version in its filename; its version must be read from the JAR metadata before pinning. No `+`, `latest`, or otherwise floating mod versions will be introduced.

The current ModRefinery declaration contains many dependencies. During implementation, source imports, mixin targets, reflection lookups, and runtime metadata will be checked before removing any entry. A dependency used only by an optional compatibility path remains necessary when that path is enabled by this project.

The local `explorerscompass-edited-1.3.3.jar` is byte-for-byte the public Explorer's Compass Edited 1.20.1 Forge file. Replace it in both RealmControl and ModRefinery with the pinned CurseMaven coordinate `curse.maven:explorers-compass-edited-1203888:7175635`.

## Dependency and repository design

1. Replace every local JAR helper and `flatDir` repository in the ten public projects with named, version-pinned dependencies. Remove all references to `D:/IDEA_Caches/libs` and per-machine library paths from those ten Gradle roots. For the three private projects, replace only dependencies with exact public equivalents; explicitly retain private/local-only inputs.
2. Prefer the upstream artifact repository documented by the mod author. Use Forge Maven and Maven Central for their own artifacts; use vendor repositories for FTB, Architectury, KubeJS/Rhino, Curios, JEI, Sophisticated mods, and other projects where the upstream publishes there. For mods distributed as files rather than Maven modules, use Modrinth Maven or CurseMaven with an immutable version/file identifier. Add repository content filters so each repository is queried only for its groups.
3. Pin Minecraft 1.20.1 Forge-compatible variants and exact versions. Do not replace an unavailable artifact with a newer Minecraft branch, an unverified mirror, or a different mod file. In the private projects, if the upstream does not provide an anonymous artifact, retain the current local file rather than making the project or artifact public.
4. Keep KineticCore's normal mod JAR as the game/runtime implementation. Add an `apiJar` task that packages only compiled classes under `dev/xyat/kineticcore/api/**`, with no KineticCore `mods.toml`, mixin files, assets, internal packages, or embedded runtime libraries. Attach this JAR as `kineticcore-api-<version>.jar` to the manual KineticCore GitHub Release. Consumers resolve the pinned API artifact from the public release asset using a small shared Gradle download/Ivy repository pattern and wrap it with ForgeGradle deobfuscation where required. A consumer's normal compile/build must download only this API JAR for KineticCore.
5. The API JAR is a compile surface. The full KineticCore mod remains a required runtime mod when launching a game or running a modpack. If a development `runClient` configuration needs the full implementation, keep that as a separately scoped, opt-in development runtime dependency; it must not leak back into the ordinary compile dependency.
6. Replace `implementation` with `compileOnly` for provided mod APIs when the artifact is needed only to compile, and add it to the local development runtime only when that run configuration actually needs the mod. Do not shade or bundle upstream mod JARs. Keep runtime requirements and version ranges in `mods.toml` authoritative for players.
7. Keep versions in each repository's existing properties/configuration, but make the artifact coordinate and version explicit. Any shared helper used by these independent repositories must be copied or versioned so a checkout does not depend on files outside its repository.

## First-party project dependencies

The source audit found these Java-level edges in addition to KineticCore API use:

| Consumer | First-party classes used | Artifact source |
| --- | --- | --- |
| CombatSystems | `ContentStudio` loot entry and server override store | Pinned JAR attached to the public ContentStudio GitHub Release |
| ModRefinery | AdventureSystems wallet `Data` and `Network` classes | Pinned JAR attached to the public AdventureSystems GitHub Release |
| MobAscension | ModRefinery EnhancedAI and L2Hostility compatibility classes | Keep the current local ModRefinery JAR; do not publish the private project or add private package credentials |

The ten public GitHub repositories can publish release assets anonymously. The other three project repositories and their first-party JARs remain private/local. No GitHub Packages credentials or public first-party artifact for those projects will be introduced.

## Manual release workflow

Each of the ten currently public projects—AdventureSystems, ContentStudio, EnchantWorks, EntityControl, ItemControl, KineticArmory, KineticCore, RealmControl, TACZWorkshop, and TextStudio—gets a GitHub Actions workflow triggered only by `workflow_dispatch`. The form takes an explicit date-based release version in `YY.M.D` form; the workflow passes that exact value to Gradle with automatic versioning disabled, rejects an existing tag/release, runs the project's Gradle build with its output redirected to a runner-local release directory, and uploads the built mod JAR to a GitHub Release. KineticCore also uploads the API-only JAR. It does not run on push or tag creation and does not call the CurseForge upload API. The resulting JAR remains ready for the user to upload to CurseForge manually.

Private projects do not get public mod releases or publishing workflows. Their Gradle files may point at public third-party and public first-party dependencies, while private project-to-project artifacts remain local.

KineticCore has no existing GitHub Release. Its first `workflow_dispatch` release must run before KineticCore consumer builds can resolve the public API artifact. Public ContentStudio and AdventureSystems releases must also exist before private CombatSystems and ModRefinery builds resolve those first-party dependencies. No release is triggered as part of implementing this migration.

GitHub documents `workflow_dispatch` as a manually runnable event in the Actions tab, and public Release assets have stable version-specific download URLs. These make a release asset suitable for the anonymous KineticCore API artifact.

## Build behavior and acceptance criteria

- All ten public projects configure without `flatDir`, `localMod(...)`, `files('...jar')`, or `D:/IDEA_Caches/libs` references.
- In the three private projects, every dependency with an exact, public, compatible artifact resolves online; only the explicitly documented private/local-only dependencies remain local.
- A build of the ten public projects with an empty temporary Gradle user cache downloads its declared dependencies from configured online repositories; it never copies or searches the local shared `libs` directory.
- KineticCore produces both the regular game mod JAR and an API JAR. Inspecting the API JAR shows only `dev/xyat/kineticcore/api/**` class files and no core implementation or runtime resources.
- Each KineticCore consumer compiles against that API artifact. Ordinary compilation does not resolve the complete KineticCore mod JAR.
- Each direct library dependency is retained only when Java source, mixins/reflection, generated sources, or runtime development launch requires it. Mod runtime dependencies remain declared in mod metadata.
- Cross-project compilation in public projects resolves exact public Release versions. A missing asset fails clearly.
- All ten public Gradle `build` tasks pass from a fresh cache after the prerequisite Core API Release exists. Private project builds are checked with their retained local first-party jars and online public dependencies.
- Release workflows can be statically checked for manual-only triggers and explicit version input. A validation/build-only option can be used for workflow checks; no actual release is published during implementation.
- Each public release includes the mod JAR expected for manual CurseForge upload; the Core release also includes the API JAR.

## Risks and operational notes

- The KineticCore API JAR is a binary surface with version coupling. Each consumer pins a specific Core API version and must be updated when it adopts a newer Core API.
- Modrinth and CurseMaven use provider-specific immutable version/file identifiers. A human-readable mod version alone is not sufficient for reproducible resolution; the selected file must also be checked for Minecraft 1.20.1 and Forge.
- The private ModRefinery edge cannot be anonymously downloaded and will intentionally remain local. This exception applies only to private projects and does not affect builds of the ten public projects.
- Normal compilation can use API artifacts, but an actual development game launch still needs runtime mod implementations required by the mod metadata. The build will keep compile and dev-runtime configurations separate.
- Repository downtime, deleted upstream files, or withdrawn releases can prevent clean builds. Exact versions and source URLs will be recorded with the dependency update so a future migration can be audited.

## Source references

- [KineticCore public repository](https://github.com/JMK-39/kineticCore)
- [GitHub Release asset download URLs](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases)
- [GitHub manual workflow runs](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
- [Explorer's Compass Edited 1.20.1 Forge file and Curse Maven coordinate](https://www.curseforge.com/minecraft/mc-mods/explorers-compass-edited/files/7175635)
