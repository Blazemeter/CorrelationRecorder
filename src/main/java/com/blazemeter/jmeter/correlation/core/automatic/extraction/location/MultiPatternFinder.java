package com.blazemeter.jmeter.correlation.core.automatic.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds which of many strings occur in a text, scanning the text once per distinct string length
 * instead of once per string.
 *
 * <p>The strings of one length are matched with a rolling hash over the text, and every hash match
 * is confirmed character by character. The result is exactly the set of strings for which
 * {@code text.contains(string)} is true. When a length has only a few strings, calling
 * {@code contains} for each of them costs less than a scan, so those are checked that way.
 */
public final class MultiPatternFinder {

  // Below this many strings of one length, a contains per string is cheaper than a scan
  private static final int MIN_STRINGS_TO_SCAN = 8;
  private static final long BASE = 1000003L;

  private final List<String> checkedOneByOne = new ArrayList<>();
  private final List<LengthGroup> scannedGroups = new ArrayList<>();
  private final boolean includesEmpty;

  public MultiPatternFinder(Collection<String> strings) {
    Map<Integer, List<String>> byLength = new HashMap<>();
    boolean empty = false;
    for (String string : new HashSet<>(strings)) {
      if (string.isEmpty()) {
        empty = true;
        continue;
      }
      byLength.computeIfAbsent(string.length(), length -> new ArrayList<>()).add(string);
    }
    includesEmpty = empty;
    for (Map.Entry<Integer, List<String>> group : byLength.entrySet()) {
      if (group.getValue().size() < MIN_STRINGS_TO_SCAN) {
        checkedOneByOne.addAll(group.getValue());
      } else {
        scannedGroups.add(new LengthGroup(group.getKey(), group.getValue()));
      }
    }
  }

  public Set<String> findIn(String text) {
    Set<String> found = new HashSet<>();
    if (includesEmpty) {
      found.add("");
    }
    for (String string : checkedOneByOne) {
      if (text.contains(string)) {
        found.add(string);
      }
    }
    if (!scannedGroups.isEmpty()) {
      char[] chars = text.toCharArray();
      for (LengthGroup group : scannedGroups) {
        group.scan(chars, found);
      }
    }
    return found;
  }

  private static long hash(String string) {
    long hash = 0L;
    for (int i = 0; i < string.length(); i++) {
      hash = hash * BASE + string.charAt(i);
    }
    return hash;
  }

  private static final class LengthGroup {

    private final int length;
    // BASE^(length - 1), to remove the char that leaves the window
    private final long leavingFactor;
    private final Map<Long, List<String>> byHash = new HashMap<>();
    // A cheap filter in front of byHash, so most windows do not box their hash for a lookup
    private final boolean[] mayMatch;
    private final int mask;

    private LengthGroup(int length, List<String> strings) {
      this.length = length;
      long factor = 1L;
      for (int i = 1; i < length; i++) {
        factor *= BASE;
      }
      leavingFactor = factor;
      int tableSize = Integer.highestOneBit(Math.max(16, strings.size() * 16) - 1) << 1;
      mayMatch = new boolean[tableSize];
      mask = tableSize - 1;
      for (String string : strings) {
        long hash = hash(string);
        byHash.computeIfAbsent(hash, h -> new ArrayList<>(1)).add(string);
        mayMatch[slot(hash)] = true;
      }
    }

    private int slot(long hash) {
      return (int) (hash ^ (hash >>> 32)) & mask;
    }

    private void scan(char[] text, Set<String> found) {
      if (text.length < length) {
        return;
      }
      long hash = 0L;
      for (int i = 0; i < length; i++) {
        hash = hash * BASE + text[i];
      }
      int lastStart = text.length - length;
      for (int start = 0; ; start++) {
        if (mayMatch[slot(hash)]) {
          List<String> candidates = byHash.get(hash);
          if (candidates != null) {
            for (String candidate : candidates) {
              if (!found.contains(candidate) && matchesAt(text, start, candidate)) {
                found.add(candidate);
              }
            }
          }
        }
        if (start == lastStart) {
          return;
        }
        hash = (hash - text[start] * leavingFactor) * BASE + text[start + length];
      }
    }

    private static boolean matchesAt(char[] text, int start, String string) {
      for (int i = 0; i < string.length(); i++) {
        if (text[start + i] != string.charAt(i)) {
          return false;
        }
      }
      return true;
    }
  }
}
