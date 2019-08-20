# Ascii Canvas

A simple ASCII-based drawing program.

Users can :
1. create a new canvas
2. draw on the canvas using text based commands
3. quit the program.

#### Commands
| Example | Description |
| --- | --- |
|&nbsp;**C**&nbsp;&nbsp;&nbsp;{width}&nbsp;&nbsp;&nbsp;{height}&nbsp;| Create a new canvas of width x height. Note that width and height are always > 0. |
|&nbsp;**L**&nbsp;&nbsp;&nbsp;{x1}&nbsp;{y1}&nbsp;&nbsp;&nbsp;{x2}&nbsp;{y2}&nbsp;| Draw a new line from coordinates (x1, y1) to (x2, y2) horizontally or vertically. |
|&nbsp;**R**&nbsp;&nbsp;&nbsp;{x1}&nbsp;{y1}&nbsp;&nbsp;&nbsp;{x2}&nbsp;{y2}&nbsp;| Draw a new rectangle, with upper left corner at coordinate (x1,y1) and lower right coordinate at (x2, y2). |
| **Q** | Quit the program. |

### Build

```shell
$ sbt clean compile
```

### Unit Tests

```shell
$ sbt test
```

### Running the programm

```shell
$ sbt run
```

### Code Coverage

```shell
$ sbt jacoco
```

### Source code style

[Scalastyle](http://www.scalastyle.org/) is used to verify the code style.

For IntelliJ users, the "**Scala style inspection**" checkbox has to be flagged (_Settings -> Editor -> Inspections -> Scala -> Code Style_)

Code style can be verified while the compilation takes place or by running the command :

```shell
$ sbt scalastyle
```

### Source code formatting

[Scalafmt](https://scalameta.org/scalafmt/) is used to format the code base.

For IntelliJ users, the [scalafmt plugin](https://plugins.jetbrains.com/plugin/8236-scalafmt) should be installed, Scalafmt as **Formatter** selected and the checkbox "**Reformat on file save**" flagged (_Settings -> Editor -> Code Style -> Scala).

Code can be formatted by using the _alt+shift+L_ shortcut (Linux)
