# Examples

In this section we will go through some examples of how to use the plugin.

## Pre-requisites

Even though you can use any platform to do your correlations, in this example, we will be using WordPress, 
since it is a very popular platform and it is easy to get a local instance running.

With that in mind, the following are the pre-requisites for do this example:

- Get [Docker](https://docs.docker.com/get-docker/)
- Get the [WordPress Docker Image](https://hub.docker.com/_/wordpress)
- Firefox or Chrome (we will be using Firefox in this example)

Aside from these steps, you will need to properly [Configure](/guide/getting-started.html) the plugin.

## Correlating a WordPress Login

In this example you will be able to see how to correlate a login flow in WordPress. The steps to do this are:

1. Go to your login page
2. Set the credentials
3. Click on login
4. (Once inside) Click logout
5. Return to your login page

*In some cases*, a normal HTTP Recorder would do that flow without any problem. The thought part comes, when the
application also validates dynamic variables like:

- Time of the login
- Session ID
- Form "secret" validation token

If you maintain the same values that you had **The First time you recorded**, the validations will kick in, and the
application, will kick you out, not allowing you to finish the flow as expected.

That's the moment when the **Correlations Recorder Plugin** comes in. With some minor configurations, those dynamic
values can be captured while you do the recording, and replaced in the following requests, so the app feels and
behaves the way it would do, if it was you who logged 1000 times in a minute for your load testing.

### Let's start

First, we need to start our WordPress instance. To do that, we will use the following command:

```bash
docker run --name wordpress -p 8080:80 -d wordpress
```

This will start a WordPress instance in your local machine, in the port 8080. You can check that it is running by
going to [http://localhost:8080](http://localhost:8080).

Once you have the instance running, you can go to the login page by going to [http://localhost:8080/wp-login.php](http://localhost:8080/wp-login.php).

### Recording

Now that we have our WordPress instance running, we can start recording. To do that, we will use the following
steps:

1. Open JMeter
2. Go to the Plugins Manager
3. Install the Correlation Recorder Plugin
4. Restart JMeter
5. Open the Correlation Recorder Plugin
6. Start the recording
7. Go to the login page
8. Set the credentials
9. Click on login
10. (Once inside) Click logout
11. Return to your login page
12. Stop the recording

### Automatic Correlation

Once you have the recording done, the plugin will offer to perform an automatic correlation. This will try to
replay your recording, if it is successful (*), no correlation will be needed, otherwise, it will:

1. Inform you of the amount of errors in your recording
2. Ask if you want to perform a correlation
3. Let you select the correlation method you want to use
4. Generate Correlation Suggestions for you

(*) The replay will be successful if the response is a 2XX or 3XX HTTP code.

::: warning
If the replay contains failed requests, the plugin will check if those failed requests are the same as the ones
that failed in the recording. If they are, it will not be count toward the error count.
:::

#### Correlating by Correlation Template

Assuming the replay in the previous section failed, you will be able to see the following window:

![Replay error message]()

If you click on the "Yes" button, you will be able to see the following window:

![Select which correlation method]()

Let's select the "Correlate by Correlation Template" option, since it is the easiest one to understand and the
most accurate one.

Once you select the option, you will be able to see the following window:

![Correlation by Template]()

In this window, you will be able to see the following:

- The correlation templates that are available for you to use (in this case, we only have the Siebel Correlation
  Template and the WordPress Correlation Template)
- The information of the correlation template that you selected
- The version of each correlation template
- The JTL file that you want to use for the analysis

In this case, we will select the WordPress Correlation Template, the version 1.0.0, and the JTL file that we
recorded.