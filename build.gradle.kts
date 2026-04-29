import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    id("org.springframework.boot") version "3.3.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "com.example.library"
version = "0.0.1-SNAPSHOT"

val springBootVersion = "3.3.7"
val queryDslVersion = "5.0.0"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

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
        options.generatedSourceOutputDirectory.set(layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
    }

    the<SourceSetContainer>()["main"].java.srcDir(
        layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")
    )

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

project(":common-events") {
    apply(plugin = "java-library")

    dependencies {
        "api"("jakarta.persistence:jakarta.persistence-api")
        "api"("com.querydsl:querydsl-core:$queryDslVersion")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "annotationProcessor"("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
        "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
        "annotationProcessor"("jakarta.annotation:jakarta.annotation-api")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    }
}

val serviceProjects = listOf(
    project(":rental-service"),
    project(":book-service"),
    project(":member-service"),
    project(":bestbook-service")
)

configure(serviceProjects) {
    apply(plugin = "org.springframework.boot")

    dependencies {
        "implementation"(project(":common-events"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "implementation"("org.springframework.boot:spring-boot-starter-security")
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
        "implementation"("org.springframework.boot:spring-boot-starter-data-redis")
        "implementation"("org.springframework.kafka:spring-kafka")
        "implementation"("com.querydsl:querydsl-jpa:$queryDslVersion:jakarta")
        "runtimeOnly"("org.mariadb.jdbc:mariadb-java-client")
        "annotationProcessor"("com.querydsl:querydsl-apt:$queryDslVersion:jakarta")
        "annotationProcessor"("jakarta.persistence:jakarta.persistence-api")
        "annotationProcessor"("jakarta.annotation:jakarta.annotation-api")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.kafka:spring-kafka-test")
    }
}
