---
sidebar : auto
---

# The Flow

In the following sections, we will cover topics regarding how JMeter process and sends the Requests and Responses to the plugin, when and where this will be affected by the configured Correlation Rules, and how the order of the methods affects the results the user will see during the recording.

## The Steps

In General Terms, the recording follows this order, for the case that involves processing with the plugin:

1. One Request goes to the server 
1. The server gives a Response 
1. JMeter takes both and sends them to the plugin
1. The Proxy verifies and sends them to the CorrelationEngine (deliverSampler() method)
1. The Engine applies all the Rules to the Response, Request and Recorded Sampler (process() method)
1. The Proxy takes the processed values back to JMeter (super.deliverSampler() method)
1. JMeter sends the Recorded Sampler to the configured Recording Controller

## The Explanation

Now, on a detailed point of view, let's talk about each one of the steps of processing the recorded Samplers

### Proxy

#### Deliver Recorded Sampler
For the Correlation Recorder Plugin, the Proxy is handled by `CorrelationProxyControl.java`, who receives an HTTPSamplerBase (the recorded Sampler), and the SampleResult (the request and the response), process and sends it to the CorrelationEngine to be processed.

All that occurs in the deliverSampler method.

````java
  public void deliverSampler(HTTPSamplerBase sampler, TestElement[] testElements, SampleResult result)
````
#### Context Reset

The `CorrelationProxyControl.java` also holds the responsibility of triggering the Contexts reset when a new recording starts, so each recorder flow starts from a clean base.

### Engine

The Engine contains, not only the configured Correlation Rules setup before starting the recording, but also each one of the Contexts (CorrelationContexts and any Custom Context) associated with then, in their updated form. This role is been handled by the `CorrelationEngine.java`

#### Responsibilities

At this point, the only responsibilities of the Engine are:

1. Apply all the configured CorrelationReplacements to the HTTPSamplerBase (the record)
2. Update all the Contexts with the info that comes from the SampleResult (the result)
3. Filter the Recorder HTTPSamplerBase by the [Response Filter](https://github.com/Blazemeter/CorrelationRecorder/blob/master/README.md#filtering-your-requests)'s type
4. Apply all the configured CorrelationExtractors to the HTTPSamplerBase (in case this one matches the [Filter MIME Type](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types) from Step 3)

Anything that happens in the steps 1 and 4, will be handled by the ``CorrelationRule`` (applyReplacements for the former, and addExtractors for the later), for each configured Rule.

#### Special Considerations

1. Is important to mention that, because of the order of how the Correlation Components are applied and updated, the values that they extract or replace, only will be used after a future request. Because of this:

* Each value that is extracted from a Correlation Extractor, during the recording, will only be available for a Correlation Replacements in the next Request made
* All the updated context values only will be considered, in the actual Sampler, by the Correlation Extractor but, the Correlation Replacements will need to wait for the next Sampler to be able to see it. The execution of the Correlation’s Flow is, as mentioned before:

> Receives a Request → Receives a Response → Applies Correlation Replacements → Update Correlation Contexts → Applies Correlation Replacements


