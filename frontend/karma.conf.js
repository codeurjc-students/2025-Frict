module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: { }
    },
    jasmineHtmlReporter: { suppressAll: true },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly' }
      ]
    },
    reporters: ['progress', 'kjhtml'],

    // ***** KEY FOR GH ACTIONS *****
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-software-rasterizer']
      }
    },

    restartOnFileChange: true
  });

  // Angular 19's esbuild karma builder calls config.set(cliOptions) AFTER loading
  // karma.conf.js, replacing the reporters array with [html, text-summary] and
  // removing lcovonly (needed by SonarCloud). Freeze the property so lodash's
  // mergeWith assignment is silently ignored while the getter always returns the
  // full reporters list.
  const coverageReporters = [
    { type: 'html' },
    { type: 'text-summary' },
    { type: 'lcovonly' }
  ];
  Object.defineProperty(config.coverageReporter, 'reporters', {
    get: () => coverageReporters,
    set: () => {},
    configurable: true,
    enumerable: true
  });
};
