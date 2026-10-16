package com.blazemeter.jmeter.correlation.core.automatic.replacement.method;

import com.blazemeter.jmeter.correlation.core.automatic.EntityArgument;
import com.blazemeter.jmeter.correlation.core.automatic.HttpAuthorization;
import com.blazemeter.jmeter.correlation.core.automatic.Sources;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides utility methods for matching and replacing regex patterns.
 */
public class ReplacementRegex {

  private static final Logger LOG = LoggerFactory.getLogger(ReplacementRegex.class);


  /**
   * This method generates a regex pattern based on the provided source.
   *
   * @param name   The name to be used in the regex pattern.
   * @param source The source from which the regex pattern is generated.
   * @return The generated regex pattern.
   */
  public static String match(String name, String source) {
    String cleanName = name;
    // Testing if this fixes the issue of BLZ-1629 without breaking other things
    if (name.contains("[") || name.contains("]")) {
      cleanName = name.replaceAll("\\[", "\\\\\\[")
          .replaceAll("\\]", "\\\\]");
    }

    String regex = "";
    switch (source) {
      case Sources.REQUEST:
      case Sources.REQUEST_HEADER_FIELDS:
        // Credentials keep their scheme ("Bearer ${token}"): only the token68 is the value, and
        // field names and schemes are case-insensitive (RFC 9110, sections 5.1, 11.1 and 11.4)
        regex = HttpAuthorization.isCredentialsField(name)
            ? "(?i)" + cleanName + ": " + HttpAuthorization.AUTH_SCHEME + " +("
            + HttpAuthorization.TOKEN68 + ")"
            : cleanName + ": ([^&]+)";
        break;
      case Sources.REQUEST_URL:
        regex = "(?:\\?|&)" + cleanName + "=(.+?)(?:&|$)";
        break;
      case Sources.REQUEST_ARGUMENTS:
        regex = cleanName + "=([^&]+)";
        break;
      case Sources.REQUEST_ENTITY_ARGUMENT:
        // order.id names the variable; the request sends id
        regex = EntityArgument.argument(cleanName) + "=([^&]+)";
        break;
      case Sources.REQUEST_PATH:
        regex = "\\/" + cleanName + "\\/([^\\/]+)(?:\\?[^&]*)?$";
        break;
      case Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_QUESTION_MARK:
        regex = "\\/" + cleanName + "\\/([0-9]+)(\\?)";
        break;
      case Sources.REQUEST_PATH_NUMBER_FOLLOWED_BY_SLASH:
        regex = "\\/" + cleanName + "\\/([0-9]+)(\\/|$)";
        break;
      case Sources.REQUEST_PATH_ALPHANUMERIC_ID:
        // the id after its concept, anywhere in the path: /carts/cart_62704d1930ac/items
        regex = "\\/" + cleanName + "\\/([A-Za-z0-9_-]+)(?=[\\/?#]|$)";
        break;
      default:
        break;
    }

    return regex;
  }

  /**
   * This method applies the generated regex pattern to the provided input.
   *
   * @param name   The name to be used in the regex pattern.
   * @param source The source from which the regex pattern is generated.
   * @param input  The input to which the regex pattern is applied.
   * @return The matched group from the input, or null if no match is found.
   */
  public static String match(String name, String source, String input) {
    String regex = match(name, source);
    return applyRegex(input, regex);
  }

  /**
   * This method applies the provided regex pattern to the provided input.
   *
   * @param input The input to which the regex pattern is applied.
   * @param regex The regex pattern to be applied.
   * @return The matched group from the input, or null if no match is found.
   */
  private static String applyRegex(String input, String regex) {
    if (input == null) {
      return null;
    }

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(input);

    if (matcher.find()) {
      return matcher.group(1); // Return the entire matched portion
    } else {
      return null;
    }
  }

  public static String matchAsJMeterRegex(String name, String source, String input) {
    String regex = match(name, source);

    Perl5Matcher matcher = JMeterUtils.getMatcher();
    org.apache.oro.text.regex.Pattern pattern;
    try {
      pattern = new Perl5Compiler().compile(regex);
      if (matcher.matches(input, pattern)) {
        return matcher.getMatch().group(1);
      } else {
        LOG.warn("No match found for regex: {} and input: {}", regex, input);
        System.out.println("No match found for regex: " + regex + " and input: " + input);
        return null;
      }
    } catch (MalformedPatternException e) {
      LOG.error("Error compiling regex: {}", regex, e);
      System.out.println("Error compiling regex: " + regex);
      return null;
    }
  }
}

