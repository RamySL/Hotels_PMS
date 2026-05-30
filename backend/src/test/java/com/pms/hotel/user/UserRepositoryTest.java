package com.pms.hotel.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.pms.hotel.TestcontainersConfiguration;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void findsUserByEmail() {
		User saved = userRepository.saveAndFlush(receptionist("jean.dupont@pms.local"));

		assertThat(userRepository.findByEmail("jean.dupont@pms.local")).get().satisfies(user -> {
			assertThat(user.getId()).isEqualTo(saved.getId());
			assertThat(user.getRole()).isEqualTo(Role.RECEPTIONIST);
			assertThat(user.isActive()).isTrue();
		});
	}

	@Test
	void storesEmailInLowerCase() {
		userRepository.saveAndFlush(receptionist("  Marie.Curie@PMS.local "));

		assertThat(userRepository.findByEmail("marie.curie@pms.local")).isPresent();
	}

	@Test
	void rejectsDuplicateEmailWhateverTheCase() {
		userRepository.saveAndFlush(receptionist("doublon@pms.local"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> userRepository.saveAndFlush(receptionist("DOUBLON@pms.local")))
			.withMessageContaining("uk_users_email");
	}

	@Test
	void databaseRejectsUpperCaseEmailInsertedDirectly() {
		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.as("La contrainte CHECK protège aussi les insertions SQL (seeds, scripts)")
			.isThrownBy(() -> jdbcTemplate.update("""
					INSERT INTO users (email, password_hash, first_name, last_name, role)
					VALUES ('Paul@pms.local', 'hash', 'Paul', 'Martin', 'RECEPTIONIST')
					"""))
			.withMessageContaining("ck_users_email_lowercase");
	}

	private static User receptionist(String email) {
		return new User(email, "hash", "Jean", "Dupont", Role.RECEPTIONIST);
	}

}
