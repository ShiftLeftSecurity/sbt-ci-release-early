# sbt-ci-release-early

[![Build Status](https://github.com/ShiftLeftSecurity/sbt-ci-release-early/workflows/release/badge.svg)](https://github.com/ShiftLeftSecurity/sbt-ci-release-early/actions?query=workflow%3Arelease)
[![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

Sbt plugin for fully automated releases, without SNAPSHOT and git sha's in the version. A remix of the best ideas from [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) and [sbt-release-early](https://github.com/scalacenter/sbt-release-early/).

- [Features](#features)
- [Installation](#installation)
- [Configuration for an in-house repository (e.g. jenkins/artifactory)](#configuration-for-an-in-house-repository-eg-jenkinsartifactory)
- [Configuration for sonatype (maven central) via github actions](#configuration-for-sonatype-maven-central-via-github-actions)
- [Dependencies](#dependencies)
- [FAQ](#faq)
- [Alternatives](#alternatives)
<!-- markdown-toc --maxdepth 1 --no-firsth1 readme.md -->

## Features
* detects last version from git tags (e.g. `v1.0.0`), and automatically tags and releases the next version as `v1.0.1`
* no snapshots, no manual tagging
* automatically performs a cross-release if your build has multiple scala versions configured
* uses sbt-sonatype's fast new `sonatypeBundleRelease`
* use `ciRelease` for your in-house setup (e.g. jenkins/artifactory/nexus etc), very easy to configure
* use `ciReleaseSonatype` for your open source actions/sonatype/maven-central setup, a little more involved to configure
* easy to test locally (faster turnaround than debugging on ci)
* verifies that your build does not depend on any snapshot dependencies

## Installation

Add the dependency in your `projects/plugins.sbt`:
```
addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % <version>)
```

Latest version: [![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

If you don't have any previous versions tagged in git, now is the time to choose your versioning scheme. To do so simply tag your current commit with the version you want: 
```
git tag v0.0.1
```
N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well, they only must start with `v`
Ensure you don't have any uncommitted local changes and run `sbt "show version"` to verify that the git version plugin works. 

## Configuration for an in-house repository (e.g. jenkins/artifactory)
Make sure the `publishTo` key in your `built.sbt` points to your repository:
```
ThisBuild/publishTo := Some("releases" at "https://shiftleft.jfrog.io/shiftleft/libs-release-local")
```
If it's a multi-project build you may need to prefix it with `ThisBuild/` in your root build.sbt.

Commit (and push) any local changes, then let's check that everything works - you can do this locally.
1) auto-tagging: determines last released version based on git tags and creates a new one:
```
sbt ciReleaseTagNextVersion
```

2) Publish a release
```
sbt ciRelease
```

If that all worked, just configure the two commands `ciReleaseTagNextVersion ciRelease` at the end of your build pipeline on your CI server. A complete command would e.g. be:
```
sbt clean test ciReleaseTagNextVersion ciRelease
```

Cross builds (for multiple scala versions) work seamlessly (the plugin just calls `+publishSigned`). 

## Configuration for sonatype (maven central) via github actions
Sonatype (which syncs to maven central) imposes additional constraints on the published artifacts, so the setup becomes a little more involved. These steps assume you're using github actions, but it'd be similar on other build servers. 

### Sonatype account
If you don't have a sonatype account yet, follow the instructions in https://central.sonatype.org/pages/ossrh-guide.html to create one. 
It's advisable (yet optional) to create a user token, which guises your actual user/password.

### build.sbt
Make sure `build.sbt` *does not* define any of the following settings:
- `version`

Ensure the following settings *are* defined in your `build.sbt`:
- `name`
- `organization`: must match your sonatype account
- `licenses`
- `developers`
- `scmInfo`
- `homepage`
- `publishTo := sonatypePublishToBundle.value`

Example: https://github.com/mpollmeier/sbt-ci-release-early-usage/blob/master/build.sbt
For a multi-project build, you can define those settings in your root `build.sbt` and prefix them with `ThisBuild/`, e.g. `ThisBuild/publishTo := sonatypePublishToBundle.value`

  > ⚠️ Legacy Host
  >
  > By default, sbt-sonatype is configured to use the legacy Sonatype repository `oss.sonatype.org`. If you created a new account from February 2021, you need to configure the new repository url. Context: https://github.com/xerial/sbt-sonatype/issues/214
  >
  > ```scala
  > // For all Sonatype accounts created from February 2021
  > sonatypeCredentialHost := "s01.oss.sonatype.org"
  > ```

### gitignore
`echo '/gnupg-*' >> .gitignore`

### gpg keys
Sonatype requires all artifacts to be signed. Since it doesn't matter which key it's signed with, and we need to share the private key with the build server (e.g. github actions), we will simply create a new one specifically for this project:

```
gpg --gen-key
```

- For real name, use "$PROJECT_NAME bot", e.g. `sbt-ci-release-early bot`
- For email, use your own email address
- Passphrase: leave empty, i.e. no passphrase. It will warn you that it's not a good idea, but this is just a pro forma key for sonatype. You'll only share the key with github, and if it had a passphrase you'd have to share that with github as well, anyway. Private key passphrases gave me a lot of headaches across different gpg versions, so I decided to advise against them. If you like you can encrypt your private key, e.g. with gpg or openssl. 

At the end you'll see output like this

```
pub   rsa2048 2018-06-10 [SC] [expires: 2022-11-13]
      $LONG_ID
uid                      $PROJECT_NAME bot <$EMAIL>
```

Take note of `$LONG_ID`, make sure to replace this ID from the code examples
below. The ID will look something like `499FD7755EC30DDAF43089355E00EC8C822C6A2A`.

```bash
export LONG_ID=499FD7755EC30DDAF43089355E00EC8C822C6A2A
```

Optional: if you would like to make the key never expire:
```bash
gpg --edit-key $LONG_ID
expire #follow prompt
key 1
expire #follow prompt
save
```

Now submit the public key to a keyserver (shouldn't matter which one, keyservers synchronize their keys with each other):

```
gpg --keyserver keyserver.ubuntu.com --send-keys $LONG_ID
```

### Secrets to share with Github actions
So that github actions can release on your behalf, we need to share some secret via environment variables in `Settings -> Secrets`. You can either do that for your project or an entire organization. 

- `SONATYPE_USERNAME`: The username you use to log into
  https://oss.sonatype.org/. Alternatively, the name part of the user token if
  you generated one above.
- `SONATYPE_PASSWORD`: The password you use to log into
  https://oss.sonatype.org/. Alternatively, the password part of the user token
  if you generated one above. 
- `PGP_SECRET`: The base64 encoded secret of your private key that you can export from the command line like here below

```
# macOS
gpg --armor --export-secret-keys $LONG_ID | base64 | pbcopy
# Ubuntu (assuming GNU base64)
gpg --armor --export-secret-keys $LONG_ID | base64 -w0 | xclip
# Arch
gpg --armor --export-secret-keys $LONG_ID | base64 | sed -z 's;\n;;g' | xclip -selection clipboard -i
# FreeBSD (assuming BSD base64)
gpg --armor --export-secret-keys $LONG_ID | base64 | xclip
# Windows
gpg --armor --export-secret-keys %LONG_ID% | openssl base64
```

Your secrets settings should look like this:
![Screenshot 2020-11-03 at 08 45 12](https://user-images.githubusercontent.com/1408093/97960055-ee09c780-1db0-11eb-961b-076d0e503b24.png)

### Github Actions Workflow

The final step is to configure your github actions workflow. There's many ways to do this, but most builds can probably take the below setup as is. It configures two workflows: one for pull requests which only runs the tests, and one for master builds, which also releases a new version. 
Both are configured with a cache to avoid downloading all your dependencies for every build. 

<project_root>/.github/workflows/pr.yml
```yml
name: pr
on: pull_request
jobs:
  pr:
    runs-on: ubuntu-18.04
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 1
      - uses: olafurpg/setup-scala@v10
      - uses: actions/cache@v2
        with:
          path: |
            ~/.sbt
            ~/.coursier
          key: ${{ runner.os }}-sbt-${{ hashfiles('**/build.sbt') }}
      - run: sbt +test
```

<project_root>/.github/workflows/release.yml
```yml
name: release
concurrency: release
on:
  push:
    branches: [master, main]
    tags: ["*"]
jobs:
  release:
    runs-on: ubuntu-18.04
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0
      - uses: olafurpg/setup-scala@v10
      - run: sudo apt update && sudo apt install -y gnupg
      - run: echo $PGP_SECRET | base64 --decode | gpg --batch --import
        env:
          PGP_SECRET: ${{ secrets.PGP_SECRET }}
      - uses: actions/cache@v2
        with:
          path: |
            ~/.sbt
            ~/.coursier
          key: ${{ runner.os }}-sbt-${{ hashfiles('**/build.sbt') }}
      - run: sbt +test ciReleaseTagNextVersion ciReleaseSonatype
        env:
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
```

If you want to customize those: the syntax is [documented here](https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions).

Optional: add a [status badge](https://docs.github.com/en/free-pro-team@latest/actions/managing-workflow-runs/adding-a-workflow-status-badge) to your readme (replace OWNER and REPOSITORY): 
```
[![Build Status](https://github.com/<OWNER>/<REPOSITORY>/workflows/release/badge.svg)](https://github.com/<OWNER>/<REPOSITORY>/actions?query=workflow%3Arelease)
```


That's all. Here's a demo repo: https://github.com/mpollmeier/sbt-ci-release-early-usage

## Dependencies
By installing `sbt-ci-release-early` the following sbt plugins are also brought in:
- [sbt-dynver](https://github.com/sbt/sbt-dynver): sets the project version based on the git tag
- [sbt-pgp](https://github.com/sbt/sbt-pgp): signs the artifacts before publishing
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype): publishes your artifacts to Sonatype

## FAQ

### How can determine the latest released version?

Other than manually looking at sonatype/maven central or git tags, you can use the following snippet that remotely gets the git tags that start with `v` and have (in this version) three decimals separated by `.`, and returns the highest version. 

```
git ls-remote --tags $REPO | awk -F"/" '{print $3}' | grep '^v[0-9]*\.[0-9]*\.[0-9]*' | grep -v {} | sort --version-sort | tail -n1
```

### My sonatype staging repos seems to be in a broken state
When a build is e.g. interrupted, or didn't satisfy the sonatype requirements for publishing, it is likely that these artifacts are still lying around in the sonatype staging area. You can log into https://oss.sonatype.org/ and clean it up, or just do it from within sbt, locally on your machine:

* `sonatypeStagingRepositoryProfiles` // lists staging repo ids
* `sonatypeDrop [id]`

### Why not just use SNAPSHOT dependencies instead?
SNAPSHOT dependencies are evil because they:
* are mutable, i.e. your builds aren't reproducible
* slow down your build, because sbt has to check for updates all the time
* involve (sometimes multiple layers) of caches, which tend to break and add complexity if you try to debug a problem

### How do I release a specific version? 
To keep things simple I decided to not add that feature to this plugin. 
If you want to release a specific version you have to do that yourself:

```
// in sbt:
set version := "1.2.3"
+publishSigned
sonatypeBundleRelease

// on the terminal:
git tag v1.2.3
git push origin v1.2.3
```
Note to future self: this would have added complexity because to trigger it we would rely on git tags, and we need a foolproof way to check if a given tag has already been released. My intial thought was to tag anything released with `_released_1.0.1_`, but it was getting quite complicated for handling an edge case. 

### How do I disable publishing in certain projects?

Add the following to the project settings:

```scala
publish/skip := true
```

### What if my build contains subprojects?
If the build defines a dependency on the subproject (e.g. `dependsOn(subProjectName)`) then it's automatically included in the release. 
Otherwise you can just append `subProjectName/publish` to your build pipeline, the version is already set for you :)

### Can I use my releases immediately?

As soon as CI "closes" the staging repository they are available on sonatype/releases and will be synchronized to maven central within ~10mins. If you want to use them immediately, add a sonatype resolver to the build that uses the released artifact:

```scala
resolvers += Resolver.sonatypeRepo("releases")
```

### Can I publish sbt plugins?

Yes. This plugin is published with a previous version of itself :)

## Alternatives

There exist great alternatives to sbt-ci-release-early that may work better for your setup.

- [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release):
  The most popular release plugin I believe. Main difference: releases versions that you previously tagged in git (rather than automatically tagging every build). 
  This plugin is essentially a fork of sbt-ci-release, they share most traits. 
- [sbt-release-early](https://github.com/scalacenter/sbt-release-early):
- [sbt-rig](https://github.com/Verizon/sbt-rig)
