import { readFileSync } from "node:fs";
import { viteBundler } from "@vuepress/bundler-vite";
import { searchPlugin } from "@vuepress/plugin-search";
import { themeDefault } from "@vuepress/theme-default";
import { defineUserConfig } from "vuepress/cli";

const { description } = JSON.parse(
  readFileSync(new URL("../../package.json", import.meta.url), "utf-8"),
);
const REPO_URL = "https://github.com/Blazemeter/CorrelationRecorder";
const BASE_PATH = process.env.CI_PAGES_URL
  ? "/jmeter-siebel-plugin/"
  : "/CorrelationRecorder/";

/**
 * @vuepress/theme-default 2.0.0-rc.141 registers its client config with `path.resolve`, which
 * gives a backslash path on Windows that VuePress writes unescaped into a generated `import`, so
 * the build fails there. Forward slashes work on every OS. Remove once the theme normalizes it.
 */
function withPosixClientConfig(theme) {
  return { ...theme, clientConfigFile: theme.clientConfigFile.replaceAll("\\", "/") };
}

export default defineUserConfig({
  /**
   * Ref：https://vuejs.press/reference/config.html
   */
  title: "Auto Correlation Recorder",
  description,
  base: BASE_PATH,

  /**
   * Extra tags to be injected to the page HTML `<head>`
   */
  head: [
    ["meta", { name: "theme-color", content: "#00ace6" }],
    ["meta", { name: "apple-mobile-web-app-capable", content: "yes" }],
    [
      "meta",
      { name: "apple-mobile-web-app-status-bar-style", content: "black" },
    ],
  ],

  bundler: viteBundler(),

  // the former default theme had a built-in header search; the current one expects a plugin
  plugins: [searchPlugin()],

  /**
   * Ref：https://ecosystem.vuejs.press/themes/default/config.html
   */
  theme: withPosixClientConfig(themeDefault({
    logo: "https://raw.githubusercontent.com/Blazemeter/jmeter-bzm-commons/refs/heads/master/src/main/resources/dark-theme/blazemeter-by-perforce-logo.png",
    lastUpdated: false,
    contributors: false,
    nav: [
      { text: "Guide", link: "/guide/" },
      { text: "Templates", link: "/guide/templates/" },
      { text: "Custom Extensions", link: "/guide/custom-extensions/" },
      { text: "Contributing", link: "/contributing/" },
      { text: "FAQ", link: "/guide/troubleshooting.md" },
      { text: "GitHub", link: REPO_URL },
    ],
    sidebar: {
      "/guide/": [
        {
          text: "Guide",
          collapsed: false,
          items: [
            { text: "User Guide", link: "/guide/" },
            { text: "Installing the Plugin", link: "/guide/installation-guide.md" },
            { text: "Usage", link: "/guide/using-the-plugin.md" },
            { text: "Correlation Process", link: "/guide/correlation-process.md" },
            { text: "Automatic Correlation methods", link: "/guide/after-recording.md" },
            {
              text: "Configuring the Auto Correlation Recorder Plugin",
              link: "/guide/analysis-configuration.md",
            },
            { text: "Before the Recording", link: "/guide/before-recording.md" },
            { text: "Concepts", link: "/guide/concepts.md" },
            { text: "Best Practices", link: "/guide/best-practices.md" },
            { text: "Troubleshooting", link: "/guide/troubleshooting.md" },
          ],
        },
        {
          text: "Templates",
          collapsed: false,
          items: [
            { text: "Correlation Templates", link: "/guide/templates/" },
            { text: "Create Template", link: "/guide/templates/create.md" },
          ],
        },
        {
          text: "Custom Extensions",
          collapsed: false,
          items: [{ text: "Custom Extensions", link: "/guide/custom-extensions/" }],
        },
      ],
    },
    footer: {
      message:
        'Made by <a href="https://guide.blazemeter.com/hc/en-us">Blazemeter</a> | Apache 2.0 Licensed | Powered by <a href="https://vuejs.press/">VuePress</a>',
    },
    themePlugins: {
      // lastUpdated and contributors are off, so the git history is not needed
      git: false,
      // no iconify icons are used; when on, every page loads iconify-icon from a public CDN
      icon: false,
    },
  })),
});
