### Development

I only recommend using IntelliJ IDEA to edit this project.

To run the project on 1.17+ branches you'll need to use the JetBrains Runtime
(vanilla JBR without fastdebug) as the project's JDK, or delete the
`-XX:+AllowEnhancedClassRedefinition` JVM arguments from the gradle build script
and rerun `:genIntellijRuns`.

Other than that, Forge development conventions apply. Remember to reimport the project
(Reload Gradle Project) and rerun `:genIntellijRuns` after checking out a branch for a
different Minecraft version.

Also, developing a feature across multiple Minecraft versions is not an enjoyable experience,
so I recommend against it. A trick is checking out each branch to a different local project
to cut down on checkout+reimport times.