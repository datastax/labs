/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.persistence;

public class MapperInitializationException extends RuntimeException {
  public MapperInitializationException(String msg) {
    super(msg);
  }
}
