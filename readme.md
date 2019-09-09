# sbt-ci-release-early

[![Build Status](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early.svg?branch=master)](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early)
[![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

Sbt plugin for fully automated releases, without SNAPSHOT and git sha's in the version. A remix of the best ideas from [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) and [sbt-release-early](https://github.com/scalacenter/sbt-release-early/).

## TOC
- [Features](#features)
- [Installation](#installation)
- [In-house setup (e.g. jenkins/artifactory)](#in-house-setup-eg-jenkinsartifactory)
- [Public build (e.g. travis.ci/sonatype)](#public-build-eg-traviscisonatype)
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
* `ci-release` for your in-house setup (e.g. jenkins/artifactory/nexus etc), very easy to configure
* `ci-release-sonatype` for your open source travis/sonatype/maven central setup, a little more involved to configure
* easy to test locally (faster turnaround than debugging on travis.ci)
* automatically handles cross release for multiple scala versions
* verifies that your build does not depend on any snapshot dependencies

## Installation

Add the dependency in your `projects/plugins.sbt`:
```
addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "1.0.23")
```
Latest version: [![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

## In-house setup (e.g. jenkins/artifactory)
Make sure the typical `publishTo` variable in your `built.sbt` points to your repository (this isn't specific to this plugin). Example in `build.sbt`:
```
ThisBuild / publishTo := Some("releases" at "https://shiftleft.jfrog.io/shiftleft/libs-release-local")
```

If you don't have any previous versions tagged in git, do so manually now (only one initial tag necessary). N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well. 
```
git tag v0.0.1
```

Then just run `ci-release` - you can first try this locally, and then as part of your build pipeline. Cross builds (for multiple scala versions) are supported. 
```
sbt ci-release
```

## Public build (e.g. travis.ci/sonatype)
Public repositories like Sonatype (which syncs to maven central) typically impose additional constraints on the published artifacts, so the setup becomes a little more involved. These steps assume you're using travis.ci, but it should be similar on other build servers. 

### Sonatype account
If you don't have a sonatype account yet, follow the instructions in https://central.sonatype.org/pages/ossrh-guide.html to create one. 

### build.sbt
Make sure `build.sbt` *does not* define any of the following settings
- `version`

Ensure the following settings *are* defined in your `build.sbt`:
- `name`
- `organization`: must match your sonatype account priviledges
- `licenses`
- `developers`
- `scmInfo`
- `publishTo`: e.g. `sonatypePublishToBundle.value`

Example: https://github.com/mpollmeier/sbt-ci-release-early-usage/blob/master/build.sbt

Example for a multi-project build:
```scala
inThisBuild(List(
  organization := "io.shiftleft",
  publishTo := sonatypePublishToBundle.value,
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/mpollmeier/sbt-ci-release")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/mpollmeier/sbt-ci-release-usage"),
      "scm:git@github.com:mpollmeier/sbt-ci-release-usage.git")),
  developers := List(
    Developer("mpollmeier", "Michael Pollmeier", "michael@michaelpollmeier.com", url("https://michaelpollmeier.com"))
  )
))
```

### initial version tag
If you don't have any previous versions tagged in git, do so manually now (only one initial tag necessary). N.b. other versioning schemes like `v1`, `v0.1`, `v0.0.0.1` will work as well. 
```
git tag v0.0.1
```

### gpg keys
Sonatype requires all artifacts to be signed. Since it doesn't matter which key it's signed with, and we need to share the private key with travis.ci, we will simply create a new one specifically for this project:

```
gpg --gen-key
```

- For real name, use "$PROJECT_NAME bot", e.g. `gremlin-scala bot`
- For email, use your own email address
- For passphrase, generate a random password

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

Now submit the public key to a keyserver (shouldn't matter which one, keyservers synchronize their keys with each other):

```
gpg --send-keys $LONG_ID
```

Then export the private key locally so we can later encrypt it for travis. Make sure you don't publish this anywhere. The actual damage is small since it's just for this project, but people will laugh at you :)

```
gpg --armor --export-secret-keys $LONG_ID > private-key.pem
echo "\nprivate-key.pem" >> .gitignore
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
* `release`: if it's the `master` branch and all tests passed, run `sbt ci-release-sonatype`

```yml
language: scala
jdk: openjdk8

before_install:
- git fetch --tags

install:
- gpg --import private-key.pem
- gpg --list-keys

stages:
- name: test
- name: release
  if: branch = master AND type = push

jobs:
  include:
    - stage: test
      script: sbt +test
    - stage: release
      script: sbt ci-release-sonatype

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

That's all - give it a try. 

## Dependencies
By installing `sbt-ci-release-early` the following sbt plugins are also brought in:
- [sbt-pgp](https://github.com/sbt/sbt-pgp): to cryptographically sign the artifacts before publishing
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype): to publish artifacts to Sonatype

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
  This plugin started as a fork of sbt-ci-release. 
  I ran into [some issues](https://github.com/olafurpg/sbt-ci-release/issues/13) with gpg keys, ymmv. 
- [sbt-release-early](https://github.com/scalacenter/sbt-release-early):
  additionally supports publishing to Bintray
- [sbt-rig](https://github.com/Verizon/sbt-rig): additionally supporting
  publishing code coverage reports, managing test dependencies and publishing
  docs.
  

