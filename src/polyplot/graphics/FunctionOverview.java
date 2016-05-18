package polyplot.graphics;

import math.CompilationContext;
import math.Function;
import sun.java2d.pipe.PixelToParallelogramConverter;
import sun.java2d.pipe.PixelToShapeConverter;

import java.awt.Color;
import java.awt.Graphics;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jannik
 */
public class FunctionOverview extends DrawableComponent {

    private boolean showOnlyUserDefined;

    private boolean showHidden;


    public FunctionOverview(Color foreground, Color background, boolean hidden,
                            boolean showOnlyUserDefined, boolean showHidden) {
        super(foreground, background, hidden);
        this.showOnlyUserDefined = showOnlyUserDefined;
        this.showHidden = showHidden;
    }

    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {
        if (super.hidden)
            return;

        final CompilationContext context = parent.getFunctionContext();

        List<String> functions = new LinkedList<>();
        List<String> constants = new LinkedList<>();
        List<DrawableFunction> drawableFunctions = parent.getFunctions();

        outer:
        for (Function f : context.getFunctions(showOnlyUserDefined))
            if (!showHidden) {
                for (DrawableFunction df : drawableFunctions)
                    if (df.getFunction().equals(f)) {
                        if (!df.isHidden())
                            functions.add(f.getFullExpression());
                        continue outer;
                    }
                functions.add(f.getFullExpression());
            } else
                functions.add(f.getFullExpression());

        for (CompilationContext.Constant c : context.getConstants(showOnlyUserDefined))
            constants.add(c.getFullExpression());

        System.out.println("_ _ _ Output: _ _ _");
        functions.stream().forEach(System.out::println);
        constants.stream().forEach(System.out::println);
    }
}
