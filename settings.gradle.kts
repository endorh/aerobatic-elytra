pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
      maven("https://files.minecraftforge.net/maven") {
         name = "Minecraft Forge"
      }

      maven("https://maven.parchmentmc.org") {
         name = "Parchment MC"
      }
   }
}

rootProject.name = "aerobatic-elytra"