# Git branching workflow (26.x and 1.21.5)

This repo keeps **three live integration lines** per mod release when that release ships on more than one Minecraft version:

- **`26.3-<mod>`** — source of truth for gameplay and shared design.
- **`26.2-<mod>`** — backport of the same mod version on Minecraft 26.2.
- **`1.21.5-<mod>`** — backport of the same mod version on Minecraft 1.21.5 (Yarn).

Only **1.21.4 and older** `1.21.x` lines are **legacy and frozen**: do not add features there.

You do not need to be a Git expert. Copy the PowerShell commands as written. Use `;` between commands (not `&&`).

## Branch names (`<mc>-<mod>`)

**New work** uses Minecraft version + mod version in the branch name (see also `.cursor/rules/branch-naming.mdc`).

| Purpose | Pattern | Example |
| --- | --- | --- |
| Integration line for one MC + mod release | `<mc>-<mod>` | `26.3-1.5.0`, `26.2-1.5.0`, `1.21.5-1.5.0` |
| Feature | `<mc>-<mod>-<feature>` | `26.3-1.5.0-shorter-reach` |

1. Create the **line branch** from the previous line on that Minecraft version (version bump / changelog stub only — no features yet).
2. Branch features off that line; merge features **into** the line branch.
3. Release from the line branch; tags stay `v<mod>-<mc>` (e.g. `v1.4.0-26.2`).

`main` is the latest **released** line. Do not commit features on `main`. Do not add features on frozen `1.21.4-*` and older `1.21.x-*`, or on abandoned old lines such as `26.3-1.2.1`.

The `base/*` and `feature/*` layout at the bottom of this file is **historical**. Do not create those names for new work.

## Source of truth: 26.3

Gameplay and shared code land on **26.3** first. **`26.2-<mod>`** and **`1.21.5-<mod>`** are **backports** of that mod version: same product intent, plus only what each Minecraft version and mappings still require.

Do **not** start a feature on 26.2 or 1.21.5 and merge it forward. Do **not** rebase 26.3 onto a backport line, cherry-pick a whole feature line, or use `merge -X ours` / `-X theirs` as the default.

Files that differ per backport (typical 1.5.0 set):

- **26.2:** `gradle.properties`, `fabric.mod.json`, `pack.mcmeta`, `DaggerEntity.java` (`velocity` sync vs `hurtMarked`, `deflect` arity, missing `findHitEntities` on 26.2), `DaggerEntityRenderer.java` (`mulPose` vs `rotate` on 26.3).
- **1.21.5:** all of the above at the Yarn/API layer, plus Yarn names, `ToolComponent` / `DataComponentTypes`, `Registries.BLOCK` tag lookup, renderer and particle API, and Java **21** toolchain (not JDK 25).

## Recipe: start a new mod version on 26.3

Example: 1.4.0 is released, start **1.5.0** on Minecraft 26.3.

```powershell
git switch 26.3-1.4.0
git pull
git switch -c 26.3-1.5.0
```

Bump `mod_version` in `gradle.properties` (and a changelog stub if you want). Commit that. Do not put the bump on `main`.

Features:

```powershell
git switch 26.3-1.5.0
git switch -c 26.3-1.5.0-foo
```

Merge `26.3-1.5.0-foo` **into** `26.3-1.5.0` when the feature is done. Compile on 26.3.

## Recipe: backport the same mod version to 26.2

Do this only when that Minecraft version still ships. **Never merge 1.5.0 into `26.2-1.4.0`.** Open a new 26.2 line first.

```powershell
git switch 26.2-1.4.0
git pull
git switch -c 26.2-1.5.0
git merge 26.3-1.5.0
```

If Git reports conflicts, keep 26.2 method names / toolchain values and take the 1.5.0 gameplay, then:

```powershell
git add -A
git commit
```

Rebuild on 26.2:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
```

`gradle.properties` almost always conflicts on a version bump: keep **26.2 engine versions** and the **new `mod_version`**.

After merging, `git diff 26.3-<mod> 26.2-<mod>` should show **only** port files (toolchain, pack format, version range, API names) plus documented version-specific gaps. Gameplay must not exist on only one of the lines.

## Recipe: backport the same mod version to 1.21.5

Do this when **1.21.5** still ships for that mod version. **Never squash-merge `26.3-<mod>` or `26.2-<mod>` into a 1.21.5 line** — a merge imports Mojmap/26.x Java into a Yarn tree.

```powershell
git switch 1.21.5-1.4.0
git pull
git switch -c 1.21.5-1.5.0
```

Bump `mod_version` in `gradle.properties`, commit the line start, then **hand-apply** the 1.5.0 gameplay delta from `26.3-1.5.0` using Yarn names and the 1.21.5 toolchain (see `docs/PORTING_1.21.5_TO_26.2.md`).

Build on 1.21.5 with JDK 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot'
.\gradlew.bat compileJava
.\gradlew.bat build
```

Post-port check: `git diff 26.2-<mod> 1.21.5-<mod>` must show only the Yarn/port delta plus **documented** gameplay gaps (for example, no `attack_range` on 1.21.5 — see `docs/PORTING_1.21.5_TO_26.2.md`).

## Recipe: new Minecraft version (port only)

When a newer MC needs a port of an **already shipped** mod version, branch from the 26.3 line of that version. Put **only** port fixes there (Gradle, `pack.mcmeta`, `fabric.mod.json`, compile/API). No new daggers or enchantments.

```powershell
git switch 26.3-1.5.0
git pull
git switch -c 26.4-1.5.0
```

Port, commit, compile. Later features still land on the newest line and backport with the recipe above.

## Everyday commands

```powershell
git status
git branch -vv
```

Throw away **uncommitted** edits in one file (cannot be undone):

```powershell
git restore path\to\file
```

Stop a merge that went wrong:

```powershell
git merge --abort
```

## Pre-merge checklist

- `.\gradlew.bat build` succeeds on the Minecraft version of that branch.
- `gradle.properties` `minecraft_version` matches the branch (`26.2` on `26.2-*`, `26.3` on `26.3-*`, `1.21.5` on `1.21.5-*`).
- No scratch files: `.cursor/mc-*-src/`, `cf_page.html`, `run/`, `.gradle/` (these are gitignored).
- After a 26.3→26.2 backport, `git diff 26.3-<mod> 26.2-<mod>` should show **only** port files plus documented gaps.
- After a 1.21.5 backport, `git diff 26.2-<mod> 1.21.5-<mod>` should match the known Yarn delta plus documented gap files (not extra gameplay on one line only).

## Tags

Example: `v1.4.0-26.2` is mod version **1.4.0** built for Minecraft **26.2**. Same mod version on 26.3 is `v1.4.0-26.3`.

---

## Historical `base/*` / `feature/*` (do not use for new work)

Older clones used frozen port bases and shared feature branches:

```
base/26.2 --> base/26.3 --> base/26.7 (later)
    |              |
    +-- feature/x --+--> release/26.2
                   \--> release/26.3
```

| Branch | What belonged there |
| --- | --- |
| `main` | Latest released state |
| `base/26.2`, `base/26.3`, … | Feature-frozen ports. Toolchain and API only |
| `feature/<name>` | Features started from the oldest base |
| `release/26.x` | Optional: one base plus merged features |

If you still have those branches, do not add new gameplay there. Use `<mc>-<mod>` instead.
