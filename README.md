# PolyPlot
A simple two dimensional function plotter in Java.
![PolyPlot](https://raw.githubusercontent.com/Polymehr/PolyPlot/master/screenshot.png)

## Controls
PolyPlot has specific key combinations that allow you to perform certain actions. 
If a key in the following list is in parentheses or brackets, it means that the press of that 
key can modify the action. All terms in the Action column that are in parentheses or brackets,
will only take effect if that key is pressed respectively.

#### Movement
Input                          | Action
-------------------------------|----------------------------------------------------------
(Alt) + [Shift] + ← or H       | Move [Function] left 10% of width (or 1px)
(Alt) + [Shift] + ↑ or J       | Move [Function] up 10% of height (or 1px)
(Alt) + [Shift] + ↓ or K       | Move [Function] down 10% of height (or 1px)
(Alt) + [Shift] + → or L       | Move [Function] right 10% of width (or 1px)
\<Mouse Dragging>              | Move coordinate system
(Alt) + Scroll Up/Down         | Move along x-axis in positive/negative direction (1px)
(Alt) + Shift + Scroll Up/Down | Move along y-axis in positive/negative direction (1px)
(Shift) + 0                    | Center the (Function at) y-axis in the middle of the window
(Shift) + G                    | Center the (Function at) x-axis in the middle of the window
(Shift) + O                    | Center the (Function at) ordinate origin in the middle of the window
(Shift) + M                    | Move function; autoselect (or autocomplete) if possible

#### Zoom
Input                          | Action
-------------------------------|----------------------------------------------------------
(Shift) + Ctrl + Plus          | Zoom in (10x faster)
(Shift) + Ctrl + Minus         | Zoom out (10x faster)
Ctrl + 0                       | Reset Zoom
(Shift) + Ctrl + Scroll Up     | Zoom in (10x faster)
(Shift) + Ctrl + Scroll Down   | Zoom out (10x faster)

#### Control
Input                          | Action
-------------------------------|----------------------------------------------------------
(Shift) + A                    | Add a function or constant (and keep prompt open)
(Shift) + F                    | Add a function, nameless possible (and keep prompt open)
(Shift) + C                    | Add a constant, nameless possible (and keep prompt open)
E                              | Evaluate expression
ESC                            | Exit to normal mode
#### View
Input                          | Action
-------------------------------|----------------------------------------------------------
F1                             | Toggle an internal help panel
B                              | Toggle the info box
R                              | Toggle rendering method of functions
D                              | Toggle showing of defined functions and constants

## Adding functions
If you want to add a function, a prompt will open.<br>
Multiple functions or constants can be defined by separating them with `;`.
```
f(x) = x ^ 2; c = 15; g(x) = x + 2
```

### Functions
A function can be added with the function name and a list of the parameters in parentheses, 
an equals sign, and the function term.
``` 
f(x)       = x ^ 2
g(x, y, z) = x + y ^ 2 + z ^ 3
```
It is possible to define multiple parameters. But every function that has more than one parameter 
will not be drawn on the coordinate system. To redefine a function just define it again.

By using the `"Add function"` (Key `F`) dialog instead of the `"Add function or constant"` dialog it is
possible to add a function without specifying a name first.
```
sin(x) + cos(-x)
e ^ x
```
**Attention:** If no name is given `x` will always be assumed as variable. To use multiple variables or
other variable names you still have to use a name.

#### Constants 
Constants can be defined with the following syntax:
``` 
c = 42
d = f(42) * 21
``` 
It is also possible to use defined functions with constant arguments in the definition of a constant.

By using the `"Add constant"` (Key `C`) dialog instead of the `"Add function or constant"` dialog it is
possible also to add a constant without specifying a name first.
```
15 + 9.3
f(17) / 29.58
```

### Pre-defined functions and constants
The following functions are already defined and can be used in expressions:
 * abs(x)
 * acos(x)
 * asin(x)
 * atan(x)
 * atan2(x, y)
 * IEEEremainder(x, y)
 * max(x, y)
 * min(x, y)
 * cbrt(x)
 * ceil(x)
 * cos(x)
 * cosh(x)
 * exp(x)
 * expm1(x)
 * floor(x)
 * log(x)
 * log10(x)
 * log1p(x)
 * round(x)
 * sin(x)
 * sinh(x)
 * sqrt(x)
 * √(x)
 * tan(x)
 * toDegrees(x)
 * toRadians(x)
 * ulp(x)

(The functions internally use the implementation in `java.lang.Math`. `√(x)` is the same as `sqrt(x)`)

The following constants are already defined and can be used in expressions:
 * pi
 * π
 * e

(The constants' values come from `java.lang.Math`. `π` is the same as `pi`.)

## Options and Themes
### Options
PolyPlot searches for the file `options.conf` in the directory of the PolyPlot jar file.

Option key                               | Default value | Description
-----------------------------------------|:-------------:|-----------------------------------------------------------------------------------------------------------
`graphics.function-overview.hide`        | `true`        | Hide the function overview on startup.
`graphics.function-overview.show-hidden` | `false`       | Also show hidden functions in function overview.
`graphics.functions.grab-radius`         | `20`          | Radius around the mouse cursor functions can be grabbed with
`graphics.functions.rendering-method`    | `LINES`       | Startup rendering method of functions.<br> Valid values: `LINES`, `PATH`, `POINTS`
`graphics.info-box.docked`               | `true`        | Dock info box to corners or  to mouse cursor.
`graphics.info-box.function-radius`      | `20`          | Show values of functions around mouse cursor.<br> `-1`: All, `0`: No, else only functions inside `radius`
`graphics.info-box.hide`                 | `true`        | Hide the info box on startup.
`graphics.info-box.show-pixels`          | `false`       | Show y and y pixel values in info box.
`graphics.scale.stretch`                 | `false`       | Stretch longer section of scale instead of adjusting its span.
`graphics.span`                          | `20.0`        | The span of the shorter section of the scale at default zoom.
`graphics.theme`                         | `default`     | The selected theme. See _Theme Section_.
`graphics.zoom-base`                     | `1.05`        | The base of the zoom. Determines the zoom factor.

### Themes
According to the `graphics.theme` option PolyPlot will search for a matching file in the
`./themes/` directory. If the value of the option does not have a file extension `.theme`
will be assumed.<br>
Values defined in a theme file will be overridden by values defined in the options file.

#### Colors
Colors are defined in hexadecimal like in HTML and can be up to 4 bytes (8 characters) long. The bytes 
represent the `red`, `green`, `blue` and `alpha` value of the color in that order.

There are two special color values: `graphics.color.background` which is the reference background 
(transparency will be ignored) and `graphics.scale.color` which serves a reference foreground.
For these special values it is mandatory to specify all `rgb` values. If zeros are missing they will
be filled out. (`FF` -> `0000FF`)

All other color values support only partial definition. The not defined parts will be generated from
the respective reference color.

Character length | Meaning
:---------------:|--------------------------------------------------------------
`0`              | The full reference color will be used.
`1`-`2`          | The value will be used as alpha value on the reference.
`3`-`6`          | The value will be used as `rgb` with an opaque `alpha` value.
`7`-`8`          | The value will be used as `rgba`.
_other_          | The value is invalid.

All theme keys matching `*.background` use `graphics.color.background` as reference and, respectively,
all theme keys matching `*.foreground` use `graphics.scale.color` as reference.<br>
Additionally all theme keys of `graphics.input.output` also use `graphics.scale.color` as reference.

Theme key                               | Default value
----------------------------------------|:------------:
`graphics.cheat-sheet.background`       | `b8`
`graphics.cheat-sheet.foreground`       | `ff`
`graphics.color.background`             | `ffffffff`
`graphics.function-overview.background` | `a0`
`graphics.function-overview.foreground` | `ff`
`graphics.info-box.background`          | `50`
`graphics.info-box.foreground`          | `ff`
`graphics.input.background`             | `7f`
`graphics.input.foreground`             | `ff`
`graphics.input.output-color.default`   | `ff`
`graphics.input.output-color.error`     | `990000`
`graphics.input.output-color.input`     | `3f3f3f`
`graphics.input.output-color.output`    | `4f4f4f`
`graphics.scale.color`                  | `000000ff`

#### Function Colors
The theme key `graphics.functions.colors` requires a specific syntax.
The list must be opened with a opening bracket (`[`) and closed with a closing bracket (`]`). 
The values in between are separated by commata (`,`). Values have the same limitations as
base colors because they do not support transparency and and missing zeros will be filled with
out.<br>
**Attention:** if one color in the list is invalid (no or more than 6 characters) or the list is
empty, it will be regarded invalid and replaced by the default value. (`[ff0000, 00ff00, 0000ff, ffc800, 00ffff, ff00ff]`)

#### Generating Themes
Own themes for PolyPlot can be generated by using the provided templates in `./themes/templates/poly-plot`. 
These templates are written for [Chriskempson's Base16-builder](https://github.com/chriskempson/base16#builder-repositories).
See their GitHib page for details.

### Command Line Options
The following command line options are supported:

 Short | Long        | Arguments     | Description
-------|-------------|---------------|-----------------------------------------------------
`-s`   | `--size`    | `X:Y`         | Set size of the window. `X` and `Y` must be positive
`-o`   | `--options` | `[OPTION]...` | Set overwriting options in `KEY=VALUE` format
`-h`   | `--help`    |               | Display help and exit

