import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.github.davidmc24.gradle.plugin.avro.AvroExtension
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    id("org.springframework.boot") version "3.3.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
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

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
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
