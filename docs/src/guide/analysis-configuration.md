---
sidebar: auto
prev: /guide/
---

# Configuring the Correlation Recorder Plugin

The Correlation Recorder plugin in JMeter allows you to automatically correlate dynamic values in your
 HTTP requests. The plugin provides several configurations that can be customized to meet your specific needs.

## Available Configurations
The following configurations can be set via JMeter properties:

### Min Value Length
The minimum length of a value to be considered for correlation. Values with less characters will be ignored.

correlation.configuration.min_value_length=3

### Context Length
The total of characters extracted for the context of a value. This is the total of characters that will be extracted from the left and right of the value when creating the Regex for the extraction.

correlation.configuration.context_length=10

### Max Number of Appearances
The maximum number of appearances of a value to be considered for correlation. Values that appear more than this number of times will be ignored.

correlation.configuration.max_number_of_appearances=50

### Ignore Boolean Values
If set to true, boolean values will be ignored for correlation.

correlation.configuration.ignore_boolean_values=true

### Ignored Domains
Requests that have any of the following domains will be ignored for correlation.

correlation.configuration.ignored_domains=mozilla.org, mozilla.net, mozilla.com

### Ignored Headers
Headers that have any of the following names will be ignored for correlation.

correlation.configuration.ignored_headers=Referer, Origin, Host, User-Agent, If-Modified-Since, Content-Length, Accept-Encoding, Connection, Accept, Accept-Language, Cache-Control, Pragma, Upgrade-Insecure-Requests, vary

### Ignored Files
Requests that have any of the following file extensions will be ignored for correlation.

correlation.configuration.ignored_files=jpg, jpeg, png, css, js, woff, txt, svg, ico, pdf, zip, gzip, tar, gz, rar, 7z, exe, msi, woff2

### Ignored Keys
Keys (arguments, JSON keys, etc.) that have any of the following names will be ignored for correlation.

correlation.configuration.ignored_keys=log, pwd, password, pass, passwd, action, testcookie, ver, widget, d, r, s, ipv6, ipv4, remind_me_later, content-type, content-length, redirect_to, pagenow, if-modified-since, url, redirect, redirect_uri, set-cookie, cache-control, host, expires, date, location, as, rel, link, returl, dur, vary, connection

## Examples
Here are some examples of how you could use these configurations in real-world scenarios:

### Exclude certain domains 
Lets say that alongside your recorded elements, some requests from `example.com` were stored. Probably you would want
 to avoid the parameters sent there to be analysed and, potentially correlated. To do so, you need to go to your
 `user.properties` file, find the `correlation.configuration.ignored_domains` property and add the `example.com`
 there. Like this:

```
correlation.configuration.ignored_domains=example.com
```

With this, the plugin will ignore any request that has `example.com` in its domain.

### Exclude small values
Don't want the Correlation Recorder Plugin to pick up certain small values? We get it! Sometimes those tiny arguments,
 like "amount" with a value of "100", just aren't worth correlating.

If you're using Automatic Correlation analysis by Correlation Templates, this particular element probably won't be
 affected, since the rules only apply to values matched by their regular expressions, regardless of length. However,
 if you've selected "by Replay and Compare", the plugin may locate all appearances of the string "100" in your
 recording, causing unnecessary correlation.

To save yourself the hassle of correlating more than you need to, you can configure the analysis to not consider
 values below a certain length. That's where this configuration comes in handy! Simply set the
 correlation.configuration.min_value_length property to 10 in your user.properties file, and any value smaller than
 that won't be considered for "potential" dynamic values. For example:

```
correlation.configuration.min_value_length=10
```

### Exclude certain file types from correlation
Let's say that you have a web application that serves different types of files such as images, PDFs, or CSS files.
 When recording your test scenario, you notice that the Correlation Recorder plugin is also capturing some of these
 files as dynamic values. Since these files are not part of the application's business logic, you may want to exclude
 them from the correlation analysis.

To exclude certain file types, you can use the correlation.configuration.ignored_files property. This property allows
 you to specify a comma-separated list of file extensions that should be ignored by the correlation analysis.
 For example, if you want to exclude all image and CSS files, you can set the property like this:

```
correlation.configuration.ignored_files=jpg,jpeg,png,css
```
This will ensure that any requests that have a file extension of .jpg, .jpeg, .png, or .css will be ignored by the
 correlation analysis. To configure this property, you can simply add it to your user.properties file and set it
 to the desired list of file extensions.

By excluding certain file types from the correlation analysis, you can improve the accuracy of the correlation
 results and avoid unnecessary correlations of non-dynamic values.
