# sbt-ci-release-early

[![Build Status](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early.svg?branch=master)](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early)
[![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

Sbt plugin for fully automated releases, without SNAPSHOT and git sha's in the version. A remix of the best ideas from [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) and [sbt-release-early](https://github.com/scalacenter/sbt-release-early/).

- [Features](#features)
- [Installation](#installation)
- [Configuration for an in-house repository (e.g. jenkins/artifactory)](#configuration-for-an-in-house-repository-eg-jenkinsartifactory)
- [Configuration for sonatype (maven central) via travis.ci](#configuration-for-sonatype-maven-central-via-travisci)
- [Configuration for bintray (jfrog jcenter) via travis.ci](#configuration-for-bintray-jfrog-jcenter-via-travisci)
- [Dependencies](#dependencies)
- [FAQ](#faq)
- [Alternatives](#alternatives)
<!-- markdown-toc --maxdepth 1 --no-firsth1 readme.md -->

## Features
* detects last version from git (e.g. `v1.0.0`) and increments the last digit, i.e. the next release is automatically inferred as `v1.0.1`
* no more need to manually tag the builds
* builds the release, creates a local git tag, publishes the artifact, publishes the git tag
* automatically performs a cross-release if your build has multiple scala versions configured
* uses sbt-sonatype's fast new `sonatypeBundleRelease`
* use `ciRelease` for your in-house setup (e.g. jenkins/artifactory/nexus etc), very easy to configure
* use `ciReleaseBintray` for your open source jfrog jcenter setup
* use `ciReleaseSonatype` for your open source travis/sonatype/maven-central setup, a little more involved to configure
* easy to test locally (faster turnaround than debugging on travis.ci)
* verifies that your build does not depend on any snapshot dependencies

## Installation

Add the dependency in your `projects/plugins.sbt`:
```
addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "1.2.1")
```

Latest version: [![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

Enable sbt-git (automatically brought in as a plugin dependency) in your `build.sbt`, this will automatically set the `version` based on your git repo (e.g. the git tag, or as a fallback the commit sha)
```
enablePlugins(GitVersioning)
```

## Configuration for an in-house repository (e.g. jenkins/artifactory)
Make sure the typical `publishTo` variable in your `built.sbt` points to your repository (this isn't specific to this plugin). Example in `build.sbt`:
```
ThisBuild / publishTo := Some("releases" at "https://shiftleft.jfrog.io/shiftleft/libs-release-local")
```

If you don't have any previous versions tagged in git, now is the time to choose your versioning scheme. To do so simply tag your current commit with the version you want: 
```
git tag v0.0.1
```
N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well. 

To double check that the auto-tagging works, let the plugin create a new version tag for you:
```
sbt ciReleaseTagNextVersion
```

Now let's try to publish a release from your local machine:
```
sbt ciRelease
```

If that all worked, just configure the two commands `ciReleaseTagNextVersion ciRelease` at the end of your build pipeline on your CI server. A complete command would e.g. be:
```
sbt clean test ciReleaseTagNextVersion ciRelease
```

Cross builds (for multiple scala versions) work seamlessly (the plugin just calls `+publishSigned`). 

## Configuration for sonatype (maven central) via travis.ci
Sonatype (which syncs to maven central) imposes additional constraints on the published artifacts, so the setup becomes a little more involved. These steps assume you're using travis.ci, but it'd be similar on other build servers. 

### Sonatype account
If you don't have a sonatype account yet, follow the instructions in https://central.sonatype.org/pages/ossrh-guide.html to create one. 

### build.sbt
Make sure `build.sbt` *does not* define any of the following settings
- `version`

Ensure the following settings *are* defined in your `build.sbt`:
- `enablePlugins(GitVersioning)`: enable sbt-git (automatically brought in as a plugin dependency)
- `name`
- `organization`: must match your sonatype account name
- `licenses`
- `developers`
- `scmInfo`
- `publishTo := sonatypePublishToBundle.value`
- `Global/useGpgPinentry := true`: to ensure we're using `gpg --pinentry-mode loopback` - otherwise the sbt prompt asks for the key password, which will timeout on travis.ci. Note that this is required for gpg2. Do not configure if you use an older version (e.g. gpg1 shipped with `dist: xenial`).

Example: https://github.com/mpollmeier/sbt-ci-release-early-usage/blob/master/build.sbt

Example for a multi-project build:
```scala
enablePlugins(GitVersioning)
ThisBuild/organization := "io.shiftleft"
ThisBuild/licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild/homepage := Some(url("https://github.com/mpollmeier/sbt-ci-release"))
ThisBuild/scmInfo := Some(ScmInfo(
    url("https://github.com/mpollmeier/sbt-ci-release-usage"),
    "scm:git@github.com:mpollmeier/sbt-ci-release-usage.git"))
ThisBuild/developers := List(
  Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("https://michaelpollmeier.com")))
ThisBuild/publishTo := sonatypePublishToBundle.value
Global/useGpgPinentry := true // to ensure we're using `--pinentry-mode loopback`
```

### initial version tag
If you don't have any previous versions tagged in git, now is the time to choose your versioning scheme. To do so simply tag your current commit with the version you want: 
```
git tag v0.0.1
```
N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well. 

To double check that the auto-tagging works, let the plugin create a new version tag for you:
```
sbt ciReleaseTagNextVersion
```

### gpg keys
Sonatype requires all artifacts to be signed. Since it doesn't matter which key it's signed with, and we need to share the private key with travis.ci, we will simply create a new one specifically for this project:

```
gpg --gen-key
```

- For real name, use "$PROJECT_NAME bot", e.g. `gremlin-scala bot`
- For email, use your own email address
- For passphrase, generate a random password, e.g. using `apg -n1 -m20 -Mncl`

At the end you'll see output like this

```
pub   rsa2048 2018-06-10 [SC] [expires: 2020-06-09]
      $LONG_ID
uid                      $PROJECT_NAME bot <$EMAIL>
```

Take note of `$LONG_ID`, make sure to replace this ID from the code examples
below. The ID will look something like
`6E8ED79B03AD527F1B281169D28FC818985732D9`.

```bash
export LONG_ID=6E8ED79B03AD527F1B281169D28FC818985732D9
```

Optional: if you would like to change the key expiry date:
```bash
gpg --edit-key $LONG_ID
expire #follow prompt
key 1
expire #follow prompt
save
```

Now have one final look and submit the public key to a keyserver (shouldn't matter which one, keyservers synchronize their keys with each other):

```
gpg --list-keys $LONG_ID
gpg --send-keys $LONG_ID
```

Then export the private key locally so we can later encrypt the private one for travis. Make sure you don't publish it anywhere. The actual damage is small since it's just for this build, but the internet will shame you.

```
gpg --armor --export-secret-keys $LONG_ID > private-key.pem
echo "\nprivate-key.pem" >> .gitignore
git add .gitignore
```

### Git push access

Travis will automatically tag each release in git. In order to push that tag, it needs push access to your repository. The easiest way to achieve that is to create a [personal access token](https://github.com/settings/tokens) for travis. Note it down somewhere, it will be used in the next section (and you can use for all your travis builds).

### travis.ci

Open the "Settings" panel for your project on Travis CI, for example
https://travis-ci.org/mpollmeier/sbt-ci-release-early-usage/settings

And define the following secret variables. They are shared with travis, but cannot be accessed outside your build:

- `SONATYPE_USERNAME`: The email you use to log into https://oss.sonatype.org/
- `SONATYPE_PASSWORD`: The password you use to log into https://oss.sonatype.org/
- `PGP_PASSPHRASE`: The randomly generated password you used to create a fresh gpg key.
- `GITHUB_TOKEN`: the token that allows travis to push to your remote git(hub) repository

Now configure your `.travis.yml`. There are many ways to do this, but to make things simple you can just copy paste the following into your `.travis.yml`. It sets up your build in two stages:

* `test`: always run `sbt +test`
* `release`: if it's the `master` branch and all tests passed, run `sbt ciReleaseTagNextVersion ciReleaseSonatype`

```yml
dist: bionic
language: scala
jdk: openjdk11
if: tag IS blank

before_install:
- git fetch --tags

install:
- gpg --import private-key.pem

stages:
- name: test
- name: release
  if: branch = master AND type = push

jobs:
  include:
    - stage: test
      script: sbt +test
    - stage: release
      script: sbt ciReleaseTagNextVersion ciReleaseSonatype

before_cache:
- find $HOME/.sbt -name "*.lock" -type f -delete
- find $HOME/.ivy2/cache -name "ivydata-*.properties" -type f -delete
- rm -rf $HOME/.ivy2/local
cache:
  directories:
  - "$HOME/.sbt/1.0/dependency"
  - "$HOME/.sbt/boot/scala*"
  - "$HOME/.sbt/launchers"
  - "$HOME/.ivy2/cache"
  - "$HOME/.coursier"
```

Finally, share the private key with travis. Note that this has to be run from within the repository. If you haven't yet, you'll need to install [travis](https://github.com/travis-ci/travis.rb) (e.g. with `gem install travis`). 

```
travis encrypt-file private-key.pem --add
```

That's all - give it a try. Remember to add `private-key.pem.enc` to your repository, but *not* `private-key.pem`.

## Configuration for bintray (jfrog jcenter) via travis.ci
Bintray is an alternative open source repository that's easier to set up on the publishing side. The downside is that users of your artifacts will have to add jcenter to their resolvers (`resolvers += Resolver.jcenterRepo` in sbt).

### Bintray setup
1. Create an account on https://bintray.com/signup if you don't have one yet.
2. Create a repository named `maven` of type `maven`
3. Create and note down an API which we'll later use for the CI server: Profile -> Edit -> Api Key

### project/plugins.sbt
```
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
```
n.b. this is not a transitive dependency of this plugin because it takes over all settings, so regardless of what else you configure, it will always publish to bintray (as `inspect publish` shows).

### build.sbt
Make sure `build.sbt` *does not* define any of the following settings
- `version`
- `publishTo`

Ensure the following settings *are* defined in your `build.sbt`:
- `enablePlugins(GitVersioning)`: enable sbt-git (automatically brought in as a plugin dependency)
- `name`
- `organization`: must match your bintray account name
- `licenses`
- `bintrayVcsUrl`: must be the `https` url, e.g. `https://github.com/mpollmeier/sbt-ci-release-early-usage-bintray.git`

Example: https://github.com/mpollmeier/sbt-ci-release-early-usage-bintray/blob/master/build.sbt

### initial version tag
If you don't have any previous versions tagged in git, now is the time to choose your versioning scheme. To do so simply tag your current commit with the version you want: 
```
git tag v0.0.1
```
N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well. 

To double check that the auto-tagging works, let the plugin create a new version tag for you:
```
sbt ciReleaseTagNextVersion
```

### Git push access

Travis will automatically tag each release in git. In order to push that tag, it needs push access to your repository. The easiest way to achieve that is to create a [personal access token](https://github.com/settings/tokens) for travis. Note it down somewhere, it will be used in the next section (and you can use for all your travis builds).

### travis.ci

Open the "Settings" panel for your project on Travis CI, for example
https://travis-ci.org/mpollmeier/sbt-ci-release-early-usage-bintray/settings

And define the following secret variables. They are shared with travis, but cannot be accessed outside your build:

- `BINTRAY_USER`: The username you use to log into https://bintray.com
- `BINTRAY_PASS`: The API key (i.e. not the password!) for your bintray account
- `GITHUB_TOKEN`: the token that allows travis to push to your remote git(hub) repository

Now configure your `.travis.yml`. There are many ways to do this, but to make things simple you can just copy paste the following into your `.travis.yml`. It sets up your build in two stages:

* `test`: always run `sbt +test`
* `release`: if it's the `master` branch and all tests passed, run `sbt ciReleaseTagNextVersion ciReleaseBintray`

```yml
dist: bionic
language: scala
jdk: openjdk11
if: tag IS blank

before_install:
- git fetch --tags

stages:
- name: test
- name: release
  if: branch = master AND type = push

jobs:
  include:
    - stage: test
      script: sbt +test
    - stage: release
      script: sbt ciReleaseTagNextVersion ciReleaseBintray

before_cache:
- find $HOME/.sbt -name "*.lock" -type f -delete
- find $HOME/.ivy2/cache -name "ivydata-*.properties" -type f -delete
- rm -rf $HOME/.ivy2/local
cache:
  directories:
  - "$HOME/.sbt/1.0/dependency"
  - "$HOME/.sbt/boot/scala*"
  - "$HOME/.sbt/launchers"
  - "$HOME/.ivy2/cache"
  - "$HOME/.coursier"
```

### provide usage information in your readme (optional)
Some snippets that may prove useful for your readme:

```
[![bintray](https://api.bintray.com/packages/shiftleft/maven/codepropertygraph/images/download.svg)](https://bintray.com/shiftleft/maven/codepropertygraph/_latestVersion)

libraryDependencies += "io.shiftleft" %% "codepropertygraph" % "x.y.z"
resolvers += Resolver.bintrayRepo("shiftleft", "maven")

Other build tools: see [bintray instructions](https://bintray.com/shiftleft/maven/codepropertygraph/_latestVersion).
```

### configure sync to jcenter (optional)

After your first successful release, your artifacts can be resolved from your personal jfrog open source repo, i.e. users need to add something like `resolvers += Resolver.bintrayRepo("mpollmeier", "maven")` to their builds. 

If you want to make them available on [jcenter](https://bintray.com/bintray/jcenter) (jfrog's equivalent of maven central), you need to manually request the inclusion for one of your published artifacts on the bintray web ui: `Actions -> add to jcenter -> tick pom project -> enter description -> Send`. The approval typically takes 15mins, you'll get a message on bintray and email. This is a one-off: all future versions of that package will be available on jcenter automatically. 

Because sbt unfortunately doesn't have jcenter as a default resolver, users still need to add it though: `resolvers += Resolver.jcenterRepo`.

N.b. sometimes the package already exists, e.g. as an automatic proxy package created by bintray. You can claim ownership on the package page though.

## Dependencies
By installing `sbt-ci-release-early` the following sbt plugins are also brought in:
- [sbt-pgp](https://github.com/sbt/sbt-pgp): signs the artifacts before publishing
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype): publishes your artifacts to Sonatype
- [sbt-git](https://github.com/sbt/sbt-git/): sets the project version based on the git tag

## FAQ

### Publishing to sonatype failed with a cryptic SAXParseException
```
org.xml.sax.SAXParseException; lineNumber: 6; columnNumber: 3; The element type "hr" must be terminated by the matching end-tag "</hr>".
```
The most likely cause is that sonatype is having infrastructure issues and sends a timeout. 
The error message is quite cryptic because sbt-sonatype doesn't handle that case well, but the root issue is (likely) that sonatype's infrastructure is flakey. 
* [sbt-sonatype parser issue](https://github.com/xerial/sbt-sonatype/issues/81)
* [sonatype status](https://status.maven.org/)

### jdk8
If you want to use jdk8 (which is end of life), you need to make the following changes:
* .travis.yml: `dist: xenial`,`jdk: openjdk8`
* build.sbt: `Global/useGpgPinentry := false`, `Global/useGpg := false`

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
skip in publish := true
```

### What if my build contains subprojects?
If the build defines a dependency on the subproject (e.g. `dependsOn(subProjectName)`) then it's automatically included in the release. 
Otherwise you can just append `subProjectName/publish` to your build pipeline, the version is already set for you :)

### Can I use my releases immediately?

Yes. As soon as CI "closes" the staging repository they are available on sonatype/releases and will be synchronized to maven central within ~10mins. If you can't wait so long, add a sonatype resolver:

```scala
resolvers += Resolver.sonatypeRepo("releases")
```

### Can I publish sbt plugins?

You can publish sbt plugins like a normal library, no custom setup required.
In fact, this plugin is published with a previous version of itself :)

## Alternatives

There exist great alternatives to sbt-ci-release-early that may work better for your setup.

- [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release):
  Releases versions that you previously tagged in git (rather than automatically tagging every build).
  This plugin started as a fork of sbt-ci-release. I ran into [some issues](https://github.com/olafurpg/sbt-ci-release/issues/13) with gpg keys, ymmv. 
- [sbt-release-early](https://github.com/scalacenter/sbt-release-early):
- [sbt-rig](https://github.com/Verizon/sbt-rig)
