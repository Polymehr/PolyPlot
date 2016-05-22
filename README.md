# PolyPlot
A simple two dimensional function plotter in Java
## Controls
PolyPlot has specific key combinations that allow you to perform certain actions. 
If a key in the following list is in parentheses, it means that the press of that 
key can modify the action. All terms in the Action column that are in parentheses,
will only take effect if that key is pressed.

#### Movement
Input                          | Action
-------------------------------|-------------------------------------------------------
(Shift + ) ←, H                | Move left 10% of width (or 1px)
(Shift + ) ↑, J                | Move up 10% of height (or 1px)
(Shift + ) ↓, K                | Move down 10% of height (or 1px)
(Shift + ) →, L                | Move right 10% of width (or 1px)
\<Mouse Dragging>              | Move coordinate system
(Alt + ) Scroll Up/Down        | Move along x-axis in positive/negative direction (1px)
Shift + (Alt) + Scroll Up/Down | Move along y-axis in positive/negative direction (1px)
0                              | Center the y-axis in the middle of the window
G                              | Center the x-axis in the middle of the window
O                              | Center the ordinate origin in the middle of the window

#### Zoom
Input                          | Action
-------------------------------|-------------------------------------------------------
Ctrl + (Shift) + plus          | Zoom in (10x faster)
Ctrl + (Shift) + minus         | Zoom out (10x faster)
Ctrl + 0                       | Reset Zoom
Ctrl + (Shift) + Scroll Up     | Zoom in (10x faster)
Ctrl + (Shift) + Scroll Down   | Zoom out (10x faster)

#### Control
Input                          | Action
-------------------------------|-------------------------------------------------------
(Shift + ) F                   | Add a function or constant (and keep prompt open)
E                              | Evaluate expression
ESC                            | Close input field

#### View
Input                          | Action
-------------------------------|-------------------------------------------------------
F1                             | Toggle an internal help panel
B                              | Toggle the info box
P                              | Toggle point rendering of functions
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

#### Constants 
Constants can be defined with the following syntax:
``` 
c = 42
d = f(42) * 21
``` 
It is also possible to use defined functions with constant arguments in the definition of a constant.

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

The following constants are already defined and can be used in expressions:
 * pi
 * π
 * e

(All functions and constants are taken from `java.lang.Math`.)
