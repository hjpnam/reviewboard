ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Yretain-trees"
)

ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

val zioVersion        = "2.1.14"
val tapirVersion      = "1.11.13"
val zioLoggingVersion = "2.4.0"
val zioConfigVersion  = "4.0.3"
val sttpVersion       = "3.10.2"
val javaMailVersion   = "1.6.2"
val stripeVersion     = "24.3.0"
val catsVersion       = "2.12.0"

val commonDependencies = Seq(
  "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"    % tapirVersion,
  "com.softwaremill.sttp.client3" %% "zio"               % sttpVersion
)

val serverDependencies = commonDependencies ++ Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion, // Brings in zio, zio-streams
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % tapirVersion, // Brings in zhttp
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"  % tapirVersion % "test",
  "dev.zio"                     %% "zio-logging"             % zioLoggingVersion,
  "dev.zio"                     %% "zio-logging-slf4j"       % zioLoggingVersion,
  "ch.qos.logback"               % "logback-classic"         % "1.5.16",
  "dev.zio"                     %% "zio-test"                % zioVersion,
  "dev.zio"                     %% "zio-test-junit"          % zioVersion   % "test",
  "dev.zio"                     %% "zio-test-sbt"            % zioVersion   % "test",
  "dev.zio"                     %% "zio-test-magnolia"       % zioVersion   % "test",
  "dev.zio"                     %% "zio-mock"                % "1.0.0-RC12" % "test",
  "dev.zio"                     %% "zio-config"              % zioConfigVersion,
  "dev.zio"                     %% "zio-config-magnolia"     % zioConfigVersion,
  "dev.zio"                     %% "zio-config-typesafe"     % zioConfigVersion,
  "dev.zio"                     %% "zio-interop-cats"        % "23.1.0.3",
  "io.getquill"                 %% "quill-jdbc-zio"          % "4.8.6",
  "org.postgresql"               % "postgresql"              % "42.7.5",
  "org.flywaydb"                 % "flyway-core"             % "11.2.0",
  "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.10.0",
  "dev.zio"               %% "zio-prelude"                       % "1.0.0-RC37",
  "com.auth0"              % "java-jwt"                          % "4.4.0",
  "com.sun.mail"           % "javax.mail"                        % javaMailVersion,
  "com.stripe"             % "stripe-java"                       % stripeVersion,
  "io.github.arainko"     %% "ducktape"                          % "0.2.7",
  "org.typelevel"         %% "cats-core"                         % catsVersion,
  "org.eclipse.angus"      % "angus-mail"                        % "2.0.3"
)

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/common"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0" // implementations of java.time classes for Scala.JS,
    )
  )

lazy val server = (project in file("modules/server"))
  .settings(
    libraryDependencies ++= serverDependencies
  )
  .dependsOn(common.jvm)

lazy val app = (project in file("modules/app"))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %%% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir"   %%% "tapir-json-zio"    % tapirVersion,
      "com.softwaremill.sttp.client3" %%% "zio"               % sttpVersion,
      "dev.zio"                       %%% "zio-json"          % "0.7.4",
      "io.frontroute"                 %%% "frontroute"        % "0.19.0" // Brings in Laminar 17
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled               := true,
    autoAPIMappings                 := true,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass             := Some("com.hjpnam.reviewboard.App")
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common.js)

lazy val root = (project in file("."))
  .settings(
    name := "zio-rite"
  )
  .aggregate(server, app)
  .dependsOn(server, app)
