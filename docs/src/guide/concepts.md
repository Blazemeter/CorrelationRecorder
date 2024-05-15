---
prev: /guide/before-recording.md
next: /guide/best-practices.md
---

# Concepts

## Dynamic Value

Dynamic values are variables that change each time a user interacts with a web application. Examples of dynamic
 values include session IDs, CSRF tokens, and timestamps. These values are used by the application to maintain
 state and security.

When recording a test scenario using JMeter, dynamic values need to be captured and included in subsequent requests.
 Failure to do so can lead to errors and incorrect test results. However, capturing and handling dynamic values can
 be challenging and requires careful configuration of the test script.

There are several potential issues that can arise when dealing with dynamic values in JMeter recordings.
 One common issue is that the recorded values may be specific to a particular user session or request, and
 therefore cannot be reused in subsequent requests without modification. Additionally, dynamic values may expire
 or become invalid after a certain period of time, requiring the test script to be updated to use new values.

Another issue is that dynamic values may need to be correlated or parameterized to ensure that each virtual user
 in the test uses a unique value. This can be complex and time-consuming, particularly for large or complex test
 scenarios.

## Correlation

As I mentioned earlier, dynamic values such as session IDs and tokens can change with each user interaction,
 and must be included in subsequent requests to properly simulate user behavior. Correlation allows JMeter to
 automatically capture these values and replace them with unique values for each virtual user in the test.

The correlation process, or Correlation for shorts, typically involves identifying the dynamic value in the
 response using a regular expression or other pattern-matching mechanism, and then storing it in a JMeter
 variable. This variable can then be used in subsequent requests by enclosing it in curly braces,
 like this: ${myVariable}.

JMeter provides several built-in correlation functions, such as Regular Expression Extractor and CSS/JQuery Extractor,
 to simplify the process of capturing and storing dynamic values. However, more complex scenarios may require custom
 scripting to properly correlate values.

### Manual Correlation

Manually correlating dynamic values in JMeter involves several steps. Here is an overview of the process:

Identify the request that needs correlation: Begin by identifying the request that contains the dynamic value that
 needs to be captured and used in subsequent requests.

Detect the argument that needs to be correlated: Once you have identified the request, you need to determine which
 argument within the request contains the dynamic value. This may involve examining the request payload or headers to
 locate the relevant argument.

Locate the appearance of the dynamic value in responses: Once you have identified the argument, you need to locate the
 corresponding dynamic value in the responses to the request. This may involve using a regular expression or other
 pattern-matching mechanism to identify the value.

Add an extractor to obtain the value: Once you have located the dynamic value in the response, you need to add an
 extractor to capture it and store it in a JMeter variable. JMeter provides several built-in extractors, such as
 Regular Expression Extractor and CSS/JQuery Extractor, to simplify this process.

Store the value in a variable: Once you have extracted the dynamic value, you need to store it in a JMeter variable.
 The variable name should be chosen carefully to ensure it is unique and descriptive.

Replace the variable in subsequent requests: Finally, you need to replace the original dynamic value in subsequent
 requests with the JMeter variable containing the captured value. This ensures that each virtual user in the test uses
 a unique value.

Manual correlation can be a time-consuming process, particularly when dealing with large or complex test scenarios
 that involve multiple dynamic values. This is where JMeter's Automatic Correlation recorder comes in handy.
 The Automatic Correlation recorder provides several methods for automatically detecting and correlating dynamic
 values, including the use of regular expressions and CSS selectors.

Here are some of the Automatic Correlation methods supported by JMeter:

1. RegEx (Regular Expression) Extractor
2. CSS/JQuery Extractor
3. XPath Extractor
4. JSON Extractor
5. Boundary Extractor

By using the Automatic Correlation recorder, however, you can simplify the process of correlating dynamic values
 and save time when creating test scripts.

Please take a look at previous sections in the guide, where you can learn about the different mechanisms for
 automatically correlating dynamic values, either after the recording is being done or after the whole recording
 is done.

## Correlation Rule

The Correlation Recorder plugin provides a powerful feature called "Correlation Rules" that simplifies the process of
 making correlations in JMeter scripts. A Correlation Rule consists of three key components:

Reference Variable: This variable is used to store the dynamic value that will be extracted from a response and
 subsequently used for replacements in subsequent requests.

Extractor: The Extractor component of a Correlation Rule allows you to configure how and where the dynamic value will
 be extracted from the response. JMeter provides several built-in Extractors such as Regular Expression Extractor,
 CSS/JQuery Extractor, XPath Extractor, JSON Extractor and Boundary Extractor, to facilitate the process of
 capturing dynamic values.

Replacement: The Replacement component of a Correlation Rule allows you to configure how and where the dynamic value
 will be replaced in subsequent requests. You can specify which request parameter to replace, and how to format the
 replacement string.

By defining Correlation Rules, you can easily extract and replace dynamic values in JMeter scripts without the need
 for manual correlation. To learn more about how to create Correlation Rules and leverage this powerful feature,
 refer to the "Correlation Rules" section of the JMeter User Manual.

## Correlation Template

The Correlation Recorder Plugin in JMeter utilizes Correlation Templates to maintain a simplified versioning of
 Correlation Rules and to store and organize them together. A Correlation Template includes essential information
 such as version number, name, description, and changes log.

The Correlation Template serves as the foundation for the Automatic Correlation Analysis in the Correlation Recorder
 Plugin. It allows the user to keep track of the version of the Correlation Rules being used for the analysis.
 This feature ensures that the user is always working with the latest version of the Correlation Rules and helps
 to maintain the accuracy of the results.

Additionally, BlazeMeter provides several Correlation Templates that are designed for different technologies and
 protocols. These templates can be used to facilitate the correlation process for specific types of applications
 and make it easier for users to get started with the Correlation Recorder Plugin. To learn more about how to use
 Correlation Templates and benefit from this powerful feature, refer to the JMeter User Manual.

## Correlation Repository

The Correlation Repository is a powerful mechanism used by the Correlation Recorder Plugin in JMeter to keep your set
 of Correlation Templates up-to-date. This feature allows for continuous updates of Correlation Templates from external
 sources.

The Correlation Repository is similar to a GitHub repository, where you can upload new versions of your templates to be
 stored and shared with others, while also being able to download templates uploaded by others. In Blazemeter,
 you have access to both your company's private Repository and the public repository, where Blazemeter's refined
 Correlation Templates are stored and updated regularly.

By utilizing the Correlation Repository, you can streamline the Correlation process and reduce the amount of time
 required to create and maintain Correlation Templates. This feature allows for collaboration between team members and
 simplifies the process of sharing Correlation Templates across different projects. To learn more about how to use the
 Correlation Repository and take advantage of this powerful feature, refer to the JMeter User Manual.