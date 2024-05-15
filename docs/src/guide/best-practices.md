---
next: /guide/troubleshooting.md
prev: /guide/concepts.md
---

# Best Practices
In this section you will find best practices for the creation of correlation rules, along with tips for optimizing
 your current and past scripts.

## Tips for optimizing performance

### Review your correlation rules
Even though the plugin is designed to be as efficient as possible, it is still a good idea to review your correlation
 rules and make sure that they are not affecting the performance of your test scripts. The addition of unnecesary
  correlation rules can have a negative impact on the performance of your test scripts, making either the recording
   or the analysis of the script slower.

Likewise, it is also a good practice to review your correlated elements after the rules or suggestions being applied:
 if they added correlations that are not needed, you can remove them to improve the performance of your test scripts.

## Collaboration and sharing correlation rules
When sharing your correlation rules (as Correlation Templates or simply sharing them individually), always remember to
 check for sensitive data. This is not as usual scenario, but often users forget to remove examples and testing information
 from their descriptions, regexes and Test Plans, exposing sensitive data to other users.

## Other Best Practices
The following are a list of best practices, in general, that should be considered when doing any sort of Recordings in JMeter:


### Always update the security certificate
Once is never enough when it comes to security. Remember to update periodically the Security Certificate used during the recordings. It expires after 7 days of been created.

### Use the latest version of JMeter
The newer, the better, try to use the latest version of the JMeter, allowing you to avoid dealing with unnecessary issues and use a more performant app.

### Use Protocol Templates
Not only they have been tested by the development team but, they also come with configured components, allowing the recording to be more efficient and faster to configure.

### Record using Incognito mode
Multiple times the browser stores information from previous visits, in order to ease the following usage of the pages, contaminating the recording with data that isn’t new and related to the flow at hand. To avoid this, always start a new “Incognito session” when making a recording. This ensures that the data that is being recorded is always complete and new, no matter when and where the recording is replayed.

### Clean cached files, cookies, and historical data
Even if you are recording in incognito mode, it is not uncommon to perform another recording after the one you just made. If you did it in an Incognito window, either close that window and start with a brand new one or clean all the information that might affect your recording, such as cookies, cache, sessions, etc.

### Close other tabs and apps connected to the internet
Avoid having other web pages or applications opened when recording. This will reduce the probability of having non-related requests in your recording.

### Identify each step of the workflow while recording
It’s suggested to use Transaction Controllers in order to group the requests for each step of the workflow, writing comments or a label to help understand and identify the requests sent in each step. The best way to do this is step by step while recording, for example: record the access to the main page, add a Transaction Controller and put all the requests recorded in it, record the next step of the workflow, add another transaction controller and put all the new recorded requests in it and so on.

### Use Request filter
Avoid recording unnecessary requests when testing a flow, unless that’s exactly what you want to do. Is better to test a specific scenario rather than stressing the server with styles and fonts that won’t do any good in the functionality itself.
