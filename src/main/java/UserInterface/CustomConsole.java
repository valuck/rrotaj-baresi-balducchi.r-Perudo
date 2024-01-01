package UserInterface;

import com.google.protobuf.Value;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Function;

public class CustomConsole {
    private final DefaultListModel<String> prompt;
    private final ListSelectionModel selection;
    private final JList<String> list;
    private OptionsMenu currentMenu;
    private final JTextField input;

    public CustomConsole(String title) {
        JFrame body = new JFrame();
        this.prompt = new DefaultListModel<String>();

        Font font = new Font(Font.MONOSPACED, Font.BOLD, 16);
        Color background = Color.BLACK;
        Color foreground = Color.WHITE;

        body.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        body.setExtendedState(JFrame.MAXIMIZED_BOTH);
        body.setUndecorated(true);
        body.setTitle(title);

        this.list = new JList<String>(this.prompt);
        this.selection = this.list.getSelectionModel();
        this.list.setBackground(background);
        this.list.setForeground(foreground);
        this.list.setFont(font);

        this.list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_ENTER) {
                    String value = list.getSelectedValue();

                    if (value != null && currentMenu != null)
                        currentMenu.doOption(value);
                }
            }
        });

        this.list.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                focusInput();
            }

            @Override
            public void focusLost(FocusEvent e) {
                list.clearSelection();
            }
        });

        this.input = new JTextField("> ");
        this.input.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        this.input.setBackground(background);
        this.input.setForeground(foreground);
        this.input.setFont(font);

        this.input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_ENTER) {
                    String text = input.getText();

                    if (text.charAt(0) == '>')
                        text = text.substring(1);

                    text = text.trim();
                    if (currentMenu == null)
                        println(text);
                    else
                        currentMenu.doOption(text);

                    input.setText("> ");
                    focusInput();
                }
            }
        });

        body.add(this.input, BorderLayout.PAGE_END);
        body.add(this.list, BorderLayout.CENTER);

        this.setTextInput(true);
        body.setVisible(true);
        this.focusInput();
    }

    public int println(String text) {
        this.prompt.addElement(text);
        return this.prompt.size() -1;
    }

    public void clear() {
        this.currentMenu = null;
        this.prompt.removeAllElements();
    }

    public void remove(int index) {
        this.prompt.remove(index);
    }

    public void focusInput() {
        if (this.input.isVisible()) {
            this.input.requestFocus();
            this.input.setCaretPosition(this.input.getDocument().getLength());
        }
        else
            this.list.requestFocus();
    }

    public void setSelectable(boolean selectable) {
        this.list.setSelectionModel(selectable ? this.selection : new DefaultListSelectionModel() {
            @Override // Removes the selection
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(-1, -1);
            }
        });

        this.list.clearSelection();
    }

    public void setTextInput(boolean enabled) {
        this.input.setVisible(enabled);
        this.setSelectable(!enabled);

        focusInput();
    }

    public void drawOptionsMenu(OptionsMenu menu) {
        this.clear();
        this.currentMenu = menu;
        menu.getOptions().forEach((value) -> {
            this.println(value);
        });
    }
}
