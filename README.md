# ZSvgLongPathPlugin

### Description

A plugin to eliminate the long path in svg images that the studio warns you about. 

Unlike other plugins, the logic of this plugin is to split a long path into multiple path tags, if the original path can be split into several by Z character.

### Getting Started

#### How to use?
- Select 1 path tag with long path warning
- Right click on mouse
- In dropdown context menu select plugin action

![How to use animation](https://raw.githubusercontent.com/Mihail-Rain-Heart/ZSvgLongPathPlugin/refs/heads/main/src/main/resources/assets/getting_started.gif)

### Restrictions
Unsupported multi cursor! Also you need select only one tag at a time. Look at the gif.

### Why plugin work finish, but long path still warn?
Maybe you path not contains more than one Z or selection is wrong. You can contact with me via email if you think that bug in logic.

### Why a plugin is not a silver bullet?
The plugin is a workaround to the studio warning and doesn't really affect anything except the svg size.

### Open for revisions
If you need additional functionality, email me or make a pool requester on github :)