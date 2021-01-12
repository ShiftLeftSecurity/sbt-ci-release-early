libraryDependencies += "org.eclipse.jgit"     % "org.eclipse.jgit" % "5.4.3.201909031940-r"
libraryDependencies += "com.michaelpollmeier" % "versionsort"      % "1.0.1"

// For using the plugin in its own build
unmanagedSourceDirectories in Compile +=
  baseDirectory.in(ThisBuild).value.getParentFile / "src" / "main" / "scala"