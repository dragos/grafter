
lazy val grafter = (project in file(".")).
  settings(
    rootSettings        ++
    compilationSettings ++
    commonSettings      ++
    publishSettings
  ).aggregate(core, macros)

lazy val core = (project in file("core")).
  settings(
    compilationSettings ++
    testSettings ++
    Seq(publishArtifact := false)
  )

lazy val macros = project.in(file("macros")).
  settings(
    compilationSettings ++
    Seq(publishArtifact := false)
  ).dependsOn(core)

lazy val examples = (project in file("examples")).
  settings(
    compilationSettings ++
      testSettings ++
      Seq(publishArtifact := false)
  ).dependsOn(core, macros)

lazy val tests = (project in file("tests")).
  settings(
    compilationSettings ++
      testSettings ++
      Seq(publishArtifact := false)
  ).dependsOn(core, core % "test->test", macros)

lazy val guide = (project in file("guide")).
  settings(
    compilationSettings ++
      testSettings ++
      Seq(git.remoteRepo := "git@github.com:zalando/grafter.git",
          siteSourceDirectory := target.value / "specs2-reports") ++
      Seq(publishArtifact := false)
  ).dependsOn(core, core % "test->test", macros).
  enablePlugins(GhpagesPlugin)

lazy val rootSettings = Seq(
  unmanagedSourceDirectories in Compile := unmanagedSourceDirectories.all(aggregateCompile).value.flatten,
  unmanagedSourceDirectories in Test    := unmanagedSourceDirectories.all(aggregateTest).value.flatten,
  sources in Compile                    := sources.all(aggregateCompile).value.flatten,
  sources in Test                       := sources.all(aggregateTest).value.flatten,
  mappings in (Test, packageBin)        ~= (_.filter(mappingFilter)),
  libraryDependencies                   := libraryDependencies.all(aggregateCompile).value.flatten
)

def mappingFilter(mapping: (File, String)): Boolean =
  mapping._1.getPath.contains("org/zalando/grafter/specs2")

lazy val aggregateCompile = ScopeFilter(
  inProjects(core, macros),
  inConfigurations(Compile))

lazy val aggregateTest = ScopeFilter(
  inProjects(core, macros, tests, examples),
  inConfigurations(Test))

lazy val commonSettings = Seq(
  organization         := "org.zalando",
  name                 := "grafter"
)

lazy val testSettings = Seq(
  fork          in Test := true,
  scalacOptions in Test ++= Seq("-Yrangepos"),
  scalacOptions in Test -= "-Ywarn-unused",
  scalacOptions in Test -= "-Xlint",
  testFrameworks in Test := Seq(TestFrameworks.Specs2),
  testOptions in Test += Tests.Filter(s => !s.endsWith("Specification")),
  coverageEnabled := false
)

val paradiseVersion = settingKey[String]("paradiseVersion")

lazy val compilationSettings = Seq(
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.6"),
  paradiseVersion := {
    val v = scalaVersion.value
    v match { case "2.11.8" => "2.1.0"; case _ => "2.1.1" }
  },
  autoCompilerPlugins := true,
  libraryDependencies +=  compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion.value cross CrossVersion.full),
  scalacOptions ++= Seq(
    "-unchecked",
    "-feature",
    "-deprecation:false",
    "-Xfatal-warnings",
    "-Xcheckinit",
    "-Xlint",
    "-Xlint:-nullary-unit",
    "-Ywarn-unused-import",
    "-Ywarn-numeric-widen",
    "-Ywarn-dead-code",
    "-Yno-adapted-args",
    // to debug the macros
    //"-Ymacro-debug-lite",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"
  )
)

lazy val publishSettings = Seq(
  publishTo := Option("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
  publishMavenStyle := true,
  homepage := Some(url("https://github.com/zalando/grafter")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(ScmInfo(url("https://github.com/zalando/grafter"), "scm:git:git@github.com:zalando/grafter.git")),
  autoAPIMappings := true,
  pomExtra := (
    <developers>
      <developer>
        <id>etorreborre</id>
        <name>Eric Torreborre</name>
        <url>https://github.com/etorreborre/</url>
      </developer>
    </developers>
  ),
  publishArtifact in (Test, packageBin) := true,
  publishArtifact in (Test, packageDoc) := true,
  publishArtifact in (Test, packageSrc) := true
) ++
  credentialSettings

lazy val credentialSettings = Seq(
  // For Travis CI - see http://www.cakesolutions.net/teamblogs/publishing-artefacts-to-oss-sonatype-nexus-using-sbt-and-travis-ci
  credentials ++= (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)

shellPrompt in ThisBuild := { state =>
  val name = Project.extract(state).currentRef.project
  (if (name == "grafter") "" else name) + "> "
}
