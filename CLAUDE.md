# capacitor-plugins

Small Capacitor plugins for the bats apps (bats-lang/quire#321), one
per folder, `packages/<name>/`. bridge names the plugins an app uses,
and pwa's generated app installs exactly those, each from a commit of
this repository's main and its folder, so one plugin's manifest and
permissions never reach an app that does not use it.

## Decisions are made by research, not left to the human

No task is left to the human unless an agent physically cannot do it (a
permission it was denied, access it does not have, a secret it cannot
see). Every other question, design choices included, is settled by
research (what the platform documents, what other plugins and apps do)
and best judgement, written down where it is decided (the issue or the
pull request), and then done. What needs the owner (a setting, a secret,
an account) is filed as a "human only" issue with the exact steps and
how to verify them.

## A plugin is a minimal wrapper

- A plugin wraps one platform API, 1:1: its methods are that API's
  calls, named as the platform (or an established API of the same
  shape) names them, and its answers are what the API answers. What to
  do with them (when to ask, what to show, what to store) is the app's,
  in Bats: no app policy lives here.
- A method that a platform does not have is unimplemented there, not
  emulated: a plugin registered with no web implementation rejects
  with Capacitor's `UNIMPLEMENTED` in a browser, and a plugin with no
  `ios/` has none on iOS.
- A new plugin, or a new method, comes only when nothing existing does
  it. Its pull request has a **Justification** section that says, with
  evidence (code, the platform's documentation, the plugins compared):
  1. that no existing plugin, nor anything the apps already have, does
     it;
  2. why it cannot be smaller (fewer methods, fewer options), and that
     it maps 1:1 to the platform API;
  3. the alternatives considered, and why each would not do.

## Every outcome is distinct, and the unexpected is said as such

(bats-lang/quire#334: a Google error that ended the consent screen was
answered as the reader backing out, so the app said nothing useful.)

- A plugin reports every error and unexpected condition as an outcome
  of its own: a cancel is answered only when the platform says the
  reader canceled, and a platform error with the platform's code and
  message. One outcome is never folded into another ("anything not OK
  is CANCELED"). An activity that ends with RESULT_CANCELED and no
  intent counts as the platform's cancel: it is Android's convention
  for the reader backing out (the back gesture, or the activity
  finishing without a result), and there is nothing else to read.
- Options a method does not take (no scopes, a blank scope) are the
  plugin's `INVALID_OPTIONS`, checked before the platform is called, so
  the platform's own failure to build a request is never what answers
  them.
- What the plugin does not recognise (an exception the platform does
  not document, a status code it does not name, an answer missing what
  it must hold) is answered as `UNEXPECTED`, with a message that says
  what it was: never as a known code.
- Each outcome, `UNEXPECTED` included, has a JVM test that drives it
  through a fake of the platform client. bridge decodes each into a
  constructor of its own, and the app handles every constructor
  visibly (bats-lang/bridge's and bats-lang/quire's CLAUDE.md).

## Names are words

Methods, variables, classes, files and ids are named by what they are,
in words: `authorizationForScopes`, `consentLauncher`, `waitingCall`,
not `afs`, `cl`, `wc`. A loop index (`i`, `j`) may stay short within a
few lines.

## A package

```
packages/<name>/
  package.json        @bats-lang/capacitor-<name>, peer @capacitor/core >=8
  src/                definitions.ts (the API, documented), index.ts
  dist/               committed: what `pnpm run build` writes
  README.md           its API section written by docgen
  rollup.config.mjs
  tsconfig.json       extends ../../tsconfig.base.json
  android/            a Gradle project of its own, with its wrapper
```

- `pnpm run build` in a package cleans `dist/`, then runs `tsc`,
  Rollup and `@capacitor/docgen` (which writes the README's API section
  between its `docgen-index` and `docgen-api` markers, and
  `dist/docs.json`).
- The package has no `prepare`, `install` or other lifecycle script: an
  app installs it from git as committed, with no build step.
- Its Android project builds against Capacitor's Android library from
  the root's `node_modules` (the workspace is hoisted, as an app's
  install is), and its JVM unit tests run in CI. The platform client is
  behind a small interface of the plugin's own, so each branch (each
  answer, each failure) is tested with a fake of it.

## `dist/` is committed and checked

An app installs a plugin from git with no build step, so `dist/` (and
the README's API section) is committed. CI builds every package from its
source and fails when anything committed differs from what the build
writes: run `corepack pnpm --recursive run build` and commit the result with the
change to the source.

## No releases, no registry

Nothing is published to npm and there are no release branches or tags.
An app pins a commit of main and the package's folder:

```
"@bats-lang/capacitor-<name>": "github:bats-lang/capacitor-plugins#<commit>&path:/packages/<name>"
```

pnpm 9 and later install from a git subfolder (npm cannot), with
`nodeLinker: hoisted` in the app's `pnpm-workspace.yaml` (pnpm 11 and
later read it there, not from `.npmrc`), so `cap sync` finds the plugin
where Capacitor expects it. A change reaches an app only when bridge
moves its pin to a newer commit of main.

## A demo app

A plugin may have a demo app in `packages/<name>/example/` (a workspace
package, so it uses the plugin as the workspace builds it), for checking
the plugin on a device. `google-authorize`'s (bats-lang/quire#321) is
built by `.github/workflows/demo-apk.yml` into an APK signed with a
throwaway, public demo key, checked (the key's SHA-1, the package), and
kept as the run's artifact `google-authorize-demo-apk` (90 days), the
owner's download. No app installs from it.

## CI is pinned

Every input to CI is pinned in the source, so a commit that passes
keeps passing:

- each action by its commit (the tag in a comment beside it);
- Node by `.nvmrc`, pnpm by the root `package.json`'s `packageManager`
  (with its hash, run through Node's corepack), and every package by
  `pnpm-lock.yaml` (`--frozen-lockfile`);
- the JDK by its exact release, Gradle by each Android project's wrapper
  (its distribution's SHA-256 in `gradle-wrapper.properties`, and the
  wrapper jar checked by Gradle's wrapper validation), and each Android
  dependency by its exact version.

`.github/workflows/check.yml` has one job, `check`, which main's ruleset
requires. It installs, lints and checks the format (ESLint, Prettier,
with Prettier's Java plugin for the Android code), builds each package
and checks that nothing committed changed, then runs each Android
project's `./gradlew :build :test`. Never skip, disable or quarantine a
test, and never re-run hoping for green: a red CI is root-caused and
fixed.

## Merging

Main's rulesets: "PR gates" requires `check` and `adversarial-review`,
with the branch up to date (strict), and nobody may bypass it; "No
force push" forbids force pushes to main and its deletion. A pull
request merges (squash) only when its head contains the current main
and both checks pass on that exact head. If main moved, merge it in
(never rebase or force-push) and push.

## Adversarial review before merge

No pull request merges before an adversarial review: a comment on the
pull request (its form is below), written by someone other than its
author (another agent or a person). The review first confirms the pull
request's kind from its diff, whatever the author called it.

- **A bug fix** (a plugin deviates from the platform API it wraps, or
  from its own documented API): the review confirms it really is a
  deviation, that the fix is minimal, and that no capability slipped in:
  a "fix" that adds or widens a method, or puts app policy in a plugin,
  is reviewed as a new capability.
- **A new plugin or method**: the review checks the pull request's
  Justification section against the evidence (code, the platform's
  documentation, the plugins it names), and answers:
  1. Does nothing existing do this, at reasonable cost?
  2. Is this the minimal wrapper? Does it map 1:1 to the platform API?
     Why can it not be smaller?
  3. Is there any app policy in it (when to ask, what to show, what to
     store, defaults an app would choose)?
  4. Do its tests cover every branch, and does the Android code handle
     each answer and failure the platform documents?

  The review analyzes the plausible alternatives and says why each
  would not work.

- **Process, documentation or CI** changes: the review confirms they add
  no capability, and that CI still pins every input and still fails
  when it should.

A pull request that puts app policy in a plugin gets `Verdict: changes
needed`.

### Who reviews, and where

Only a comment by a trusted author counts: its `author_association` is
OWNER, MEMBER or COLLABORATOR. This repository is public, so anyone
else's comment, approving or not, is ignored; the gate's log names each
review attempt's author and association. The reviewer is another agent
or person than the one that wrote the change: all agents here post from
one account, so the review is done by a separate reviewer agent, named
on its `Reviewer:` line.

A review that approves is a comment in the pull request's conversation,
not a formal pull request review and not a line comment. A formal review
by a trusted author can only block: as the newest review attempt it
makes the gate pending, whatever it says, and one that requests changes
stands until it is dismissed.

### The review comment

The gate reads each comment (and each formal review's text) twice: as
written, and as GitHub renders it (`body_text`), so what it judges is
what a reader sees. A comment is a review attempt when one of the first
three non-blank lines of either says "adversarial review", read loosely
(case, look-alike digits, marks, zero-width characters and up to two
wrong letters do not hide it). The newest attempt decides, alone. It
approves only when it is perfectly formed:

- written, its first line is exactly `## Adversarial review`;
- it has exactly one line with `Verdict:` in it, and that line is
  exactly `Verdict: approved` (or `Verdict: changes needed`): no bold,
  no indent, no trailing space or period, no other words;
- it has exactly one line with `Reviewed:` in it, and that line is
  exactly `Reviewed: <SHA>`, the full 40-character lowercase SHA of the
  pull request's head commit it reviewed;
- its letters are ASCII, and it has no invisible characters, no
  combining marks, no code fence, no HTML comment or tag, and no link or
  footnote definition. Reviews use inline code and plain text only.

For example:

```
## Adversarial review

Reviewer: a separate reviewer agent
Kind: a new plugin
Reviewed: 0123456789abcdef0123456789abcdef01234567

(findings, each with its evidence)

Verdict: approved
```

A verdict is never edited: an edited review counts as no approval, and
a new verdict is a new comment. Other comments should not say
"adversarial review" in their first three lines: such a comment is an
attempt, and as the newest it makes the gate pending.

### The gate

`.github/workflows/review-gate.yml` (bridge's, with the same rules) sets
the commit status `adversarial-review` on the pull request's head
commit: success only for a perfectly formed, unedited approval of the
current head in the newest attempt by a trusted author, with no standing
request for changes. Anything else is pending, and so is any failure,
so an earlier success never outlives a failed run. A push after an
approval turns it back to pending, until a new review names the new
head. Every trigger runs the gate as main has it, never as the pull
request has it; `review-gate-relay.yml` only passes formal reviews on,
and a schedule runs the gate over every open pull request.
