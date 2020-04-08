/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.service;

import com.datastax.oss.spring.demo.api.ProductDto;
import com.datastax.oss.spring.demo.mapper.Product;
import com.datastax.oss.spring.demo.mapper.ProductDao;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

  private ProductDao productDao;

  @Autowired
  public ProductService(ProductDao productDao, MainAppSettings setting) {
    this.productDao = productDao;
    LOGGER.trace("Starting service with setting: {}", setting.getA());
  }

  public Optional<ProductDto> getProduct(int id) {
    return productDao.findById(id).map(this::mapToDto);
  }

  private ProductDto mapToDto(Product product) {
    return new ProductDto(product.getId(), product.getDescription());
  }

  public boolean saveProduct(ProductDto productDto) {
    return productDao.save(mapToEntity(productDto));
  }

  private Product mapToEntity(ProductDto productDto) {
    return new Product(productDto.getId(), productDto.getDescription());
  }

  public boolean deleteProduct(int id) {
    return productDao.delete(id);
  }
}
