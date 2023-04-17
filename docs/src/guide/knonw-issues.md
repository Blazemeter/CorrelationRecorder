# Known issues

This section contains a list of known issues and workarounds.

## 1. Correlation History is isolated
After a recording or a correlation process is done and, the Test Plan is saved, if you open it again,
 the Correlation History of that "session" is not associated to the Test Plan anymore. This impacts the correlation
 process, as there is no Original Recording JMX or JTL associated to the Correlation History for the "current" session.

### Workaround: Manually feeding the files to the Correlation Process

#### Correlating by Correlation Template 
1. You need to open the correlation history JSON file stored at `jmeter/bin/History/`
2. Take the path of the `recordingTraceFilepath` in the `Original Recording` step.
3. Use that path in the JTL file selector in the "Select Correlation Template" window.

This will enforce the use of the JTL file you want to use for the correlation process for that "session".

#### Correlating by Replay and Comparison
Take the steps from the previous section, but instead of feeding the JTL file to the JTL file selector,
 set it as the JTL file in the View result tree for the "bzm - Correlation Recorder" sampler.

This will use the back method of the Correlating by Replay and Comparison method of using that auxiliary JTL file to
 perform the correlation process.

::: warning roadmap
We will be working on a solution for this issue in the future. Potentially, adding the Correlation History complete
feature (such as visualization, rollback, etc) to the plugin.
:::
