plugins {
    id("java")
    id("jacoco")
}

group = "dhbw"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val jgivenReport by configurations.creating

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.tngtech.jgiven:jgiven-junit5:2.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    jgivenReport("com.tngtech.jgiven:jgiven-html5-report:2.0.3")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

tasks.test {
    useJUnitPlatform()
    doNotTrackState("OneDrive can expose generated test report files as unreadable placeholders.")
    systemProperty("jgiven.report.dir", layout.projectDirectory.dir("target/jgiven-reports/json").asFile.absolutePath)
    finalizedBy(tasks.jacocoTestReport)
    finalizedBy("jgivenHtmlReport")
}

tasks.withType<JavaExec>().configureEach {
    doNotTrackState("OneDrive can expose generated class files as unreadable placeholders.")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}

tasks.register<JavaExec>("jgivenHtmlReport") {
    dependsOn(tasks.test)
    classpath = jgivenReport
    mainClass.set("com.tngtech.jgiven.report.ReportGenerator")
    args(
        "--format=html",
        "--sourceDir=${layout.projectDirectory.dir("target/jgiven-reports/json").asFile.absolutePath}",
        "--targetDir=${layout.projectDirectory.dir("target/jgiven-reports/html").asFile.absolutePath}"
    )
}

tasks.register<JavaExec>("runMain") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("edu.swarmintelligence.mayfly.Main")
}

tasks.register<JavaExec>("generateAnalyticsReport") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("edu.swarmintelligence.mayfly.AnalyticsReportMain")
}

tasks.register("multiRunVerify") {
    dependsOn(tasks.check)
    dependsOn("generateAnalyticsReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            include("edu/swarmintelligence/mayfly/*Analyzer*.class")
            include("edu/swarmintelligence/mayfly/AnalyticsEngine.class")
            include("edu/swarmintelligence/mayfly/AnalyticsReport.class")
        }
    }))
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
