package com.example.library.book.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.library.book", importOptions = DoNotIncludeTests.class)
class HexagonalArchitectureTest {
    private static final String[] DOMAIN_FORBIDDEN_DEPENDENCIES = {
        "org.springframework..",
        "jakarta.persistence..",
        "javax.persistence..",
        "org.apache.kafka..",
        "com.example.library.common.event..",
        "com.example.library..application..",
        "com.example.library..adapter..",
        "com.example.library..config.."
    };

    private static final String[] APPLICATION_FORBIDDEN_DEPENDENCIES = {
        "com.example.library..adapter..",
        "com.example.library..config..",
        "org.springframework.web..",
        "org.springframework.kafka..",
        "jakarta.persistence..",
        "javax.persistence..",
        "org.springframework.data.."
    };

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers_or_frameworks = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(DOMAIN_FORBIDDEN_DEPENDENCIES)
        .because("domain code must stay pure and independent from adapters, application, common integration messages, and frameworks");

    @ArchTest
    static final ArchRule application_must_not_depend_on_adapters_config_or_technical_frameworks = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage(APPLICATION_FORBIDDEN_DEPENDENCIES)
        .because("application code should orchestrate use cases through ports, not concrete adapters or technical APIs");

    @ArchTest
    static final ArchRule inbound_adapters_must_not_depend_on_outbound_adapters = noClasses()
        .that().resideInAPackage("..adapter.in..")
        .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
        .because("inbound and outbound adapters should meet only through application ports");

    @ArchTest
    static final ArchRule web_requests_must_not_create_domain_inputs = noClasses()
        .that().haveSimpleNameEndingWith("Request")
        .and().resideInAnyPackage("..adapter.in.web..")
        .should().dependOnClassesThat().resideInAnyPackage("..domain..")
        .because("web requests should convert to primitive/simple application commands, leaving domain VO creation to application services");

    @ArchTest
    static final ArchRule application_commands_must_not_use_domain_types = noClasses()
        .that().haveSimpleNameEndingWith("Command")
        .and().resideInAPackage("..application.dto..")
        .should().dependOnClassesThat().resideInAnyPackage("..domain..")
        .because("adapter-facing commands should carry primitive/simple use-case input rather than domain value objects");

    @ArchTest
    static final ArchRule services_must_not_use_direct_service_http_clients = noClasses()
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework.web.client..",
            "org.springframework.web.reactive.function.client..",
            "feign.."
        )
        .because("service-to-service communication in this project must stay Kafka based");
}
