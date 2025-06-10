import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.logging.*;

/**
 * Enhanced ATM Application with comprehensive features
 * Features: PIN validation, transaction limits, history, receipt generation, 
 * error handling, data validation, logging, and modern UI
 * 
 * @author SecureBank Development Team
 * @version 2.1
 * Compatible with Java 17+
 */

// Transaction data model with validation
class Transaction {
    private final String type;
    private final String id;
    private final double amount;
    private final double balanceAfter;
    private final Date timestamp;
    
    public Transaction(String type, double amount, double balanceAfter) {
        this.type = validateString(type, "Transaction type");
        this.amount = validateAmount(amount);
        this.balanceAfter = validateAmount(balanceAfter);
        this.timestamp = new Date();
        this.id = "TXN" + System.currentTimeMillis();
    }
    
    private String validateString(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return value.trim();
    }
    
    private double validateAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount;
    }
    
    // Getters with formatted output
    public String getType() { 
        return type; 
    }
    
    public double getAmount() { 
        return amount; 
    }
    
    public Date getTimestamp() { 
        return timestamp; 
    }
    
    public double getBalanceAfter() { 
        return balanceAfter; 
    }
    
    public String getId() { 
        return id; 
    }
    
    public String getFormattedTimestamp() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(timestamp);
    }
    
    public String getFormattedAmount() { 
        return String.format("₹%.2f", amount); 
    }
    
    public String getFormattedBalance() { 
        return String.format("₹%.2f", balanceAfter); 
    }
}

// Enhanced ATM Operations with comprehensive validation
class ATMOperations {
    private double balance;
    private String pin;
    private String accountNumber;
    private String holderName;
    private List<Transaction> history;
    private boolean isBlocked;
    private int failedAttempts;
    private static final Logger logger = Logger.getLogger(ATMOperations.class.getName());
    
    // Transaction limits with validation
    private static final double MAX_WITHDRAWAL = 50000.0;
    private static final double MAX_DEPOSIT = 200000.0;
    private static final double MAX_TRANSFER = 100000.0;
    private static final int MAX_FAILED_ATTEMPTS = 3;
    
    public ATMOperations(double balance, String pin, String accountNumber, String holderName) {
        this.balance = validateAmount(balance);
        this.pin = validatePinFormat(pin);
        this.accountNumber = validateAccountNumber(accountNumber);
        this.holderName = validateString(holderName, "Account holder name");
        this.history = new ArrayList<>();
        this.isBlocked = false;
        this.failedAttempts = 0;
        setupLogging();
        logger.info("ATM initialized for account: " + accountNumber);
    }
    
    private void setupLogging() {
        try {
            FileHandler fileHandler = new FileHandler("atm_operations.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Logging setup failed: " + e.getMessage());
        }
    }
    
    // Comprehensive validation methods
    private String validatePinFormat(String pin) {
        if (pin == null || !Pattern.matches("\\d{4}", pin)) {
            throw new IllegalArgumentException("PIN must be 4 digits");
        }
        return pin;
    }
    
    private String validateAccountNumber(String accountNum) {
        if (accountNum == null || !Pattern.matches("ACC\\d{9}", accountNum)) {
            throw new IllegalArgumentException("Invalid account number format");
        }
        return accountNum;
    }
    
    private String validateString(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return value.trim();
    }
    
    private double validateAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount;
    }
    
    // Core banking operations with enhanced error handling
    public boolean validatePin(String enteredPin) {
        try {
            if (isBlocked) {
                logger.warning("Login attempt on blocked account: " + accountNumber);
                return false;
            }
            
            if (this.pin.equals(enteredPin)) {
                failedAttempts = 0;
                logger.info("Successful login for account: " + accountNumber);
                return true;
            } else {
                failedAttempts++;
                logger.warning("Failed login attempt " + failedAttempts + " for account: " + accountNumber);
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    isBlocked = true;
                    logger.severe("Account blocked due to multiple failed attempts: " + accountNumber);
                }
                return false;
            }
        } catch (Exception e) {
            logger.severe("Error in PIN validation: " + e.getMessage());
            return false;
        }
    }
    
    public TransactionResult withdraw(double amount) {
        try {
            validateAmount(amount);
            if (amount > balance) {
                return new TransactionResult(false, "Insufficient balance. Current: ₹" + String.format("%.2f", balance));
            }
            if (amount > MAX_WITHDRAWAL) {
                return new TransactionResult(false, "Daily limit exceeded. Max: ₹" + String.format("%.2f", MAX_WITHDRAWAL));
            }
            
            balance -= amount;
            addTransaction("WITHDRAWAL", amount);
            logger.info("Withdrawal successful: ₹" + amount + " from account: " + accountNumber);
            return new TransactionResult(true, "Successfully withdrawn ₹" + String.format("%.2f", amount));
        } catch (Exception e) {
            logger.severe("Withdrawal error: " + e.getMessage());
            return new TransactionResult(false, "Transaction failed: " + e.getMessage());
        }
    }
    
    public TransactionResult deposit(double amount) {
        try {
            validateAmount(amount);
            if (amount > MAX_DEPOSIT) {
                return new TransactionResult(false, "Daily limit exceeded. Max: ₹" + String.format("%.2f", MAX_DEPOSIT));
            }
            
            balance += amount;
            addTransaction("DEPOSIT", amount);
            logger.info("Deposit successful: ₹" + amount + " to account: " + accountNumber);
            return new TransactionResult(true, "Successfully deposited ₹" + String.format("%.2f", amount));
        } catch (Exception e) {
            logger.severe("Deposit error: " + e.getMessage());
            return new TransactionResult(false, "Transaction failed: " + e.getMessage());
        }
    }
    
    public TransactionResult transfer(double amount, String targetAccount) {
        try {
            validateAmount(amount);
            validateString(targetAccount, "Target account");
            
            if (amount > balance) {
                return new TransactionResult(false, "Insufficient balance");
            }
            if (amount > MAX_TRANSFER) {
                return new TransactionResult(false, "Transfer limit exceeded");
            }
            if (targetAccount.equals(accountNumber)) {
                return new TransactionResult(false, "Cannot transfer to same account");
            }
            
            balance -= amount;
            addTransaction("TRANSFER TO " + targetAccount, amount);
            logger.info("Transfer successful: ₹" + amount + " from " + accountNumber + " to " + targetAccount);
            return new TransactionResult(true, "Successfully transferred ₹" + String.format("%.2f", amount));
        } catch (Exception e) {
            logger.severe("Transfer error: " + e.getMessage());
            return new TransactionResult(false, "Transfer failed: " + e.getMessage());
        }
    }
    
    public boolean changePin(String oldPin, String newPin) {
        try {
            if (!this.pin.equals(oldPin)) {
                return false;
            }
            validatePinFormat(newPin);
            
            this.pin = newPin;
            addTransaction("PIN CHANGE", 0);
            logger.info("PIN changed successfully for account: " + accountNumber);
            return true;
        } catch (Exception e) {
            logger.severe("PIN change error: " + e.getMessage());
            return false;
        }
    }
    
    private void addTransaction(String type, double amount) {
        history.add(new Transaction(type, amount, balance));
    }
    
    // Getters
    public double getBalance() { 
        return balance; 
    }
    
    public String getAccountNumber() { 
        return accountNumber; 
    }
    
    public String getHolderName() { 
        return holderName; 
    }
    
    public List<Transaction> getHistory() { 
        return new ArrayList<>(history); 
    }
    
    public boolean isBlocked() { 
        return isBlocked; 
    }
    
    public int getFailedAttempts() { 
        return failedAttempts; 
    }
}

// Transaction result wrapper
class TransactionResult {
    private final boolean success;
    private final String message;
    
    public TransactionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public boolean isSuccess() { 
        return success; 
    }
    
    public String getMessage() { 
        return message; 
    }
}

// Main GUI Application with modern design
public class EnhancedATMApp extends JFrame {
    private ATMOperations atm;
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private Transaction lastTransaction;
    
    // Modern color scheme
    private static final Color PRIMARY = new Color(33, 150, 243);
    private static final Color SUCCESS = new Color(76, 175, 80);
    private static final Color ERROR = new Color(244, 67, 54);
    private static final Color BACKGROUND = new Color(245, 245, 245);
    
    public EnhancedATMApp() {
        try {
            // Initialize with sample data
            atm = new ATMOperations(50000.00, "1234", "ACC123456789", "John Doe");
            initializeGUI();
        } catch (Exception e) {
            showError("Initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void initializeGUI() {
        setTitle("SecureBank ATM - Enhanced System v2.1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        
        // Create panels
        mainContainer.add(createWelcomePanel(), "WELCOME");
        mainContainer.add(createPinPanel(), "PIN");
        mainContainer.add(createMenuPanel(), "MENU");
        mainContainer.add(createTransactionPanel("WITHDRAW"), "WITHDRAW");
        mainContainer.add(createTransactionPanel("DEPOSIT"), "DEPOSIT");
        mainContainer.add(createTransferPanel(), "TRANSFER");
        mainContainer.add(createHistoryPanel(), "HISTORY");
        mainContainer.add(createSettingsPanel(), "SETTINGS");
        
        add(mainContainer);
        cardLayout.show(mainContainer, "WELCOME");
    }
    
    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY);
        
        JLabel title = new JLabel("SecureBank ATM", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(50, 0, 20, 0));
        
        JButton startBtn = createButton("START BANKING", SUCCESS);
        startBtn.addActionListener(e -> cardLayout.show(mainContainer, "PIN"));
        
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(startBtn);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createPinPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        
        JLabel instruction = new JLabel("Enter 4-digit PIN:");
        instruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPasswordField pinField = new JPasswordField(4);
        pinField.setMaximumSize(new Dimension(150, 30));
        pinField.setHorizontalAlignment(SwingConstants.CENTER);
        
        JButton enterBtn = createButton("ENTER", SUCCESS);
        JButton cancelBtn = createButton("CANCEL", ERROR);
        
        enterBtn.addActionListener(e -> {
            String pin = new String(pinField.getPassword());
            if (validateInput(pin, "PIN", "\\d{4}")) {
                if (atm.validatePin(pin)) {
                    cardLayout.show(mainContainer, "MENU");
                    pinField.setText("");
                } else {
                    String msg = atm.isBlocked() ? "Account blocked!" : 
                        "Invalid PIN. Attempts left: " + (3 - atm.getFailedAttempts());
                    showError(msg);
                    pinField.setText("");
                }
            }
        });
        
        cancelBtn.addActionListener(e -> {
            pinField.setText("");
            cardLayout.show(mainContainer, "WELCOME");
        });
        
        card.add(Box.createVerticalStrut(20));
        card.add(instruction);
        card.add(Box.createVerticalStrut(15));
        card.add(pinField);
        card.add(Box.createVerticalStrut(20));
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(enterBtn);
        btnPanel.add(cancelBtn);
        card.add(btnPanel);
        
        panel.add(card);
        return panel;
    }
    
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        
        JPanel card = createCard();
        card.setLayout(new GridLayout(3, 2, 10, 10));
        
        String[] options = {"Balance: ₹" + String.format("%.2f", atm.getBalance()), "Withdraw", 
                           "Deposit", "Transfer", "History", "Settings"};
        String[] actions = {"BALANCE", "WITHDRAW", "DEPOSIT", "TRANSFER", "HISTORY", "SETTINGS"};
        
        for (int i = 0; i < options.length; i++) {
            JButton btn = createMenuButton(options[i]);
            final String action = actions[i];
            if (action.equals("BALANCE")) {
                btn.addActionListener(e -> showInfo("Current Balance: ₹" + String.format("%.2f", atm.getBalance())));
            } else {
                btn.addActionListener(e -> cardLayout.show(mainContainer, action));
            }
            card.add(btn);
        }
        
        JButton logoutBtn = createButton("LOGOUT", ERROR);
        logoutBtn.addActionListener(e -> cardLayout.show(mainContainer, "WELCOME"));
        
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(card, BorderLayout.CENTER);
        container.add(logoutBtn, BorderLayout.SOUTH);
        
        panel.add(container);
        return panel;
    }
    
    private JPanel createTransactionPanel(String type) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel(type + " Money");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField amountField = new JTextField(15);
        amountField.setMaximumSize(new Dimension(200, 30));
        
        JButton processBtn = createButton(type, type.equals("WITHDRAW") ? ERROR : SUCCESS);
        JButton backBtn = createButton("BACK", PRIMARY);
        
        processBtn.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                TransactionResult result = type.equals("WITHDRAW") ? 
                    atm.withdraw(amount) : atm.deposit(amount);
                
                if (result.isSuccess()) {
                    lastTransaction = atm.getHistory().get(atm.getHistory().size() - 1);
                    showSuccess(result.getMessage() + "\nNew Balance: ₹" + String.format("%.2f", atm.getBalance()));
                    amountField.setText("");
                } else {
                    showError(result.getMessage());
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid amount");
            }
        });
        
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));
        
        card.add(Box.createVerticalStrut(20));
        card.add(title);
        card.add(Box.createVerticalStrut(15));
        card.add(new JLabel("Enter amount:"));
        card.add(Box.createVerticalStrut(5));
        card.add(amountField);
        card.add(Box.createVerticalStrut(20));
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(backBtn);
        btnPanel.add(processBtn);
        card.add(btnPanel);
        
        panel.add(card);
        return panel;
    }
    
    private JPanel createTransferPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        
        JTextField accountField = new JTextField(15);
        accountField.setMaximumSize(new Dimension(200, 30));
        
        JTextField amountField = new JTextField(15);
        amountField.setMaximumSize(new Dimension(200, 30));
        
        JButton transferBtn = createButton("TRANSFER", PRIMARY);
        JButton backBtn = createButton("BACK", ERROR);
        
        transferBtn.addActionListener(e -> {
            try {
                String targetAccount = accountField.getText().trim();
                double amount = Double.parseDouble(amountField.getText());
                
                TransactionResult result = atm.transfer(amount, targetAccount);
                if (result.isSuccess()) {
                    showSuccess(result.getMessage());
                    accountField.setText("");
                    amountField.setText("");
                } else {
                    showError(result.getMessage());
                }
            } catch (Exception ex) {
                showError("Please enter valid details");
            }
        });
        
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));
        
        card.add(Box.createVerticalStrut(20));
        card.add(new JLabel("Target Account:"));
        card.add(accountField);
        card.add(Box.createVerticalStrut(10));
        card.add(new JLabel("Amount:"));
        card.add(amountField);
        card.add(Box.createVerticalStrut(20));
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(backBtn);
        btnPanel.add(transferBtn);
        card.add(btnPanel);
        
        panel.add(card);
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        
        String[] columns = {"Date/Time", "Type", "Amount", "Balance"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        for (Transaction t : atm.getHistory()) {
            model.addRow(new Object[]{
                t.getFormattedTimestamp(), t.getType(), 
                t.getFormattedAmount(), t.getFormattedBalance()
            });
        }
        
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        
        JButton backBtn = createButton("BACK TO MENU", PRIMARY);
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(backBtn, BorderLayout.SOUTH);
        return panel;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        
        JPasswordField oldPinField = new JPasswordField(4);
        oldPinField.setMaximumSize(new Dimension(150, 30));
        
        JPasswordField newPinField = new JPasswordField(4);
        newPinField.setMaximumSize(new Dimension(150, 30));
        
        JButton changeBtn = createButton("CHANGE PIN", SUCCESS);
        JButton backBtn = createButton("BACK", PRIMARY);
        
        changeBtn.addActionListener(e -> {
            String oldPin = new String(oldPinField.getPassword());
            String newPin = new String(newPinField.getPassword());
            
            if (validateInput(newPin, "New PIN", "\\d{4}")) {
                if (atm.changePin(oldPin, newPin)) {
                    showSuccess("PIN changed successfully!");
                    oldPinField.setText("");
                    newPinField.setText("");
                } else {
                    showError("Invalid current PIN");
                }
            }
        });
        
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));
        
        card.add(new JLabel("Current PIN:"));
        card.add(oldPinField);
        card.add(Box.createVerticalStrut(10));
        card.add(new JLabel("New PIN:"));
        card.add(newPinField);
        card.add(Box.createVerticalStrut(20));
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);
        btnPanel.add(backBtn);
        btnPanel.add(changeBtn);
        card.add(btnPanel);
        
        panel.add(card);
        return panel;
    }
    
    // Utility methods
    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.cyan);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));
        return card;
    }
    
    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setFocusPainted(false);
        return btn;
    }
    
    private JButton createMenuButton(String text) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setPreferredSize(new Dimension(200, 80));
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(PRIMARY, 2));
        return btn;
    }
    
    private boolean validateInput(String input, String field, String pattern) {
        if (input == null || input.trim().isEmpty()) {
            showError(field + " cannot be empty");
            return false;
        }
        if (pattern != null && !Pattern.matches(pattern, input)) {
            showError("Invalid " + field + " format");
            return false;
        }
        return true;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new EnhancedATMApp().setVisible(true);
            } catch (Exception e) {
                System.err.println("Application startup failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}