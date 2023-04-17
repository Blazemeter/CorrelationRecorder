---
sidebar: auto
next: /guide/before-recording.md
prev: /guide/correlation-process.md
---

# Automatic Correlation methods

This section covers the Automatic Correlation methods available in the plugin. These methods allow you to
automatically correlate dynamic values in your recordings. Unlike the Correlation Rules method, which must be
configured before recording, these methods are performed after the recording is complete. This provides greater
flexibility, as the correlations can be reviewed and rolled back if necessary. 

We'll discuss the pros and cons of each method to help you determine which one is best suited for your needs.

[[toc]]


**Supported methods**

At this stage, the supported methods can correlate by:

- [By Using Correlation Templates](#by-using-correlation-templates)
- [By Replay and compare](#by-replay-and-compare)

Let's see what each one of these methods has to offer.

## By using Correlation Templates

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

## By Replay and Compare

This method involves analyzing the results of a recording replay by comparing them with the original recording. It
only focuses on the arguments of the requests that failed in the replay. By doing so, it generates a list of
Correlation Suggestions based on the differences found, allowing you to select which ones to apply in the Test Plan.

**Pros and Cons**

Pros:
- It is pretty flexible, since it detects dynamic values that you might not be aware of.
- It is customizable, since you can configure how the analysis is done.

Cons:
- It is not 100% bullet-proof, since it might correlate dynamic values that are not necessarily dynamic.

### Usage

**TL;DR**: Follow the steps below to use this method:

1. Open the Correlation Wizard
2. Select the **By Replay and Compare** method.
3. Select the Templates to use for the analysis.
4. Add the JTL file of the recording (if it isn't already loaded).
5. Press "Continue"
6. Review the Correlation Suggestions
7. Press "Auto Correlate" to apply the suggestions to the Test Plan.

With this, you should have a Test Plan with the dynamic values already correlated. Replay the Test Plan to make sure
that it works as expected.

**Detailed steps**

1. Open the Correlation Wizard

To open the Correlation Wizard, you can either:
- Accept the replay report Dialog, after the recording.
- Click on the **Correlation Wizard** button in the **Correlation** tab.
![Replay Report Dialog](/guide/img/replay-report-dialog.png)

2. Select the **By Replay and Compare** method

Regardless of the method used, the "Select Correlation Method" dialog will be displayed.
Select the **Existing correlation rules (recommended** method and press "Continue".
![Select Correlation Method](/guide/img/select-correlation-method.png)

3. Select the Templates to use for the analysis

The next step is to select the Correlation Templates to use for the analysis. You can select one or more templates.
If you select more than one, the plugin will use the union of the dynamic values found in all of them.

![Select Correlation Templates](/images/select-correlation-template.png)

::: tip
You can select which version of each Correlation Template to use. By default, the latest version is selected.
:::

4. Add the JTL file of the recording (if it isn't already loaded)

By default, the plugin loads the JTL file that is found in the View Result Tree of the "bzm - Correlation Recorder" element.
If you want to use a different JTL file, you can click on the "Browse" button and select the file you want to use.

::: warning
It is highly recommended that you use the JTL file that that comes from the recording, since it contains the
raw data of the recording. If you use a different JTL file, the analyzed data might not be accurate, hence
the results might not be as realistic as expected.
:::

5. Press "Continue"
6. Review the Correlation Suggestions
Once the analysis is done, the plugin will display the Correlation Suggestions in the "Correlation Suggestions dialog".

![Correlation Suggestions](/guide/img/correlation-suggestions.png)
Review the name of the arguments, the values, where they were found and used. If you want to apply a suggestion,
select the checkbox next to it. If you want to ignore a suggestion, uncheck the checkbox.

7. Press "Auto Correlate" to apply the suggestions to the Test Plan.

Once you are done reviewing the suggestions, press the "Auto Correlate" button to apply the suggestions to the Test Plan.
The plugin will automatically correlate the dynamic values in the Test Plan, and will display a dialog informing you
that the process was successful.

![Auto Correlate Successful](/guide/img/auto-correlate-successful.png)

