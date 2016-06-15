package polyplot.graphics;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A component that represents a input field the user can use to input data.<br>
 * The input field can process the data and output given output. The output can
 * be formatted as errors, output or the normal output color.
 *
 * @author Jannik
 */
public class InputField extends DrawableComponent {

    private TitledBorder title;
    private Consumer<String> toPerform;
    private FunctionPlotter client;

    private final TransparentTextArea inputField;
    private final TransparentTextPane outputField;

    private boolean working;
    private boolean hideOutput;
    private boolean keepField;
    private boolean clearOutput;

    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 11);
    private final AttributeSet DEFAULT;
    private final AttributeSet OUTPUT;
    private final AttributeSet ERROR;


    InputField(Color foreground, Color background, boolean hidden,
               Color outputColorDefault, Color outputColorOutput, Color outputColorError,
               FunctionPlotter parent) {
        super(foreground, background, hidden);
        working = false;
        hideOutput = true;


        inputField = new TransparentTextArea();
        outputField = new TransparentTextPane();

        inputField .setVisible(false);
        inputField.setForeground(super.foreground);
        inputField.setBackground(super.background);
        inputField.setCaretColor(super.foreground);
        outputField.setForeground(super.foreground);
        outputField.setBackground(super.background);
        outputField.setCaretColor(super.background);
        outputField.setVisible(false);
        inputField.setEditable(true);
        inputField.setFont(FONT);
        outputField.setFont(FONT);
        outputField.setContentType("text/plain");
        outputField.setEditable(false);

        JPanel tmpPanel = new JPanel(new BorderLayout());
        inputField.setOpaque(false);
        outputField.setOpaque(false);
        tmpPanel.setOpaque(false);
        tmpPanel.add(inputField, BorderLayout.SOUTH);
        tmpPanel.add(outputField, BorderLayout.NORTH);

        parent.getOverlay().add(tmpPanel, BorderLayout.SOUTH);

        DEFAULT = new SimpleAttributeSet();
        StyleConstants.setForeground((MutableAttributeSet) DEFAULT, outputColorDefault);
        StyleConstants.setBackground((MutableAttributeSet) DEFAULT, background);
        StyleConstants.setFontFamily((MutableAttributeSet) DEFAULT, Font.MONOSPACED);
        StyleConstants.setFontSize((MutableAttributeSet) DEFAULT, 11);
        OUTPUT = new SimpleAttributeSet(DEFAULT);
        StyleConstants.setForeground((MutableAttributeSet) OUTPUT, outputColorOutput);
        ERROR = new SimpleAttributeSet(DEFAULT);
        StyleConstants.setForeground((MutableAttributeSet) ERROR, outputColorError);

        Border tmpBorder = BorderFactory.createMatteBorder(1,0,0,0, foreground);

        title = BorderFactory.createTitledBorder(tmpBorder);

        title.setTitleFont(FONT);
        title.setTitleColor(foreground);
        title.setTitleJustification(TitledBorder.LEFT);

        inputField.setBorder(title);
        inputField.updateBorderOffset();
        outputField.setBorder(tmpBorder);
        inputField.addKeyListener(new Receiver());

    }

    @Override
    public void setHidden(boolean hidden) {
        inputField.setVisible(!hidden);
        outputField.setVisible(!hidden && !hideOutput);
        super.setHidden(hidden);
    }

    @Override
    public void toggleHidden() {
        super.toggleHidden();
        inputField.setVisible(!hidden);
        outputField.setVisible(!hidden && !hideOutput);
    }

    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {
        if (working && !inputField.hasFocus() && !outputField.hasFocus())
            inputField.requestFocusInWindow();
        inputField.setVisible(!hidden);
        outputField.setVisible(!hidden && !hideOutput);

        if (super.hidden) {
            parent.setBoundOffsetBottom(0);
        } else {
            parent.setBoundOffsetBottom(inputField.getAdjustedHeight());
            outputField.setMaximumSize(new Dimension(parent.getWidth(), parent.getHeight()/2));
        }
    }

    void read(String prompt, boolean keepField, boolean clearOutput, Consumer<String> toPerform, FunctionPlotter parent) {
        if (working)
            return;

        this.title.setTitle(prompt);

        this.client = parent;
        parent.enableKeyBindings(false);
        this.toPerform = Objects.requireNonNull(toPerform);
        this.working  = true;
        super.hidden = false;
        this.hideOutput = true;
        this.keepField = keepField;
        this.clearOutput = clearOutput;
    }

    void outputLine(String output) {
        try {
            postString(output, DEFAULT);
        } catch (BadLocationException e) {
            outputException(e);
        }
    }

    void outputOutput(String output) {
        try {
            postString(output, OUTPUT);
        } catch (BadLocationException e) {
            outputException(e);
        }
    }

    void outputError(String output) {
        try {
            postString(output, ERROR);
        } catch (BadLocationException e) {
            outputException(e);
        }
    }

    void outputException(Throwable t) {
        try {
            String result;

            if (client.isDebugActive()) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);

                result = sw.toString().replaceAll("\t", "    ");
            } else {
                if (t.getMessage() == null || t.getMessage().trim().isEmpty())
                    result = t.getClass().getSimpleName();
                else
                    result = t.getMessage();
            }
            postString(result, ERROR);

        } catch (Throwable throwable) {
            throwable.addSuppressed(t);
            throwable.printStackTrace();
        }
    }

    private void postString(String output, AttributeSet style)
            throws BadLocationException {
        if (outputField.getText().trim().isEmpty()) {
            outputField.setText("");
            outputField.getDocument().insertString(outputField.getDocument().getLength(),
                    output.trim(), style);
        } else
            outputField.getDocument().insertString(outputField.getDocument().getLength(),
                    "\n" + output.trim(), style);

    }

    void clearOutput() {
        outputField.setText("");
    }

    void approve() {
        if (clearOutput)
            outputField.setText("");
        toPerform.accept(inputField.getText());
        hideOutput = outputField.getText().trim().isEmpty();
        inputField.updateBorderOffset();
        if (hideOutput && !keepField)
            cancel();
        else {
            this.inputField.setText("");
        }
    }

    void cancel() {
        inputField.setText("");
        inputField.setVisible(false);
        outputField.setText("");
        outputField.setVisible(false);
        this.title.setTitle("");
        this.working = false;
        super.hidden = true;
        this.toPerform = null;
        client.enableKeyBindings(true);
    }

    static List<String> getArguments(CharSequence input) {
        LinkedList<String> result = new LinkedList<>();
        StringBuilder argument = new StringBuilder();

        boolean doubleQuoted = false, simpleQuoted = false;
        int lastCp = 0x20;
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

    private class Receiver extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    if (e.getModifiers() == 0) {
                        approve();
                    } else {
                        inputField.append("\n");
                        inputField.repaint();
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_ESCAPE:
                    cancel();
                    e.consume();
                    break;
            }
        }
    }

    private class TransparentTextArea extends JTextArea {

        private Color background;

        private int borderOffset;

        public TransparentTextArea() {
            super.setBackground(Color.BLACK);
            this.background = new Color(0x00, true);
        }

        @Override
        public void setBackground(Color background) {
            this.background = background;
        }

        @Override
        protected void paintComponent(Graphics g) {
            updateBorderOffset();
            g.setColor(background);
            g.fillRect(0, borderOffset, super.getWidth(), super.getHeight());
            super.paintComponent(g);
        }

        final private void updateBorderOffset() {
            if (hideOutput) {
                borderOffset = this.getBorder().getBorderInsets(this).top/2;
            }
            else
                borderOffset = 0;
        }

        public int getAdjustedHeight() {
            return super.getHeight()-borderOffset;
        }
    }

    private class TransparentTextPane extends JTextPane {

        private Color background;

        public TransparentTextPane() {
            super();
            super.setBackground(Color.BLACK);
            this.background = new Color(0x00, true);
        }

        @Override
        public void setBackground(Color background) {
            this.background = background;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(background);
            g.fillRect(0, 0, super.getWidth(), super.getHeight());
            super.paintComponent(g);
        }
    }
}
