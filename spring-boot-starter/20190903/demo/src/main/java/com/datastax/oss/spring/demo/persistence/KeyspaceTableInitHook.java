/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.persistence;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.spring.configuration.AdminSessionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeyspaceTableInitHook implements AdminSessionHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeyspaceTableInitHook.class);

  @Value("${datastax-java-driver.basic.session-keyspace}")
  private String keyspace;

  @Override
  public void executeOnAdminSession(CqlSession adminSession) {
    LOGGER.trace("creating keyspace: {} for KeyspaceTableInitHook", keyspace);
    adminSession.execute(
        String.format(
            "CREATE KEYSPACE IF NOT EXISTS %s "
                + "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}",
            keyspace));

    adminSession.execute(
        String.format(
            "CREATE TABLE IF NOT EXISTS %s.product(id int PRIMARY KEY, description text)",
            keyspace));
  }
}
