pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
      maven("https://maven.neoforged.net/releases") {
         name = "NeoForge"
      }

      maven("https://maven.parchmentmc.org") {
         name = "Parchment MC"
      }
   }
}

rootProject.name = "aerobatic-elytra"