package com.blazemeter.jmeter.correlation.core.automatic.extraction.method;

import com.blazemeter.jmeter.correlation.core.automatic.extraction.StructureType;
import com.blazemeter.jmeter.correlation.core.automatic.extraction.location.LocationType;

public class ExtractorFactory {
  public Extractor getExtractor(LocationType locationType, StructureType structureType) {
    if (locationType == LocationType.BODY) {
      if (structureType == StructureType.JSON) {
        return new JsonBodyExtractor();
      } else if (structureType == StructureType.XML) {
        return new XmlBodyExtractor();
      } else {
        return new RawTextBodyExtractor();
      }
    } else if (locationType == LocationType.HEADER) {
      return new HeaderExtractor();
    } else if (locationType == LocationType.COOKIE) {
      return new CookieExtractor();
    }

    return null;
  }
}

