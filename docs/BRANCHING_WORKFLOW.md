# Git branching workflow (26.x)

This repo supports **Minecraft 26.x only**. Older `1.21.x` branches are **legacy and frozen**: do not add features there.

You do not need to be a Git expert. Copy the PowerShell commands as written. Use `;` between commands (not `&&`).

## Branch names in use now (`<mc>-<mod>`)

**New work** uses Minecraft version + mod version in the branch name (see also `.cursor/rules/branch-naming.mdc`).

| Purpose | Pattern | Example |
| --- | --- | --- |
| Integration line for one release | `<mc>-<mod>` | `26.2-1.3.0` (current), `26.2-1.4.0` (next line) |
| Feature | `<mc>-<mod>-<feature>` | `26.2-1.4.0-custom-arrows` |

1. Create the **line branch** when you bump `mod_version` (ideally empty aside from version/metadata).
2. Branch features off that line; merge features **into** the line branch.
3. Release from the line branch; tags stay `v<mod>-<mc>` (e.g. `v1.3.0-26.2`).

Per-Minecraft lines are separate (e.g. `26.3-1.2.1`). Do not add features on legacy `1.21.x-*` branches.

The `base/*` and `feature/*` layout below is **historical**; prefer `<mc>-<mod>` for new branches unless you are maintaining an old clone workflow.

## Layers (historical `base/*` / `feature/*`)

```
base/26.2 --> base/26.3 --> base/26.7 (later)
    |              |
    +-- feature/x --+--> release/26.2
                   \--> release/26.3
```

| Branch | What belongs there |
| --- | --- |
| `main` | Latest **released** state. Merge into it only from a release branch, not from random feature work. |
| `base/26.2`, `base/26.3`, … | **Feature-frozen ports.** Toolchain, pack format, and API fixes for that Minecraft version. **Never** new gameplay. |
| `feature/<name>` | New features. Always start from the **oldest supported base** (`base/26.2` today) so the same branch can merge forward. |
| `release/26.x` | Optional later: one `base/26.x` plus merged features. Used to build jars. |

**Rules**

- New Minecraft versions: branch the next `base/*` from the previous `base/*`. Put **only** port fixes on it.
- Features live on `feature/*`. Merge them into `release/26.x` (or the version branch you ship). **Do not cherry-pick features into `base/*`.**
- Porting breaks are fixed **once** on the new `base/*`, so later feature merges stay clean.
- Do not force-push shared branches (`base/*`, `release/*`, `main`, `feature/*` that others use).

## Everyday commands

See where you are and what changed:

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

## Recipe: start a new feature

Branch from the oldest supported base so the work can merge into every 26.x release:

```powershell
git switch base/26.2
git pull
git switch -c feature/foo
```

Edit, then:

```powershell
git add -A
git status
git commit -m "Add foo: short why, not a file list"
git push -u origin feature/foo
```

## Recipe: bring a feature onto another version

When `release/26.3` exists:

```powershell
git switch release/26.3
git pull
git merge feature/foo
```

If Git reports conflicts, open the listed files, keep the 26.3 API names plus the new feature, then:

```powershell
git add -A
git commit
```

Rebuild:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
```

Until `release/26.x` exists, merge into a local test branch from that version's `base/*` the same way, and fix port conflicts there. Still **do not** commit the feature onto `base/26.3`.

## Recipe: new Minecraft version (example 26.7)

```powershell
git switch -c base/26.7 base/26.3
```

Port **only** (Gradle, `pack.mcmeta`, `fabric.mod.json`, compile fixes). No new daggers or enchantments.

```powershell
git add -A
git commit -m "Port to Minecraft 26.7 (no feature changes)"
git push -u origin base/26.7
git tag v1.2.1-26.7
git push origin v1.2.1-26.7
```

Then merge existing `feature/*` branches into the new `release/26.7` (or equivalent), not into `base/26.7`.

## Recipe: bug in shared base code

Fix on the **oldest** base that has the bug, then merge **forward**:

```powershell
git switch base/26.2
git pull
# fix, commit
git switch base/26.3
git pull
git merge base/26.2
```

Resolve conflicts (keep 26.3-only API names). Repeat for newer bases.

## Recipe: keep your clone up to date

```powershell
git switch base/26.2
git pull
git switch base/26.3
git pull
```

On a feature branch, rebase or merge the oldest base you started from if you need its bugfixes. Ask before rewriting history on a branch others already pulled.

## Pre-merge checklist

- `.\gradlew.bat build` succeeds.
- `gradle.properties` `minecraft_version` matches the branch (`26.2` on `base/26.2`, `26.3` on `base/26.3`).
- No scratch files: `.cursor/mc-*-src/`, `cf_page.html`, `run/`, `.gradle/` (these are gitignored).
- `git diff base/26.2 base/26.3` shows **only** port files (toolchain, pack format, version range, 26.3 API renames) plus version-specific notes. Feature commits must not appear only on one base.

## Tags

Example: `v1.2.1-26.2` is mod version **1.2.1** built for Minecraft **26.2**. Same mod version on 26.3 is `v1.2.1-26.3`.
