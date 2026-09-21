# IDEA warning cleanup implementation plan

**Goal:** Apply the user's selected safe inspection fixes while preserving unused declarations and runtime behavior.

**Architecture:** Make local source changes only. Check inherited nullability using compiler type information; preserve event subscriptions for their intended lifetime.

**Tech stack:** Java 17, Forge 1.20.1, Gradle 8.8.

**Spec:** User screenshots and instructions in this session: inverted boolean helpers, redundant null guards, missing override annotations, resource handling, fields convertible to locals, unreachable labels, dangling Javadoc, redundant locals, method references, redundant throws and conditions. Do not remove unused declarations.

## Constraints and review focus

- Preserve all existing user changes and public API behavior.
- Boolean inversion must preserve short circuiting, null handling, and every call site.
- Subscription handles must remain active after registration returns.
- Nullability annotations must follow inherited contracts, preserving nullable parameters.
- Keep comment contents and avoid unused-member cleanup.
- Verify with compileJava, project API/architecture checks, and existing relevant verification tasks; no implementation-mirroring tests for cosmetic edits.

## Tasks

- [x] Inspect screenshot locations and collect equivalent source diagnostics; snapshot the current source.
- [x] Fix private inverted helpers, null guards, local fields/variables, Javadoc, method references, redundant throws/conditions and the unreachable enum branch.
- [x] Audit inherited parameter contracts and add missing nonnull annotations.
- [x] Preserve long-lived subscription handles to address resource-lifetime warnings.
- [x] Compile, run relevant existing checks, review the changes against the source snapshot, and report any skipped uncertain categories.

## Verification results

- Updated 28 production Java files and one existing source-contract regression assertion.
- 25 private helpers now express the polarity their callers use; 29 inherited nonnull parameters are annotated; 17 adjacent Javadoc pairs are merged.
- Preserved long-lived NBT logout subscription via a retained handle; kept network-to-client lambdas to avoid eager client class resolution on dedicated servers.
- Gradle 8.8 check passed, including API, architecture, compilation and widget state checks.
- 41 additional regression assertions passed across numeric, search and config rollback checks.
- Four extra client regressions fail without a Forge client runtime; the same startup failures were reproduced with baseline classes compiled from the pre-edit snapshot.
- Independent snapshot-based review found no remaining critical or important issue. Floating-point negations retain NaN semantics.
- Unused declarations remain unchanged. No runtime build configuration or warning suppression was added.
