import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.file.Files;

public class NotesApp extends JFrame {
    private static final String NOTES_FOLDER = "saved_notes";
    private static final String PASSWORD_FILE = "password.dat";

    private JPanel notesPanel;
    private JTextField searchField;
    private java.util.List<File> allNotes;

    // Define color palette
    private final Color PRIMARY = Color.decode("#006077");
    private final Color SECONDARY = Color.decode("#83C5BE");
    private final Color LIGHT = Color.decode("#EDF6F9");
    private final Color ACCENT = Color.decode("#FFDDD2");

    public NotesApp() {
        if (!authenticateUser()) {
            System.exit(0);
        }

        setTitle("📓 Notes");
        setSize(630, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(LIGHT);

        JLabel header = new JLabel("My Stylish Notes", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 32));
        header.setForeground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(LIGHT);
        searchField = new JTextField(2);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        searchField.setBorder(BorderFactory.createTitledBorder("Search Notes 🔎"));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText().toLowerCase();
                displayNotes(query);
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.BEFORE_FIRST_LINE);

        notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
        notesPanel.setBackground(LIGHT);
        JScrollPane scrollPane = new JScrollPane(notesPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        JButton newNoteBtn = new JButton("➕ New Note");
        newNoteBtn.setBackground(SECONDARY);
        newNoteBtn.setForeground(Color.BLACK);
        newNoteBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        newNoteBtn.addActionListener(e -> showNoteEditor(null, null, null));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(LIGHT);
        footer.add(newNoteBtn);
        add(footer, BorderLayout.SOUTH);

        File dir = new File(NOTES_FOLDER);
        if (!dir.exists()) {
            dir.mkdir();
        }

        loadNotes();
    }

    private boolean authenticateUser() {
        try {
            File dir = new File(NOTES_FOLDER);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File passwordFile = new File(NOTES_FOLDER, PASSWORD_FILE);

            if (!passwordFile.exists()) {
                String newPassword = promptPassword("Set a password to protect your notes:");
                if (newPassword == null || newPassword.trim().isEmpty()) {
                    return false;
                }

                String hash = hashPassword(newPassword);
                try (FileWriter fw = new FileWriter(passwordFile)) {
                    fw.write(hash);
                }
                return true;
            } else {
                String savedHash = new String(Files.readAllBytes(passwordFile.toPath()));
                for (int i = 0; i < 3; i++) {
                    String input = promptPassword("Enter password:");
                    if (input == null) {
                        return false;
                    }

                    String inputHash = hashPassword(input);
                    if (inputHash.equals(savedHash)) {
                        return true;
                    }

                    JOptionPane.showMessageDialog(null, "Incorrect password!", "Authentication Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        } catch (Exception e) {
            showError("Error with password authentication.");
            return false;
        }
    }

    private String promptPassword(String message) {
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, message, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (okCxl == JOptionPane.OK_OPTION) {
            return new String(pf.getPassword());
        }
        return null;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void loadNotes() {
        File dir = new File(NOTES_FOLDER);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            allNotes = new ArrayList<>();
            return;
        }

        // Sort files by second line (date) in descending order
        Arrays.sort(files, (f1, f2) -> {
            try (BufferedReader br1 = new BufferedReader(new FileReader(f1));
                 BufferedReader br2 = new BufferedReader(new FileReader(f2))) {
                br1.readLine(); // Skip title
                br2.readLine();
                String d1 = br1.readLine(); // Read date
                String d2 = br2.readLine();

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                Date date1 = sdf.parse(d1);
                Date date2 = sdf.parse(d2);
                return date2.compareTo(date1); // descending
            } catch (Exception e) {
                return 0;
            }
        });

        allNotes = Arrays.asList(files);
        displayNotes("");
    }

    private void displayNotes(String query) {
        notesPanel.removeAll();
        boolean hasNotes = false;
        for (File file : allNotes) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String title = br.readLine();
                String date = br.readLine();
                String preview = br.readLine();

                if (title.toLowerCase().contains(query)) {
                    hasNotes = true;

                    JPanel noteCard = new JPanel();
                    noteCard.setLayout(new BoxLayout(noteCard, BoxLayout.X_AXIS));
                    noteCard.setMaximumSize(new Dimension(650, 80));
                    noteCard.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JButton noteBtn = new JButton("<html><b>" + title + "</b><br><i>" + date + "</i><br>" + preview + "...</html>");
                    noteBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
                    noteBtn.setBackground(ACCENT);
                    noteBtn.setForeground(Color.BLACK);
                    noteBtn.setFocusPainted(false);
                    noteBtn.setHorizontalAlignment(SwingConstants.LEFT);
                    noteBtn.setPreferredSize(new Dimension(580, 80));
                    noteBtn.addActionListener(e -> showNoteEditor(file.getName(), title, readFullContent(file)));

                    JButton deleteBtn = new JButton("🗑️");
                    deleteBtn.setFocusPainted(false);
                    deleteBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
                    deleteBtn.setBackground(Color.WHITE);
                    deleteBtn.setBorder(null);
                    deleteBtn.setToolTipText("Delete Note");
                    deleteBtn.setPreferredSize(new Dimension(60, 60));
                    deleteBtn.setMaximumSize(new Dimension(60, 60));
                    deleteBtn.setMinimumSize(new Dimension(60, 60));
                    deleteBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
                    deleteBtn.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Are you sure you want to delete this note?",
                                "Delete Note", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (file.delete()) {
                                JOptionPane.showMessageDialog(this, "Note deleted.");
                                allNotes = Arrays.asList(new File(NOTES_FOLDER)
                                        .listFiles((dir, name) -> name.endsWith(".txt")));
                                displayNotes(searchField.getText().toLowerCase());
                            } else {
                                JOptionPane.showMessageDialog(this, "Failed to delete note.");
                            }
                        }
                    });

                    noteCard.add(noteBtn);
                    noteCard.add(Box.createRigidArea(new Dimension(10, 0)));
                    noteCard.add(deleteBtn);

                    notesPanel.add(Box.createVerticalStrut(10));
                    notesPanel.add(noteCard);
                }
            } catch (IOException e) {
                showError("Error reading note: " + file.getName());
            }
        }

        if (!hasNotes) {
            JLabel emptyLabel = new JLabel("✍️ Write something!", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 24));
            emptyLabel.setForeground(PRIMARY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            notesPanel.add(emptyLabel);
        }

        notesPanel.revalidate();
        notesPanel.repaint();
    }

    private void showNoteEditor(String fileName, String oldTitle, String content) {
        JDialog dialog = new JDialog(this, "Note Editor", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(LIGHT);

        JTextField titleField = new JTextField(oldTitle != null ? oldTitle : "", 2);
        titleField.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleField.setBorder(BorderFactory.createTitledBorder("Title"));

        JTextField dateField = new JTextField(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        dateField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        dateField.setBorder(BorderFactory.createTitledBorder("Date"));

        JTextArea contentArea = new JTextArea(content != null ? content : "", 50, 20);
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        contentArea.setBackground(Color.WHITE);
        contentArea.setForeground(Color.BLACK);
        contentArea.setCaretColor(Color.BLACK);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(BorderFactory.createTitledBorder("Content"));

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(LIGHT);
        fieldsPanel.add(titleField);
        fieldsPanel.add(dateField);
        fieldsPanel.add(contentScroll);

        JButton saveBtn = new JButton("💾 Save Note");
        saveBtn.setBackground(SECONDARY);
        saveBtn.setForeground(Color.BLACK);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        saveBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String date = dateField.getText().trim();
            String text = contentArea.getText().trim();
            if (title.isEmpty()) {
                showError("Title cannot be empty.");
                return;
            } else if (text.isEmpty()) {
                showError("Content cannot be empty.");
                return;
            }

            try {
                String fname = fileName != null ? fileName
                        : title.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis() + ".txt";
                FileWriter writer = new FileWriter(new File(NOTES_FOLDER, fname));
                writer.write(title + "\n" + date + "\n" + text);
                writer.close();
                dialog.dispose();
                loadNotes();
            } catch (IOException ex) {
                showError("Failed to save note.");
            }
        });

        dialog.add(fieldsPanel, BorderLayout.CENTER);
        dialog.add(saveBtn, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String readFullContent(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine(); // title
            br.readLine(); // date
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NotesApp().setVisible(true));
    }
}
