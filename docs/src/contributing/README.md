---
sidebar: auto
---

# Contributing

We are using a custom [checkstyle](http://checkstyle.sourceforge.net/index.html) configuration file which is based on google's one. It is advisable to use one of the [google style configuration files](https://github.com/google/styleguide) in IDEs to reduce the friction with checkstyle and automate styling.

## Building

### Pre-requisites

- [jdk 1.8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- [maven 3.3+](https://maven.apache.org/)

### Build

To build the plugin and run all tests just run `mvn clean verify`

### Installation

To use the plugin, install it (by copying the jar from `target` folder) in `lib/ext/` folder of the JMeter installation.

Run JMeter and check the new Siebel HTTP(S) Test Script Recorder is available.

### Class Diagram

The following diagram contains all the relationships between the classes. Please take a close look at it, and also the code, for further understanding.

![Project Diagram](/assets/umlDiagram.png)

