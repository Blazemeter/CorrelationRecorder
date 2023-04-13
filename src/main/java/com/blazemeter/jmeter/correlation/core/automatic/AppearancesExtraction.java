package com.blazemeter.jmeter.correlation.core.automatic;

import java.util.List;
import java.util.Map;

public interface AppearancesExtraction {

  Map<String, List<Appearances>> extractAppearanceMap(String filepath);

}
