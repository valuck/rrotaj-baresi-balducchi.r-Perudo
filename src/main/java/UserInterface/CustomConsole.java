package UserInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CustomConsole {
    private DefaultListModel<String> prompt;
    private ListSelectionModel selection;
    private JList<String> list;
    private OptionsMenu currentMenu;
    private JTextField input;

    private void initialize(String title, Color foreground, Color background, Font font) {
        if (foreground == null) // Set default values if missing
            foreground = Color.WHITE;

        if (background == null)
            background = Color.BLACK;

        if (font == null)
            font = new Font(Font.MONOSPACED, Font.BOLD, 16);

        JFrame body = new JFrame(); // Initialize GUI
        this.prompt = new DefaultListModel<String>();

        body.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        body.setExtendedState(JFrame.MAXIMIZED_BOTH);
        body.setUndecorated(true);
        body.setTitle(title);

        this.list = new JList<String>(this.prompt);
        this.selection = this.list.getSelectionModel();
        this.list.setBackground(background);
        this.list.setForeground(foreground);
        this.list.setFont(font);

        // Listen for key events
        this.list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_ENTER) { // do action
                    String value = list.getSelectedValue();

                    if (value != null && currentMenu != null)
                        currentMenu.doOption(value);
                }
            }
        });

        // Listen for focus changes
        this.list.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                focusInput(); // Focus input where its required
            }

            @Override
            public void focusLost(FocusEvent e) {
                list.clearSelection(); // Clear the selection
            }
        });

        this.input = new JTextField("> ");
        this.input.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        this.input.setBackground(background);
        this.input.setForeground(foreground);
        this.input.setFont(font);

        // Listen for key events
        this.input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_ENTER) { // Enter command and do action
                    String text = input.getText();

                    if (text.charAt(0) == '>')
                        text = text.substring(1);

                    text = text.trim();
                    println(text);

                    if (currentMenu != null)
                        currentMenu.doOption(text);

                    input.setText("> ");
                    focusInput();
                }
            }
        });

        body.add(this.input, BorderLayout.PAGE_END);
        body.add(this.list, BorderLayout.CENTER);

        // Set mode and display
        this.setTextInput(true);
        body.setVisible(true);
        this.focusInput();
    }

    public CustomConsole(String title) {
        initialize(title, null, null, null);
    }

    public CustomConsole(String title, Color foreground) {
        initialize(title, foreground, null, null);
    }

    public CustomConsole(String title, Color foreground, Color background) {
        initialize(title, foreground, background, null);
    }

    public CustomConsole(String title, Color foreground, Color background, Font font) {
        initialize(title, foreground, background, font);
    }

    public int println(String text) { // Print a new line
        this.prompt.addElement(text);
        return this.prompt.size() -1;
    }

    public void clear() { // Clear the console
        this.currentMenu = null;
        this.prompt.removeAllElements();
    }

    public void edit(String text, int index) { // Edit a prompt
        if (index >= 0 && index < this.prompt.size())
            this.prompt.setElementAt(text, index);
    }

    public void focusInput() {
        // Focus on the used object
        if (this.input.isVisible()) {
            this.input.requestFocus();
            this.input.setCaretPosition(this.input.getDocument().getLength());
        }
        else
            this.list.requestFocus();
    }

    public void setSelectable(boolean selectable) {
        // Makes the list selectable or not
        this.list.setSelectionModel(selectable ? this.selection : new DefaultListSelectionModel() {
            @Override // Removes the selection
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });

        this.list.clearSelection();
    }

    public void setTextInput(boolean enabled) {
        // Switch between text and selection modes
        this.input.setVisible(enabled);
        this.setSelectable(!enabled);

        focusInput();
    }

    public void setBackground(Color background) { // Set background color
        this.list.setBackground(background);
        this.input.setBackground(background);
    }

    public Color getBackground() { // Get background color
        return this.list.getBackground();
    }

    public void setForeground(Color foreground) { // Set foreground color
        this.list.setForeground(foreground);
        this.input.setForeground(foreground);
    }

    public Color getForeground() { // Get foreground color
        return this.list.getForeground();
    }

    public void setFont(Font font) { // Set font
        this.list.setFont(font);
        this.input.setFont(font);
    }

    public Font getFont() { // Get font
        return this.list.getFont();
    }

    public void drawOptionsMenu(OptionsMenu menu) {
        //this.clear();
        this.currentMenu = menu; // Set and draw all options
        menu.getOptions().forEach((value) -> {
            this.println(value);
        });
    }
}
