# sbt-ci-release-early

[![Build Status](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early.svg?branch=master)](https://travis-ci.org/ShiftLeftSecurity/sbt-ci-release-early)
[![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

Sbt plugin for fully automated releases, without SNAPSHOT and git sha's in the version. A remix of the best ideas from [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) and [sbt-release-early](https://github.com/scalacenter/sbt-release-early/).

Features:
* detects last version from git (e.g. `v1.0.0`) and increments the last digit, i.e. the next release is automatically inferred as `v1.0.1`
* no more git hashes in the released version
* no more need to manually tag the builds
* builds the release, creates and pushes the git tag, publishes the artifact
* `ci-release` for your in-house setup (e.g. jenkins/artifactory/nexus etc), very easy to configure
* `ci-release-sonatype` for your open source travis/sonatype/maven central setup, a little more involved to configure
* easy to test locally (faster turnaround than debugging on travis.ci)
* automatically handles cross release for multiple scala versions
* verifies that your build does not depend on any snapshot dependencies

# TODO skim through old readme, cleanup
# TODO factor in
## public build (travis.ci)
Note: the plugin doesn't have any dependency on travis.ci and will work with any other public build server.
* requirement: travis needs to be able to push to your repository
  * explain steps. screenshot in ~
  https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/
* section for jenkins/manual
  * for sonatype: configure `publishTo := sonatypePublishTo.value` in build.sbt
* SCM info etc: travis specific
* set PGP_PASSPHRASE as an environment variable
* list/delete failed staging attempts on sonatype:
  `sonatypeStagingRepositoryProfiles` // lists staging repo ids
  `sonatypeDrop [id]`
* this originally started as a fork of sbt-ci-release by olafurpg, but quickly evolved into something new, so I gave it a new name. 
* subprojects: can run `publish`, the version is already set for you :)
* describe simple key handling that works consistently 
* how do I release a specific version? 
  To keep things simple I decided to not add that feature to this plugin. I.e. if you want to release a specific version you have to do that yourself, e.g. by running the following sbt commands:
  ```
    set version := "1.2.3"
    +publishSigned
    sonatypeReleaseAll
  ```
  Followed by `git tag v1.2.3 && git push origin v1.2.3`.
  Note to future self: this would have added complexity because to trigger it we would rely on git tags, and we need a foolproof way to check if a given tag has already been released. My intial thought was to tag anything released with `_released_1.0.1_`, but it was getting quite complicated for handling an edge case. 
* differences to the original from olafur:
* verifies that there are no snapshot dependencies (copied from sbt-release)
* less dependencies on other plugins
* this plugin automatically increases your version, rather than appending a long suffix to the last version (resulting in in e.g. version `1.2.2` rather than `1.2.1+1-1955bf0a+20180824-0758`)
* downside: you have less control over which version exactly is being built
* it tags that version automatically
* not specifically on travis.ci. It's even easier to use it on your own jenkins and artifactory

<!-- TOC depthFrom:2 depthTo:2 -->

- [In-house setup](#In-house)
- [Sonatype](#sonatype)
- [sbt](#sbt)
- [GPG](#gpg)
- [Travis](#travis)
- [Git](#git)
- [FAQ](#faq)
- [Alternatives](#alternatives)

<!-- /TOC -->


## In-house setup (e.g. jenkins/artifactory)
The majority of builds aren't in the open, but on private machines (e.g. jenkins), published to a private repository like nexus or artifactory. Setup:

1) `projects/plugins.sbt`:
```
addSbtPlugin("io.shiftleft" % "sbt-ci-release-early" % "1.0.4")
```
Latest version: [![Scaladex](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)](https://index.scala-lang.org/ShiftLeftSecurity/sbt-ci-release-early/latest.svg)

2) Make sure the typical `publishTo` variable in your `built.sbt` points to your repository (this isn't specific to this plugin).

Just run 
```
sbt ci-release
```
as part of your build pipeline, and that's all. 

## Public build (e.g. travis.ci/sonatype)
Since those internal repositories typically don't impose constraints on the published artifacts,

## Sonatype

First, follow the instructions in
https://central.sonatype.org/pages/ossrh-guide.html to create a Sonatype account
and make sure you have publishing rights for a domain name. This is a one-time
setup per domain name.

If you don't have a domain name, you can use `com.github.<@your_username>`.
Here is a template you can use to write the Sonatype issue:

```
Title:
Publish rights for com.github.olafurpg
Description:
Hi, I would like to publish under the groupId: com.github.olafurpg.
It's my GitHub account https://github.com/olafurpg/
```

## sbt

Next, install this plugin in `project/plugins.sbt`

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.geirsson/sbt-ci-release/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.geirsson/sbt-ci-release)

```scala
// sbt 1 only, see FAQ for 0.13 support
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.1")
```

By installing `sbt-ci-release` the following sbt plugins are also brought in:

- [sbt-pgp](https://github.com/sbt/sbt-pgp): to cryptographically sign the artifacts before publishing
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype): to publish artifacts to Sonatype

Make sure `build.sbt` *does not* define any of the following settings

- `publishTo`: handled by sbt-ci-release
- `publishMavenStyle`: handled by sbt-ci-release
- `credentials`: handled by sbt-sonatype

Ensure the following settings *are* defined in your `build.sbt`:

- `name`
- `organization`: must match your sonatype account priviledges
- `licenses`
- `developers`
- `scmInfo`

Example:
```scala
inThisBuild(List(
  organization := "io.shiftleft",
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/mpollmeier/sbt-ci-release")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/mpollmeier/sbt-ci-release-usage"),
      "scm:git@github.com:mpollmeier/sbt-ci-release-usage.git")),
  developers := List(
    Developer(
      "mpollmeier",
      "Michael Pollmeier",
      "michael@michaelpollmeier.com",
      url("https://michaelpollmeier.com")
    )
  )
))
```

## GPG

Next, create a fresh gpg key that you will share with Travis CI and only use for
this project.

```
gpg --gen-key
```

- For real name, use "$PROJECT_NAME bot". For example: "Scalafmt bot"
- For email, use your own email address
- For passphrase, generate a random password with a password manager

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

Submit the public key to a keyserver:

```
gpg --send-keys $LONG_ID
```

Then export the private key locally so we can later encrypt it for travis. Make sure you don't publish this anywhere. The actual damage is small since it's just for this project, but people will look down on you :)

```
gpg --armor --export-secret-keys $LONG_ID > private-key.pem
echo "\nprivate-key.pem" >> .gitignore
```

## Travis

Next, open the "Settings" panel for your project on Travis CI, for example
https://travis-ci.org/mpollmeier/sbt-ci-release-early-usage/settings

Define secret variables

![](https://user-images.githubusercontent.com/1408093/41207402-bbb3970a-6d15-11e8-8772-000cc194ee92.png)

- `SONATYPE_USERNAME`: The email you use to log into https://oss.sonatype.org/
- `SONATYPE_PASSWORD`: The password you use to log into https://oss.sonatype.org/
- `PGP_PASSPHRASE`: The randomly generated password you used to create a fresh gpg key.
- `GH_TOKEN`: the token that allows travis to push to your remote git(hub) repository

### .travis.yml

Next, update `.travis.yml` to trigger `ci-release` on successful merge into
master and on tag push. There are many ways to do this, but I recommend using
[Travis "build stages"](https://docs.travis-ci.com/user/build-stages/). It's not
necessary to use build stages but they make it easy to avoid publishing the
same module multiple times from parallel jobs.

- First, ensure that git tags are always fetched so that we can determine the correct version

```yml
before_install:
  - git fetch --tags
```

- Next, define `test` and `release` build stages

```yml
stages:
  - name: test
  - name: release
    if: (branch = master AND type = push) OR (tag IS present)
```

- Lastly, define your build matrix with `ci-release` at the bottom, for example:

```yml
jobs:
  include:
    # stage="test" if no stage is specified
    - env: TEST="compile"
      script: sbt compile
    - env: TEST="formatting"
      script: ./bin/scalafmt --test
    # run ci-release only if previous stages passed
    - stage: release
      script: sbt ci-release
```

- Last lastely, share the private key with travis. Note that this has to be run from within the repository.

```
travis encrypt-file private-key.pem --add
```

Notes:

- for a complete example of the Travis configuration, see the [.travis.yml in
  this repository](https://github.com/olafurpg/sbt-ci-release/blob/master/.travis.yml)
- if we use `after_success` instead of build stages, we would run `ci-release`
  after both `TEST="formatting"` and `TEST="compile"`. As long as you make sure
  you don't publish the same module multiple times, you can use any Travis
  configuration you like
- the `env: TEST="compile"` part is optional but it makes it easy to distinguish
  different jobs in the Travis UI

![build__48_-_olafurpg_sbt-ci-release_-_travis_ci](https://user-images.githubusercontent.com/1408093/41810442-a44ef526-76fe-11e8-92f4-4c4b61af4d38.jpg)

## Git

We're all set! Time to manually try out the new setup

- Open a PR and merge it to watch the CI release a -SNAPSHOT version
- Push a tag and watch the CI do a regular release

```
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

Note that the tag version MUST start with `v`.

It's normal that something fails on the first attempt to publish from CI.
Even if it takes 10 attempts to get it right, it's still worth it because it's
so nice to have automatic CI releases. If all is correctly setup, your Travis
jobs page will look like this:

<img width="1058" alt="screen shot 2018-06-23 at 15 48 43" src="https://user-images.githubusercontent.com/1408093/41810386-b8c11198-76fd-11e8-8be1-54b84181e60d.png">

Enjoy 👌

## FAQ

### How do I disable publishing in certain projects?

Add the following to the project settings:

```scala
skip in publish := true
```

### Can I depend on Maven Central releases immediately?

Yes! As soon as CI "closes" the staging repository you can depend on those
artifacts with

```scala
resolvers += Resolver.sonatypeRepo("releases")
```

### Can I publish sbt plugins?

You can publish sbt plugins to Maven Central like a normal library, no custom
setup required. In fact, this plugin is published with a previous version of itself :)


## Alternatives

There exist great alternatives to sbt-ci-release that may work better for your
setup.

- [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release):
  Releases versions that you previously tagged in git (rather than automatically tagging every build).
  This plugin started as a fork of sbt-ci-release. 
  I ran into [some issues](https://github.com/olafurpg/sbt-ci-release/issues/13) with gpg keys, ymmv. 
- [sbt-release-early](https://github.com/scalacenter/sbt-release-early):
  additionally supports publishing to Bintray
- [sbt-rig](https://github.com/Verizon/sbt-rig): additionally supporting
  publishing code coverage reports, managing test dependencies and publishing
  docs.
