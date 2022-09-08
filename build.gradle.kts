import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import java.text.SimpleDateFormat
import java.util.*

buildscript {
	repositories {
		maven("https://files.minecraftforge.net/maven")
		mavenCentral()
	}
	dependencies {
		classpath("net.minecraftforge.gradle:ForgeGradle:5.1.+") {
			isChanging = true
		}
	}
}

// Plugins
plugins {
	java
	id("net.minecraftforge.gradle")
	`maven-publish`
	// id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Mod info --------------------------------------------------------------------

val modId = "aerobaticelytra"
val modGroup = "endorh.aerobaticelytra"
val githubRepo = "endorh/aerobaticelytra"
val modVersion = "0.2.19"
val mcVersion = "1.16.5"
val forge = "36.1.0"
val forgeVersion = "$mcVersion-$forge"
val mappingsChannel = "snapshot"
val mappingsVersion = "20201028-1.16.3"

group = modGroup
version = modVersion
val groupSlashed = modGroup.replace(".", "/")
val className = "AerobaticElytra"
val modArtifactId = "$modId-$mcVersion"
val modMavenArtifact = "$modGroup:$modArtifactId:$modVersion"

// Attributes
val displayName = "Aerobatic Elytra"
val vendor = "Endor H"
val credits = ""
val authors = "Endor H"
val issueTracker = ""
val page = ""
val updateJson = ""
val logoFile = "$modId.png"
val modDescription = """
	Adds an special elytra able to roll, fly and leave a trail, like an aerobatic plane.
	All recipes and elytra upgrades can be modified by datapacks with great flexibility.
	Other mods may register their own flight modes for the elytra.
""".trimIndent()

// License
val license = "LGPL"

// Dependencies
val mixinVersion = "0.8.2"
val minimalMixinVersion = "0.7.10"
val flightCoreVersion = "0.5.+"
val simpleConfigApiVersion = "1.0.+"
val simpleConfigVersion = "1.0.+"
val lazuLibVersion = "0.5.+"

// Integration
val jeiVersion = "7.6.1.75"
val curiosVersion = "1.16.5-4.0.5.0"
val caelusVersion = "1.16.5-2.1.3.0"
val aerobaticElytraJetpackVersion = "0.2.+"

val jarAttributes = mapOf(
	"Specification-Title"      to modId,
	"Specification-Vendor"     to vendor,
	"Specification-Version"    to "1",
	"Implementation-Title"     to project.name,
	"Implementation-Version"   to version,
	"Implementation-Vendor"    to vendor,
	"Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
	"Maven-Artifact"           to modMavenArtifact
)

val modProperties = mapOf(
	"modid"         to modId,
	"display"       to displayName,
	"version"       to modVersion,
	"mcversion"     to mcVersion,
	"mixinver"      to mixinVersion,
	"minmixin"      to minimalMixinVersion,
	"vendor"        to vendor,
	"authors"       to authors,
	"credits"       to credits,
	"license"       to license,
	"page"          to page,
	"issue_tracker" to issueTracker,
	"update_json"   to updateJson,
	"logo_file"     to logoFile,
	"description"   to modDescription,
	"group"         to group,
	"class_name"    to className,
	"group_slashed" to groupSlashed
)

// Source Sets -----------------------------------------------------------------

sourceSets.main.get().resources {
	// Include resources generated by data generators.
	srcDir("src/generated/resources")
}

// Java options ----------------------------------------------------------------

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(8))
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

println(
	"Java: " + System.getProperty("java.version")
	+ " JVM: " + System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor")
	+ ") Arch: " + System.getProperty("os.arch"))

// Minecraft options -----------------------------------------------------------

minecraft {
	mappings(mappingsChannel, mappingsVersion)
	
	// Run configurations
	runs {
		val client = create("client") {
			workingDirectory(file("run"))
			
			// Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
			property("forge.logging.markers", "REGISTRIES")
			property("forge.logging.console.level", "debug")
			property("mixin.env.disableRefMap", "true")
			
			mods {
				create(modId) {
					source(sourceSets.main.get())
				}
			}
		}
		
		create("server") {
			workingDirectory(file("run"))
			
			// Allowed flags: SCAN, REGISTRIES, REGISTRYDUMP
			property("forge.logging.markers", "REGISTRIES")
			property("forge.logging.console.level", "debug")
			property("mixin.env.disableRefMap", "true")
			
			arg("nogui")
			
			mods {
				create(modId) {
					source(sourceSets.main.get())
				}
			}
		}
		
		create("client2") {
			parent(client)
			args("--username", "Dev2")
		}
	}
}

// Dependencies ----------------------------------------------------------------

repositories {
	maven("https://repo.maven.apache.org/maven2") {
		name = "Maven Central"
	}
	maven("https://www.cursemaven.com") {
		name = "Curse Maven" // Curse Maven
		content {
			includeGroup("curse.maven")
		}
	}
	
	maven("https://dvs1.progwml6.com/files/maven/") {
		name = "Progwml6 maven" // JEI
	}
	maven("https://modmaven.k-4u.nl") {
		name = "ModMaven" // JEI fallback
	}
	
	maven("https://maven.theillusivec4.top/") {
		name = "TheIllusiveC4" // Curios API
	}
	
	maven(rootProject.projectDir.parentFile.resolve("maven")) {
		name = "LocalMods" // Local repository
	}
	
	val gitHubRepos = listOf("endorh/lazulib", "endorh/flightcore", "endorh/simpleconfig")
	for (repo in gitHubRepos) maven("https://maven.pkg.github.com/$repo") {
		name = "GitHub/$repo"
	}
	
	mavenCentral()
}

dependencies {
	// IDEε
    implementation("org.junit.jupiter:junit-jupiter:5.9.0")
	implementation("org.jetbrains:annotations:23.0.0")

	// Minecraft
    "minecraft"("net.minecraftforge:forge:$forgeVersion")

	// Mod dependencies
	// Flight Core
	// TODO: Replace with curse maven or GitHub maven once published
	implementation(fg.deobf("endorh.flightcore:flightcore-$mcVersion:$flightCoreVersion"))

	// Simple Config
	compileOnly("endorh.simpleconfig:simpleconfig-$mcVersion-api:$simpleConfigApiVersion")
	runtimeOnly(fg.deobf("endorh.simpleconfig:simpleconfig-$mcVersion:$simpleConfigVersion"))

	// Endor8 Util
	implementation(fg.deobf("endorh.util.lazulib:lazulib-$mcVersion:$lazuLibVersion"))

	// Mod integrations
	// JEI
	compileOnly(fg.deobf("mezz.jei:jei-$mcVersion:$jeiVersion:api"))
	runtimeOnly(fg.deobf("mezz.jei:jei-$mcVersion:$jeiVersion"))

	// Curios API
	compileOnly(fg.deobf("top.theillusivec4.curios:curios-forge:$curiosVersion:api"))
	runtimeOnly(fg.deobf("top.theillusivec4.curios:curios-forge:$curiosVersion"))

	// Caelus API
	compileOnly(fg.deobf("top.theillusivec4.caelus:caelus-forge:$caelusVersion:api"))
	runtimeOnly(fg.deobf("top.theillusivec4.caelus:caelus-forge:$caelusVersion"))

	// Used for debug
	// Aerobatic Elytra Jetpack
	runtimeOnly(fg.deobf("endorh.aerobaticelytra.jetpack:aerobaticelytrajetpack-$mcVersion:$aerobaticElytraJetpackVersion"))

	// Curious Elytra
	runtimeOnly(fg.deobf("curse.maven:curiouselytra-317716:3231248"))

	// Colytra
	runtimeOnly(fg.deobf("curse.maven:colytra-280200:3113926"))

	// Customizable Elytra
	runtimeOnly(fg.deobf("curse.maven:customizableelytra-440047:3248968"))

	// Additional Banners
	// runtimeOnly("curse.maven:bookshelf-228525:3241077:deobf")
	// runtimeOnly("curse.maven:additionalbanners-230137:3170181:deobf")

	// Xaero's World Map
	// runtimeOnly(fg.deobf("curse.maven:xaeros-worldmap-317780:3375878"))

	// Xaero's Minimap (waypoint rendering doesn't account for camera roll)
	// runtimeOnly(fg.deobf("curse.maven:xaeros-minimap-263420:3375900"))

	// Immersive Portals (untestable in an unobfuscated environment, crashes without refmaps)
	//   Portals with rotation override roll with a fixed animation that is sometimes in the wrong axis
	//   Wings of players in the portal frontier bind the wrong texture when rendering
	// runtimeOnly(fg.deobf("curse.maven:immersive-portals-355440:3215256"))

	// Catalogue
	runtimeOnly(fg.deobf("curse.maven:catalogue-459701:3529457"))
}

// Tasks --------------------------------------------------------------------------

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.classes {
	dependsOn(tasks.extractNatives.get())
}

lateinit var reobfJar: RenameJarInPlace
reobf {
	reobfJar = create("jar")
}

// Jar attributes
tasks.jar {
	archiveBaseName.set(modArtifactId)
	
	manifest {
		attributes(jarAttributes)
	}

	finalizedBy(reobfJar)
}

val sourcesJarTask = tasks.register<Jar>("sourcesJar") {
	group = "build"
	archiveBaseName.set(modArtifactId)
	archiveClassifier.set("sources")
	
	from(sourceSets.main.get().allJava)
	
	manifest {
		attributes(jarAttributes)
		attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
	}
}

val deobfJarTask = tasks.register<Jar>("deobfJar") {
	group = "build"
	archiveBaseName.set(modArtifactId)
	archiveClassifier.set("deobf")
	
	from(sourceSets.main.get().output)
	
	manifest {
		attributes(jarAttributes)
		attributes(mapOf("Maven-Artifact" to "$modMavenArtifact:${archiveClassifier.get()}"))
	}
}

// Process resources
tasks.processResources {
	inputs.properties(modProperties)
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
	
	// Exclude development files
	exclude("**/.dev/**")
	
	from(sourceSets.main.get().resources.srcDirs) {
		// Expand properties in manifest files
		filesMatching(listOf("**/*.toml", "**/*.mcmeta")) {
			expand(modProperties)
		}
		// Expand properties in JSON resources except for translations
		filesMatching("**/*.json") {
			if (!path.contains("/lang/"))
				expand(modProperties)
		}
	}
}

val saveModsTask = tasks.register<Copy>("saveMods") {
	from("run/mods")
	into("saves/mods")
}

val setupMinecraftTask = tasks.register<Copy>("setupMinecraft") {
	from("saves")
	into("run")
}

val cleanBuildAssetsTask = tasks.register<Delete>("cleanBuildAssets") {
	delete("build/resources/main/assets")
}

// Make the clean task remove the run and logs folder
tasks.clean {
	delete("run")
	delete("logs")
	dependsOn(saveModsTask)
	dependsOn(cleanBuildAssetsTask)
	finalizedBy(setupMinecraftTask)
}

// Publishing ------------------------------------------------------------------

artifacts {
	archives(tasks.jar.get())
	archives(sourcesJarTask)
	archives(deobfJarTask)
}

publishing {
	repositories {
		maven("https://maven.pkg.github.com/$githubRepo") {
			name = "GitHubPackages"
			credentials {
				username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
				password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
			}
		}
		
		maven(rootProject.projectDir.parentFile.resolve("maven")) {
			name = "LocalMods"
		}
	}
	
	publications {
		register<MavenPublication>("mod") {
			artifactId = "$modId-$mcVersion"
			version = modVersion
			
			artifact(tasks.jar.get())
			artifact(sourcesJarTask)
			artifact(deobfJarTask)
			
			pom {
				name.set(displayName)
				url.set(page)
				description.set(modDescription)
			}
		}
	}
}