[<p align="center"><img src="https://support.crowdin.com/assets/logos/crowdin-dark-symbol.png" data-canonical-src="https://support.crowdin.com/assets/logos/crowdin-dark-symbol.png" width="200" height="200" align="center"/></p>](https://crowdin.com)

# Crowdin Android Studio Plugin

The plugin lets you integrate android project with Crowdin. It enables you to upload new source strings to the system instantly as well as download translations from your Crowdin project.

## Status

[![Build Status](https://dev.azure.com/crowdin/Android%20Studio%20Plugin/_apis/build/status/Build?branchName=master&cacheSeconds=1000)](https://dev.azure.com/crowdin/Android%20Studio%20Plugin/_build/latest?definitionId=23&branchName=master)
[![GitHub](https://img.shields.io/github/license/crowdin/android-studio-plugin?cacheSeconds=50000)](https://github.com/crowdin/android-studio-plugin/blob/master/LICENSE)

## Getting started

* Install plugin via [JetBrains Plugin repository](https://plugins.jetbrains.com/idea/plugin/9463-crowdin).
* Plugin automatically detects the file with sources strings (`\*\*/values/strings.xml`). If changed, the file will be updated in Crowdin itself.
* Source file can also be manually uploaded to Crowdin via menu `Tools > Crowdin > Upload` or just select `Upload to Crowdin` option using the Right Mouse clicking on the file.
* To download translations use menu `Tools > Crowdin > Download`. Translations will be exported to the resource folder (`\*\*/resources/values-uk/strings.xml`, `\*\*/resources/values-fr/strings.xml`, ...)

---

:bookmark_tabs: For version *0.5.x* see the [branch 0.5.x](https://github.com/crowdin/android-studio-plugin/tree/0.5.x). Please note that these versions are no longer supported.

:notebook: Complete list of changes: [CHANGELOG.md](/CHANGELOG.md)

:exclamation: Migration from version *0.5.x* to *1.x.x* requires changes in your *crowdin.properties* file.

---

## Configuration

To start using this plugin, create a file with project credentials named *crowdin.properties* in the root directory of the project.

```ini
project_id=your-project-identifier
api_token=your-api-token
```

`project_id` - This is a project *numeric id*

`api_token` - This is a *personal access token*. Can be generated in your *Account Settings*

If you are using Crowdin Enterprise, you also need to specify `base_url`:

```ini
base_url=https://{organization-name}.crowdin.com
```

Plugin will automatically find `strings.xml` file in the values directory and if renewed it will be uploaded to Crowdin instantly.
If you have more source files or the source file's name is other than `strings.xml` please specify this in the `sources` parameter.

```ini
sources=file1.xml, file2.xml
```

For Android Studio projects that use a git VCS, the plugin will automatically create corresponding branches in Crowdin.
If you do not use branches feature in Crowdin, use `disable-branches` parameter:

```ini
disable-branches=true
```

To prevent automatic file upload to Crowdin use `auto-upload`:

```ini
auto-upload=false
```

To download translations from Crowdin, choose in menu: `Tools > Crowdin > Download`. Translations will be exported to the `Resources` folder.

## Seeking Assistance

If you find any problems or would like to suggest a feature, please read the [How can I contribute](/CONTRIBUTING.md#how-can-i-contribute) section in our contributing guidelines.

Need help working with Crowdin Android Studio Plugin or have any questions? [Contact](https://crowdin.com/contacts) Customer Success Service.

## Contributing

If you want to contribute please read the [Contributing](/CONTRIBUTING.md) guidelines.

## Authors

* Ihor Popyk (ihor.popyk@crowdin.com)
* Yevheniy Oliynyk (evgen41293@gmail.com)

## License
<pre>
The Crowdin Android Studio Plugin is licensed under the MIT License. 
See the LICENSE.md file distributed with this work for additional 
information regarding copyright ownership.

Except as contained in the LICENSE file, the name(s) of the above copyright
holders shall not be used in advertising or otherwise to promote the sale,
use or other dealings in this Software without prior written authorization.
</pre>
