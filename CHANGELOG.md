# Changelog
All notable changes to this project will be documented in this file.

## [2.0.1]

- fix: improved error messages ([#124](https://github.com/crowdin/android-studio-plugin/pull/124))

## [2.0.0]

- Plugin UI redesign
- New configuration file format
- String-based projects support
- Plugin settings added
- Upgraded dependencies

([#119](https://github.com/crowdin/android-studio-plugin/pull/119),
[#121](https://github.com/crowdin/android-studio-plugin/pull/121))

## [1.6.3]

- fix: branch remove special characters ([#114](https://github.com/crowdin/android-studio-plugin/pull/114))
- chore: bump com.github.crowdin:crowdin-api-client-java ([#116](https://github.com/crowdin/android-studio-plugin/pull/116))

## [1.6.2]

- ci: use GH Actions ([#106](https://github.com/crowdin/android-studio-plugin/pull/106))
- Remove Project constructor argument of Crowdin ([#100](https://github.com/crowdin/android-studio-plugin/pull/100))
- Introduce CrowdinClient as base type ([#100](https://github.com/crowdin/android-studio-plugin/pull/100))
- Add tests for CrowdinProjectCacheProvider.getInstance() ([#100](https://github.com/crowdin/android-studio-plugin/pull/100))
- updated to new logo ([#111](https://github.com/crowdin/android-studio-plugin/pull/111))

## [1.6.1]

- Add import options for translations upload ([#81](https://github.com/crowdin/android-studio-plugin/pull/81))
- Fixed typo in UIUtil:confirmDialog method name ([#80](https://github.com/crowdin/android-studio-plugin/pull/80))
- Added unit tests to improve code coverage for the CrowdinFileUtil.java ([#87](https://github.com/crowdin/android-studio-plugin/pull/87))
- Added unit tests to improve code coverage for RetryUtil.java ([#88](https://github.com/crowdin/android-studio-plugin/pull/88))
- Add LanguageMapping tests ([#89](https://github.com/crowdin/android-studio-plugin/pull/89))
- Added Test cases for PlaceholderUtil.class ([#90](https://github.com/crowdin/android-studio-plugin/pull/90))
- Added unit tests to improve code coverage for FileUtil.java ([#94](https://github.com/crowdin/android-studio-plugin/pull/94))
- Added unit test for util.java ([#96](https://github.com/crowdin/android-studio-plugin/pull/96))
- Added Unit Test Case For CrowdinProjectCacheProvider ([#98](https://github.com/crowdin/android-studio-plugin/pull/98))
- Added Unit Test Case For CrowdinPropertiesLoader ([#101](https://github.com/crowdin/android-studio-plugin/pull/101))
- Added unit tests for CrowdinSettings.java ([#103](https://github.com/crowdin/android-studio-plugin/pull/103))

## [1.6.0]

- Added: String keys autocompletion ([#75](https://github.com/crowdin/android-studio-plugin/pull/75))

## [1.5.1]

- Added: Add new context actions and refactor code ([#71](https://github.com/crowdin/android-studio-plugin/pull/71))
- Fixed: Fix 'download sources' for Windows ([#72](https://github.com/crowdin/android-studio-plugin/pull/72))

## [1.5.0]

- Added: Add 'Download Sources' command ([#65](https://github.com/crowdin/android-studio-plugin/pull/65))
- Added: Add 'labels' and 'excluded-target-languages' parameters and more unifying for uploading sources process ([#65](https://github.com/crowdin/android-studio-plugin/pull/65))
- Added: Add exception handling to getting git branch name ([#61](https://github.com/crowdin/android-studio-plugin/pull/61))

## [1.4.1]

- Updated: Update Crowdin API client to 1.3.10

## [1.4.0]

- Added: 'Project progress' UI ([#55](https://github.com/crowdin/android-studio-plugin/pull/55))

## [1.3.1]

- Updated: Improve error wrapping for api client ([#52](https://github.com/crowdin/android-studio-plugin/pull/52))
- Fixed: Fix deprecated API usage ([#51](https://github.com/crowdin/android-studio-plugin/pull/51))

## [1.3.0]

- Added: Language Mapping support ([#49](https://github.com/crowdin/android-studio-plugin/pull/49))

## [1.2.1]

- Added: Debug mode. To enable add `debug=true` to the configuration file
- Updated: Improve for finding translation files
- Fixed: Fix escaping for Windows OS

## [1.2.0]

- Added: `preserve-hierarchy` config option
- Added: Environment variables support
- Added: Upload translations confirm dialog
- Added: Warning to 'download' command about omitted translations
- Updated: Deprecated API usage and fix compatibility with versions prior to 181+
- Updated: Improve error message for wrong branch name
- Other fixes and improvements

## [1.1.0]
- Added: Upload Translation to Crowdin
- Added: Set custom directory to sources and translations files
- Added: Wrapped plugin actions into background tasks
- Added: Crowdin icon for menu
- Updated: Improved base path validation
- Fixed: Work in multiple projects
- Fixed: Removed error notification if project does not have crowdin property file

## [1.0.0]
- Updated: Migrated project to gradle-based intellij plugin
- Updated: API v2 Support
- Updated: Notifications improve
- Updated: Refactoring and fixes
- Fixed: `auto-upload` disabling

## [0.5.10]
- Fix bug with disabling auto-upload

## [0.5.9]
- Add `disable-branches` parameter
- Add `auto-upload` parameter

## [0.5.8]
- Updated languages mapping

## [0.5.7]
- Added languages mapping

## [0.5.6]
- Bug fixes

## [0.5.5]
- A new possibility to customize source files

## [0.5.4]
- Bug fixes

## [0.5.3]
- Added new header for requests

## [0.5.2]
- Bug fixes

## [0.5.1]
- Updated documentation

## [0.5.0]
- Published plugin
