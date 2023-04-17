---
sidebar: auto
next: /guide/after-recording.md
prev: /guide/installation-configuration.md
---

# Correlation Process

Let's review the whole process of correlation, from the beginning, when we start doing the recording and configuring
 the plugin JMeter, until the end, when we have a fully functional test plan.

## Recording

If you are new to making recordings in JMeter, we recommend you to read the following articles from their
 official documentation:

- [Recording a Test Plan](https://jmeter.apache.org/usermanual/jmeter_proxy_step_by_step.html)
- [Recording best practices](https://jmeter.apache.org/usermanual/best-practices.html)

With this, you will be able to understand the basics of how to make a recording in JMeter, and how to get the most
 out of it.

## Correlation Process

The "Correlation Process" is, in a nutshell, the technique of locating within the recording requests, the values (arguments)
 that vary in each iteration (replays), calling them "dynamic". Whenever one dynamic value is found in a Request,
 usually one that failed, we need to locate the response upon which it depends, extract it using extractors in
 the Test Plan, store it in a variable, and replace it in the corresponding request. 

This process is repeated on each failed request, until all the dynamic values are found and replaced. Sometimes,
 the dynamic values are not easy to locate, and sometimes, they might even have different values for the same argument,
 within the same recording.

When performing this process manually, it can be very time-consuming, and sometimes, it can be very frustrating. That's
 why we created the Correlation Recorder plugin, to automate this process, and make it easier for you.

## Methods of Correlation

The Correlation Recorder plugin offers different methods of correlation, each one with its own advantages and
 disadvantages. In this section, we will be covering all of them divided into two categories: Before the Recording and
 After the Recording.

### Before the Recording

When we say 'Before the Recording,' it is meant to refer to the process between when the Correlation Template is
 loaded into JMeter and when the recording is completed. We use this terminology instead of saying 'while it is
 being recorded' because this method requires you to properly configure the plugin before performing the
 recording using what is known as a Correlation Rule (or Rule for short). 

Once you have finished configuring the rules, you can start the recording, and the plugin will automatically 
 compare each request and response with the configured rules

Pros:
- It is the most flexible method, since you can configure it to your needs.
- It is the most efficient method, since it is the only one that does not require you to replay the recording.
- It supports extensibility, allowing you to develop your own rules according to your needs.

Cons:
- It requires you to configure the rules before the recording is done. 
- It required prior knowledge of potencial dynamic values, and how to configure the rules to extract them.
- It requires a re-recording in order to test the changes made to the rules.
- It doesn't support rolling back changes made to the recording, since the elements are modified in the recording itself.

To know more about this method, how to configure it and how to use it, read [Correlation Rules](/guide/before-recording.md).

### After the Recording

Unlike the previous point, these methods are performed after the recording is done, and they are used analyse
 the recording and the recording results, in order to find the dynamic values. It is important to note that
 these methods work in the form of an analysis of the recording, and they do not modify the recording itself and they
 **are done automatically by the plugin**.

At this stage, the supported methods can correlate by:

- [By Using Correlation Templates](/guide/after-recording.md#by-using-correlation-templates)
- [By Replay and compare](/guide/after-recording.md#by-replay-and-compare)

Let's see what each one of these methods has to offer.

#### Correlation by using Correlation Templates

This method involves analyzing a recording using different Correlation Templates to automatically detect dynamic
 values. It generates a list of Correlation Suggestions based on those values, and lets you choose which ones
 to apply in the Test Plan.

Pros:

- This is the most reliable method as it only correlates the dynamic values found with the rules in the Correlation Template.
- It lets you test any set of Correlation Rules before applying them to the Test Plan.
- You can easily roll-back changes made to the Test Plan as it stores it in a separate file.
- It integrates with the Correlation Repository feature, allowing you to use the Correlation Templates from BlazeMeter,
GitHub or any other sources, aside from your local ones.

Cons:

- It still requires Rules to properly correlate the dynamic values.
- It does not detect dynamic values that are not present in the Correlation Templates.

#### By using Correlation Templates

This method involves analyzing the results of a recording replay by comparing them with the original recording. It
 only focuses on the arguments of the requests that failed in the replay. By doing so, it generates a list of
 Correlation Suggestions based on the differences found, allowing you to select which ones to apply in the Test Plan.

Pros:
- It is pretty flexible, since it detects dynamic values that you might not be aware of.
- It is customizable, since you can configure how the analysis is done.

Cons:
- It is not 100% bullet-proof, since it might correlate dynamic values that are not dynamic.
 present in the Correlation Templates.

While they both have their own advantages and disadvantages, we recommend you to use the **Correlation by using Correlation
 Templates** method, as it is the most reliable one. With that being said, the **Correlation by Replay and Compare** method
 can be pretty useful when you are not sure what dynamic values are present in the recording.
