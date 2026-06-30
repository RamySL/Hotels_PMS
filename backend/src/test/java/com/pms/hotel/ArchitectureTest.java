package com.pms.hotel;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** Règles d'architecture vérifiées à chaque build, sur le code de production. */
class ArchitectureTest {

	private static final JavaClasses CLASSES = new ClassFileImporter()
		.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
		.importPackages("com.pms.hotel");

	/**
	 * Un package par feature (ADR-001), sans dépendance cyclique entre eux : quand un package a
	 * besoin d'un autre qui dépend déjà de lui, il définit une interface que l'autre implémente
	 * (ADR-007).
	 */
	@Test
	void featurePackagesHaveNoCyclicDependencies() {
		slices().matching("com.pms.hotel.(*)..").should().beFreeOfCycles().check(CLASSES);
	}

}
