# Stylint Plugin

This plugin provides integration for [Stylint](https://github.com/rossPatton/stylint) inside Intellij IDEA editors family by showing errors and warnings inside the editor.

![Stylint-plugin example](docs/example.png)

## How to use
### Requirements

- Java JDK = 8u74
- Intellij branch 145: `IntelliJ IDEA 2016.1, WebStorm 2016.1, PhpStorm 2016.1`
- `stylint` >= 1.3.7
    - PR#265 for column number and displaying error in the right place
    - PR#264 for rule name display
    - Until PRs are merged you can use my dev integration branch: [#sertae-stylint](https://github.com/sertae/stylint/tree/sertae-stylint)
- `stylint-json-reporter`

### Install required node modules

Plugin require [stylint](https://github.com/rossPatton/stylint) and [stylint-json-reporter](https://github.com/sertae/stylint-json-reporter) to do the hard work.

```
npm install -g stylint stylint-json-reporter
```

### Install plugin

Download  JAR file from [releases](https://github.com/sertae/stylint-plugin/releases) section. Then follow JetBrains [Installing Plugin from Disk](https://www.jetbrains.com/help/webstorm/2016.1/installing-plugin-from-disk.html?origin=old_help) instructions. Plugin is not yet available in JetBrains plugins repository. It's not tested enough.

### Configure plugin settings page
To get started, you need to set the Stylint plugin settings:

* Go to preferences, Stylint plugin page and check the Enable plugin.
* Select the path to the Stylint executable.
* Set the `.stylintrc` file, or Stylint will use the default settings.
* By default, Stylint plugin annotate the editor with warning or error based on the Stylint configuration, you can check the 'Treat all stylint issues as warnings' checkbox to display all issues from stylint as warnings.
* Stylint plugin will also try to highlight error in the right place. If you prefer more verbose display you can check the 'Highlight whole line' setting. Otherwise Stylint plugin will highlight whole lines only for those errors where more precise display was not possible.
* Show column number setting is mostly for debugging, but maybe someone find it useful.

Configuration:

![Stylint-plugin config](docs/settings.png)

Inspection:

![Stylint-plugin rule example](docs/highlight.png)

## Changelog

#### [0.1.0] - 2016-03-29

First public version of styling-plugin

## Credit

Thanks @idok for two great plugins [scss-lint-plugin](https://github.com/idok/scss-lint-plugin) and [eslint-plugin](https://github.com/idok/eslint-plugin) on which stylint-plugin is build on.

## License

MIT (c) Wojciech Czerniak