# InstaEclipse plugin registry

`plugins/<plugin-id>/plugin.json` is the canonical release metadata for every downloadable executable plugin.

## Publishing a plugin

1. Add the plugin Android module to `settings.gradle`.
2. Add `plugins/<plugin-id>/plugin.json` with the required schema fields, including the Gradle `module` name.
3. Keep the plugin entrypoint and compatibility ranges in that manifest.
4. Ensure the plugin module builds an APK and is configured for the InstaEclipse release signing identity.
5. Push a tag named `plugin/<plugin-id>/v<major>.<minor>.<patch>`.

The Plugin Packs workflow then:

- validates the canonical manifest;
- injects the release version into the plugin APK manifest;
- builds the plugin;
- signs and verifies the release APK;
- publishes the APK and SHA-256 sidecar as a GitHub Release;
- updates `plugins/catalog.json` automatically.

Feature Hub reads the live catalog, so a newly published plugin or a newer plugin version becomes visible without a Core APK release.

## Hotfix / update

Publish the next semantic version using the same tag format. For example:

`plugin/ghost/v1.2.0`

The workflow replaces that plugin's catalog entry with v1.2.0. Installed Core users will see the newer version in Feature Hub and can update the plugin independently.

Core signing identity remains the trust boundary: plugin APKs must use the same trusted signing certificate as Core.
