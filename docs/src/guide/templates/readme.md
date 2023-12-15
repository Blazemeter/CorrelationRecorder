# Correlation Templates

The Templates are part of the core functionalities of the Correlation Recorder Plugin. They are the structure that groups a set of Correlation Rules inside the Plugin for various uses.

In this section, you will learn about:

1. Definitions
2. Types
3. Management
4. Limitations

## Definitions

A Correlation Template, or a Template for shorts, is the structure used to group a set of Correlation Rules inside the Correlation Recorder Plugin for different purposes, such as:

* Sharing with coworkers
* Analyzing the recordings
* Versioning your changes
* Organizing your Correlation Rules

We will talk about each other in their respective sections. For now, let's jump into the types of Templates you can access.

## Types
What differentiates one Template from another is the source or Repository it comes from, and it highly impacts your capabilities over them.

Initially, you can lose control over their content and configuration while you gain a range of Correlation possibilities. It is a matter of the policies of your Template providers.

Based on their sources, you might have:

* Local Templates
* Central Templates
* BlazeMeter Templates (built-in, Workspace and Enterprise)
* Custom Templates

Most of these Templates have the same capabilities. However, they might restrict you based on your user access level. Let's talk about it.

### Local Templates
These Templates are the ones created by yourself. They are located in your local machine and maintained by you. Because of this, you have full access to them, allowing you to:

* Load them
* Configure them
* Save new versions
* Use them for Analysis
* Export the suggestions as Rules

### Central Templates
These Templates come from GitHub Central, and since they come from the community, you can treat them as a Local Template, with the difference that the Saving of a new version (directly to GitHub) still needs to be supported.

* For the sake of consistency, these Template allows you to:
* Load them
* Configure them
* Use them for Analysis
* Export the suggestions as Rules

### BlazeMeter Templates
These are the Templates that come from BlazeMeter's Repository. You get access to them after configuring your BlazeMeter's API and forcing a sync of your Templates if needed.

Depending on your account level, you might need special access to some of their capabilities; however, in general terms, you are allowed to:

* Use them for Analysis (Built-in, Workspace, Enterprise(*))
* Load them (Workspace)
* Configure them (Workspace)
* Save new versions (Workspace)
* Export the suggestions as Rules (Workspace)

In general terms, everything you or your coworkers created is available as if they were Local Templates: you own and use them. These are marked as "Workspace Templates".

The rest of the Templates from BlazeMeter are proprietary; you can access them "as it is" exclusively for Analysis.

### Manually Imported Templates
These are Templates that come from manually importing them. Since there is no way to keep track of them, we consider them just like a Local Template, meaning that you are allowed to:

* Load them
* Configure them
* Save new versions
* Use them for Analysis
* Export the suggestions as Rules

## Management
Let's talk about how you get access to your Templates and how to store them properly.

You can either create your Templates or import them from external sources or Repositories, as we call them.

### Create Template
The only requisite for creating a Template is having Correlation Rules on it. These can be added to the Plugin by:

* Manually inserting a new Rule
* Exporting the Correlation Suggestions as Rules
* Loading one or multiple templates

Once you are satisfied with the final set of Correlation Rules, you can click on "Save Template", proceed to fill in all the required fields, and click on "Save".

### Load Template
When you load a Template to the Plugin, you manually edit the configurations the Correlation Rules have to fit your use case properly. Bear in mind that only some of the Templates can be loaded to be manually edited (this might depend on your access level over these Templates, depending on the sources they come from).

To load a Template to the Plugin, you must:

1. Load the Correlation Recorder Plugin
2. Click on the "Load Template" button
3. Select the Template from the list of Installed and Available (*)
4. Click on the "Load" button

Depending on your access level over the selected Template, you can load it into the Plugin. If you don't have access, you will be prompted with a warning pop-up informing you that you don't have access.

(*): If you select a Template from the Available list, you should install that Template first before. Sometimes, this installation might require resetting your JMeter; in such scenarios, you will be prompted to ask if you want to restart, giving you time to store unsaved changes in your Test Plan.

### Sync Templates
We have spoken about sources or Repositories, which are the ones that supply you with Templates when they are not created locally in your machine. Some of these Repositories come from external sources (such as BlazeMeter, GitHub, or other HTTP-related locations) and might require you to manually sync them to obtain the most recent versions of their Templates.

To keep your local Repository up to date, please remember to click the "Refresh" button. It can be found right next to the "Config" button and looks like two arrows following each other.

### Manually importing Templates
If anyone gave you the JSON file related to a Template, you could import it into your local Repository by adding the file to your `correlation-template` folder, located in `<JMeter>/bin/.` after that, you force a manual sync so the Plugin to index it as local.

### Delete Templates
Currently, the Correlation Recorder Plugin does not support Deleting Templates, neither from your local Repository nor any other external repository.

You can delete your local repository Templates by manually deleting the correlation-templates folder in the bin folder inside your JMeter installation folder; however, this is not advisable, and you must do it at your own risk.

## Usages

Templates are used in two different ways:

1. To store Correlation Rules for a specific application or type of application
2. To do an Analysis of your recordings and suggest changes

:::tip
Always sync your repositories before analyzing your recordings to have the latest version of the Templates.
:::

## Limitations

There are some limitations when using Templates:

1. You can't edit BlazeMeter built-in Enterprise Templates
2. You can't overwrite versions of any Template (regardless of the type)