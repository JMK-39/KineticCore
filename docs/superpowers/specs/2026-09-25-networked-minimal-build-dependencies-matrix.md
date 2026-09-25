# Networked Dependency Matrix

This matrix records the versioned artifacts selected for the Gradle migration. Modrinth entries use immutable version IDs so the loader and game build cannot drift. `fg.deobf(...)` is applied to mod JARs when used from ForgeGradle. Unless noted otherwise, these replace existing `implementation` declarations, preserving the development runtime classpath without shading the upstream mod into the output JAR.

## First-party releases

| Artifact | Pinned version | Gradle coordinate | Source and asset layout | Consumers |
| --- | --- | --- | --- | --- |
| KineticCore complete mod | `26.9.25` | `dev.xyat.kineticcore:kineticcore:26.9.25` | `https://github.com/JMK-39/kineticCore/releases/download/v[revision]/[artifact]-[revision].jar` | All projects except KineticCore |
| ContentStudio complete mod | `26.9.25` | `dev.xyat.contentstudio:contentstudio:26.9.25` | `https://github.com/JMK-39/ContentStudio/releases/download/v[revision]/[artifact]-[revision].jar` | Private CombatSystems |
| AdventureSystems complete mod | `26.9.25` | `dev.xyat.adventuresystems:adventuresystems:26.9.25` | `https://github.com/JMK-39/AdventureSystems/releases/download/v[revision]/[artifact]-[revision].jar` | Private ModRefinery |

All are complete, reobfuscated mod JARs. No API-only artifact is built. At the time of the audit, these GitHub repositories had no Releases; consumer builds that need one of these first-party assets remain pending until the author manually runs its release workflow.

## FTB Maven artifacts

| Mod | Version | Gradle coordinate | Repository | Consumer |
| --- | --- | --- | --- | --- |
| FTB Library for Forge | `2001.2.12` | `dev.ftb.mods:ftb-library-forge:2001.2.12` | `https://maven.ftb.dev/releases` | AdventureSystems |
| FTB Quests for Forge | `2001.4.22` | `dev.ftb.mods:ftb-quests-forge:2001.4.22` | `https://maven.ftb.dev/releases` | AdventureSystems |
| FTB Teams for Forge | `2001.3.2` | `dev.ftb.mods:ftb-teams-forge:2001.3.2` | `https://maven.ftb.dev/releases` | AdventureSystems |

## Modrinth Maven artifacts

All rows below are Forge files for Minecraft 1.20.1. Repository: `https://api.modrinth.com/maven`, restricted to group `maven.modrinth`. Modrinth version IDs are immutable and were checked against the v2 version metadata and the artifact POM URL.

| Mod | Exact upstream version | Gradle coordinate | Consumer projects |
| --- | --- | --- | --- |
| Curios API | `5.14.1+1.20.1` | `maven.modrinth:curios:IPQlZkz1` | AdventureSystems, EntityControl, KineticArmory, KineticCore, ModRefinery |
| Refined Storage | `1.12.4` | `maven.modrinth:refined-storage:ZITLFjjf` | AdventureSystems, ModRefinery |
| Sophisticated Core | `1.20.1-1.3.50.2005` | `maven.modrinth:sophisticated-core:t68nu57v` | AdventureSystems, TACZWorkshop |
| Sophisticated Backpacks | `1.20.1-3.24.53.1877` | `maven.modrinth:sophisticated-backpacks:i8yHivUh` | AdventureSystems, TACZWorkshop |
| JEI | `15.20.0.130` | `maven.modrinth:jei:RTFeXsvE` | AdventureSystems, ContentStudio, ItemControl, KineticCore, ModRefinery |
| KubeJS Forge | `2001.6.5-build.16+forge` | `maven.modrinth:kubejs:g5igndAv` | ItemControl, KineticArmory, ModRefinery, RealmControl |
| Rhino Forge | `2001.2.3-build.10+forge` | `maven.modrinth:rhino:uNALdylI` | ItemControl, KineticArmory, ModRefinery, RealmControl |
| Timeless & Classics Zero | `1.1.8` | `maven.modrinth:timeless-and-classics-zero:C2pCZ5ht` | CombatSystems, TACZWorkshop |
| Alex's Mobs | `1.22.9` | `maven.modrinth:alexs-mobs:XoIASRVU` | ModRefinery |
| Enhanced AI | `3.3.6` | `maven.modrinth:enhanced-ai:EtXZ60iI` | ModRefinery |
| Legendary Tooltips | `1.4.5` | `maven.modrinth:legendary-tooltips:JhxD2e6J` | ModRefinery |
| Nature's Compass | `1.20.1-1.11.2-forge` | `maven.modrinth:natures-compass:Og40jCNX` | ModRefinery, RealmControl |
| Waystones | `14.1.18+forge-1.20.1` | `maven.modrinth:waystones:Z7sBfgIR` | ModRefinery |
| Balm | `7.3.38+forge-1.20.1` | `maven.modrinth:balm:8rz04Kha` | ModRefinery |
| Goety | `2.5.52.2` | `maven.modrinth:goety:9ZQT26U1` | ModRefinery |
| Iron's Spells 'n Spellbooks | `1.20.1-3.15.2` | `maven.modrinth:irons-spells-n-spellbooks:T1VAZjU6` | ModRefinery |
| L2 Hostility | `2.5.10` | `maven.modrinth:l2hostility:S1Gdt2wI` | MobAscension, ModRefinery |
| L2 Library | `2.5.3` | `maven.modrinth:l2library:lg7mW3eS` | MobAscension, ModRefinery |
| GeckoLib | `4.8.3` | `maven.modrinth:geckolib:HVdLnQMI` | ModRefinery |
| Cerbons API | `1.1.0` | `maven.modrinth:cerbons-api:XWZQbKsr` | ModRefinery |
| L2 Complements | `2.6.1` | `maven.modrinth:l2-complements:TvdiYrC4` | ModRefinery |
| Bosses of Mass Destruction Forge | `1.1.2` | `maven.modrinth:bosses-of-mass-destruction-forge:2Kw4xPhp` | ModRefinery |
| L_Ender's Cataclysm | `3.27` | `maven.modrinth:l_enders-cataclysm:TFKWu05d` | ModRefinery |

## CurseMaven artifacts

Repository: `https://cursemaven.com`, restricted to group `curse.maven`. The identifiers are immutable CurseForge project/file IDs and match the existing 1.20.1 Forge JARs.

| Mod | CurseForge project/file | Gradle coordinate | Consumer projects |
| --- | --- | --- | --- |
| Jade | `324717 / 6855440` | `curse.maven:jade-324717:6855440` | EntityControl, RealmControl |
| Explorer's Compass Edited | `1203888 / 7175635` | `curse.maven:explorers-compass-edited-1203888:7175635` | RealmControl, ModRefinery |
| Spice of Life: Carrot Edition | `277616 / 4888575` | `curse.maven:spice-of-life-carrot-edition-277616:4888575` | ModRefinery |
| ConnectedTexturesMod | `267602 / 5983309` | `curse.maven:ctm-267602:5983309` | ModRefinery |
| The Twilight Forest | `227639 / 5468648` | `curse.maven:the-twilight-forest-227639:5468648` | ModRefinery |

Jade's CurseForge file `6855440` is the 1.20.1 Forge build `Jade-1.20.1-Forge-11.13.2.jar` ([upstream file](https://www.curseforge.com/minecraft/mc-mods/jade/files/6855440)).

## Author Maven artifact

| Mod | Version | Gradle coordinate | Repository | Consumer |
| --- | --- | --- | --- | --- |
| Architectury API for Forge | `9.2.14` | `dev.architectury:architectury-forge:9.2.14` | `https://maven.architectury.dev/` | AdventureSystems, ItemControl, KineticArmory, ModRefinery, RealmControl |
| Registrate for Minecraft 1.20 | `MC1.20-1.3.3` | `com.tterrag.registrate:Registrate:MC1.20-1.3.3` | `https://maven.tterrag.com/` | ModRefinery |

## Public Maven mirror artifact

| Library | Version | Gradle coordinate | Repository | Consumer |
| --- | --- | --- | --- | --- |
KineticCore's Chinese item search uses its own Java transliteration and matching code. The checked-in reading table is derived from the Unicode 18.0.0 Unihan `kMandarin` field, with the first reading selected for Simplified Chinese; the table is data only and introduces no runtime library or repository dependency. See [the bundled-data notice](../../src/main/resources/assets/kineticcore/pinyin/NOTICE.txt), [the Unicode license](../../src/main/resources/assets/kineticcore/pinyin/UNICODE-LICENSE.txt), and the [table generator](../../tools/generate_pinyin_data.ps1).

## Deliberate private exception

MobAscension continues to consume the private ModRefinery JAR from the existing local library directory. Do not publish that private artifact or introduce credentials. ModRefinery itself consumes public third-party dependencies online and the public AdventureSystems/KineticCore releases.
