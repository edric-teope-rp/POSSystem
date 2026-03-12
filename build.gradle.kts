plugins {
    id("java")
    id("application")
}

group = "org.possystem"
version = "1.0-SNAPSHOT"

application {
    mainClass = "org.possystem.Main"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.h2database:h2:2.2.224")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}