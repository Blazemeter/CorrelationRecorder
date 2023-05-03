---
sidebar: auto
next: /guide/
prev: /guide/best-practices.md
---

# Troubleshooting
In this section you will find information about common errors and behaviors that you may encounter while using the plugin.

## Common issues and solutions

### My correlation rules are not being applied
There is a great chance that the rules you created are not being applied because they are not being matched. This usually happens for a variety of reasons, but the most common ones are:

- Your regular expression is not matching the value you are trying to correlate. 
- Your regular expression is matching the value you are trying to correlate when extracting, but the correlation rule is not applying in the request where it should be. 
- Your current Test Plan does not reach the request where the correlation rule should be applied due to the lack of correlation. 
- Your current Test Plan goes through the whole recorded process but the element you are trying to correlate is not being executed (it appears disabled in the Tree View).

- As a general rule, you should always check the following:

- The state of the elements in your Test Plan (enabled/disabled). 
- The state of your correlation rules (enabled/disabled). 
- The configuration of your JMeter (have you configured the flags correctly in the user.properties?)
- The variables extracted by the plugin at any moment of the replay. 
- The regular expressions you are using to extract the values. 
- The regular expressions you are using to match the requests where the correlation rules should be applied. 
- The response of the elements where supposedly the value should be extracted. 
- Other third-party plugins that may be affecting the behavior of the plugin.

## Debugging tips

In general, we recommend always using the Correlation Template that comes with the plugin to do all your recordings since we have added all the necessary elements, preconfigured, to make the recording process as easy as possible. This template also has all the required elements in the correct order and in the right places so that the information is not only shown correctly but can also be used in the debugging process.

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
