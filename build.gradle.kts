plugins { java }

group = "tw.crestnetwork"
version = "2.1.0"

repositories {
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:26.2.build.+")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("com.google.code.gson:gson:2.13.2")
    testImplementation("org.purpurmc.purpur:purpur-api:26.2.build.+")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

sourceSets {
    main { java.exclude("**/* 2.java") }
    test { java.exclude("**/* 2.java") }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.test { useJUnitPlatform() }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release.set(25) }
