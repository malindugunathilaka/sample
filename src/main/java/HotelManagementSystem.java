import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Date;

public class HotelManagementSystem extends JFrame {
    private Connection connection;
    private String currentUser;
    private String currentRole;

    public HotelManagementSystem() {
        super("Hotel Management System");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize database
        initializeDatabase();

        // Show login screen
        showLoginScreen();

        setVisible(true);
    }

    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/hotel_db";
            String username = "root";
            String password = "root"; // Replace with your MySQL password
            connection = DriverManager.getConnection(url, username, password);
            createTables();
            insertSampleData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Create Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(50) NOT NULL," +
                    "role VARCHAR(20) NOT NULL," +
                    "fullname VARCHAR(100))");

            // Create Rooms table
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "room_number VARCHAR(10) UNIQUE NOT NULL," +
                    "type VARCHAR(20) NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "status VARCHAR(20) DEFAULT 'Available')");

            // Create Bookings table
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "guest_id INT NOT NULL," +
                    "room_id INT NOT NULL," +
                    "check_in_date DATE NOT NULL," +
                    "check_out_date DATE NOT NULL," +
                    "total_price DOUBLE NOT NULL," +
                    "status VARCHAR(20) DEFAULT 'Booked')");

            // Create Payments table
            stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "booking_id INT NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "payment_date DATETIME NOT NULL," +
                    "method VARCHAR(20) NOT NULL)");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Table Creation Error: " + e.getMessage());
        }
    }

    private void insertSampleData() {
        try (Statement stmt = connection.createStatement()) {
            // Insert sample users
            stmt.execute("INSERT IGNORE INTO users (username, password, role, fullname) VALUES " +
                    "('admin', 'admin123', 'admin', 'Admin User')," +
                    "('staff', 'staff123', 'staff', 'Staff Member')," +
                    "('guest', 'guest123', 'guest', 'John Doe')");

            // Insert sample rooms
            stmt.execute("INSERT IGNORE INTO rooms (room_number, type, price) VALUES " +
                    "('101', 'Standard', 100.00)," +
                    "('102', 'Deluxe', 150.00)," +
                    "('201', 'Suite', 250.00)");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Data Insertion Error: " + e.getMessage());
        }
    }

    private void showLoginScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("HOTEL MANAGEMENT SYSTEM", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 28;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton loginButton = new JButton("Login");
        panel.add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (authenticateUser(username, password)) {
                getContentPane().removeAll();
                showMainDashboard();
                revalidate();
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password");
            }
        });

        getContentPane().add(panel);
    }

    private boolean authenticateUser(String username, String password) {
        String sql = "SELECT role, fullname FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentUser = username;
                    currentRole = rs.getString("role");
                    return true;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Authentication Error: " + e.getMessage());
        }
        return false;
    }

    private void showMainDashboard() {
        setLayout(new BorderLayout());

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Add common tabs
        tabbedPane.addTab("Rooms", createRoomsPanel());

        // Role-specific tabs
        if ("guest".equals(currentRole)) {
            tabbedPane.addTab("My Bookings", createBookingsPanel());
            tabbedPane.addTab("Book a Room", createBookingPanel());
        } else if ("staff".equals(currentRole)) {
            tabbedPane.addTab("Manage Bookings", createBookingsPanel());
            tabbedPane.addTab("Check In/Out", createCheckInOutPanel());
        } else if ("admin".equals(currentRole)) {
            tabbedPane.addTab("Manage Users", createUsersPanel());
            tabbedPane.addTab("Reports", createReportsPanel());
        }

        // Create header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Welcome, " + currentUser + " (" + currentRole + ")",
                SwingConstants.LEFT), BorderLayout.WEST);

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            getContentPane().removeAll();
            showLoginScreen();
            revalidate();
            repaint();
        });
        headerPanel.add(logoutButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Table setup
        String[] columns = {"Room Number", "Type", "Price", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Load room data
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("room_number"),
                        rs.getString("type"),
                        rs.getDouble("price"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading rooms: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add room button for admin
        if ("admin".equals(currentRole)) {
            JButton addButton = new JButton("Add Room");
            addButton.addActionListener(e -> showAddRoomDialog(model));
            panel.add(addButton, BorderLayout.SOUTH);
        }

        return panel;
    }

    private void showAddRoomDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New Room", true);
        dialog.setSize(300, 250);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));

        JTextField roomNumberField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Standard", "Deluxe", "Suite"});
        JTextField priceField = new JTextField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Available", "Occupied", "Maintenance"});

        dialog.add(new JLabel("Room Number:"));
        dialog.add(roomNumberField);
        dialog.add(new JLabel("Room Type:"));
        dialog.add(typeCombo);
        dialog.add(new JLabel("Price:"));
        dialog.add(priceField);
        dialog.add(new JLabel("Status:"));
        dialog.add(statusCombo);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            try {
                String sql = "INSERT INTO rooms (room_number, type, price, status) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, roomNumberField.getText());
                    pstmt.setString(2, (String) typeCombo.getSelectedItem());
                    pstmt.setDouble(3, Double.parseDouble(priceField.getText()));
                    pstmt.setString(4, (String) statusCombo.getSelectedItem());

                    pstmt.executeUpdate();

                    // Update table
                    model.addRow(new Object[]{
                            roomNumberField.getText(),
                            typeCombo.getSelectedItem(),
                            Double.parseDouble(priceField.getText()),
                            statusCombo.getSelectedItem()
                    });

                    dialog.dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.add(new JLabel());
        dialog.add(saveButton);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Booking ID");
        model.addColumn("Room Number");
        model.addColumn("Check-in Date");
        model.addColumn("Check-out Date");
        model.addColumn("Total Price");
        model.addColumn("Status");

        JTable table = new JTable(model);

        // Load bookings based on user role
        String sql;
        if ("guest".equals(currentRole)) {
            sql = "SELECT b.id, r.room_number, b.check_in_date, b.check_out_date, b.total_price, b.status " +
                    "FROM bookings b " +
                    "JOIN rooms r ON b.room_id = r.id " +
                    "JOIN users u ON b.guest_id = u.id " +
                    "WHERE u.username = ?";
        } else {
            sql = "SELECT b.id, r.room_number, b.check_in_date, b.check_out_date, b.total_price, b.status " +
                    "FROM bookings b " +
                    "JOIN rooms r ON b.room_id = r.id";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if ("guest".equals(currentRole)) {
                pstmt.setString(1, currentUser);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("room_number"),
                            rs.getString("check_in_date"),
                            rs.getString("check_out_date"),
                            rs.getDouble("total_price"),
                            rs.getString("status")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add action buttons
        JPanel buttonPanel = new JPanel();

        if ("staff".equals(currentRole) || "admin".equals(currentRole)) {
            JButton checkInButton = new JButton("Check In");
            checkInButton.addActionListener(e -> updateBookingStatus(table, model, "Checked In"));
            buttonPanel.add(checkInButton);

            JButton checkOutButton = new JButton("Check Out");
            checkOutButton.addActionListener(e -> updateBookingStatus(table, model, "Checked Out"));
            buttonPanel.add(checkOutButton);
        }

        if ("guest".equals(currentRole)) {
            JButton cancelButton = new JButton("Cancel Booking");
            cancelButton.addActionListener(e -> updateBookingStatus(table, model, "Cancelled"));
            buttonPanel.add(cancelButton);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateBookingStatus(JTable table, DefaultTableModel model, String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking");
            return;
        }

        int bookingId = (Integer) model.getValueAt(row, 0);

        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE bookings SET status = ? WHERE id = ?")) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, bookingId);

            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                model.setValueAt(newStatus, row, 5);
                JOptionPane.showMessageDialog(this, "Booking status updated to: " + newStatus);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating booking: " + e.getMessage());
        }
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));

        // Form components
        JComboBox<String> roomCombo = new JComboBox<>();
        JSpinner checkInSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor checkInEditor = new JSpinner.DateEditor(checkInSpinner, "yyyy-MM-dd");
        checkInSpinner.setEditor(checkInEditor);

        JSpinner checkOutSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor checkOutEditor = new JSpinner.DateEditor(checkOutSpinner, "yyyy-MM-dd");
        checkOutSpinner.setEditor(checkOutEditor);

        JLabel totalLabel = new JLabel("0.00");
        JComboBox<String> paymentMethodCombo = new JComboBox<>(new String[]{"Credit Card", "Cash", "Bank Transfer"});

        // Load available rooms
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT room_number FROM rooms WHERE status = 'Available'")) {

            while (rs.next()) {
                roomCombo.addItem(rs.getString("room_number"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading rooms: " + e.getMessage());
        }

        // Calculate total when dates change
        checkOutSpinner.addChangeListener(e -> calculateTotal(roomCombo, checkInSpinner, checkOutSpinner, totalLabel));

        // Form layout
        formPanel.add(new JLabel("Room:"));
        formPanel.add(roomCombo);
        formPanel.add(new JLabel("Check-in Date:"));
        formPanel.add(checkInSpinner);
        formPanel.add(new JLabel("Check-out Date:"));
        formPanel.add(checkOutSpinner);
        formPanel.add(new JLabel("Total Amount:"));
        formPanel.add(totalLabel);
        formPanel.add(new JLabel("Payment Method:"));
        formPanel.add(paymentMethodCombo);

        JButton bookButton = new JButton("Book Now");
        bookButton.addActionListener(e -> createBooking(
                roomCombo,
                (Date) checkInSpinner.getValue(),
                (Date) checkOutSpinner.getValue(),
                totalLabel,
                (String) paymentMethodCombo.getSelectedItem()
        ));

        formPanel.add(new JLabel());
        formPanel.add(bookButton);

        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    private void calculateTotal(JComboBox<String> roomCombo, JSpinner checkIn, JSpinner checkOut, JLabel totalLabel) {
        if (checkIn.getValue() == null || checkOut.getValue() == null) return;

        try {
            Date checkInDate = (Date) checkIn.getValue();
            Date checkOutDate = (Date) checkOut.getValue();

            // Calculate nights
            long diff = checkOutDate.getTime() - checkInDate.getTime();
            int nights = (int) (diff / (1000 * 60 * 60 * 24));

            if (nights <= 0) {
                totalLabel.setText("Invalid dates");
                return;
            }

            // Get room price
            String roomNumber = (String) roomCombo.getSelectedItem();
            String sql = "SELECT price FROM rooms WHERE room_number = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, roomNumber);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        double pricePerNight = rs.getDouble("price");
                        double total = pricePerNight * nights;
                        totalLabel.setText(String.format("%.2f", total));
                    }
                }
            }
        } catch (SQLException ex) {
            totalLabel.setText("Error");
        }
    }

    private void createBooking(JComboBox<String> roomCombo, Date checkIn, Date checkOut, JLabel totalLabel, String paymentMethod) {
        if (roomCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a room");
            return;
        }

        if (checkIn == null || checkOut == null) {
            JOptionPane.showMessageDialog(this, "Please select dates");
            return;
        }

        if (checkIn.after(checkOut)) {
            JOptionPane.showMessageDialog(this, "Check-in date must be before check-out date");
            return;
        }

        try {
            // Get guest ID
            int guestId = getUserId(currentUser);

            // Get room ID
            String roomNumber = (String) roomCombo.getSelectedItem();
            int roomId = getRoomId(roomNumber);

            // Calculate total
            double total = Double.parseDouble(totalLabel.getText());

            // Create booking
            String sql = "INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, guestId);
                pstmt.setInt(2, roomId);
                pstmt.setString(3, new java.sql.Date(checkIn.getTime()).toString());
                pstmt.setString(4, new java.sql.Date(checkOut.getTime()).toString());
                pstmt.setDouble(5, total);

                pstmt.executeUpdate();

                // Create payment
                int bookingId = getLastInsertId();
                createPayment(bookingId, total, paymentMethod);

                // Update room status
                updateRoomStatus(roomId, "Booked");

                JOptionPane.showMessageDialog(this, "Booking created successfully!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating booking: " + e.getMessage());
        }
    }

    private void createPayment(int bookingId, double amount, String method) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, amount, payment_date, method) VALUES (?, ?, NOW(), ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, method);
            pstmt.executeUpdate();
        }
    }

    private void updateRoomStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
        }
    }

    private int getUserId(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("User not found");
    }

    private int getRoomId(String roomNumber) throws SQLException {
        String sql = "SELECT id FROM rooms WHERE room_number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("Room not found");
    }

    private int getLastInsertId() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to get last ID");
    }

    private JPanel createCheckInOutPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField bookingIdField = new JTextField();
        JComboBox<String> actionCombo = new JComboBox<>(new String[]{"Check In", "Check Out"});
        JTextField roomField = new JTextField();
        roomField.setEditable(false);
        JTextField guestField = new JTextField();
        guestField.setEditable(false);

        panel.add(new JLabel("Booking ID:"));
        panel.add(bookingIdField);
        panel.add(new JLabel("Action:"));
        panel.add(actionCombo);
        panel.add(new JLabel("Room:"));
        panel.add(roomField);
        panel.add(new JLabel("Guest:"));
        panel.add(guestField);

        JButton loadButton = new JButton("Load Booking");
        loadButton.addActionListener(e -> loadBookingDetails(
                bookingIdField.getText(), roomField, guestField));

        JButton processButton = new JButton("Process");
        processButton.addActionListener(e -> processCheckInOut(
                bookingIdField.getText(), (String) actionCombo.getSelectedItem()));

        panel.add(loadButton);
        panel.add(processButton);

        return panel;
    }

    private void loadBookingDetails(String bookingId, JTextField roomField, JTextField guestField) {
        if (bookingId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a booking ID");
            return;
        }

        try {
            String sql = "SELECT r.room_number, u.fullname " +
                    "FROM bookings b " +
                    "JOIN rooms r ON b.room_id = r.id " +
                    "JOIN users u ON b.guest_id = u.id " +
                    "WHERE b.id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(bookingId));

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        roomField.setText(rs.getString("room_number"));
                        guestField.setText(rs.getString("fullname"));
                    } else {
                        JOptionPane.showMessageDialog(this, "Booking not found");
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading booking: " + e.getMessage());
        }
    }

    private void processCheckInOut(String bookingId, String action) {
        if (bookingId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a booking ID");
            return;
        }

        try {
            String newStatus = action.equals("Check In") ? "Checked In" : "Checked Out";

            String sql = "UPDATE bookings SET status = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, Integer.parseInt(bookingId));

                int updated = pstmt.executeUpdate();
                if (updated > 0) {
                    JOptionPane.showMessageDialog(this, "Booking status updated to: " + newStatus);
                } else {
                    JOptionPane.showMessageDialog(this, "Booking not found");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating booking: " + e.getMessage());
        }
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID");
        model.addColumn("Username");
        model.addColumn("Full Name");
        model.addColumn("Role");

        JTable table = new JTable(model);

        // Load user data
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("fullname"),
                        rs.getString("role")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add user button
        JButton addButton = new JButton("Add User");
        addButton.addActionListener(e -> showAddUserDialog(model));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void showAddUserDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(300, 250);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField fullNameField = new JTextField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "staff", "guest"});

        dialog.add(new JLabel("Username:"));
        dialog.add(usernameField);
        dialog.add(new JLabel("Password:"));
        dialog.add(passwordField);
        dialog.add(new JLabel("Full Name:"));
        dialog.add(fullNameField);
        dialog.add(new JLabel("Role:"));
        dialog.add(roleCombo);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            try {
                String sql = "INSERT INTO users (username, password, role, fullname) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, usernameField.getText());
                    pstmt.setString(2, new String(passwordField.getPassword()));
                    pstmt.setString(3, (String) roleCombo.getSelectedItem());
                    pstmt.setString(4, fullNameField.getText());

                    pstmt.executeUpdate();

                    // Update table
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            model.addRow(new Object[]{
                                    rs.getInt(1),
                                    usernameField.getText(),
                                    fullNameField.getText(),
                                    roleCombo.getSelectedItem()
                            });
                        }
                    }

                    dialog.dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });

        dialog.add(new JLabel());
        dialog.add(saveButton);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Occupancy report
        JPanel occupancyPanel = new JPanel(new BorderLayout());
        occupancyPanel.setBorder(BorderFactory.createTitledBorder("Occupancy Report"));

        DefaultTableModel occupancyModel = new DefaultTableModel();
        occupancyModel.addColumn("Status");
        occupancyModel.addColumn("Count");
        JTable occupancyTable = new JTable(occupancyModel);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT status, COUNT(*) AS count FROM rooms GROUP BY status")) {

            while (rs.next()) {
                occupancyModel.addRow(new Object[]{
                        rs.getString("status"),
                        rs.getInt("count")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading occupancy: " + e.getMessage());
        }

        occupancyPanel.add(new JScrollPane(occupancyTable), BorderLayout.CENTER);

        // Revenue report
        JPanel revenuePanel = new JPanel(new BorderLayout());
        revenuePanel.setBorder(BorderFactory.createTitledBorder("Revenue Report"));

        DefaultTableModel revenueModel = new DefaultTableModel();
        revenueModel.addColumn("Month");
        revenueModel.addColumn("Total Revenue");
        JTable revenueTable = new JTable(revenueModel);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT DATE_FORMAT(payment_date, '%Y-%m') AS month, SUM(amount) AS revenue " +
                             "FROM payments GROUP BY month")) {

            while (rs.next()) {
                revenueModel.addRow(new Object[]{
                        rs.getString("month"),
                        rs.getDouble("revenue")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading revenue: " + e.getMessage());
        }

        revenuePanel.add(new JScrollPane(revenueTable), BorderLayout.CENTER);

        panel.add(occupancyPanel);
        panel.add(revenuePanel);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelManagementSystem());
    }
}
