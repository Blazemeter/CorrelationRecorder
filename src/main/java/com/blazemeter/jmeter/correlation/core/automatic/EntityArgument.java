package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.regex.Pattern;

/**
 * A generic argument, such as {@code id}, named after the entity of its request: the concept of
 * the request's path, as REST paths name it ({@code /order.html?id=} is {@code order.id},
 * {@code /api/orders?id=} is {@code orders.id}). Without it, every {@code id} of an application, of
 * orders, products and users alike, ends up in one suggestion.
 */
public final class EntityArgument {

  public static final String SEPARATOR = ".";
  private static final Pattern CONCEPT = Pattern.compile("[A-Za-z][A-Za-z_-]*");
  private static final Pattern EXTENSION = Pattern.compile("\\.[A-Za-z0-9]+$");

  private EntityArgument() {
  }

  /**
   * The concept of a path: its last segment that names something, without its extension, after
   * the ids and versions that follow it ({@code /api/carts/cart_62704d1930ac} is {@code carts}).
   * Null when no segment names one ({@code /}).
   */
  public static String concept(String path) {
    if (path == null) {
      return null;
    }
    String[] segments = path.split("[?#]", 2)[0].split("/");
    for (int i = segments.length - 1; i >= 0; i--) {
      String segment = EXTENSION.matcher(segments[i]).replaceFirst("");
      if (CONCEPT.matcher(segment).matches()) {
        return segment;
      }
    }
    return null;
  }

  public static String name(String concept, String argument) {
    return concept + SEPARATOR + argument;
  }

  // The concept has no separator, so the argument is all that follows the first one
  public static String argument(String name) {
    int separator = name.indexOf(SEPARATOR);
    return separator < 0 ? name : name.substring(separator + SEPARATOR.length());
  }
}
