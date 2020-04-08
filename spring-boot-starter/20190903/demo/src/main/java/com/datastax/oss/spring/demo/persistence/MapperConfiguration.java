/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.persistence;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.spring.demo.mapper.InventoryMapper;
import com.datastax.oss.spring.demo.mapper.InventoryMapperBuilder;
import com.datastax.oss.spring.demo.mapper.ProductDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MapperConfiguration.class);

  @Bean
  public InventoryMapper inventoryMapper(@Autowired CqlSession cqlSession) {
    LOGGER.trace("create inventoryMapper");
    return new InventoryMapperBuilder(cqlSession).build();
  }

  @Bean
  public ProductDao productDao(
      @Autowired CqlSession cqlSession, @Autowired InventoryMapper inventoryMapper) {
    LOGGER.trace("create ProductDao");
    if (!cqlSession.getKeyspace().isPresent()) {
      throw new MapperInitializationException(
          String.format("keyspace on session: %s was not set", cqlSession.getName()));
    }
    return inventoryMapper.productDao(cqlSession.getKeyspace().get());
  }
}
