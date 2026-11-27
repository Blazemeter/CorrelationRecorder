package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Credentials of the Authorization and Proxy-Authorization header fields, as RFC 9110 defines
 * them (section 11.4): {@code auth-scheme [ 1*SP ( token68 / #auth-param ) ]}, with a
 * case-insensitive scheme (section 11.1). Only token68 credentials, such as Bearer's (RFC 6750,
 * section 2.1), are one value that can be correlated; a list of auth-params (Digest) is not.
 */
public final class HttpAuthorization {

  /**
   * An auth-scheme: an RFC 9110 token (section 5.6.2).
   */
  public static final String AUTH_SCHEME = "[!#$%&'*+.^_`|~0-9A-Za-z-]+";
  /**
   * RFC 9110 token68 (section 11.2), which is also RFC 6750's b64token.
   */
  public static final String TOKEN68 = "[A-Za-z0-9._~+/-]+=*";

  private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
  private static final String AUTHORIZATION = "Authorization";
  private static final Pattern CREDENTIALS =
      Pattern.compile("(" + AUTH_SCHEME + ") +(" + TOKEN68 + ")");

  private HttpAuthorization() {
  }

  /**
   * Whether a header field carries credentials (field names are case-insensitive, RFC 9110
   * section 5.1).
   */
  public static boolean isCredentialsField(String name) {
    return StringUtils.equalsIgnoreCase(AUTHORIZATION, name)
        || StringUtils.equalsIgnoreCase(PROXY_AUTHORIZATION, name);
  }

  /**
   * The token68 credentials of a field value, or null when it has none: a scheme alone, a list of
   * auth-params, or a value that is not credentials.
   */
  public static String token68(String value) {
    if (value == null) {
      return null;
    }
    Matcher matcher = CREDENTIALS.matcher(value.trim());
    return matcher.matches() ? matcher.group(2) : null;
  }
}
