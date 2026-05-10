package com.example.library.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.library.common", importOptions = DoNotIncludeTests.class)
class CommonEventsArchitectureTest {
    @ArchTest
    static final ArchRule common_events_must_not_define_shared_domain_vo_package = noClasses()
        .should().resideInAPackage("..common.vo..")
        .because("common-events should contain shared Kafka contracts, not service domain value objects");

    @ArchTest
    static final ArchRule shared_messages_must_not_define_service_specific_conversions = noMethods()
        .that().areDeclaredInClassesThat().resideInAPackage("..common.event..")
        .should().haveNameMatching("(to(Rental|Member|Book|BestBook|Domain|Command).*)|(fromRequest)")
        .because("shared message records must be converted at service adapter boundaries");
}
