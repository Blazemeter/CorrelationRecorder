package com.blazemeter.jmeter.correlation.core.automatic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HttpAuthorizationTest {

  @Test
  public void shouldReturnTokenWhenBearerCredentials() {
    assertThat(HttpAuthorization.token68("Bearer mF_9.B5f-4.1JqM")).isEqualTo("mF_9.B5f-4.1JqM");
  }

  @Test
  public void shouldReturnTokenWhenSchemeInAnotherCaseAndSeveralSpaces() {
    assertThat(HttpAuthorization.token68("bearer   abc~+/=="))
        .isEqualTo("abc~+/==");
  }

  @Test
  public void shouldReturnTokenWhenJsonWebToken() {
    String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    assertThat(HttpAuthorization.token68("Bearer " + jwt)).isEqualTo(jwt);
  }

  @Test
  public void shouldReturnTokenWhenOtherSchemeWithToken68() {
    assertThat(HttpAuthorization.token68("Token 9944b09199c62bcf9418ad846dd0e4bbdfc6ee4b"))
        .isEqualTo("9944b09199c62bcf9418ad846dd0e4bbdfc6ee4b");
  }

  @Test
  public void shouldReturnNullWhenNoScheme() {
    assertThat(HttpAuthorization.token68("q2EIFnQXWFEVvGMQczWsEa3IDLHeWZ19Qtzypu2Uqhk")).isNull();
  }

  @Test
  public void shouldReturnNullWhenAuthParams() {
    assertThat(HttpAuthorization.token68(
        "Digest username=\"Mufasa\", realm=\"http-auth@example.org\", nonce=\"7ypf\"")).isNull();
  }

  @Test
  public void shouldReturnNullWhenMoreThanOneValueAfterScheme() {
    assertThat(HttpAuthorization.token68("Bearer abc def")).isNull();
  }

  @Test
  public void shouldReturnNullWhenNoValue() {
    assertThat(HttpAuthorization.token68(null)).isNull();
  }

  @Test
  public void shouldRecognizeCredentialsFieldsInAnyCase() {
    assertThat(HttpAuthorization.isCredentialsField("authorization")).isTrue();
    assertThat(HttpAuthorization.isCredentialsField("PROXY-AUTHORIZATION")).isTrue();
    assertThat(HttpAuthorization.isCredentialsField("X-Authorization")).isFalse();
  }
}
