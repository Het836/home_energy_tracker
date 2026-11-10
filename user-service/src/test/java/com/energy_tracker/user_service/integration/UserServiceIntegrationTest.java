package com.energy_tracker.user_service.integration;
import com.energy_tracker.user_service.dto.UserDto;
import com.energy_tracker.user_service.entity.User;
import com.energy_tracker.user_service.repository.UserRepository;
import com.energy_tracker.user_service.testsupport.MySqlTestcontainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public class UserServiceIntegrationTest extends MySqlTestcontainersBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_viaRestApi_persistsAndReturnsUser() {
        UserDto request = UserDto.builder()
                .name("Het")
                .surname("Patel")
                .email("hetpatel@gmail.com")
                .address("123 Coding St")
                .alerting(true)
                .energyAlertingThreshold(2000.0)
                .build();

        ResponseEntity<UserDto> response =
                restTemplate.postForEntity("/api/v1/user", request, UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Het");
        assertThat(response.getBody().getSurname()).isEqualTo("Patel");
        assertThat(response.getBody().getAddress()).isEqualTo("123 Coding St");
        assertThat(response.getBody().getEmail()).isEqualTo("hetpatel@gmail.com");
        assertThat(response.getBody().isAlerting()).isTrue();
        assertThat(response.getBody().getEnergyAlertingThreshold()).isEqualTo(2000.0);

        ResponseEntity<UserDto> loaded =
                restTemplate.getForEntity("/api/v1/user/"
                        + response.getBody().getId(), UserDto.class);

        assertThat(loaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loaded.getBody()).isNotNull();
        assertThat(loaded.getBody().getEmail()).isEqualTo("hetpatel@gmail.com");
    }
}