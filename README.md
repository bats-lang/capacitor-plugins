# capacitor-plugins

Small [Capacitor](https://capacitorjs.com/) plugins for the bats apps,
each a minimal 1:1 wrapper over one platform API, in a folder of its own
under `packages/`. They are written for apps built by
[bats-lang/pwa](https://github.com/bats-lang/pwa), whose plugins
[bats-lang/bridge](https://github.com/bats-lang/bridge) names, but any
Capacitor 8 app can use them.

## Installing a plugin

Nothing here is published to npm. An app installs a plugin from a commit
of this repository's `main` and the plugin's folder, with pnpm 9 or
later (npm cannot install from a subfolder of a git repository):

```json
"dependencies": {
  "@bats-lang/capacitor-<name>": "github:bats-lang/capacitor-plugins#<commit>&path:/packages/<name>"
}
```

Each plugin's built `dist/` is committed, so nothing is built at
install. Capacitor's `cap sync` expects the plugin in the app's
top-level `node_modules`, so the app's `pnpm-workspace.yaml` says:

```yaml
nodeLinker: hoisted
```

(pnpm 11 and later read this setting there, not from `.npmrc`.) Then
`pnpm exec cap sync android` registers the plugin.

## Plugins

None yet. Each plugin's own README documents its API.

## Working here

```sh
corepack pnpm install        # pnpm as package.json pins it
corepack pnpm run lint       # ESLint and Prettier
corepack pnpm --recursive run build   # each package: tsc, Rollup, docgen
```

Each package's Android project builds with its own Gradle wrapper
(`./gradlew build test` in `packages/<name>/android`) on JDK 21. The
rules (minimal wrappers, the justification a new plugin needs, the
adversarial review, the pinned CI) are in [CLAUDE.md](CLAUDE.md).
