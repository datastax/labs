/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.datastax.oss.spring.configuration.ccm.CcmSpringRuleTestBase;
import com.datastax.oss.spring.demo.api.ProductDto;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductControllerIT extends CcmSpringRuleTestBase {
  // Inject which port was assigned
  @Value("${local.server.port}")
  private String port;

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductControllerIT.class);

  @Test
  public void should_create_and_retrieve_product() {
    LOGGER.trace("Started at port: {}", port);
    // given
    RestTemplate restTemplate = new RestTemplate();
    int id = 1;
    ProductDto p1 = new ProductDto(id, "desc_1");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<ProductDto> entity = new HttpEntity<>(p1, httpHeaders);

    // when
    ResponseEntity<Boolean> response = createProduct(restTemplate, entity);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // when
    ResponseEntity<ProductDto> getResponse = getForProduct(restTemplate, id);

    // then
    assertThat(getResponse.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(getResponse.getBody()).getDescription()).isEqualTo("desc_1");
    assertThat(getResponse.getBody().getId()).isEqualTo(id);
  }

  @Test
  public void should_create_and_delete_product() {
    LOGGER.trace("Started at port: {}", port);
    // given
    RestTemplate restTemplate = new RestTemplate();
    int id = 2;
    ProductDto p1 = new ProductDto(id, "desc_1");
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<ProductDto> entity = new HttpEntity<>(p1, httpHeaders);

    // when CREATE
    ResponseEntity<Boolean> response = createProduct(restTemplate, entity);

    // then
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    // when GET
    ResponseEntity<ProductDto> getResponse = getForProduct(restTemplate, id);

    // then
    assertThat(getResponse.getStatusCode().value()).isEqualTo(200);
    assertThat(Objects.requireNonNull(getResponse.getBody()).getDescription()).isEqualTo("desc_1");
    assertThat(getResponse.getBody().getId()).isEqualTo(id);

    // AND when DELETE
    ResponseEntity<Boolean> deleteResponse = deleteProduct(restTemplate, id);
    // then
    assertThat(deleteResponse.getStatusCode().value()).isEqualTo(200);
    assertThat(deleteResponse.getBody()).isTrue();

    // AND when GET then throw
    assertThatThrownBy(() -> getForProduct(restTemplate, id))
        .isInstanceOf(HttpClientErrorException.NotFound.class);
  }

  private String createTestUrl(String suffix) {
    return "http://localhost:" + port + suffix;
  }

  @NonNull
  private ResponseEntity<Boolean> createProduct(
      RestTemplate restTemplate, HttpEntity<ProductDto> entity) {
    return restTemplate.postForEntity(createTestUrl("/product/"), entity, Boolean.class);
  }

  @NonNull
  private ResponseEntity<Boolean> deleteProduct(RestTemplate restTemplate, int id) {
    return restTemplate.exchange(
        createTestUrl("/product/" + id),
        HttpMethod.DELETE,
        null,
        new ParameterizedTypeReference<Boolean>() {});
  }

  @NonNull
  private ResponseEntity<ProductDto> getForProduct(RestTemplate restTemplate, int id) {
    return restTemplate.exchange(
        createTestUrl("/product/" + id),
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<ProductDto>() {});
  }
}
