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
set it as the JTL file in the View result tree for the "bzm - Auto Correlation Recorder" sampler.

This will use the back method of the Correlating by Replay and Comparison method of using that auxiliary JTL file to
perform the correlation process.

## 2. Manual Replay Correlation Analysis Issue After Choosing 'No' to Wizard Prompt

We are aware that there is an issue where, if you click "No" when the Automatic Correlation Wizard offers
to generate suggestions after recording, manually opening the wizard, replaying, and selecting the Correlation
method may not trigger the analysis.

We recommend selecting "Yes" when the wizard prompts you to generate suggestions to avoid this issue.

## 3. GUI crash and console error logging null pointer.

We have identified an issue with java 1.8 primarily happening on macOS when using this plugin.
We have fixed the issue in the table renderer, but there can still be some cases where this might happen.
In case you are getting a GUI crash and null pointer references in jmeter console, you might want to consider
upgrading java version.

If you are getting this error or a similar one:

```
 java.lang.NullPointerException: null
 at com.blazemeter.jmeter.correlation.gui.RulesTableGui$CustomCellRenderer.getTableCellRendererComponent(RulesTableGui.java:178) ~[jmeter-bzm-correlation-recorder-2.2.1.jar:?]
  at javax.swing.JTable$AccessibleJTable.getAccessibleChild(JTable.java:7037) ~[?:1.8.0_345]
```

We recommend using java 11 or higher.
