package com.blazemeter.jmeter.correlation.core.proxy;

import java.util.Objects;

/*
  we don't use existing java or jmeter class for cookies since they are either not comparable or
  are missing properties.
  */
public class ComparableCookie {

  private final String name;
  private final String value;
  private final String domain;

  public ComparableCookie(String name, String value, String domain) {
    this.name = name;
    this.value = value;
    this.domain = domain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComparableCookie comparableCookie = (ComparableCookie) o;
    return name.equals(comparableCookie.name) &&
        value.equals(comparableCookie.value) &&
        domain.equals(comparableCookie.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value, domain);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getDomain() {
    return domain;
  }
}
