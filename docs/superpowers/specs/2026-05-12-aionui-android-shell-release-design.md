# AionUi Android Shell Release Publishing (Design)

## Goal

Add a manual GitHub Actions based release flow for the Android shell that:

- is triggered manually from GitHub Actions
- accepts a version input such as `0.1.0`
- builds a release APK in CI
- creates a Git tag `v0.1.0`
- creates a GitHub Release `v0.1.0`
- uploads a versioned asset named `aionui-shell-v0.1.0.apk`

This phase prioritizes release automation over production signing. The first release flow should be able to run end-to-end before formal signing is introduced.

## Confirmed Product Decisions

- Release should be published via GitHub Release
- Release should be triggered manually, not by pushing a tag
- Version naming should use semantic version style and produce filenames like `aionui-shell-v0.1.0.apk`
- For now, the release flow should not depend on a production keystore being present

## Non-Goals

- No Play Store publishing
- No signing key management in this phase
- No automatic release on every push
- No debug and release workflow consolidation unless needed for maintainability

## User Experience

### Release Operator Flow

From the GitHub repository Actions tab, the operator:

1. Opens the release workflow
2. Clicks `Run workflow`
3. Inputs a version such as `0.1.0`
4. Waits for CI to:
   - build the release APK
   - create the matching tag
   - create the matching GitHub Release
   - upload the APK asset

Successful output:

- Release page title: `v0.1.0`
- Release asset: `aionui-shell-v0.1.0.apk`

## Release Strategy

### Recommended Approach

Use a dedicated workflow such as `.github/workflows/android-release.yml` instead of reusing the debug workflow.

Reasons:

- formal release behavior should be isolated from debug CI
- manual release inputs fit poorly into the current push-driven workflow
- release tagging and asset upload are easier to reason about when separated

### Version Rules

Input format:

- operator enters `0.1.0`

Derived values:

- tag: `v0.1.0`
- release title: `v0.1.0`
- APK filename: `aionui-shell-v0.1.0.apk`

The workflow should reject obviously invalid versions rather than creating malformed tags or files.

## Technical Design

### Workflow Trigger

Workflow trigger:

- `workflow_dispatch`

Inputs:

- `version`

Validation:

- non-empty
- basic semantic-style format such as `x.y.z`

If validation fails:

- workflow exits early with a clear error

### Build Process

The workflow should follow the same Android environment preparation path already proven in the debug workflow:

- checkout
- Java 17
- Gradle setup
- Android SDK setup
- explicit SDK package installation
- Gradle wrapper bootstrap

Then switch the build step to release:

- `:app:assembleRelease`

The workflow should also capture release build logs similarly to the debug workflow so failures remain diagnosable.

### Release Artifact Handling

After a successful release build:

- rename or copy the generated APK to `aionui-shell-v<version>.apk`
- upload it as both:
  - an Actions artifact
  - a GitHub Release asset

This gives both short-term CI download access and durable release distribution.

### Tag and Release Creation

The workflow should:

1. check whether tag `v<version>` already exists
2. fail clearly if the tag already exists, unless an explicit overwrite policy is introduced later
3. create tag `v<version>`
4. create GitHub Release `v<version>`
5. upload the versioned APK asset

This phase should prefer a conservative no-overwrite policy to avoid accidental release mutation.

### Permissions

The workflow needs repository write permissions sufficient to:

- create tags
- create releases
- upload release assets

The existing pattern of explicit workflow permissions should continue to be used.

## Signing Strategy For This Phase

Current assumption:

- release automation comes first
- formal signing comes later

Design expectation:

- if the current Android project can build a release APK without custom signing secrets, use that path now
- if CI requires explicit release signing config, add the smallest possible temporary release configuration that keeps the workflow functioning until proper signing is provided

This should be documented clearly so the current release process is understood as a workflow milestone, not the final distribution security posture.

## Files Likely To Change

- Create: `.github/workflows/android-release.yml`
- Possibly modify: `android-shell/app/build.gradle.kts`
- Possibly modify: `android-shell/README.md`

The debug workflow should remain intact unless a tiny shared cleanup is clearly beneficial.

## Error Handling

- Invalid version input: fail fast with a clear message
- Existing tag: fail fast and do not create a duplicate release
- Release APK missing after build: fail before tag/release publication if possible
- Release creation failure after build: preserve uploaded Actions artifact and logs for recovery

## Testing Strategy

### CI Acceptance

Manual workflow dispatch with version `0.1.0` should result in:

- successful release APK build
- created tag `v0.1.0`
- created GitHub Release `v0.1.0`
- uploaded asset `aionui-shell-v0.1.0.apk`

### Failure Observability

If the release build fails:

- logs should still be available through Actions artifacts or build output
- the workflow should not silently create an incomplete release

## Acceptance Criteria

- A manual GitHub Actions workflow exists for Android release publishing
- The workflow accepts a version input
- The workflow builds a release APK in CI
- The workflow creates `v<version>` tag and GitHub Release
- The workflow uploads `aionui-shell-v<version>.apk` as the release asset
- The workflow is usable before formal production signing is introduced
