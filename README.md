<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://support.crowdin.com/assets/logos/symbol/png/crowdin-symbol-cWhite.png">
    <source media="(prefers-color-scheme: light)" srcset="https://support.crowdin.com/assets/logos/symbol/png/crowdin-symbol-cDark.png">
    <img width="150" height="150" src="https://support.crowdin.com/assets/logos/symbol/png/crowdin-symbol-cDark.png">
  </picture>
</p>

# Crowdin Android Studio Plugin [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fcrowdin%2Fandroid-studio-plugin&text=Manage%20and%20synchronize%20your%20localization%20resources%20with%20Crowdin%20project%20instantly%20from%20IDE)

This plugin lets you integrate your Android project with Crowdin. It enables you to upload new source strings to the system instantly as well as download translations or source strings from your Crowdin project.

Also, it allows you to track your Crowdin project translation and proofreading progress directly from the IDE :computer:

The plugin is compatible with all the **JetBrains IDE's** such as PHPStorm, IntelliJ Idea and other :rocket:

<div align="center">

[![Tests](https://github.com/crowdin/android-studio-plugin/actions/workflows/basic.yml/badge.svg)](https://github.com/crowdin/android-studio-plugin/actions/workflows/basic.yml)
[![codecov](https://codecov.io/gh/crowdin/android-studio-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/crowdin/android-studio-plugin)
[![JetBrains IntelliJ plugins](https://img.shields.io/jetbrains/plugin/d/9463-crowdin?cacheSeconds=50000)](https://plugins.jetbrains.com/plugin/9463-crowdin)
[![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/r/stars/9463-crowdin?cacheSeconds=50000)](https://plugins.jetbrains.com/plugin/9463-crowdin)
![GitHub contributors](https://img.shields.io/github/contributors/crowdin/android-studio-plugin?logo=github&cacheSeconds=50000)
[![GitHub](https://img.shields.io/github/license/crowdin/android-studio-plugin?cacheSeconds=50000)](https://github.com/crowdin/android-studio-plugin/blob/master/LICENSE)

</div>

## Getting started

* Install plugin via [JetBrains Plugin repository](https://plugins.jetbrains.com/idea/plugin/9463-crowdin).
* Plugin automatically detects the file with sources strings. If changed, the file will be updated in Crowdin itself.
* Plugin contains 3 tabs in the plugin panel
  * Upload tab that allows you to upload translations and sources
  * Download tab that allows you to download translations and sources or bundles for string-based projects
  * Progress tab that allows you to track your Crowdin project translation and proofreading progress directly from the IDE :computer:
* Uploading and Downloading sources and translations also possible by selecting option using the Right Mouse clicking on the file
* Plugin also provide autocompletion of Crowdin strings keys. It helps to enter correct string key.

## Configuration

### Credentials

To start using this plugin, create a file with project credentials named *crowdin.yml* in the root directory of the project.

```yml
project_id: your-project-identifier
api_token: your-api-token
```

`project_id` - This is a project *numeric id*

`api_token` - This is a *personal access token*. Can be generated in your *Account Settings*

If you are using Crowdin Enterprise, you also need to specify `base_url`:

```yml
base_url: https://{organization-name}.crowdin.com
```

Also, you could load the credentials from environment variables:

```yml
project_id_env: CROWDIN_PROJECT_ID
api_token_env: CROWDIN_TOKEN
base_url_env: CROWDIN_BASE_URL
```

If mixed, `project_id`, `api_token` and `base_url` are prioritized:

```yml
project_id_env: CROWDIN_PROJECT_ID                   # Low priority
api_token_env: CROWDIN_TOKEN                         # Low priority
base_url_env: CROWDIN_URL                            # Low priority
project_id: your_project_identifier                  # High priority
api_token: your-api-token                            # High priority
base_url: https://{organization-name}.crowdin.com    # High priority
```

Options above can also be specific in Crowdin plugin settings `File > Settings > Tools > Crowdin`

### Source files and translations

To define source and translation patterns use `files` key. Example:

```yml
preserve_hierarchy: true

files:
  - source: "app/src/main/res/values/file.xml"
    translation: "app/src/main/res/values-%android_code%/%original_file_name%"
  - source: "ext/src/main/res/values/file.xml"
    translation: "ext/src/main/res/values-%android_code%/%original_file_name%"
```

Use `preserve_hierarchy` if your project contains multiple modules you want to localize with the same source files naming.

**Note**: Both `source` and `translation` keys should be specified

**Note**: If `preserve_hierarchy` is set to `true`, plugin adds path to your translation pattern.

```yml
preserve_hierarchy: true

files:
  - source: "**/values/strings.xml"
    translation: "/values-%two_letters_code%/%original_file_name%" #CORRECT
# this will be transformed to 'app/src/main/res/values-%two_letter_code%/%original_file_name%' export pattern for each file
```

### Additional properties

#### File properties

To attach labels to the uploaded strings use `labels`:

```yml

files:
  - source: "**/values/strings.xml"
    translation: "/values-%two_letters_code%/%original_file_name%"
    labels:
      - android
      - help-menu
```

To specify excluded target languages use `excluded_target_languages`:

```yml
files:
  - source: "**/values/strings.xml"
    translation: "/values-%two_letters_code%/%original_file_name%"
    excluded_target_languages:
      - uk
      - fr
```

To specify cleanup mode or update strings flags for string-based projects use `cleanup_mode` and `update_strings`:

```ini
files:
  - source: "**/values/strings.xml"
    translation: "/values-%two_letters_code%/%original_file_name%"
    update_strings: true
    cleanup_mode: true
```

#### Translations Upload Options

The below properties can be used to configure the import options to the uploaded translations

`import_eq_suggestions` - Defines whether to add translation if it's the same as the source string

`auto_approve_imported` - Mark uploaded translations as approved

`translate_hidden` - Allow translations upload to hidden source strings

```yml
# Applies to the default behavior and all files
import_eq_suggestions: true
auto_approve_imported: true
translate_hidden: true
```

### Placeholders

See the [Placeholders](https://support.crowdin.com/configuration-file/#placeholders) article to put appropriate variables.

**Note**: `%android_code%` placeholder means a format such as `'fr-rCA'` ([<ISO 639-1>](http://www.loc.gov/standards/iso639-2/php/code_list.php) -r[<ISO 3166-1-alpha-2>](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)). When applying format with only two-letter language code such as `'fr'`([<ISO 639-1>](http://www.loc.gov/standards/iso639-2/php/code_list.php)) format, use `%two_letters_code%` placeholder.

**Note**: Currently `%original_path%` placeholder is not supported.

### Additional settings

Plugin settings are located in `File > Settings > Tools > Crowdin`.

### Login

In settings menu you can specify project id, api token and base url. Same options can be defined in `yml` configuration file.  
If those settings will be specified in both places, `yml` and `Settings` menu, plugin will use values from `yml` file.

#### Branch

To specify concrete branch you can use `branch` key in yml file or turn on `Use Git Branch`.
Then plugin will use local Git branch.  
If you do not use branches feature in Crowdin, make sure `Use Git Branch` option is disabled and `branch` is not defined in yml file.  
Keep in mind that branch is required for string-based projects.

#### Automatic uploads

By default, plugin will automatically upload source files to Crowdin on any change.  
To disable this please turn off `Automatically upload on change` option.

#### Strings autocompletion

By default, this autocompletion feature will be enabled in all files. But you can configure files extensions where it should work.  
To enable autocompletion only in json and xml files `File extensions` settings should be set to `json,xml`.  
To disable autocompletion turn off `Enable Autocompletion` option.

## Seeking Assistance

If you find any problems or would like to suggest a feature, please read the [How can I contribute](/CONTRIBUTING.md#how-can-i-contribute) section in our contributing guidelines.

Need help working with Crowdin Android Studio Plugin or have a question? Visit our [Community Forum](https://community.crowdin.com/).

## Contributing

If you would like to contribute please read the [Contributing](/CONTRIBUTING.md) guidelines.

## License
<pre>
The Crowdin Android Studio Plugin is licensed under the MIT License. 
See the LICENSE.md file distributed with this work for additional 
information regarding copyright ownership.

Except as contained in the LICENSE file, the name(s) of the above copyright
holders shall not be used in advertising or otherwise to promote the sale,
use or other dealings in this Software without prior written authorization.
</pre>
