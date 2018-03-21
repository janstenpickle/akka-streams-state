import microsites._

val commonSettings = Seq(
  organization := "io.otters",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
  addCompilerPlugin(("org.spire-math"  % "kind-projector" % "0.9.4").cross(CrossVersion.binary)),
  addCompilerPlugin(("org.scalamacros" % "paradise"       % "2.1.0").cross(CrossVersion.full)),
  scalacOptions ++= Seq(
    "-unchecked",
    "-feature",
    "-deprecation:false",
    "-Xcheckinit",
    "-Xlint:-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-dead-code",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8"
  ),
  publishMavenStyle := true,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("https://github.com/janstenpickle/otters")),
  developers := List(
    Developer(
      "janstenpickle",
      "Chris Jansen",
      "janstenpickle@users.noreply.github.com",
      url = url("https://github.com/janstepickle")
    )
  ),
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  bintrayReleaseOnPublish := true,
  coverageMinimum := 85,
  releaseCrossBuild := true,
  scalafmtOnCompile := true,
  scalafmtTestOnCompile := true,
  releaseIgnoreUntrackedFiles := true
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(name := "otters", publishArtifact := false)
  .aggregate(core, akka, fs2, laws, `monix-reactive`, `monix-tail`)

lazy val core = (project in file("core"))
  .settings(commonSettings)
  .settings(
    name := "otters-core",
    libraryDependencies ++= Seq(
      Dependencies.cats,
      Dependencies.simulacrum,
      Dependencies.scalaCheck % Test,
      Dependencies.scalaTest  % Test
    ),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )

lazy val laws = (project in file("laws"))
  .settings(commonSettings)
  .settings(
    name := "otters-laws",
    libraryDependencies ++= Seq(
      Dependencies.cats,
      Dependencies.catsLaws,
      Dependencies.catsEffectLaws,
      Dependencies.discipline,
      Dependencies.scalaCheck,
      Dependencies.scalaTest
    ),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )
  .dependsOn(core)

lazy val akka = (project in file("akka"))
  .settings(commonSettings)
  .settings(
    name := "otters-akka",
    libraryDependencies ++= Seq(
      Dependencies.akkaStreams,
      Dependencies.discipline,
      Dependencies.catsEffectLaws % Test,
      Dependencies.scalaCheck     % Test,
      Dependencies.scalaTest      % Test
    ),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )
  .dependsOn(core % "compile->compile;test->test", laws % "test->compile")

lazy val fs2 = (project in file("fs2"))
  .settings(commonSettings)
  .settings(
    name := "otters-fs2",
    libraryDependencies ++= Seq(Dependencies.fs2, Dependencies.scalaCheck % Test, Dependencies.scalaTest % Test),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )
  .dependsOn(core % "compile->compile;test->test", laws % "test->compile")

lazy val `monix-reactive` = (project in file("monix-reactive"))
  .settings(commonSettings)
  .settings(
    name := "otters-monix-reactive",
    libraryDependencies ++= Seq(
      Dependencies.monixReactive,
      Dependencies.monixTail,
      Dependencies.scalaCheck % Test,
      Dependencies.scalaTest  % Test
    ),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )
  .dependsOn(core % "compile->compile;test->test", laws % "test->compile")

lazy val `monix-tail` = (project in file("monix-tail"))
  .settings(commonSettings)
  .settings(
    name := "otters-monix-tail",
    libraryDependencies ++= Seq(Dependencies.monixTail, Dependencies.scalaCheck % Test, Dependencies.scalaTest % Test),
    publishArtifact in Test := true,
    coverageEnabled.in(Test, test) := true
  )
  .dependsOn(core % "compile->compile;test->test", laws % "test->compile")

lazy val docSettings = commonSettings ++ Seq(
  micrositeName := "Otters",
  micrositeDescription := "Otters",
  micrositeAuthor := "Chris Jansen",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "https://janstenpickle.github.io/otters/",
  micrositeBaseUrl := "otters",
  micrositeDocumentationUrl := "api",
  micrositeGithubOwner := "janstenpickle",
  micrositeGithubRepo := "otters",
  micrositeExtraMdFiles := Map(
    file("CONTRIBUTING.md") -> ExtraMdFileConfig("contributing.md", "docs"),
    file("README.md") -> ExtraMdFileConfig("index.md", "home", Map("section" -> "home", "position" -> "0"))
  ),
  micrositeFavicons := Seq(
    MicrositeFavicon("favicon16x16.png", "16x16"),
    MicrositeFavicon("favicon32x32.png", "32x32")
  ),
  micrositePalette := Map(
    "brand-primary" -> "#009933",
    "brand-secondary" -> "#006600",
    "brand-tertiary" -> "#339933",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"
  ),
  micrositePushSiteWith := GitHub4s,
  micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
  micrositeGitterChannel := false,
  micrositeCDNDirectives := CdnDirectives(
    jsList = List("https://cdn.rawgit.com/knsv/mermaid/6.0.0/dist/mermaid.min.js"),
    cssList = List("https://cdn.rawgit.com/knsv/mermaid/6.0.0/dist/mermaid.css")
  ),
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  ghpagesNoJekyll := false,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-skip-packages",
    "scalaz",
    "-sourcepath",
    baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-doc-root-content",
    (resourceDirectory.in(Compile).value / "rootdoc.txt").getAbsolutePath
  ),
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  git.remoteRepo := "git@github.com:janstenpickle/otters.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inAnyProject -- inProjects(root),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project
  .dependsOn(core, akka, fs2, `monix-reactive`, `monix-tail`)
  .settings(
    moduleName := "otters-docs",
    name := "Otters docs",
    publish := (()),
    publishLocal := (()),
    publishArtifact := false
  )
  .settings(docSettings)
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MicrositesPlugin)
