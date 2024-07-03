---
next: /guide/using-the-plugin.md
prev: /guide/
---

# Installing the Plugin

In this section, you will find instructions on how to install the plugin on your system.

The installation is usually the same regardless of your operating system. However, the configuration steps may vary
since it depends on how your proxy is configured.

## Prerequisites

### Plugin

Before attempting to install the plugin, make sure you have the following prerequisites:

1. **JMeter**: If you haven't done so already, download and install JMeter. You can find the latest version of
   JMeter [here](https://jmeter.apache.org/download_jmeter.cgi).
2. **JMeter Plugins Manager**: Ensure that you have installed the JMeter Plugins Manager before installing
   the Auto Correlation Recorder plugin. Learn how to install it in [this article.](https://jmeter-plugins.org/install/Install/)

These two downloads are all you need to get started.

### Integration with BlazeMeter

If you want to use the plugin with BlazeMeter, you will also need to have the following:

1. A BlazeMeter account. If you don't have one, you can [sign up for free](https://accounts.blazemeter.com/).
1. A BlazeMeter api-key. If you don't have one, you can learn how to generate it from this article [BlazeMeter Api Key](https://guide.blazemeter.com/hc/en-us/articles/13329040973073-BlazeMeter-API-keys-).

## Installation

The installation of the plugin is usually done using the Plugin Manager, which is the recommended way of installing, however,
you can also do it manually. This section will cover both methods.

### With Plugin Manager

1. Launch **JMeter** and open the **JMeter Plugins Manager**.
1. In the Available Plugins tab, search and select "**BlazeMeter - Auto Correlation Recorder Plugin**".
1. Click the "**Apply Changes and Restart JMeter**" button and wait for the installation process to complete.

Once JMeter restarts, the Auto Correlation Recorder plugin will be installed.

### Manually

1. Go to the [Auto Correlation Recorder plugin page](https://jmeter-plugins.org/?search=BlazeMeter%20-%20Correlation%20Recorder%20Plugin) and download the
   latest version of the plugin, with the dependencies.
1. Place the Plugin jar in the ext folder of your JMeter installation. The ext folder is usually located in
   `<JMeter_Home>/lib/ext`.
1. Place the dependencies jars in the lib folder of your JMeter installation. The lib folder is usually located in
   `<JMeter_Home>/lib`.
1. Restart JMeter.

## Verifying

You can verify the plugin being installed by opening the Plugins Manager and checking the Installed Plugins tab. Search for
the Auto Correlation Recorder plugin and make sure it is listed there.

Another way to ensure the plugin is properly installed, is by attempting to load the Auto Correlation Recorder template. To do so,
follow these steps:

1. Launch **JMeter** and open the **File** menu.
1. Select the **Templates** option and then click on **Load**.
1. Search for the **Auto Correlation Recorder** template and click on **Open**.
1. If the template loads successfully, the plugin is properly installed.

## Updating

If you already had the plugin installed and want to update it, you can do so by following the same steps as the installation,
but instead of searching the plugin in the Available Plugins tab, search for it in the Installed Plugins tab.

In case there is a new version of the plugin, the name of the extension will be bold and list of updated artifacts will be listed. Press _Install and Restart JMeter_

## Uninstall

If you want to uninstall the plugin, you can do so by following these steps:

1. Launch **JMeter** and open the **JMeter Plugins Manager**.
1. In the Installed Plugins tab, search and select "**BlazeMeter - Auto Correlation Recorder Plugin**".
1. Uncheck the plugin and click the "**Apply Changes and Restart JMeter**" button.
1. Wait for the uninstallation process to complete.
1. Restart JMeter.

## Configuration

Before we jump right into recording, let's take a look at the basic configuration options available for the Auto Correlation Recorder plugin.

### Properties

Here is a list of properties that you need to configure in order to use the Auto Correlation Recorder plugin:

1. Disable redirect disabling: Set the `proxy.redirect.disabling` property to false in your `user.properties` file.
   This is required for a proper and automatic correlation experience.
2. Set deflate mode: If you plan to record in _Siebel CRM environments_, set the `httpclient4.deflate_relax_mode`
   property to true in your `user.properties` file. This will help you avoid `Unexpected end of input stream` errors.
3. (Optional) Set the scope of post-processors to all: Set the `Sample.scope` property to `all` in your
   `user.properties` file. This will help you to avoid the post-processors to only limit to the main sampler.

### BlazeMeter Api Key

If you are planning on using the integration with Blazemeter, you need to provide your BlazeMeter api-key.
You can do that by doing one of the following options:

**1. Drop the file in your bin directory**

It is as simple as it sounds. You just need to drop the `api-key.json` file in your `<JMeter_Home>/bin` directory,
making sure you don't have any other `api-key.json` file in that directory.

Restart JMeter

**2. Configure the properties file**

If you don't want to drop the api-key.json file in your bin directory, you can provide the path to your api-key.json file in the configuration.
To do that, you need to open the `blazemeter.properties` file in your `<JMeter_Home>/bin` directory and add the following line:

```
blazemeter.api.key.file=<path_to_your_api_key.json>
```

Restart JMeter

Note: make sure the path you provide is correct, it points to a file, rather than a folder, and the file exists.
It should look something like this:

```
blazemeter.api.key.file=/Users/username/.blazemeter/api-key.json
```

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
1. Right-click on the Test Plan and select "Add" > "Threads (Users)" > "Thread Group".
1. Right-click on the Thread Group and select "Add" > "Logic Controller" > "Recording Controller".
1. Right-click on the Recording Controller and select "Add" > "Sampler" > "HTTP(S) Test Script Recorder".
1. In the HTTP(S) Test Script Recorder, click on the "Start" button to start the proxy server.
1. Click on the "HTTP(S) Test Script Recorder" element in the tree view and configure the following settings:
1. Set the "Target Controller" to the Recording Controller you created in step 3.
1. Set the "Port" to an available port (e.g. 8888).
1. Set the "Grouping" to "Put each group in a new transaction controller".
1. Click on the "SSL Manager" button and create a new SSL certificate.

#### **2. Configure Web Browser**

1. Open Chrome/Firefox/Opera and go to the settings menu.
1. Search for "proxy" or "network settings".
1. Under the "Proxy" section, select "Manual proxy configuration".
1. In the "HTTP Proxy" field, enter "localhost" and the port number you set in step 6 of the JMeter configuration (e.g. 8888).
1. Click on the "OK" button to save the settings.

#### **3. Record Traffic**

1. In JMeter, click on the "Start" button in the HTTP(S) Test Script Recorder to start recording.
1. In your web browser, navigate to the website you want to record.
1. Perform the actions you want to record (e.g. filling out forms, clicking links).
1. In JMeter, click on the "Stop" button to stop recording.

#### macOS

##### 1. **Configure JMeter Proxy**

1. Open JMeter and create a new Test Plan.
1. Right-click on the Test Plan and select "Add" > "Threads (Users)" > "Thread Group".
1. Right-click on the Thread Group and select "Add" > "Logic Controller" > "Recording Controller".
1. Right-click on the Recording Controller and select "Add" > "Sampler" > "HTTP(S) Test Script Recorder".
1. In the HTTP(S) Test Script Recorder, click on the "Start" button to start the proxy server.
1. Click on the "HTTP(S) Test Script Recorder" element in the tree view and configure the following settings:

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

#### Recording on Localhost configurations

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
