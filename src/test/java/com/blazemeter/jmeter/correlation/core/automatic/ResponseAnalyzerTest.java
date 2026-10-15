package com.blazemeter.jmeter.correlation.core.automatic;

import static org.junit.Assert.assertEquals;

import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.LocationType;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.Test;

public class ResponseAnalyzerTest {

  private static final List<String> VALUES = Arrays.asList(
      // only in the body, as is
      "body-0123456789",
      // only in the body, URL encoded there
      "a b/c d",
      // only in a header, URL decoded there
      "x%2Fy%3Dz",
      // in a header and in the body: the header wins
      "both-0123456789",
      // only in a Set-Cookie header
      "cookie-0123456789",
      // nowhere
      "absent-0123456789",
      // a literal % that is not a valid escape
      "50 % p");

  @Test
  public void shouldLocateTheSameWithAndWithoutCandidateValues() {
    SampleResult response = new SampleResult();
    response.setResponseHeaders("HTTP/1.1 200 OK\n"
        + "X-Path: x/y=z\n"
        + "X-Both: both-0123456789\n"
        + "Set-Cookie: SID=cookie-0123456789; Path=/\n");
    response.setResponseData("{\"id\":\"body-0123456789\",\"q\":\"a+b%2Fc+d\","
        + "\"also\":\"both-0123456789\",\"note\":\"50 % p\"}", "UTF-8");
    response.setDataType(SampleResult.TEXT);

    ResponseAnalyzer direct = new ResponseAnalyzer();
    ResponseAnalyzer indexed = new ResponseAnalyzer();
    indexed.setCandidateValues(VALUES);
    for (String value : VALUES) {
      assertEquals(value, direct.identifyArgumentLocation(response, value),
          indexed.identifyArgumentLocation(response, value));
    }
  }

  @Test
  public void shouldSearchDirectlyAValueThatWasNotACandidate() {
    SampleResult response = new SampleResult();
    response.setResponseHeaders("HTTP/1.1 200 OK\n");
    response.setResponseData("{\"token\":\"late-0123456789\"}", "UTF-8");
    response.setDataType(SampleResult.TEXT);

    ResponseAnalyzer indexed = new ResponseAnalyzer();
    indexed.setCandidateValues(VALUES);
    assertEquals(Pair.of(LocationType.BODY, "late-0123456789"),
        indexed.identifyArgumentLocation(response, "late-0123456789"));
  }

  @Test
  public void shouldKeepTheExpectedLocationsForTheSample() {
    SampleResult response = new SampleResult();
    response.setResponseHeaders("HTTP/1.1 200 OK\nX-Both: both-0123456789\n");
    response.setResponseData("{\"q\":\"a+b%2Fc+d\",\"also\":\"both-0123456789\"}", "UTF-8");
    response.setDataType(SampleResult.TEXT);

    ResponseAnalyzer indexed = new ResponseAnalyzer();
    indexed.setCandidateValues(VALUES);
    assertEquals(Pair.of(LocationType.HEADER, "both-0123456789"),
        indexed.identifyArgumentLocation(response, "both-0123456789"));
    assertEquals(Pair.of(LocationType.BODY, "a+b%2Fc+d"),
        indexed.identifyArgumentLocation(response, "a b/c d"));
  }
}
