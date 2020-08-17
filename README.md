[<p align="center"><img src="https://support.crowdin.com/assets/logos/crowdin-dark-symbol.png" data-canonical-src="https://support.crowdin.com/assets/logos/crowdin-dark-symbol.png" width="200" height="200" align="center"/></p>](https://crowdin.com)

# Crowdin Android Studio Plugin [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?url=https%3A%2F%2Fgithub.com%2Fcrowdin%2Fandroid-studio-plugin&text=Manage%20and%20synchronize%20your%20localization%20resources%20with%20Crowdin%20project%20instantly%20from%20IDE)

This plugin lets you integrate Android project with Crowdin. It enables you to upload new source strings to the system instantly as well as download translations from your Crowdin project.

## Status

[![Build Status](https://dev.azure.com/crowdin/Android%20Studio%20Plugin/_apis/build/status/Build?branchName=master&cacheSeconds=1000)](https://dev.azure.com/crowdin/Android%20Studio%20Plugin/_build/latest?definitionId=23&branchName=master)
[![JetBrains IntelliJ plugins](https://img.shields.io/jetbrains/plugin/d/9463-crowdin?cacheSeconds=50000)](https://plugins.jetbrains.com/plugin/9463-crowdin)
[![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/r/stars/9463-crowdin?cacheSeconds=50000)](https://plugins.jetbrains.com/plugin/9463-crowdin)
[![GitHub](https://img.shields.io/github/license/crowdin/android-studio-plugin?cacheSeconds=50000)](https://github.com/crowdin/android-studio-plugin/blob/master/LICENSE)

[![Azure DevOps tests (branch)](https://img.shields.io/azure-devops/tests/crowdin/Android%20Studio%20Plugin/23/master?cacheSeconds=1000)](https://dev.azure.com/crowdin/Android%20Studio%20Plugin/_build/latest?definitionId=23&branchName=master)
[![codecov](https://codecov.io/gh/crowdin/android-studio-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/crowdin/android-studio-plugin)

## Getting started

* Install plugin via [JetBrains Plugin repository](https://plugins.jetbrains.com/idea/plugin/9463-crowdin).
* Plugin automatically detects the file with sources strings. If changed, the file will be updated in Crowdin itself.
* Source file can also be manually uploaded to Crowdin via menu `Tools > Crowdin > Upload Sources` or just select `Upload to Crowdin` option using the Right Mouse clicking on the file.
* To upload translations use menu `Tools > Crowdin > Upload Translations`.
* To download translations use menu `Tools > Crowdin > Download Translations`.

---

:bookmark_tabs: For version *0.5.x* see the [branch 0.5.x](https://github.com/crowdin/android-studio-plugin/tree/0.5.x). Please note that these versions are no longer supported.

:notebook: Complete list of changes: [CHANGELOG.md](/CHANGELOG.md)

:exclamation: Migration from version *0.5.x* to *1.x.x* requires changes in your *crowdin.properties* file.

---

## Configuration

### Credentials

To start using this plugin, create a file with project credentials named *crowdin.properties* in the root directory of the project.

```ini
project-id=your-project-identifier
api-token=your-api-token
```

`project-id` - This is a project *numeric id*

`api-token` - This is a *personal access token*. Can be generated in your *Account Settings*

If you are using Crowdin Enterprise, you also need to specify `base-url`:

```ini
base-url=https://{organization-name}.crowdin.com
```

Also, you could load the credentials from environment variables:

```ini
project-id-env=CROWDIN_PROJECT_ID
api-token-env=CROWDIN_TOKEN
base-url-env=CROWDIN_BASE_URL
```

If mixed, project-id, api-token and base-url are prioritized:

```ini
project-id-env=CROWDIN_PROJECT_ID                   # Low priority
api-token-env=CROWDIN_TOKEN                         # Low priority
base-url-env=CROWDIN_URL                            # Low priority
project-id=your-project-identifier                  # High priority
api-token=your-api-token                            # High priority
base-url=https://{organization-name}.crowdin.com    # High priority
```

### Source files and translations

#### Default behaviour

By default, plugin searches for a source file by `**/values/strings.xml` pattern, and for translation files by `/values-%android_code%/%original_file_name%` pattern.

#### `sources` parameter

If there are multiple source files in the same `values` directory, or if source file has a different name, it can be specified in `sources` parameter:

```properties
sources=file1.xml, file2.xml
```

By default, plugin works as if `sources` parameter were specified like this:

```properties
sources=strings.xml
```

For such a parameter, the passed names are substituted into the `**/values/<source_file>` pattern, while for translations the pattern remains standard – `/values-%android_code%/%original_file_name%`.

#### `files.#.(source|translation)` parameters

For more flexibility, there is `files.#` parameter.

```properties
files.source=/values/*.xml
files.translation=/values-%android_code%/%original_file_name%

files.1.source=/another/path/*.xml
files.1.translation=/another/path-%android_code%/%original_file_name%
```

Example of having multiple source files with the same name:

```properties
preserve-hierarchy=true

files.source=**/values/strings.xml
files.translation=/values-%two_letters_code%/%original_file_name%

files.1.source=app/src/main/res/values/file.xml
files.1.translation=app/src/main/res/values-%android_code%/%original_file_name%
files.2.source=ext/src/main/res/values/file.xml
files.2.translation=ext/src/main/res/values-%android_code%/%original_file_name%
```

**Note**: Both `.source` and `.translation` parts should be specified

**Note**: If `preserve-hierarchy` is set to `true`, plugin adds path to your translation pattern.

```properties
preserve-hierarchy=true

files.source=**/values/strings.xml
files.translation=/values-%two_letters_code%/%original_file_name% #CORRECT
# this will be transformed to 'app/src/main/res/values-%two_letter_code%/%original_file_name%' export pattern for each file
```

#### Placeholders

See the [Placeholders](https://support.crowdin.com/configuration-file/#placeholders) article to put appropriate variables.

**Note**: `%android_code%` placeholder means a format such as `'fr-rCA'` ([<ISO 639-1>](http://www.loc.gov/standards/iso639-2/php/code_list.php) -r[<ISO 3166-1-alpha-2>](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)). When applying format with only two-letter language code such as `'fr'`([<ISO 639-1>](http://www.loc.gov/standards/iso639-2/php/code_list.php)) format, use `%two_letters_code%` placeholder.

**Note**: Currently `%original_path%` placeholder is not supported.

### Additional options

For Android Studio projects that use a git VCS, the plugin will automatically create corresponding branches in Crowdin.
If you do not use branches feature in Crowdin, use `disable-branches` parameter:

```ini
disable-branches=true
```

To prevent automatic file upload to Crowdin use `auto-upload`:

```ini
auto-upload=false
```

If your project contains multiple modules you want to localize with the same source files naming you need to use the following option:

```ini
preserve-hierarchy=true
```

## Seeking Assistance

If you find any problems or would like to suggest a feature, please read the [How can I contribute](/CONTRIBUTING.md#how-can-i-contribute) section in our contributing guidelines.

Need help working with Crowdin Android Studio Plugin or have any questions? [Contact](https://crowdin.com/contacts) Customer Success Service.

## Contributing

If you want to contribute please read the [Contributing](/CONTRIBUTING.md) guidelines.

## Authors

* Ihor Popyk (ihor.popyk@crowdin.com)
* Yevheniy Oliynyk (evgen41293@gmail.com)
* Daniil Barabash (dbarabash42@gmail.com)

## License
<pre>
The Crowdin Android Studio Plugin is licensed under the MIT License. 
See the LICENSE.md file distributed with this work for additional 
information regarding copyright ownership.

Except as contained in the LICENSE file, the name(s) of the above copyright
holders shall not be used in advertising or otherwise to promote the sale,
use or other dealings in this Software without prior written authorization.
</pre>
