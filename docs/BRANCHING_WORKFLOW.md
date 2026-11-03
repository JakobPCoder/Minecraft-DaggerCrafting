# Git branching workflow (26.x)

This repo supports **Minecraft 26.x only**. Older `1.21.x` branches are **legacy and frozen**: do not add features there.

You do not need to be a Git expert. Copy the PowerShell commands as written. Use `;` between commands (not `&&`).

## Branch names (`<mc>-<mod>`)

**New work** uses Minecraft version + mod version in the branch name (see also `.cursor/rules/branch-naming.mdc`).

| Purpose | Pattern | Example |
| --- | --- | --- |
| Integration line for one MC + mod release | `<mc>-<mod>` | `26.2-1.5.0`, `26.3-1.4.0` |
| Feature | `<mc>-<mod>-<feature>` | `26.2-1.5.0-custom-arrows` |

1. Create the **line branch** from the previous line on that Minecraft version (version bump / changelog stub only — no features yet).
2. Branch features off that line; merge features **into** the line branch.
3. Release from the line branch; tags stay `v<mod>-<mc>` (e.g. `v1.4.0-26.2`).

`main` is the latest **released** 26.2 line. Do not commit features on `main`. Do not add features on legacy `1.21.x-*` or on frozen old lines such as `26.3-1.2.1`.

The `base/*` and `feature/*` layout at the bottom of this file is **historical**. Do not create those names for new work.

## Source of truth: 26.2, then merge to 26.3

Gameplay and shared code are written on **26.2**. Each 26.3 line is a **port** of the same mod version: same features, plus only the files Minecraft 26.3 requires.

Git merge does this if you branch 26.3 from the previous 26.3 line, then merge the 26.2 line in:

- Files only 26.3 changed (toolchain, pack format, 26.3 API names) **stay as they are**.
- Files only 26.2 changed (new features, README, most Java) **get the 26.2 updates**.
- Files both sides edited **conflict**. Keep 26.3 API names and take the new gameplay.

Do **not** rebase 26.3 onto 26.2, cherry-pick a whole feature line, or use `merge -X ours` / `-X theirs` as the default.

Typical 26.3-only files today: `gradle.properties` (Minecraft / Fabric version), `fabric.mod.json`, `pack.mcmeta`, `DaggerEntity.java` (`syncVelocity`, `findHitEntities`, 5-arg `deflect`), `DaggerEntityRenderer.java` (`rotate` instead of `mulPose`).

`gradle.properties` almost always conflicts on a version bump: keep **26.3 engine versions** and the **new `mod_version`**.

## Recipe: start a new mod version on 26.2

Example: 1.4.0 is released, start **1.5.0** on Minecraft 26.2.

```powershell
git switch 26.2-1.4.0
git pull
git switch -c 26.2-1.5.0
```

Bump `mod_version` in `gradle.properties` (and a changelog stub if you want). Commit that. Do not put the bump on `main`.

Features:

```powershell
git switch 26.2-1.5.0
git switch -c 26.2-1.5.0-foo
```

Merge `26.2-1.5.0-foo` **into** `26.2-1.5.0` when the feature is done. Compile on 26.2.

## Recipe: make the same mod version on 26.3

Do this when 26.2 1.5.0 is ready to port (or whenever you want a 26.3 line for that mod version). **Never merge 1.5.0 into `26.3-1.4.0`.** Open a new 26.3 line first.

```powershell
git switch 26.3-1.4.0
git pull
git switch -c 26.3-1.5.0
git merge 26.2-1.5.0
```

If Git reports conflicts, keep 26.3 method names / toolchain values plus the 1.5.0 gameplay, then:

```powershell
git add -A
git commit
```

Rebuild on 26.3:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
```

Do **not** develop 1.5.0 on 26.3 first and copy back to 26.2.

## Recipe: new Minecraft version (port only)

When a new MC needs a port of an **already shipped** mod version, branch from that version's previous MC line. Put **only** port fixes there (Gradle, `pack.mcmeta`, `fabric.mod.json`, compile/API). No new daggers or enchantments.

Example: 1.4.0 already exists on 26.2, add 26.3:

```powershell
git switch 26.2-1.4.0
git pull
git switch -c 26.3-1.4.0
```

Port, commit, compile. Later features still land on 26.2 and merge forward with the recipe above.

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
- `gradle.properties` `minecraft_version` matches the branch (`26.2` on `26.2-*`, `26.3` on `26.3-*`).
- No scratch files: `.cursor/mc-*-src/`, `cf_page.html`, `run/`, `.gradle/` (these are gitignored).
- After a port or a 26.2→26.3 merge, `git diff 26.2-<mod> 26.3-<mod>` should show **only** port files (toolchain, pack format, version range, 26.3 API names) plus version-specific notes. Gameplay must not exist on only one of the two lines.

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
