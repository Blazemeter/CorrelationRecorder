---
sidebar: auto
next: /guide/using-the-plugin.md
prev: /guide/
---

# Installing the Plugin
In this section, you will find instructions on how to install the plugin on your system.

## Installation instructions for different operating systems
In this section we will cover the prerequisites, installation and configuration steps for each operating system.
Remember that, if you already have the plugin installed, you can skip the installation step and go straight to
the configuration section.

The installation is usually the same regardless of your operating system. However, the configuration steps may vary
since it depends on how your proxy is configured.

### Prerequisites

Before attempting to install the plugin, make sure you have the following prerequisites:

1. **JMeter**: If you haven't done so already, download and install JMeter. You can find the latest version of
   JMeter [here](https://jmeter.apache.org/download_jmeter.cgi).
2. **JMeter Plugins Manager**: Ensure that you have installed the JMeter Plugins Manager before installing
   the Correlation Recorder plugin. Learn how to install it in [this article.](https://jmeter-plugins.org/install/Install/)

These two downloads are all you need to get started.

### Installing the plugin
The installation of the plugin is usually done using the Plugin Manager, which is the recommended way of installing, however,
you can also do it manually. This section will cover both methods.

#### Installing using the Plugin Manager
1. Launch **JMeter** and open the **JMeter Plugins Manager**.
2. In the Available Plugins tab, search and select "**BlazeMeter - Correlation Recorder Plugin**".
3. Click the "**Apply Changes and Restart JMeter**" button and wait for the installation process to complete.

Once JMeter restarts, the Correlation Recorder plugin will be installed. 

## Verifying the installation

You can verify the plugin being installed by opening the Plugins Manager and checking the Installed Plugins tab. Search for
the Correlation Recorder plugin and make sure it is listed there.

Another way to ensure the plugin is properly installed, is by attempting to load the Correlation Recorder template. To do so,
follow these steps:

1. Launch **JMeter** and open the **File** menu.
2. Select the **Templates** option and then click on **Load**.
3. Search for the **Correlation Recorder** template and click on **Open**.
4. If the template loads successfully, the plugin is properly installed.

## Updating or uninstalling the plugin
If you already had the plugin installed and want to update it, you can do so by following the same steps as the installation,
but instead of searching the plugin in the Available Plugins tab, search for it in the Available Plugins tab.

If you want to uninstall the plugin, you can do so by following these steps:

1. Launch **JMeter** and open the **JMeter Plugins Manager**.
2. In the Installed Plugins tab, search and select "**BlazeMeter - Correlation Recorder Plugin**".
3. Uncheck the plugin and click the "**Apply Changes and Restart JMeter**" button.
4. Wait for the uninstallation process to complete.
5. Restart JMeter.

## Configuring the plugin
Before we jump right into recording, let's take a look at the basic configuration options available for the
Correlation Recorder plugin.

### Local configurations

1. Disable redirect disabling: Set the `proxy.redirect.disabling` property to false in your `user.properties` file.
   This is required for a proper and automatic correlation experience.
2. Set deflate mode: If you plan to record in *Siebel CRM environments*, set the `httpclient4.deflate_relax_mode`
   property to true in your `user.properties` file. This will help you avoid `Unexpected end of input stream` errors.

### Proxy configurations

::: warning
_If you have already configured the local proxy, you can skip this section._
:::

You might also follow the steps in JMeter's Official documentation as can be seen in
 [this article.](https://jmeter.apache.org/usermanual/jmeter_proxy_step_by_step.pdf).

To configure JMeter to record HTTP/HTTPS traffic in **Chrome**, **Firefox**, or **Opera**, you need to set up a proxy server in
 JMeter and configure your web browser to use that proxy.

#### Windows

#### 1. **Configure JMeter Proxy**

1. Open JMeter and create a new Test Plan.
2. Right-click on the Test Plan and select "Add" > "Threads (Users)" > "Thread Group".
3. Right-click on the Thread Group and select "Add" > "Logic Controller" > "Recording Controller".
4. Right-click on the Recording Controller and select "Add" > "Sampler" > "HTTP(S) Test Script Recorder".
5. In the HTTP(S) Test Script Recorder, click on the "Start" button to start the proxy server.
6. Click on the "HTTP(S) Test Script Recorder" element in the tree view and configure the following settings:
7. Set the "Target Controller" to the Recording Controller you created in step 3.
8. Set the "Port" to an available port (e.g. 8888).
9. Set the "Grouping" to "Put each group in a new transaction controller".
10. Click on the "SSL Manager" button and create a new SSL certificate.

#### **2. Configure Web Browser**

1. Open Chrome/Firefox/Opera and go to the settings menu.
2. Search for "proxy" or "network settings".
3. Under the "Proxy" section, select "Manual proxy configuration".
4. In the "HTTP Proxy" field, enter "localhost" and the port number you set in step 6 of the JMeter configuration (e.g. 8888).
5. Click on the "OK" button to save the settings.

#### **3. Record Traffic**

1. In JMeter, click on the "Start" button in the HTTP(S) Test Script Recorder to start recording.
2. In your web browser, navigate to the website you want to record.
3. Perform the actions you want to record (e.g. filling out forms, clicking links).
4. In JMeter, click on the "Stop" button to stop recording.

#### macOS

##### 1. **Configure JMeter Proxy**

1. Open JMeter and create a new Test Plan.
2. Right-click on the Test Plan and select "Add" > "Threads (Users)" > "Thread Group".
3. Right-click on the Thread Group and select "Add" > "Logic Controller" > "Recording Controller".
4. Right-click on the Recording Controller and select "Add" > "Sampler" > "HTTP(S) Test Script Recorder".
5. In the HTTP(S) Test Script Recorder, click on the "Start" button to start the proxy server.
6. Click on the "HTTP(S) Test Script Recorder" element in the tree view and configure the following settings:
  - Set the "Target Controller" to the Recording Controller you created in step 3.
  - Set the "Port" to an available port (e.g. 8888).
  - Set the "Grouping" to "Put each group in a new transaction controller".
7. Click on the "SSL Manager" button and create a new SSL certificate.

##### **2. Configure Web Browser**

1. Open Chrome/Firefox/Opera and go to the settings menu.
2. Search for "proxy" or "network settings".
3. Under the "Proxy" section, select "Manual proxy configuration".
4. In the "HTTP Proxy" field, enter "localhost" and the port number you set in step 6 of the JMeter configuration (e.g. 8888).
5. Click on the "OK" button to save the settings.

##### **3. Record Traffic**

1. In JMeter, click on the "Start" button in the HTTP(S) Test Script Recorder to start recording.
2. In your web browser, navigate to the website you want to record.
3. Perform the actions you want to record (e.g. filling out forms, clicking links).
4. In JMeter, click on the "Stop" button to stop recording.

### Recording on Localhost configurations
We need to configure the _local proxy_, otherwise, **you will not be able to record
any requests**. To do so, take a look at the "Configure your browser to use the JMeter Proxy" section in the

::: warning
If the server you are recording is running in your local machine, you will need to configure your browser to
allow recording of local requests.

In such case, you will need to search for "How to configure the JMeter proxy to record local requests" and follow
the instructions for your browser.

In **Firefox**, for instance, go to `about:config` and set `network.proxy.allow_hijacking_localhost` to true.
:::

After this, you should be able to start recording.