---
sidebar: auto
next: /guide/correlation-process.md
prev: /guide/
---

# Installation and Configuration Guide

This guide will walk you through the process of installing the Correlation Recorder plugin for JMeter.
 It's quick and easy, but you will need to make sure you have a few prerequisites before you start.

## Prerequisites

1. **JMeter**: If you haven't done so already, download and install JMeter. You can find the latest version of
 JMeter [here](https://jmeter.apache.org/download_jmeter.cgi).
2. **JMeter Plugins Manager**: Ensure that you have installed the JMeter Plugins Manager before installing
 the Correlation Recorder plugin. Learn how to install it in [this article.](https://jmeter-plugins.org/install/Install/)

## Installation

Follow these steps for an automatic installation of the latest version:

1. Launch **JMeter** and open the **JMeter Plugins Manager**.
2. In the Available Plugins tab, search and select "**BlazeMeter - Correlation Recorder Plugin**".
3. Click the "**Apply Changes and Restart JMeter**" button and wait for the installation process to complete.

Once JMeter restarts, the Correlation Recorder plugin will be installed. You can verify this by opening the
 Plugins Manager and checking the Installed Plugins tab.

## Configuration

Before we jump right into recording, let's take a look at the basic configuration options available for the
 Correlation Recorder plugin.

### Local configurations

1. Disable redirect disabling: Set the `proxy.redirect.disabling` property to false in your `user.properties` file.
 This is required for a proper and automatic correlation experience.
2. Set deflate mode: If you plan to record in *Siebel CRM environments*, set the `httpclient4.deflate_relax_mode`
 property to true in your `user.properties` file. This will help you avoid `Unexpected end of input stream` errors.

### Proxy configurations

_If you have already configured the local proxy, you can skip this section._

We need to configure the _local proxy_, otherwise, **you will not be able to record
 any requests**. To do so, take a look at the "Configure your browser to use the JMeter Proxy" section in the
 [JMeter documentation](https://jmeter.apache.org/usermanual/jmeter_proxy_step_by_step.pdf). 

::: warning
If the server you are recording is running in your local machine, you will need to configure your browser to
 allow recording of local requests.

In such case, you will need to search for "How to configure the JMeter proxy to record local requests" and follow
 the instructions for your browser.

In **Firefox**, for instance, go to `about:config` and set `network.proxy.allow_hijacking_localhost` to true.
:::

After this, you should be able to start recording.



