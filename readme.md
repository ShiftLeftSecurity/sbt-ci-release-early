# sbt-ci-release-early

[![Build Status](https://github.com/ShiftLeftSecurity/sbt-ci-release-early/workflows/release/badge.svg)](https://github.com/ShiftLeftSecurity/sbt-ci-release-early/actions?query=workflow%3Arelease)
[![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

sbt plugin for fully automated releases, without SNAPSHOT and git sha's in the version. You can easily create e.g. daily or weekly releases, or even release every single commit on your main branch. A remix of [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) and [sbt-release-early](https://github.com/scalacenter/sbt-release-early/) with a spin.

* automatically tags and releases the next version, no manual tagging required
* can be used in scheduled release jobs (no duplicate releases if there haven't been any changes)
* for both maven central and custom repositories (e.g. jenkins/artifactory/nexus etc)
* verifies that your build does not depend on any snapshot dependencies to prevent problems early on

## Usage

`projects/plugins.sbt`:
```
addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "<version>")
```

## TOC
<!-- markdown-toc --maxdepth 1 --no-firsth1 readme.md | tail -n +3 -->
- [Tasks](#tasks)
- [Setup for a custom repository (e.g. jfrog artifactory)](#setup-for-a-custom-repository-eg-jfrog-artifactory)
- [Setup for Sonatype / Maven central](#setup-for-sonatype--maven-central)
- [Dependencies](#dependencies)
- [FAQ](#faq)
- [Alternatives](#alternatives)


## Tasks
* `ciReleaseSkipIfAlreadyReleased`: check if your current HEAD commit already has a version tag, and in that case skip the other `ciRelease*` tasks below. Useful e.g. for daily builds. 
* `ciReleaseTagNextVersion`: determine the next version (by finding the highest version and incrementing the last digit), then create a tag with that version and push it
* `ciRelease`: publish to the configured repository
* `ciReleaseSonatype`: publish to Sonatype (using a [patched version](https://github.com/xerial/sbt-sonatype/pull/591) of [sbt-sonatype](https://github.com/xerial/sbt-sonatype))

> [!NOTE]
> If you don't have any previous versions tagged in git, the plugin will automatically create a `v0.1.0` tag for you. Alternatively you can manually create an initial version tag (e.g. `git tag v0.0.1`) and the plugin will take it from there. The same applies if you want to use a different versioning scheme, e.g. `v1`, `v0.1` or `v0.0.0.1`. All that matters is that they must start with `v` (by convention).

## Setup for a custom repository (e.g. jfrog artifactory)
In your `build.sbt` do *not* define the `version` setting, and configure your repository in the `publishTo` setting:
```
ThisBuild/publishTo := Some("releases" at "https://shiftleft.jfrog.io/shiftleft/libs-release-local")
```

In your release pipeline run:
```
sbt ciReleaseSkipIfAlreadyReleased ciReleaseTagNextVersion ciRelease
```

> [!NOTE]
> cross builds (for multiple scala versions) work seamlessly (the plugin just calls `+publishSigned`)

## Setup for Sonatype / Maven central
Sonatype central (which syncs to maven central) imposes additional constraints on the published artifacts, so the setup becomes a little more involved. These steps assume you're using github actions, but it'd be similar on other build servers. 

### Sonatype account
If you don't have a Sonatype account yet, follow the instructions in https://central.sonatype.org/pages/ossrh-guide.html to create one.

### build.sbt
In your `build.sbt` do *not* define the `version` setting and ensure the following settings *are* configured:
- `name`
- `organization`: must match your Sonatype account
- `licenses`
- `developers`
- `scmInfo`
- `homepage`
- `publishTo := sonatypePublishToBundle.value`
- `sonatypeCredentialHost` potentially - see next section

Example: https://github.com/mpollmeier/sbt-ci-release-early-usage/blob/master/build.sbt
For a multi-project build, you can define those settings in your root `build.sbt` and prefix them with `ThisBuild/`, e.g. `ThisBuild/publishTo := sonatypePublishToBundle.value`

### Choosing the correct Sonatype host
Unfortunately there's been a few changes re sonatype hosts, leading to confusion. You will need to configure the `sonatypeCredentialHost` setting according to the sonatype host your account is on. If you're unsure, you'll need to test them individually, but here's a rough guideline:

- `sonatypeCredentialHost := "central.sonatype.com"` - if you are on Sonatype Central
- `sonatypeCredentialHost := "oss.sonatype.org"` - the sonatype legacy OSSRH host
- `sonatypeCredentialHost := "s01.oss.sonatype.org"` - the sonatype legacy OSSRH host, for accounts created after February 2021
 
> [!NOTE] The legacy OSSRH service will sunset on June 30th, 2025, i.e. you will need to migrate to Sonatype Central after that. See https://central.sonatype.org/publish/publish-guide

### gitignore
`echo '/gnupg-*' >> .gitignore`

### gpg keys
Sonatype requires all artifacts to be signed. Since it doesn't matter which key it's signed with, and we need to share the private key with the build server (e.g. github actions), we will simply create a new one specifically for this project:

```
gpg --gen-key
```

- For real name, use "$PROJECT_NAME bot", e.g. `sbt-ci-release-early bot`
- For email, use your own email address
- Passphrase: leave empty, i.e. no passphrase. It will warn you that it's not a good idea, but this is just a pro forma key for Sonatype. You'll only share the key with github, and if it had a passphrase you'd have to share that with github as well, anyway. Private key passphrases gave me a lot of headaches across different gpg versions, so I decided to advise against them. If you like you can encrypt your private key, e.g. with gpg or openssl. 

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
So that Github Actions can release on your behalf, we need to share some secrets via environment variables with github actions. You can either do that for your project or an entire organization. 

> [!NOTE]
> As of June 2024, Sonatype requires you to log in with an access token, you can no longer use your regular username/password. 
  
First you need to obtain a Sonatype username/password token: 
- log into https://oss.sonatype.org
- select `Profile` from the dropdown at the top right
- `User Token` -> `Access` -> `Access user token`

Now go to your github project or organization and navigate to `Settings` -> `Secrets and variables` -> `Actions` and add the following `Repository secrets`:
- `SONATYPE_USERNAME`: the name part of the user token you generated in the previous step
- `SONATYPE_PASSWORD`: the password part of the user token you generated in the previous step
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
![secrets](https://user-images.githubusercontent.com/506752/197265585-29ee3599-3f39-44c2-bff0-8f89cbfadc32.png)

### Github Actions Workflow

The final step is to configure your github actions workflow. There's many ways to do this, but most builds can probably take the below setup as is. It configures two workflows: one for pull requests which only runs the tests, and one for master builds, which also releases a new version. 
Both are configured with a cache to avoid downloading all your dependencies for every build. 

<project_root>/.github/workflows/pr.yml
```yml
name: pr
on: pull_request
jobs:
  pr:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 1
      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: sbt
      - uses: sbt/setup-sbt@v1
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
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: sbt
      - uses: sbt/setup-sbt@v1
      - run: sudo apt update && sudo apt install -y gnupg
      - run: echo $PGP_SECRET | base64 --decode | gpg --batch --import
        env:
          PGP_SECRET: ${{ secrets.PGP_SECRET }}
      - run: sbt +test ciReleaseSkipIfAlreadyReleased ciReleaseTagNextVersion ciReleaseSonatype
        env:
          SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
          SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
```

That's all. Here's a demo repo: https://github.com/mpollmeier/sbt-ci-release-early-usage

## Dependencies
By installing `sbt-ci-release-early` the following sbt plugins are also brought in:
- [sbt-dynver](https://github.com/sbt/sbt-dynver): sets the project version based on the git tag
- [sbt-pgp](https://github.com/sbt/sbt-pgp): signs the artifacts before publishing
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype): publishes your artifacts to Sonatype

## FAQ

### How can determine the latest released version?

Other than manually looking at Sonatype/Maven central or git tags, you can use the following snippet that remotely gets the git tags that start with `v` and have (in this version) three decimals separated by `.`, and returns the highest version. 

```
git ls-remote --tags $REPO | awk -F"/" '{print $3}' | grep '^v[0-9]*\.[0-9]*\.[0-9]*' | grep -v {} | sort --version-sort | tail -n1
```

### My Sonatype staging repos seems to be in a broken state
When a build is e.g. interrupted, or didn't satisfy the Sonatype requirements for publishing, it is likely that these artifacts are still lying around in the Sonatype staging area. You can log into https://oss.sonatype.org/ and clean it up, or just do it from within sbt, locally on your machine:

* `sonatypeStagingRepositoryProfiles` // lists staging repo ids
* `sonatypeDrop [id]`

### Why not just use SNAPSHOT dependencies instead?
SNAPSHOT dependencies have major downsides:
* are mutable, i.e. your builds aren't reproducible
* slow down your build, because sbt has to check for updates all the time
* involve multiple layers of caches, which tend to break and add complexity if you try to debug a problem

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

As soon as Sonatype "closes" the staging repository they become available on Sonatype/releases and will be synchronized to maven central within ~10mins. If you want to use them immediately, add a Sonatype resolver to the build that uses the released artifact:

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
