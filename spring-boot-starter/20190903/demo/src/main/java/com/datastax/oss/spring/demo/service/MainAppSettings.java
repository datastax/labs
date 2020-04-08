/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "your-setting")
public class MainAppSettings {
  private String a;

  public String getA() {
    return a;
  }

  public void setA(String a) {
    this.a = a;
  }
}
