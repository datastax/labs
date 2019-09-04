/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.controller;

import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.datastax.oss.spring.demo.MainApplication;
import com.datastax.oss.spring.demo.api.ProductDto;
import com.datastax.oss.spring.demo.service.ProductService;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {MainApplication.class})
@WebMvcTest(controllers = ProductController.class)
public class ProductControllerMockMvcIT {

  @Autowired private MockMvc mockMvc;

  @MockBean private ProductService productService;

  @Test
  public void should_return_product_for_id() throws Exception {
    // given
    ProductDto productDto = new ProductDto(1, "desc");
    when(productService.getProduct(1)).thenReturn(Optional.of(productDto));

    // when, then
    this.mockMvc
        .perform(get("/product/1"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("1")))
        .andExpect(content().string(containsString("desc")))
        .andReturn();
  }
}
