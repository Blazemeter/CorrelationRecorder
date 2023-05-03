---
sidebar: auto
next: /guide/installation-configuration.md
---

# User Guide

The Correlations Recorder is a [JMeter](http://jmeter.apache.org/usermanual/get-started.html) plugin that simplifies
 the process of recording test scripts for applications with dynamic variables by providing automatic **correlation of variables**.

You can perform exploratory detection of dynamic variables and create **correlation rules** to correlate them
 in future recordings, or you can use the **automatic correlations** and let the plugin do the work for you. 

Regardless of the method you choose, the plugin will help you create a test script that is more reliable and
 easier to maintain.

## Key Features

- Automated correlation detection and suggestions for faster and more accurate test script creation
- Preloaded SIEBEL templates for convenience and faster test script creation
- Auto install, download and update repositories for convenient and hassle-free team collaboration
- Easy customization options for efficient test script creation and increased productivity
- Customizable correlations to match your specific testing needs and requirements
- Shareable templates to streamline team collaboration and accelerate project development
- Customizable extensions to enhance functionality and add new capabilities
- Comprehensive examples and documentation for easy learning and fast onboarding

Additional Features:

- Automatic proposal of changes to correlate detected dynamic values for faster script development
- Automatic generation of correlation rules for use in subsequent recordings for even faster script creation
- Automatic testing of proposed changes for a faster and more reliable test script

## Usage

The Correlation Recorder plugin for JMeter can perform correlations in two scenarios:
 using [Automatic correlations](#automatic-correlations) after the recording is complete,
 or applying [Correlation Rules](#correlation-rules) before or after the recording.

We recommend using the Automatic Correlations method, since it is the most efficient and reliable way to correlate
 dynamic values. However, if you need to correlate dynamic values that are not detected by the plugin, you can use
 Correlation Rules.

With that in mind, let's take a look at the two methods.

### Automatic Correlations

Using automatic correlations, the plugin will request permission to analyze the recording and attempt to automatically
 detect potential correlations.

Currently, there are two methods for automatic correlations:

1. **Correlation Templates** (recommended): This method applies pre-defined correlation templates that have been 
 specifically designed for different applications.
2. **Replay and Compare**: This method involves replaying the recording and comparing responses to identify potential
 dynamic values that need to be correlated.

::: tip Note
You no longer need to configure Correlation Rules and repeat the recording over and over. With the Correlation Template method,
you just save them and test directly in your recordings.
:::

### Correlation Rules
Using Correlation Rules involves configuring a set of rules in the plugin that will be used to evaluate all requests and responses
    during the recording, making the correlations automatically for you. 

After they are applied, simply replay the recording and see the results.
