import com.github.davidmc24.gradle.plugin.avro.AvroExtension
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.3.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.github.spotbugs") version "6.5.5" apply false
    id("org.sonarqube") version "7.3.0.8198"
}

group = "com.example.library"
version = "0.0.1-SNAPSHOT"

val springBootVersion = "3.3.7"
val queryDslVersion = "5.0.0"
val springDocVersion = "2.6.0"
val archUnitVersion = "1.4.2"
val avroVersion = "1.11.3"
val confluentVersion = "7.7.1"
val mapstructVersion = "1.6.3"
val lombokMapstructBindingVersion = "0.2.0"
val jacocoVersion = "0.8.14"
val pmdVersion = "7.16.0"
val spotbugsVersion = "4.9.7"
val jetbrainsAnnotationsVersion = "26.1.0"

val generatedSourceExcludes = listOf(
    "**/common/event/schema/**",
    "**/Q*.java",
    "**/*MapperImpl.java"
)
val generatedClassExcludes = listOf(
    "**/common/event/schema/**",
    "**/Q*.class",
    "**/*MapperImpl.class"
)
val coverageClassExcludes = generatedClassExcludes + listOf(
    "**/*Application.class",
    "**/config/**"
)
val minimumLineCoverage = providers.gradleProperty("minimumLineCoverage")
    .orElse("0.00")
    .map { it.toBigDecimal() }
val minimumBranchCoverage = providers.gradleProperty("minimumBranchCoverage")
    .orElse("0.00")
    .map { it.toBigDecimal() }

extensions.configure<JacocoPluginExtension> {
    toolVersion = jacocoVersion
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        val generatedSourceSet = if (name.contains("Test")) "test" else "main"
        options.generatedSourceOutputDirectory.set(
            layout.buildDirectory.dir("generated/sources/annotationProcessor/java/$generatedSourceSet")
        )
    }

    the<SourceSetContainer>()["main"].java.srcDir(
        layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")
    )

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "compileOnly"("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion
    }

    extensions.configure<PmdExtension> {
        toolVersion = pmdVersion
        isConsoleOutput = true
        rulesMinimumPriority.set(3)
        ruleSets = emptyList()
        ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
    }

    extensions.configure<SpotBugsExtension> {
        toolVersion.set(spotbugsVersion)
        effort.set(Effort.MAX)
        reportLevel.set(Confidence.MEDIUM)
        excludeFilter.set(rootProject.file("config/spotbugs/excludeFilter.xml"))
    }

    configurations.named("spotbugs") {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.apache.commons:commons-lang3"))
                .using(module("org.apache.commons:commons-lang3:3.19.0"))
                .because("SpotBugs 4.9.x requires commons-lang3 APIs newer than the Spring Boot managed 3.14.0 version.")
        }
    }

    tasks.withType<Pmd>().configureEach {
        exclude(generatedSourceExcludes)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<Pmd>("pmdTest") {
        enabled = false
    }

    tasks.withType<SpotBugsTask>().configureEach {
        reports.create("xml") {
            required = true
        }
        reports.create("html") {
            required = true
        }
    }

    tasks.named<SpotBugsTask>("spotbugsTest") {
        enabled = false
    }

    val mainSourceSet = extensions.getByType<SourceSetContainer>()["main"]

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        classDirectories.setFrom(
            files(mainSourceSet.output.classesDirs.files.map {
                fileTree(it) {
                    exclude(coverageClassExcludes)
                }
            })
        )
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        classDirectories.setFrom(
            files(mainSourceSet.output.classesDirs.files.map {
                fileTree(it) {
                    exclude(coverageClassExcludes)
                }
            })
        )
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = minimumLineCoverage.get()
                }
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = minimumBranchCoverage.get()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }
}

project(":common-events") {
    apply(plugin = "java-library")
    apply(plugin = "com.github.davidmc24.gradle.plugin.avro")

    extensions.configure<AvroExtension>("avro") {
        isCreateSetters.set(false)
        stringType.set("String")
    }

    dependencies {
        "api"("jakarta.persistence:jakarta.persistence-api")
        "api"("com.querydsl:querydsl-core:$queryDslVersion")
        "api"("org.apache.avro:avro:$avroVersion")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "annotationProcessor"("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
        "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
        "annotationProcessor"("jakarta.annotation:jakarta.annotation-api")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("com.tngtech.archunit:archunit-junit5:$archUnitVersion")
    }
}

project(":common-core") {
    apply(plugin = "java-library")

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-autoconfigure")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.security:spring-security-core")
        "implementation"("org.springframework:spring-tx")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    }
}

val relationalServiceProjects = listOf(
    project(":rental-service"),
    project(":book-service"),
    project(":member-service")
)
val bestbookServiceProject = project(":bestbook-service")
val serviceProjects = relationalServiceProjects + bestbookServiceProject

configure(serviceProjects) {
    apply(plugin = "org.springframework.boot")

    dependencies {
        "implementation"(project(":common-core"))
        "implementation"(project(":common-events"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
        "implementation"("org.springframework.kafka:spring-kafka")
        "implementation"("io.confluent:kafka-avro-serializer:$confluentVersion")
        "implementation"("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
        "runtimeOnly"("io.micrometer:micrometer-registry-prometheus")
        "annotationProcessor"("org.springframework.boot:spring-boot-configuration-processor")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.kafka:spring-kafka-test")
        "testImplementation"("com.tngtech.archunit:archunit-junit5:$archUnitVersion")
    }
}

configure(relationalServiceProjects) {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")
        "runtimeOnly"("org.mariadb.jdbc:mariadb-java-client")
        "annotationProcessor"("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
        "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
        "annotationProcessor"("jakarta.annotation:jakarta.annotation-api")
    }
}

project(":book-service") {
    dependencies {
        "implementation"("org.mapstruct:mapstruct:$mapstructVersion")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:$mapstructVersion")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    }
}

project(":bestbook-service") {
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-data-mongodb")
        "implementation"("org.mapstruct:mapstruct:$mapstructVersion")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:$mapstructVersion")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
    }
}

val jacocoRootReport by tasks.registering(JacocoReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate JaCoCo coverage report for all modules."
    dependsOn(subprojects.map { it.tasks.named("test") })

    val mainSourceSets = subprojects.map {
        it.extensions.getByType<SourceSetContainer>()["main"]
    }

    executionData.setFrom(
        subprojects.map {
            it.layout.buildDirectory.file("jacoco/test.exec")
        }
    )
    sourceDirectories.setFrom(files(mainSourceSets.flatMap { it.allSource.srcDirs }))
    classDirectories.setFrom(
        files(mainSourceSets.flatMap { sourceSet ->
            sourceSet.output.classesDirs.files.map {
                fileTree(it) {
                    exclude(coverageClassExcludes)
                }
            }
        })
    )
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoRootReport/jacocoRootReport.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoRootReport/html"))
        csv.required.set(false)
    }
}

tasks.register("qualityCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests, ArchUnit tests, PMD, SpotBugs, and JaCoCo verification for all modules."
    dependsOn(subprojects.map { it.tasks.named("check") })
    dependsOn(jacocoRootReport)
}

sonar {
    properties {
        property("sonar.projectKey", "library-rental-eda")
        property("sonar.projectName", "library-rental-eda")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.gradle.scanAll", "true")
        property(
            "sonar.exclusions",
            listOf(
                "**/build/**",
                "**/generated/**",
                "**/common/event/schema/**",
                "**/Q*.java",
                "**/*MapperImpl.java"
            ).joinToString(",")
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/*Application.java",
                "**/config/**",
                "**/common/event/schema/**",
                "**/Q*.java",
                "**/*MapperImpl.java"
            ).joinToString(",")
        )
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            subprojects.joinToString(",") {
                it.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile.invariantSeparatorsPath
            }
        )
        property(
            "sonar.java.pmd.reportPaths",
            subprojects.joinToString(",") {
                it.layout.buildDirectory.file("reports/pmd/main.xml").get().asFile.invariantSeparatorsPath
            }
        )
        property(
            "sonar.java.spotbugs.reportPaths",
            subprojects.joinToString(",") {
                it.layout.buildDirectory.file("reports/spotbugs/main.xml").get().asFile.invariantSeparatorsPath
            }
        )
    }
}

tasks.named("sonar") {
    dependsOn(subprojects.map { it.tasks.named("test") })
    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })
    dependsOn(subprojects.map { it.tasks.named("pmdMain") })
    dependsOn(subprojects.map { it.tasks.named("spotbugsMain") })
}
