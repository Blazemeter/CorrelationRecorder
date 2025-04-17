const {description} = require("../../package");
const REPO_URL = "https://github.com/Blazemeter/CorrelationRecorder";
const BASE_PATH = buildBaseUrl();

function buildBaseUrl() {
  return process.env.CI_PAGES_URL
      ? "/jmeter-siebel-plugin/"
      : "/CorrelationRecorder/";
}

module.exports = {
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#title
   */
  title: "Auto Correlation Recorder",
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#description
   */
  description: description,
  base: BASE_PATH,

  /**
   * Extra tags to be injected to the page HTML `<head>`
   *
   * ref：https://v1.vuepress.vuejs.org/config/#head
   */
  head: [
    ["meta", {name: "theme-color", content: "#00ace6"}],
    ["meta", {name: "apple-mobile-web-app-capable", content: "yes"}],
    [
      "meta",
      {name: "apple-mobile-web-app-status-bar-style", content: "black"},
    ],
  ],

  /**
   * Theme configuration, here is the default theme configuration for VuePress.
   *
   * ref：https://v1.vuepress.vuejs.org/theme/default-theme-config.html
   */
  themeConfig: {
    repo: REPO_URL,
    editLinks: false,
    docsDir: "",
    editLinkText: "",
    lastUpdated: false,
    logo: "https://raw.githubusercontent.com/Blazemeter/jmeter-bzm-commons/refs/heads/master/src/main/resources/dark-theme/blazemeter-by-perforce-logo.png",
    nav: [
      {
        text: "Guide",
        link: "/guide/",
      },
      {
        text: "Templates",
        link: "/guide/templates/",
      },
      {
        text: "Custom Extensions",
        link: "/guide/custom-extensions/",
      },
      {
        text: "Contributing",
        link: "/contributing/",
      },
      {
        text: "FAQ",
        link: "/guide/troubleshooting.md",
      },
    ],
    sidebar: {
      "/guide/": [
        {
          title: "Guide",
          collapsable: true,
          children: [
            "/guide/",
            "/guide/installation-guide.md",
            "/guide/using-the-plugin.md",
            "/guide/correlation-process.md",
            "/guide/after-recording.md",
            "/guide/analysis-configuration.md",

            "/guide/before-recording.md",
            "/guide/concepts.md",
            "/guide/best-practices.md",
            "/guide/troubleshooting.md",
          ],
        },
        {
          title: "Templates",
          collapsable: true,
          children: ["/guide/templates/", "/guide/templates/create.md"],
        },
        {
          title: "Custom Extensions",
          collapsable: true,
          children: ["/guide/custom-extensions/"],
        },
      ],
    },
  },

  /**
   * Apply plugins，ref：https://v1.vuepress.vuejs.org/zh/plugin/
   */
  plugins: ["@vuepress/plugin-back-to-top", "@vuepress/plugin-medium-zoom"],
};
