## Crowdin AndroidStudio Plugin

The plugin lets you integrate android project with Crowdin. It enables you to upload new source strings to the system instantly as well as download translations from your Crowdin project.

To start using this plugin, create a file with project credentials named **crowdin.properties** in the root directory of the project.

```
project-identifier=your-project-identifier
project-key=your-project-key
```

Plugin will automatically find `strings.xml` file in the values directory and if renewed it will be uploaded to Crowdin instantly.
If you have more source files or the source file's name is other than `strings.xml` please specify this in the `sources` parameter.

```
sources=file1.xml, file2.xml
```

For Android Studio projects that use a git VCS, the plugin will automatically create corresponding branches in Crowdin. 
If you do not use branches feature in Crowdin, add `disable-branches=true` parameter into the configuration file.

```
disable-branches=true
```

To download translations from Crowdin, choose in menu: `Tools > Crowdin > Download`. Translations will be exported to the `Resources` folder.

### Workflow
* Install plugin via [JetBrains Plugin repository](https://plugins.jetbrains.com/idea/plugin/9463-crowdin).
* Plugin automatically detects the file with sources strings ("\*\*/values/strings.xml"). If changed, the file will be updated in Crowdin itself.
* Source file can also be manually uploaded to Crowdin via menu `Tools > Crowdin > Upload` or just select `Upload to Crowdin` option using the Right Mouse clicking on the file.
* To download translations use menu `Tools > Crowdin > Download`. Translations will be exported to the resource folder (\*\*/resources/values-uk/strings.xml, \*\*/resources/values-fr/strings.xml, ...)

### Change log
**Version 0.5.9**
+ Add `disable-branches` parameter

**Version 0.5.8**
+ Updated languages mapping

**Version 0.5.7**
+ Added languages mapping

**Version 0.5.6**
+ Bug fixes

**Version 0.5.5**
+ A new possibility to customize source files

**Version 0.5.4**
+ Bug fixes

**Version 0.5.3**
+ Added new header for requests

**Version 0.5.2**
+ Bug fixes

**Version 0.5.1**
+ Updated documentation

**Version 0.5.0**
+ Published plugin

### Seeking Assistance
Need help working with Crowdin CLI or have any questions? <a href="https://crowdin.com/contacts" target="_blank">Contact Customer Success Service</a>.

### Contributing
1. Fork it
2. Create your feature branch (git checkout -b my-new-feature)
3. Commit your changes (git commit -am 'Added some feature')
4. Push to the branch (git push origin my-new-feature)
5. Create new Pull Request

### License and Author
Author: Ihor Popyk (ihor.popyk@crowdin.com)
Copyright: 2017 crowdin.com
This project is licensed under the MIT license, a copy of which can be found in the LICENSE file.
