package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

public class DrawableFunction extends DrawableComponent {

  private String name;

  private double xOffset;
  private double yOffset;

  public DrawableFunction(Color color) {
    super(color);
  }

  public boolean intersectsWith(final Point p, int radius, FunctionPlotter parent) {
    return false;
  }

  @Override
  public void draw(Graphics gc, FunctionPlotter parent) {
    // TODO Auto-generated method stub
  }

  public String getName() {
    return name;
  }
}
