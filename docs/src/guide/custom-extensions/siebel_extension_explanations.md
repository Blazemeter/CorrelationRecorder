---
sidebar : auto
---

<h4 align="center">Correlations Recorder Plugin for JMeter</h4>

# How Siebel CRM Extensions works

## Basic Concepts

Remember to review [The Flow](the_flow_explanation.md) to know when and where the methods and Context classes are being used. 

The following section will give an explanation of the problem and the necessity of a Siebel CRM Extensions.

##Some Context

While the user is moving from one point of the page to another, some data is sent under the hood (embedded inside JS scripts, before the HTML or inside it). That information is used by Siebel CRM to keep track of where the user is coming. 

Those values don’t come in a format that will allow you to correlate them easily because they may vary on position, on the way they are formed, even by the separator and the values represented in them. 

Because of this, the default core tooling that comes with the Correlation Recorder Plugin needed to be customized, allowing the users:

* To stop using complex scripts at the start of recording to correlate those values
* To ease the customization of the Correlation Rules when using this protocol
Reduce the boilerplate to test each one of their scenarios.

In the following sections, we will talk on how the Correlation Recorder handles the customization of the default set of Correlation Extractors and Correlation Replacements (Correlation Components, for short) in order to tackle the complexity of correlating dynamic data inside a Siebel CRM environment.

## Structure of the extension

### Siebel Correlation Context
As explained before, the Siebel CRM handles dynamic values inside their pages, some of them could be stored in the HTML tags and displayed to the user, and others could appear using more complex logic like Start Arrays and so on. 

Regardless of the way that the server provides the data, the plugin needs to find a way to extract the information, parse and share it between the Correlation Components, during recording process, so they can be properly correlated.

That's why the [SiebelContext](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelContext.java) is necessary. This class implements [CorrelationContext](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/core/CorrelationContext.java)'s methods:

1. `void update(SampleResult sampleResult)`
2. `void reset()`

Which are important for correlating dynamic variables during the recording and testing phases. 

#### Update

This handles the process of updating the values stored in the Siebel Correlation Context after a response is received. This might be important if your application needs to know, for example:

1. The source of the action that took the user to a certain page
2. The number of times a certain type of users go to one specific page
3. Or any particularly sensitive information that is needed to be encrypted and sent to another part of the app

This method not only contains the logic to extract, and share that information between the Correlation Components but also validates if any update is needed. 

::: warning
As mentioned in the [Special Considerations](the_flow_explanation.md#special-considerations), the availability of the updated information will differ from Correlation Replacements to Correlation Extractors.
:::

### Reset
As mentioned in [proxy's methods](the_flow_explanation.md#context-reset), this method is an important part of the proxy's responsibilities, since it triggers the reset of the contexts for every Correlation Rule when a Recording starts. 

This method will contain all the logic required to clean the variables inside a Correlation Context, allowing to commence with a blank slate.

## Siebel Extensions

Now that the concepts of the Siebel CRM Environment are clear, and you have a glance at the importance of shared variables and how the whole process takes place, let’s talk about each one of the Extensions: let’s begin explaining rule parts.  

### Siebel Correlation Components

#### Siebel Correlation Extractors

One of the many structures that Siebel CRM uses to store information inside a page is called Star Array. It represents the values as Strings and their lengths in numbers, in decimal or hexadecimal, separated by “*” or “_” respectively. 

The logic to extract and store those values is handled by the [Siebel Row Correlation Extractor](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelRowCorrelationExtractor.java).

When a Star Array is found, this Correlation Extractor creates a [JSR223 PostProcessor](https://jmeter.apache.org/usermanual/component_reference.html#JSR223_PostProcessor) in the `buildArrayParserPostProcessor` containing the logic for: 

The extraction of the appearances for the values found.
The storage of those variables, during the replay, to be used in future correlations.

Is important to mention that this method also stores the values in the Siebel Correlation Context during recording. This is key for correlating its appearances in future requests.

The logic is overwritten in the `process( … )` method, which is one of the APIs from its father, the  [RegexCorrelationExtractor](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/core/extractors/RegexCorrelationExtractor.java).

For this Correlation Extension, a JSR223 PostProcessor component was used, because of the dynamic nature of the Siebel CRM’s Star Array. If the Array would always have the same number of values, or their length remained the same, other components like [Regular Expression Extractor](https://jmeter.apache.org/usermanual/component_reference.html#Regular_Expression_Extractor) could also do the trick. Use the Components that fit your particular scenario.

#### Siebel Correlation Replacements

All the values obtained from the Correlation Extractors and the ones in the updated Siebel Context will be replaced wherever they appear in requests by the Siebel Correlation Replacements. 

The Siebel CRM Extension contains following Replacements:

* [Siebel Row Params Correlation Replacement](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelRowParamsCorrelationReplacement.java)
* [Siebel Row Id Correlation Replacement](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelRowIdCorrelationReplacement.java)
* [Siebel Counter Correlation Replacement](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelCounterCorrelationReplacement.java)

As mentioned on the [Special Considerations](the_flow_explanation.md#special-considerations), a value that was updated on the Siebel Context from a response, won’t be available for the Correlation Replacements until the next request is processed.

Once a value is matched by any Correlation Replacement, its appearance in the arguments will be replaced by the Reference Variable surrounded by ${} (eg: with a Reference Variable "RV", the replaced value will be ${RV}).

One can implement the replacement functionality by replacing directly the value in an argument, or by using other JMeter's Components, like Pre/Post Processors for more complex logic. Taking for example the [Siebel Counter Correlation Replacement](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/siebel/SiebelCounterCorrelationReplacement.java):

The method `public void process(HTTPSamplerBase sampler, List<TestElement> children, SampleResult result, JMeterVariables vars)` is overwritten from [RegexCorrelationReplacement](https://github.com/Blazemeter/CorrelationRecorder/blob/master/src/main/java/com/blazemeter/jmeter/correlation/core/replacements/RegexCorrelationReplacement.java) because the Siebel Counter Correlation Replacement doesn't take the "Counter" value from a response, previously extracted, instead, it calculates the difference of the SWEC variable from previous stored value, and updates it accordingly on a previously stored value of the SWEC variable. This logic allows doing the replacement even if the previous steps are disabled, since this logic is not bounded to fixed values that might be affected by their presence/absence.  

You can start using the Templates provided in the following section, refactor the ones that the Plugin has for Siebel or, make of your own.

# Templates

Remember to take a look at [Make your own Custom Extension](..) section for detailed info on the Correlation Components. The following examples can be used as a base:

* [Basic Correlation Extractor](https://github.com/Blazemeter/CorrelationRecorder/blob/master/examples/CustomCorrelationExtractor.java)
* [Basic Correlation Context](https://github.com/Blazemeter/CorrelationRecorder/blob/master/examples/CustomContext.java)


