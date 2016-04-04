package polyplot.graphics;

import java.awt.Color;

/**
 * Options of the function plotter.<br>
 * Variables should be self-explaining.
 */
class Options {
  
          boolean stretch              = false;
          boolean autoCenter           = true;
  
          boolean infobox              = true;
  private int     functionsInInfobox   = 1; // 0 = No functions; 1 = Only in search radius; 2 = all
  private int     functionSearchRadius = 5;
          int     infoboxAlpha         = 200;
          boolean pixelsInfobox        = false;
  private int     roundInfoBox         = 3;
          boolean dockInfobox          = true;
  
  private int     roundScale           = 3;
  
          Color   backgroundColor      = new Color(0xFFFFFF);
          Color   graphColor           = new Color(0xFF0000);
          Color   scaleColor           = new Color(0x000000);
          int[]   functionColors = {
              0xFF0000, 0x00FF00, 0x0000FF,
              0xFFFF00, 0x00FFFF, 0xFF00FF,
          };
  
  
  
          boolean poleRecognition      = true;
  
          boolean debug                = false;
  
  
          
          
          

  
  
  
  
  
  
  
  public int getFunctionsInInfobox() {
    return functionsInInfobox;
  }

  public void setFunctionsInInfobox(int functionsInInfobox) {
    this.functionsInInfobox = functionsInInfobox%3;
  }

  public int getFunctionSearchRadius() {
    return functionSearchRadius;
  }

  public void setFunctionSearchRadius(int functionSearchRadius) {
    this.functionSearchRadius = functionSearchRadius < 1 ? 5 : functionSearchRadius;
  }

  public int getRoundInfoBox() {
    return roundInfoBox;
  }

  public void setRoundInfoBox(int roundInfoBox) {
    this.roundInfoBox = roundInfoBox;
  }

  public int getRoundScale() {
    return roundScale;
  }

  public void setRoundScale(int roundScale) {
    this.roundScale = roundScale;
  }

  public int getMaxFunctions() {
    return functionColors.length;
  }
  
  
  
}
