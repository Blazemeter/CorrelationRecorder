---
sidebar: auto
next: /guide/best-practices.md
prev: /guide/installation-configuration.md
---

# Correlation Process

Let's review the whole process of correlation, from the beginning, when we start recording and configuring
the JMeter plugin, until the end, when we have a fully functional test plan.

## Recording

If you are new to making recordings in JMeter, we recommend you read the following articles from their
official documentation:

- [Recording a Test Plan](https://jmeter.apache.org/usermanual/jmeter_proxy_step_by_step.html)
- [Recording best practices](https://jmeter.apache.org/usermanual/best-practices.html)

With this, you will be able to understand the basics of how to make a recording in JMeter and how to get the most out of it.

## Correlation Process

The "Correlation Process" is the technique of locating within the recording requests the values (arguments)
that vary in each iteration (replays), calling them "dynamic". Whenever a dynamic value is found in a request, usually one that failed, we need to locate the response upon which it depends, extract it using extractors in
the Test Plan, store it in a variable, and replace it in the corresponding request.

This process is repeated on each failed request until all the dynamic values are found and replaced. Sometimes,
the dynamic values are not easy to locate, and sometimes, they might even have different values for the same argument
within the same recording.

When performing this process manually, it can be very time-consuming and frustrating. That's why we created the Correlation Recorder plugin, to automate this process and make it easier for you.

## Methods of Correlation

The Correlation Recorder plugin offers different methods of correlation, each one with its own advantages and disadvantages. In this section, we will cover all of them divided into two categories: Before the Recording and After the Recording.

### Before the Recording

When we say 'Before the Recording,' it refers to the process between loading the Correlation Template into JMeter and completing the recording. We use this terminology instead of saying 'while it is being recorded' because this method requires you to properly configure the plugin before performing the recording using what is known as a Correlation Rule (or Rule for short).

Once you have finished configuring the rules, you can start the recording, and the plugin will automatically compare each request and response with the configured rules.

Pros:
- It is the most flexible method since you can configure it to your needs.
- It is the most efficient method since it is the only one that does not require you to replay the recording.
- It supports extensibility, allowing you to develop your own rules according to your needs.

Cons:
- It requires you to configure the rules before the recording is done.
- It requires prior knowledge of potential dynamic values and how to configure the rules to extract them.
- It requires a re-recording to test the changes made to the rules.
- It doesn't support rolling back changes made to the recording, since the elements are modified in the recording itself.

To know more about this method, how to configure it, and how to use it, read [Correlation Rules](/guide/before-recording.md).

### After the Recording

Unlike the previous point, these methods are performed after the recording is done, and they are used to analyze the recording and the recording results to find the dynamic values. It is important to note that these methods work in the form of an analysis of the recording, and they do not modify the recording itself, and they are done automatically by the plugin.

At this stage, the supported methods can correlate by:

- [By Using Correlation Templates](/guide/after-recording.md#by-using-correlation-templates)
- [By Replay and Compare](/guide/after-recording.md#by-replay-and-compare)

Let's see what each one of these methods has to offer.

#### Correlation by Using Correlation Templates

This method involves analyzing a recording using different Correlation Templates to automatically detect dynamic values. It generates a list of Correlation Suggestions based on those values and lets you choose which ones to apply in the Test Plan.

Pros:
- This is the most reliable method as it only correlates the dynamic values found with the rules in the Correlation Template.
- It lets you test any set of Correlation Rules before applying them to the Test Plan.
- You can easily roll-back changes made to the Test Plan as it stores it in a separate file.
- It integrates with the Correlation Repository feature, allowing you to use the Correlation Templates from BlazeMeter, GitHub, or any other sources, aside from your local ones.

Cons:
- It still requires Rules to properly correlate the dynamic values.
- It does not detect dynamic values that are not present in the Correlation Templates.

#### Correlation by Replay and Compare

This method involves analyzing the results of a recording replay by comparing them with the original recording. It only focuses on the arguments of the requests that failed in the replay. By doing so, it generates a list of Correlation Suggestions based on the differences found, allowing you to select which ones to apply in the Test Plan.

Pros:
- It is pretty flexible since it detects dynamic values that you might not be aware of.
- It is customizable since you can configure how the analysis is done.

Cons:
- It is not 100% bullet-proof since it might correlate dynamic values that are not dynamic.
- It requires your input of which of the detected dynamic values you are interested in correlating. Otherwise, it will end up correlating values that you might not be interested in.

While they both have their own advantages and disadvantages, we recommend you use the **Correlation by Using Correlation Templates** method as it is the most reliable one. With that being said, the **Correlation by Replay and Compare** method can be pretty useful when you are not sure where the dynamic values are present in the recording.

In conclusion, the Correlation Process is a crucial step in JMeter testing, and the Correlation Recorder plugin provides several methods to make it easier and more efficient. By following the recommended methods, you can save time and effort and ensure a more accurate and reliable test plan.

If you are new to JMeter, we recommend you start with the basics of recording and gradually move on to the more advanced topics of correlation. Take your time to learn and practice the different methods, and find the one that works best for your specific scenario.

Also, keep in mind that correlation is not a one-time process. Dynamic values can change over time, and you may need to update your correlation rules or templates accordingly. So, make sure to regularly review and update your test plan to ensure its accuracy and reliability.

In summary, the correlation process is a crucial part of JMeter testing, and the Correlation Recorder plugin provides valuable tools to make it easier and more efficient. By mastering the different methods and techniques, you can create accurate and reliable test plans that can help you identify performance issues and optimize your applications.