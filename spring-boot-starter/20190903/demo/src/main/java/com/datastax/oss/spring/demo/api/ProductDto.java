/*
 * The use of the software described here is subject to the DataStax Labs Terms
 * [https://www.datastax.com/terms/datastax-labs-terms]
 */
package com.datastax.oss.spring.demo.api;

public class ProductDto {
  private Integer id;
  private String description;

  public ProductDto() {}

  public ProductDto(Integer id, String description) {
    this.id = id;
    this.description = description;
  }

  public Integer getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "ProductDto{" + "id=" + id + ", description='" + description + '\'' + '}';
  }
}
