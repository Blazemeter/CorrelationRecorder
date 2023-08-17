---
sidebar: auto
---

# Correlation Templates

In this section, you will:

1. Definitions
2. Types
3. Usages
4. Limitations

## Definitions

A Correlation Template, or Template for shorts, is the structure used to group a set of Correlation Rules.

Usually, a Template is created to handle a specific application, or a specific type of application. It will have:

1. A name or Id representing the application or type of application it handles (e.g. "SAP", "Salesforce", "SAP Fiori", etc.)
2. A version number (e.g. "1.0.0", "1.0.1", etc.)
3. A description to inform the user what the Template is for (e.g. "Template to handle SAP applications", "Template to handle Salesforce applications", etc.)
4. A change text to inform about the recent changes in the version you are uploading (e.g. "Added new rules to handle SAP Fiori applications", "Added new rules to handle Salesforce Lightning applications", etc.)
5. A set of Correlation Rules that will be used to handle the application or type of application (e.g. "SAP", "Salesforce", "SAP Fiori", etc.)

## Types

Depending on the source they come from, there are different types of Templates.

1. Local Templates
2. BlazeMeter Templates
3. BlazeMeter Enterprise Templates
4. BlazeMeter Workspaces Templates
5. Custom Templates

Usually, depending on the type of Template and your account permissions, you will have different levels of access to them.

### Local Templates

These are the Templates that you create and store in your local repository. Every time you create a new Correlation Rule and save it, it will be stored in a Template.
Since these are local Templates, you have full access to them, meaning that you can:

- Viewing them
- Deleting them
- Load their rules into your plugin for you to edit
- Use them when doing analysis of your recordings
- Export the associated Correlation Rules from the Correlation Suggestions table

### BlazeMeter Templates

These are the Templates that come by default with your BlazeMeter account. They are free and you can use them for:

- Viewing them
- Doing analysis of your recordings

Since they are proprietary Templates, you can't edit them or load them into your plugin.

### BlazeMeter Enterprise Templates

These are Templates that are tailored by our team of experts to work with specific applications.

If you have a BlazeMeter Enterprise account, you can use them for:

- Viewing them
- Doing analysis of your recordings

Just like the built-in Templates, you can't edit them or load their rules into your plugin for you to edit.

### BlazeMeter Workspaces Templates

These are Templates that are created by you or your team and are stored in a Workspace. You can use them for:

- Viewing them
- Deleting them
- Load their rules into your plugin for you to edit
- Upload different versions of the same Template
- Use them when doing analysis of your recordings
- Export the associated Correlation Rules from the Correlation Suggestions table

### Custom Templates

These are Templates that come from other sources, like a colleague or a third party. You can use them just like you would use a local Template.

## Usages

Templates are used in two different ways:

1. To store Correlation Rules for a specific application or type of application
2. To do analysis of your recordings and suggest changes

:::tip 
We recommend to always sync your repositories before doing analysis of your recordings, so you can have the latest version of the Templates.
:::

## Limitations

There are some limitations when using Templates:

1. You can't edit BlazeMeter built-in, Enterprise Templates
2. You can't overwrite versions of any Template (regardless of the type)
3. You can't save a Template into a Workspace from the plugin (_Coming soon_)