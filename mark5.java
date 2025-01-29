import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class mark5 {
    private static final String apikey = "YOUR_API_KEY";
    private static final String baseurl = "https://newsapi.org/v2/";
    private static final String[] categories = { "Business", "Entertainment", "General", "Health", "Science", "Sports",
            "Technology" };
    private static String lastSelectedCategory = "";
    private static HashMap<String, JSONArray> newsCache = new HashMap<>(); // Cache for fetched news

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/newsappdb"; // Update DB URL
    private static final String USER = "root"; // Update DB user
    private static final String PASS = "0000"; // Update DB password

    public static void main(String[] args) {
        SwingUtilities.invokeLater(mark5::createGUI);
    }

    private static void createGUI() {
        JFrame frame = new JFrame("News App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

        JPanel panel = new JPanel(new BorderLayout());

        // Header panel with dropdowns and buttons
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.setBackground(Color.black);

        JComboBox<String> categoryDropdown = new JComboBox<>(categories);
        categoryDropdown.setBackground(Color.LIGHT_GRAY);
        categoryDropdown.setForeground(Color.BLACK);

        JButton fetchButton = new JButton("Fetch News");
        JButton refreshButton = new JButton("Refresh News");
        JButton displaySavedButton = new JButton("Display Saved News");
        JTextField searchField = new JTextField(15);
        JButton searchButton = new JButton("Search News");

        formatButton(fetchButton, new Color(0, 123, 255));
        formatButton(refreshButton, new Color(0, 200, 100));
        formatButton(displaySavedButton, new Color(79, 25, 175));
        formatButton(searchButton, new Color(228, 76, 97));

        inputPanel.add(categoryDropdown);
        inputPanel.add(fetchButton);
        inputPanel.add(refreshButton);
        inputPanel.add(displaySavedButton);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(Color.WHITE); // Change font color to white
        inputPanel.add(searchLabel);
        inputPanel.add(searchField);
        inputPanel.add(searchButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical alignment
        JScrollPane scrollPane = new JScrollPane(newsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        panel.add(scrollPane, BorderLayout.CENTER);

        fetchButton.addActionListener(e -> {
            lastSelectedCategory = (String) categoryDropdown.getSelectedItem();
            fetchNewsAsync(lastSelectedCategory.toLowerCase(), newsPanel, frame);
        });

        refreshButton.addActionListener(e -> {
            if (!lastSelectedCategory.isEmpty()) {
                fetchNewsAsync(lastSelectedCategory.toLowerCase(), newsPanel, frame);
            } else {
                JOptionPane.showMessageDialog(frame, "Please fetch news first using the Fetch button.");
            }
        });

        displaySavedButton.addActionListener(e -> displaySavedNews(newsPanel));

        searchField.addActionListener(e -> searchNews(searchField.getText(), newsPanel));

        searchButton.addActionListener(e -> {
            String query = searchField.getText();
            if (!query.isEmpty()) {
                searchNewsAsync(query, newsPanel, frame);
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter a search term.");
            }
        });

        frame.add(panel);
        frame.setVisible(true);

        // Fetch general news initially
        fetchNewsAsync("general", newsPanel, frame);
    }

    private static void fetchNewsAsync(String category, JPanel newsPanel, JFrame frame) {
        JDialog loadingDialog = createLoadingDialog(frame);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
                fetchNews(category, newsPanel);
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
            }
        };
        worker.execute();
    }

    // Asynchronous search news
    private static void searchNewsAsync(String query, JPanel newsPanel, JFrame frame) {
        JDialog loadingDialog = createLoadingDialog(frame);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
                searchNews(query, newsPanel);
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
            }
        };
        worker.execute();
    }

    private static void fetchNews(String category, JPanel newsPanel) {
        try {
            if (newsCache.containsKey(category)) {
                displayNews(newsCache.get(category), newsPanel);
                return;
            }

            String apiurl = baseurl + "top-headlines?category=" + category + "&language=en&apiKey=" + apikey;
            HttpURLConnection con = (HttpURLConnection) new URL(apiurl).openConnection();
            con.setRequestMethod("GET");

            BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                json.append(line);
            }
            r.close();

            JSONObject response = new JSONObject(json.toString());
            JSONArray articles = response.getJSONArray("articles");

            newsCache.put(category, articles);
            displayNews(articles, newsPanel);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Network error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JLabel errorLabel = new JLabel("Error: " + e.getMessage());
            errorLabel.setForeground(Color.RED);
            newsPanel.add(errorLabel);
            newsPanel.revalidate();
            newsPanel.repaint();
        }
    }

    private static void displayNews(JSONArray articles, JPanel newsPanel) {
        newsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 15, 15, 15);
        for (int i = 0; i < articles.length(); i++) {
            JSONObject article = articles.getJSONObject(i);

            JPanel articlePanel = new JPanel(new BorderLayout());
            articlePanel.setBorder(BorderFactory.createEmptyBorder());
            articlePanel.setBackground(Color.black);

            String title = article.getString("title");
            JLabel titleLabel = new JLabel(
                    "<html><h2 style='color:white; text-align:center;'>" + title + "</h2></html>");
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            articlePanel.add(titleLabel, BorderLayout.NORTH);

            String imageUrl = article.optString("urlToImage", null);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                loadImageAsync(imageUrl, articlePanel);
            }

            String description = article.optString("description", "No description available.");
            JLabel descLabel = new JLabel(
                    "<html><p style='color:white; margin:10px; word-wrap: break-word;'>" + getLongText(description)
                            + "</p></html>");

            descLabel.setVerticalAlignment(SwingConstants.TOP);
            descLabel.setPreferredSize(new Dimension(400, 100));
            articlePanel.add(descLabel, BorderLayout.CENTER);

            String url = article.getString("url");
            JLabel sourceLabel = new JLabel("<html><a href=''>" + "Click to read more</a></html>");
            sourceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sourceLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Error opening URL: " + ex.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(e -> saveNews(title, description, url, imageUrl));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.black);
            buttonPanel.add(sourceLabel);
            buttonPanel.add(saveButton);
            articlePanel.add(buttonPanel, BorderLayout.SOUTH);

            newsPanel.add(articlePanel);
            newsPanel.add(Box.createVerticalStrut(10)); // Add space between articles
        }

        newsPanel.revalidate();
        newsPanel.repaint();
    }

    private static void saveNews(String title, String description, String url, String imageUrl) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "INSERT INTO news (title, description, image_url, source_url) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, imageUrl);
            pstmt.setString(4, url);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(null, "News saved successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unexpected error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void displaySavedNews(JPanel newsPanel) {
        newsPanel.removeAll();
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "SELECT * FROM news";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                JPanel articlePanel = new JPanel(new BorderLayout());
                articlePanel.setBorder(BorderFactory.createEmptyBorder());
                articlePanel.setBackground(Color.black);

                String title = rs.getString("title");
                int newsId = rs.getInt("id"); // Assuming `id` is a primary key in the table
                JLabel titleLabel = new JLabel(
                        "<html><h2 style='color:white; text-align:center;'>" + title + "</h2></html>");
                titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
                articlePanel.add(titleLabel, BorderLayout.NORTH);

                String imageUrl = rs.getString("image_url");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    loadImageAsync(imageUrl, articlePanel);
                }

                String description = rs.getString("description");
                JLabel descLabel = new JLabel(
                        "<html><p style='color:white; margin:10px; word-wrap: break-word;'>" + getLongText(description)
                                + "</p></html>");

                descLabel.setVerticalAlignment(SwingConstants.TOP);
                descLabel.setPreferredSize(new Dimension(400, 100));
                articlePanel.add(descLabel, BorderLayout.CENTER);

                String url = rs.getString("source_url");
                JLabel sourceLabel = new JLabel("<html><a href=''>" + "Click to read more</a></html>");
                sourceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                sourceLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            Desktop.getDesktop().browse(new URI(url));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(null, "Error opening URL: " + ex.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                JButton deleteButton = new JButton("Delete");
                deleteButton.addActionListener(e -> {
                    deleteNews(newsId);
                    displaySavedNews(newsPanel); // Refresh saved news after deletion
                });

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.setBackground(Color.black);
                buttonPanel.add(sourceLabel);
                buttonPanel.add(deleteButton);
                articlePanel.add(buttonPanel, BorderLayout.SOUTH);

                newsPanel.add(articlePanel);
                newsPanel.add(Box.createVerticalStrut(10)); // Add space between articles
            }

            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unexpected error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        newsPanel.revalidate();
        newsPanel.repaint();
    }

    private static void deleteNews(int newsId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String sql = "DELETE FROM news WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, newsId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "News deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(null, "No news found with the given ID.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unexpected error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void searchNews(String query, JPanel newsPanel) {
        try {
            String apiurl = baseurl + "everything?q=" + URLEncoder.encode(query, "UTF-8") + "&language=en&apiKey="
                    + apikey;
            HttpURLConnection con = (HttpURLConnection) new URL(apiurl).openConnection();
            con.setRequestMethod("GET");

            BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                json.append(line);
            }
            r.close();

            JSONObject response = new JSONObject(json.toString());
            JSONArray articles = response.getJSONArray("articles");

            displayNews(articles, newsPanel);
        } catch (Exception e) {
            JLabel errorLabel = new JLabel("Error: " + e.getMessage());
            errorLabel.setForeground(Color.RED);
            newsPanel.add(errorLabel);
            newsPanel.revalidate();
            newsPanel.repaint();
        }
    }

    private static void loadImageAsync(String imageUrl, JPanel articlePanel) {
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    BufferedImage img = ImageIO.read(new URL(imageUrl));
                    return new ImageIcon(img.getScaledInstance(200, 150, Image.SCALE_SMOOTH));
                } catch (IOException e) {
                    return null; // Handle error silently
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        JLabel imageLabel = new JLabel(icon);
                        articlePanel.add(imageLabel, BorderLayout.WEST);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // Handle exceptions if needed
                }
            }
        };
        worker.execute();
    }

    private static JDialog createLoadingDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Loading...", true);
        dialog.setSize(200, 100);
        dialog.setLocationRelativeTo(parent);
        JLabel label = new JLabel("Fetching news, please wait...");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        dialog.add(label);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        return dialog;
    }

    private static String getLongText(String text) {
        return text.length() > 1000 ? text.substring(0, 1000) + "..." : text; // Trim long texts
    }

    private static void formatButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}
