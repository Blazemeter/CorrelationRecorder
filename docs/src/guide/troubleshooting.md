---
next: /guide/templates/
prev: /guide/best-practices.md
---

# Troubleshooting

In this section you will find information about:
- [Common issues and solutions](#common-issues-and-solutions)
- [Debugging tips](#debugging-tips)
- [FAQ](#faq)

Most of these issues, if not all, can be solved by: 

- Ensuring your plugin is appropriately [configured](/guide/installation-guide.html#configuration)
- Ensuring that your Test Plan aligns [consistently with the recording](#the-replay-is-not-consistent-with-the-recording).

Make sure you check aforementioned point and the following sections before opening a [new issue](https://github.com/Blazemeter/CorrelationRecorder/issues) in the repository.

## Common issues and solutions

We will assume that you have already reviewed the previously mentioned guides and that you have a basic understanding of how the plugin works.

### My correlation rules are not being applied

If your correlation rules aren't being applied, it's likely that they aren't matching. This mismatch can occur due to various reasons, but the most prevalent ones include:

- The Regex for the Extractor doesn't match the value you aim to correlate.
- The Regex for the Replacement doesn't match the request.
- The Regex for the Replacement matches the value, but the extracted value isn't what you anticipated.
- Interference from other third-party plugins affecting the behavior of your plugin.

If, after reviewing the previous reasons, you still can't find the root cause of the issue, we recommend you:

- Review if your issues was already [reported](https://github.com/Blazemeter/CorrelationRecorder/issues) or [solved](https://github.com/Blazemeter/CorrelationRecorder/issues?q=is%3Aissue+is%3Aclosed) in our GitHub's repository.
- For Local Repository Templates, initiate a [new issue](https://github.com/Blazemeter/CorrelationRecorder/issues/new/choose) in the GitHub's repository.
- For BlazeMeter Cloud Templates, please reach out to [BlazeMeter Support](https://guide.blazemeter.com/hc/en-us/requests/new).

## Debugging tips

In general, we recommend always using the JMeter Template (not to be confused with Correlation Templates) for the Plugin to do all your recordings since we have added all the necessary elements, preconfigured, to make the recording process as easy as possible. This template also has all the required elements in the correct order and in the right places so that the information is not only shown correctly but can also be used in the debugging process.

The most common debugging tips are:

- Enable the Debug Post processor element when doing the replay as it will show the variables in JMeter by the moment the replay reaches that particular step.
- Always review the state of the value in both the recording and the replay. It is pretty common that the value is not being displayed correctly in the rendered view in JMeter (due to the encoding of the value).
- Use the Raw Display of the values in the view result tree. It is pretty common that, when using other rendering of the results in JMeter, the value is shown in a different manner than its real state, making it difficult to debug.
- Keep the Test Plan as clean as possible. Always try to filter the requests that are not needed (such as fonts, styles, telemetry, google ads, etc.) as they only add noise to the debugging process.
- Alternatively, the opposite is also possible: add all the elements to the Test Plan, in cases where you might end up filtering requests that could contain important information for the correlation. This does not occur that often with the default filters added to the template, but if you find yourself simply unable to locate a value (neither in the recording nor the replay), it could be possible that it was filtered.

## How to properly report an issue
If by the end of the debugging process you are still not able to find the root cause of the issue, you can always 
request help. We encourage you ask for help in the following ways:

1. **Prepare the information following the steps in the next section**
2. Contact the BlazeMeter team for proper support and guidance.
3. Alternatively, you can create an issue in the [GitHub repository](https://github.com/Blazemeter/CorrelationRecorder/issues) of the plugin, and get assistance by the Open source community.

### How to prepare the information
In order to properly report an issue, we need to have as much information as possible. This is why we encourage you to
follow the next steps:

#### Gather system information
Make sure to include relevant information about your system and environment, such as:

Operating system (e.g. Windows, macOS, Linux)
Browser and version (if applicable)
Java version (if applicable)
Other relevant software versions


#### Describe the issue
Provide a clear and concise description of the issue you are experiencing. Include any error messages or unexpected behavior that you have observed.

#### Steps to reproduce
Provide a step-by-step guide to reproduce the issue. Include any relevant information, such as the test scenario you were running, the specific action you were performing, or the data you were using.

#### Expected behavior
Describe what you expected to happen when performing the steps outlined in the previous section.

#### Actual behavior
Describe what actually happened when performing the steps outlined in the previous section.

#### Additional information
Include any additional information that you think might be relevant or helpful in diagnosing the issue. This could include screenshots, log files, or any other relevant details.

By following these steps and providing as much detail as possible, you can help ensure that your issue is properly diagnosed and resolved.


## Guides

### Checking your JMeter configuration
One of the many reason as why the Templates or Rules do not work is because the plugin is not properly configured. This means that the plugin might be missing critical configuration in order to work properly.

Make sure you have followed the instructions in the [Installation Guide](/guide/installation-guide.html) and the [Configuration Guide](/guide/installation-guide.html#configuration) in order to properly configure the plugin.


### Checking your Test Plan for consistency
One of the many reason as why the Templates or Rules do not work is because the Test Plan is not consistent. This means that the Test Plan is not being executed in the same way as it was recorded.

This can happen for a variety of reasons, but the most common ones are:

- The Test Plan's elements is not being executed in the same order as it was recorded.
- The Test Plan is missing elements that were present in the recording.
- The Test Plan has elements that are disabled and are relevant in the recording.


### Checking the server responses
Another common reason as why the Templates or Rules do not work is because the responses from the server are totally different from the recording. This means that the server is not responding with the same values as it was recorded.

This can happen for a variety of reasons, but the most common ones are:

- The server returns a different response due to: 
  - previous requests not being properly correlated.
  - the Test Plan not being consistent.
  - Additional configuration's required to run the Test Plan.
- The server needs Javascript to be executed in order to return the correct response.





## FAQ

### The value that I need to Correlate does not appear in the Tree View
This is a pretty common issue that can affect the capabilities of you or the plugin to correlate a value. 

The most common reasons are:

1. Cookies and cached values are not being cleared before recording. 
2. Requests are being filtered on recording.

Can these be fixed? Yes, they can. Here is how:

#### Cookies and cached values are not being cleared before recording

The short recommendation for this problem is: **Always clear your cookies and cached values before recording**

**Explanation:**

If your browser has **cached values** or **cookies** from previous sessions, it is possible that the value you are trying 
to correlate **is not being sent to JMeter** from the server (because your browser already has the value stored and
use it directly). 

This is why we always **recommend** to _clear your cookies and cached values before recording_ or _make a new Incognito
session in your browser_ each time you record.

#### Requests are being filtered on recording
If the Request are being filtered on recording, it is possible that the server is sending the request to JMeter, 
but it is not being stored since it matches the filtering configurations.

By default, these filtered request won't appear in the Recording's View Result tree. However, you can disable this
behavior and retry the recording to ensure this is the problem.

::: warning
**Note:** Only use this for debugging purposes since the plugin can misbehave if there are inconsistencies between the recorded elements and the requests that are stored in the View Result Tree.

Along the possible issues this can cause, the most common ones are:

- The plugin will identify values in requests that is not part of the Test Plan and will make incorrect suggestions. This will give the impression of a value being correlated when it is not.
- The plugin will identify the appearance of a value more times than it should. This will give the impression of a value being more important than it actually is.
- The analysis (either by Templates or by Automatic detection) will take longer than usual since it will have to go through all the requests in the View Result Tree, even the ones that are not part of the Test Plan.
:::

Before jumping into conclusions, let's test the hypothesis that the request is being filtered.

**Check if the request appears in the Recording JTL:** 

1. Go to your Test Plan 
2. Click in the "bzm - Correlation Recorder" element
3. Click the "View Results Tree" element

Either manually review the list of elements or use the search field to find the request you are looking for.

If the request does not appear in the recording JTL, it might have been filtered.
If the request is present in the recording JTL, it was not filtered.

::: tip
If the request is present in the Recording JTL but the name appears between brackets ([]), it means that the request was filtered, and you have the notify children option enabled. 
:::

**Disable the filtering of requests in the Recording JTL:**
If you validated that the request is being filtered, you can disable the filtering of requests in the Recording JTL by following these steps:

1. Go to the Request filtering tab (Test Plan > bzm - Correlation Recorder > Requests Filtering
   tab).
2. Check the "Notify Child Listeners of filtered samplers" option at the bottom of the
   element in the "Notify Child Listeners of filtered samplers" section.
3. Clear the recording (Test Plan > Recording Controller > Clear all the recorded samples).
4. Clear the Recording JTL (Test Plan > bzm - Correlation Recorder > View Results Tree >
   Right Click > Clear).
5. Record again.

Now, when you check the Recording's View Result Tree, you should see all the requests that are being sent to JMeter. 
The ones that are being filtered will appear between brackets ([]), while the ones that are not being filtered will
appear normally (without the wrapping brackets).

Just like the previous step, you can either manually review the list of elements or use the search field to find the
request you are looking for.

If you confirmed that the request is being filtered, you might need to review the filtering configuration to ensure
that the request is not being filtered.

::: warning
**Note:** You will feel tempted to leave the "Notify Child Listeners of filtered samplers" option enabled. However, this is not recommended since it can cause the plugin to misbehave.
Likewise, we highly encourage you to fine tune the filtering configuration to ensure that the request is not being filtered rather than removing the filtering altogether. Having extra requests in the Recording will only make the analysis slower and suggest values that are not relevant or should not be correlated.
:::


## Your regular expression is not matching the value you are trying to correlate
More often than not, the regular expression that you are using to correlate a value is not matching the value you are
trying to correlate. 

Depending on which part of the Correlation Rule you are configuring or testing, you might need to use a different
mechanism to validate that the regular expression is matching the value you are trying to correlate.

### For Extraction
You can test your Regex inside JMeter by going into your Recording's View Result Tree do one of the following:

1. Search by Regular exp.
2. Use the "RegExp Tester" view.

### For Replacement
By default, the Plugin will concatenate both the `name of the argument` and `value` with `:`, when evaluating the replacement. 

For instance:
If the value you want to correlate appears in the HTTP Sampler as `wpnonce` (in name's column) and `123ABC` (in value's column), when the plugin tries to match the Regex, it will do it against `wpnonce: 123ABC`.

::: tip
Even if your Regex matches the value you are trying to correlate, in the request body or headers, you still need to make sure that the Extractor effectively extracted the value you are trying to correlate.
:::

## How to confirm the value is being extracted correctly
If you are not sure if the value is being extracted correctly, you can use the "Debug Post-Processor" to confirm it.

To do this, you need to:

1. Add a "Debug Post-Processor" to your Test Plan.
2. Add a "View Results Tree" element to your Test Plan.
3. Replay the Test Plan

You will see that now every request will have a child element inside them in the View Result Tree. Inside this child element, in the Response Body tab, you will see all the JMeter variables and their respective values during the replay in that request.

If your Regex Extractor is located inside the Request #1, the value will appear (or will be available) from the Request #2 onwards.

If the variable **is not present** in the Debug Sampler, it means that the **regex is not matching** any value in the recording until that point and that **you don't have a default value assigned to it**.
If the variable **is present** but the **value is "NOT_FOUND"**, it means that the regex **is not matching** any value.
If the variable **is present** but the value **is not the one that you want**, it means **you need to improve** either **the Regex** or **the configuration** of the Extractor.