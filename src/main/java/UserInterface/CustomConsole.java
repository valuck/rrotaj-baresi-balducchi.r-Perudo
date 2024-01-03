package UserInterface;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;

public class CustomConsole {
    private DefaultListModel<String> prompt;
    private ListSelectionModel selection;
    private JScrollBar scrollable;
    private JList<String> list;
    private JFrame body;

    private boolean selectable = false;
    private String commandBuffer = "";
    private OptionsMenu currentMenu;
    private int commandBufferId = 0;
    private int optionsMax = -1;
    private int optionsMin = -1;
    private JTextField input;

    private void initialize(String title, Color foreground, Color background, Font font) {
        if (foreground == null) // Set default values if missing
            foreground = Color.WHITE;

        if (background == null)
            background = Color.BLACK;

        if (font == null)
            font = new Font(Font.MONOSPACED, Font.BOLD, 16);

        this.body = new JFrame(); // Initialize GUI
        this.prompt = new DefaultListModel<>();

        this.body.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //this.body.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.body.setSize(1024, 512);
        this.body.setUndecorated(false);
        this.body.setTitle(title);

        this.list = new JList<>(this.prompt);
        this.selection = this.list.getSelectionModel();
        this.list.setLayoutOrientation(JList.VERTICAL);
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

        // Listen for selection changes
        this.list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) { // Redirect selection on menu options
                if (selectable) {
                    int index = list.getSelectedIndex();
                    int size = prompt.size();

                    if (optionsMin >= 0 && optionsMin < size && index < optionsMin)
                        list.setSelectedIndex(optionsMin);
                    else if (optionsMax >= 0 && optionsMax < size && index > optionsMax)
                        list.setSelectedIndex(optionsMax);
                }
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
                    commandBuffer = text;
                    commandBufferId++;
                    println(text);

                    if (currentMenu != null)
                        currentMenu.doOption(text);

                    input.setText("> ");
                    focusInput();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(this.list);

        this.scrollable = scrollPane.getVerticalScrollBar();
        this.body.add(this.input, BorderLayout.PAGE_END);
        this.body.add(scrollPane, BorderLayout.CENTER);

        // Set mode and display
        this.setTextInput(true);
        this.body.setVisible(true);
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

        SwingUtilities.invokeLater(() -> { // Without this the console sometimes will glitch, #JavaSwingTheBest
            DefaultListModel<String> newModel = new DefaultListModel<>();
            for (int i = 0; i < prompt.size(); i++) {
                newModel.addElement(prompt.getElementAt(i));
            }

            list.setModel(newModel);
        });

        this.scrollable.setValue(this.scrollable.getMaximum());
        return this.prompt.size() -1;
    }

    public String readln() { // Wait for a new command, Call from a different thread
        boolean originalState = this.isTextInput();
        this.setTextInput(true);

        int last = this.commandBufferId;
        while (this.commandBufferId <= last) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        this.setTextInput(originalState);
        return this.commandBuffer;
    }

    public void clear() { // Clear the console
        this.currentMenu = null;
        this.optionsMin = -1;
        this.optionsMax = -1;

        this.list.clearSelection();
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
        else {
            this.list.requestFocus();
            if (this.selectable && this.optionsMin >= 0)
                this.list.setSelectedIndex(this.optionsMin);
        }
    }

    public void setSelectable(boolean selectable) {
        // Makes the list selectable or not
        this.selectable = selectable;
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

    public boolean isTextInput() {
        return this.input.isVisible();
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
        this.optionsMin = this.prompt.size();

        menu.getOptions().forEach(this::println);
        this.optionsMax = this.prompt.size() -1;

        this.focusInput();
    }

    public void close() {
        this.body.dispatchEvent(new WindowEvent(this.body, WindowEvent.WINDOW_CLOSING));
    }
}
