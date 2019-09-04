/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.datastax.oss.spring.configuration.ccm.CcmSpringRuleTestBase;
import com.datastax.oss.spring.demo.api.ProductDto;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class ProductServiceIT extends CcmSpringRuleTestBase {

  @Autowired private ProductService productService;
  @Autowired private MainAppSettings mainAppSettings;

  @Test
  public void should_insert_and_retrieve_product() {
    // given
    ProductDto p = new ProductDto(1, "desc_1");

    // when
    productService.saveProduct(p);

    // then
    Optional<ProductDto> product = productService.getProduct(p.getId());
    assertThat(product.get().getId()).isEqualTo(p.getId());
    assertThat(product.get().getDescription()).isEqualTo("desc_1");
  }

  @Test
  public void should_insert_and_delete_product() {
    // given
    ProductDto p = new ProductDto(2, "desc_1");

    // when
    productService.saveProduct(p);

    // then
    Optional<ProductDto> product = productService.getProduct(p.getId());
    assertThat(product.get().getId()).isEqualTo(p.getId());
    assertThat(product.get().getDescription()).isEqualTo("desc_1");

    // when
    productService.deleteProduct(p.getId());

    // then
    assertThat(productService.getProduct(p.getId()).isPresent()).isFalse();
  }

  @Test
  public void should_load_settings_from_integration_profile() {
    assertThat(mainAppSettings.getA()).isEqualTo("value-integration");
  }
}
