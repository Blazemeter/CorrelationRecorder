---
prev: /guide/templates/
next: /guide/custom-extensions/
---
# Create Template
The only requisite for creating a Template is having Correlation Rules on it. These can be added to the Plugin by:

* Manually inserting a new Rule
* Exporting the Correlation Suggestions as Rules
* Loading one or multiple templates

Once you are satisfied with the final set of Correlation Rules, you can click on "Save Template", proceed to fill in all the required fields, and click on "Save".

## Manual Template Creation
By default, you can create local Templates, which are stored in your local Repository, but you can also create Templates in your BlazeMeter account and upload them to a Repository for you and your team to use (since they are stored in the cloud, you can access them from any machine with the Plugin installed and they are shared with your team).

To create a Template, regardless to where you want to store it, you need to provide the following information:

1. A **Name** or **Id** (\*): representing the application or type of application it handles (e.g. "SAP", "Salesforce", "SAP Fiori", etc.)
2. A **Version number** (\*): to identify the version of the Template (we recommend to use [semantic versioning](https://semver.org/) for this, e.g. "1.0.0", "1.0.1", "1.1.0", etc.)
3. A list of **Changes** (\*): to inform the others what has changed in the Template (e.g. "Added a new Correlation Rule to handle the CSRF token", "Added a new Correlation Rule to handle the session ID", etc.)
4. A **Description** (\*) text: to inform the others what the Template is for (e.g. "Template to handle SAP applications", "Template to handle Salesforce applications", etc.)
5. The Author's **Name** (\*): to identify who created the Template (e.g. "John Doe", "Your Company", etc.)
6. The Author's **Email** (\*): to contact the author (e.g. "email@example.com", "example.com", etc.)
7. Dependencies list: to inform the others what other libraries are required to use this one (e.g. "our-logic.jar", "logging.jar", etc.)

**Note**: The fields marked with an asterisk (\*) are mandatory. The rest are optional.

When you are creating a new Template, you can either create it from scratch or use an existing one as a base. If you choose to use an existing one, you will be able to select it from a list of Templates that are stored in your configured Repository.
Even if they are a combination of other Templates, you are not limited to store them in the same Repository, on any of the selected Templates, you can store it in a different repository with a different name and version; it's up to you and how you want to organize your Templates.
It is advisable to make mention of the Templates you used as a base in the Description field, specially if you are combining support for different applications and technologies or if they have dependencies associated.

## Export Suggestions as Rules
If you have Correlation Suggestions generated from your Test Plan, you can export them as Rules to your plugin, modify them and, after, to create a Template. 

To do so, you need to:

1. Make a Recording and generate Correlation Suggestions (by any method)
2. Click on the "Export Suggestions" button in the Correlation Recorder Plugin
3. Select the Rules you want to save as a Template (eliminate the rest)
4. Click on "Save Template" and fill in the required fields
5. Click on "Save"

Note: If you are generating Suggestions from a Template that you have access to manage (e.g. a local Template), you can make use of the Export Suggestions as Rules as a way to ensure **which Rules are effectively applying**, since the Suggestions are generated from the Rules that actually applied in your Recordings. This is a goo way to refine your Rules before saving or sharing them.

## Load Template
Another way to create a Template is by loading different Templates into the Plugin and manually editing them to fit your use case. This is useful when you want to combine different Templates into a single one or when you want to create a Template from scratch.
When you are done editing the Template, you can save it as a new one. 

The Correlation Recorder Plugin do not keep track of the Templates you load, except from the last one, so you need to make sure you save them in the proper Repository.