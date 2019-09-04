/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.mapper;

import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Delete;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import java.util.Optional;

@Dao
public interface ProductDao {
  @Select
  Optional<Product> findById(Integer productId);

  @Insert
  boolean save(Product product);

  @Delete(entityClass = Product.class)
  boolean delete(Integer productId);
}
