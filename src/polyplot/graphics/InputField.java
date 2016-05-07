package polyplot.graphics;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author Jannik
 */
public class InputField extends DrawableComponent {

    private String prompt;
    private StringBuilder input;
    private Font font;
    private Performer toPerform;
    private FunctionPlotter client;
    private List<String> output;

    private boolean working;
    private boolean keepField;


    public InputField(Color foreground, Color background, boolean hidden, FunctionPlotter parent) {
        super(foreground, background, hidden);
        working = false;
        input = new StringBuilder();
        output = new LinkedList<>();
    }

    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {
        if (super.hidden) {
            parent.getBoundOffset().bottom = 0;
            return;
        }

        final int margin = 6;
        final int fontHeight = gc.getFontMetrics().getHeight(), upperY = parent.getHeight()-margin-fontHeight;
        parent.getBoundOffset().bottom = margin+fontHeight;
        Font origin = gc.getFont();
        font = new Font(Font.MONOSPACED, Font.PLAIN, origin.getSize());

        // Draw Box
        gc.setColor(super.background);
        gc.fillRect(0, upperY, parent.getWidth(), upperY);
        gc.setColor(super.foreground);
        gc.drawLine(0, parent.getHeight()-margin-fontHeight, parent.getWidth(), parent.getHeight()-margin-fontHeight);


        int promptWidth = gc.getFontMetrics().stringWidth(prompt) + margin;

        gc.setFont(font.deriveFont(UNDERLINED));

        // Draw prompt
        gc.drawString(prompt, margin/2, parent.getHeight()-margin);
        gc.setFont(font);
        gc.drawString(":", promptWidth, parent.getHeight()-margin);
        promptWidth+= margin*2;

        // Draw input
        gc.drawString(input.toString(), promptWidth, parent.getHeight()-margin);

        // Draw "caret"
        final int xCaret = gc.getFontMetrics().stringWidth(input.toString()) + promptWidth + 2;
        gc.drawLine(xCaret, parent.getHeight()-margin, xCaret, parent.getHeight()-fontHeight-2);

        // Draw output
        if (output != null && output.size() > 0)
            drawOutput(margin, upperY, gc, parent);

        gc.setFont(origin);

    }

    private void drawOutput(int margin, int upperY, Graphics gc, FunctionPlotter parent) {
        final int fontHeight = gc.getFontMetrics().getHeight();
        int boxHeight = output.size()*fontHeight+margin;
        int yPos = upperY-boxHeight-margin;

        // Draw box
        gc.setColor(background);
        gc.fillRect(0, yPos, parent.getWidth(), boxHeight+margin);
        gc.setColor(foreground);
        gc.drawLine(0, yPos, parent.getWidth(), yPos);

        yPos += margin/2;
        for (String s : output)
            gc.drawString(s, margin/2, yPos+=fontHeight);


    }

    public void read(String prompt, boolean keepField, Performer p, FunctionPlotter parent) {
        if (working)
            return;

        this.prompt = prompt;
        this.input.setLength(0);

        parent.setReceiver(this.new Receiver());
        this.client = parent;
        this.toPerform = Objects.requireNonNull(p);
        this.working  = true;
        super.hidden = false;
        this.keepField = keepField;
    }

    public void append(char c) {
        input.append(c);
    }

    public void remove() {
        if (input.length() > 0)
            input.setLength(input.length()-1);
    }

    public void clear() {
        input.setLength(0);
    }

    public void approve() {
        List<String> output = toPerform.perform(getArguments(input));
        if (output.size() == 0 && !keepField)
            cancel();
        else {
            this.input.setLength(0);
            this.output = output;
        }
    }

    public void cancel() {
        this.input.setLength(0);
        this.prompt = "";
        this.working = false;
        super.hidden = true;
        this.toPerform = null;
        this.output.clear();
        this.font = null; // Dirty & Fast fix of "first char"
        client.setReceiver(null);
    }

    private List<String> getArguments(CharSequence input) {
        LinkedList<String> result = new LinkedList<>();
        StringBuilder argument = new StringBuilder();

        boolean doubleQuoted = false, simpleQuoted = false;
        int lastCp = 0;
        for (int cp : input.chars().toArray()) {
            if (lastCp == '\\')
                argument.appendCodePoint(cp);
            else if (cp == '"')
                if (simpleQuoted)
                    argument.appendCodePoint(cp);
                else
                    doubleQuoted = !doubleQuoted;
            else if (cp == '\'')
                if (doubleQuoted)
                    argument.appendCodePoint(cp);
                else
                    simpleQuoted = !simpleQuoted;
            else if (Character.isWhitespace(cp))
                if (doubleQuoted || simpleQuoted)
                    argument.appendCodePoint(cp);
                else if (Character.isWhitespace(lastCp))
                    continue;
                else {
                    result.add(argument.toString());
                    argument.setLength(0);
                }
            else
                argument.appendCodePoint(cp);
            lastCp = cp;
        } // end for
        if (argument.length() > 0)
            result.add(argument.toString());

        return result;
    }

    @FunctionalInterface
    /**
     * Determines what happens to the input of a input field.
     */
    public interface Performer {
        /**
         * Performs the input of the {@link InputField}.
         *
         * @param arguments
         *      the argument that have been extracted from the input.
         *      The input is split at every whitespace.
         * @return
         *      the output after the performing is done.
         */
        List<String> perform(List<String> arguments);
    }

    private class Receiver extends KeyAdapter {

        private boolean wasCaptured = false;

        @Override
        public void keyTyped(KeyEvent e) {
            final char ch = e.getKeyChar();
            if (!wasCaptured && font != null && font.canDisplay(ch))
                append(e.getKeyChar());
        }

        @Override
        public void keyPressed(KeyEvent e) {
            wasCaptured = true;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:
                    remove();
                    break;
                case KeyEvent.VK_DELETE:
                    clear();
                    break;
                case KeyEvent.VK_ENTER:
                    approve();
                    break;
                case KeyEvent.VK_ESCAPE:
                    cancel();
                    break;
                default:
                    wasCaptured = false;
            }
        }
    }
}
