// This code implements mainly with MVC design pattern. Other than that, it also implement some other design patterns such as:
// Observer pattern: Interaction between KwazamModel class and KwazamView class. KwazamView acts as an observer and changes whenever KwazamModel has updates
// Composite pattern: The chess board grid structure is an example.
// Builder pattern: Setting up main menu and game UI.
//
// This code also implements object oriented programming concepts such as:
// Inheritance: all the class that uses 'extends'.
// Encapsulation: The implementation of MVC pattern.
// Polymorphism: All types of pieces (Sau, Biz, etc.) are treated as Piece 

import java.util.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;


// Main Menu class,it displays a menu interface with 3 options, Start a new game, Load a save game and quit game. 
// It will navigate to the game interface or quit game. 
class MainMenu extends JFrame {
    public MainMenu() { //Rusyaidi
        setTitle("Welcome to Kwazam Chess");//title for game
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window on screen

        JPanel menu = new JPanel();//panel for buttons 
        menu.setLayout(new GridLayout(4, 1, 10, 10));

        JLabel title = new JLabel("Kwazam Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 24));

        // Buttons
        JButton start = new JButton("New Game");
        JButton load = new JButton("Load Game");
        JButton quit = new JButton("Quit");

        start.addActionListener(e -> startNewGame()); //add action if button pressed
        load.addActionListener(e -> loadGame());
        quit.addActionListener(e -> System.exit(0));

        menu.add(title);
        menu.add(start);
        menu.add(load);
        menu.add(quit);
        add(menu);

        setVisible(true);
    }

//here it will create the game
    private void startNewGame() { 
        dispose(); // Close the main menu, no duplicate window
        KwazamModel model = new KwazamModel();
        KwazamView view = new KwazamView();
        new KwazamController(model, view);
        view.setVisible(true);
    }

    private void loadGame() { 
// Loads a saved game, initializes a new view, and closes the current window if successful..
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                KwazamModel model = new KwazamModel();
                model.loadGame(file);
                KwazamView view = new KwazamView();
                new KwazamController(model, view);
                view.setVisible(true);
                dispose();  // Close the current window
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Failed to load game: " + e.getMessage());
            }
        }
    }
}

// Model
class KwazamModel {  
    private Piece[][] board; // Represents the game board with pieces
    private boolean isBlueTurn = true; // Indicate if it is Blue’s turn
    private int turnCounter = 0; // Tracks the number of turns taken
    private List<String> moveHistory;  // To track the move history

    public KwazamModel() { 
    // Constructor, initializes the board and move history
        board = new Piece[8][5]; // 8 rows x 5 column
        initializeBoard(); // Set up the initial positions of pieces
        moveHistory = new ArrayList<>();  // Initialize move history
    }

    // Sets up initial position of the pieces on the board
    private void initializeBoard() { 
        // Blue pieces
        board[0][0] = new Xor(false); // Bottom left
        board[0][1] = new Biz(false);
        board[0][2] = new Sau(false); // Bottom center
        board[0][3] = new Biz(false);
        board[0][4] = new Tor(false); // Bottom right
        for (int col = 0; col < 5; col++) {
            board[1][col] = new Ram(false); // Fill the second row with rams
        }

        // Red pieces
        board[7][0] = new Xor(true); // Top left
        board[7][1] = new Biz(true);
        board[7][2] = new Sau(true); // Top center
        board[7][3] = new Biz(true);
        board[7][4] = new Tor(true); // Top right
        for (int col = 0; col < 5; col++) {
            board[6][col] = new Ram(true); // Fill second to last row with rams
        }
    }

    // Saves move history to a file
    public void saveMoveHistoryToFile(File file) throws IOException { // Low Mun Kit
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String move : moveHistory) {
                writer.write(move);  // Write each move to the file
                writer.newLine();    // Add a new line after each move
            }
        }
    }

    // Returns the piece to a specific location
    public Piece getPieceAt(int row, int col) { 
        return board[row][col];
    }

    // Move a piece on the board
    public void movePiece(int startRow, int startCol, int endRow, int endCol) { 
        Piece piece = board[startRow][startCol];
        Piece targetPiece = board[endRow][endCol];

        if (piece instanceof Ram) { 
            if (!piece.isValidMove(startRow, startCol, endRow, endCol, board)) {
                throw new IllegalArgumentException("Invalid move for Ram.");
            }
            ((Ram) piece).handleEndOfBoard(endRow);
        }

        // Check if the move ends on a sau
       // GameOverException, the exception is thrown when the Sau is captured
        if (targetPiece instanceof Sau) { 
            board[endRow][endCol] = piece; // Replace sau with the moving piece
            board[startRow][startCol] = null; // Clear the starting position
            throw new GameOverException(piece.isBlue() ? "Blue" : "Red"); // Game over
        }

        // Perform a regular move
        board[endRow][endCol] = piece;
        board[startRow][startCol] = null;

        turnCounter++;//keep track of turn to switch
        if (turnCounter % 4 == 0) {//if it reach 2 turn swap Tor/Xor
            transformPieces();
        }

        // Add the move to the history (format: Blue: Xor moves from (0,0) to (1,0))
        String move = (piece.isBlue() ? "Blue" : "Red") + ": " + piece.getClass().getSimpleName() +
            " moves from (" + startRow + "," + startCol + ") to (" + endRow + "," + endCol + ")";
        moveHistory.add(move);

        isBlueTurn = !isBlueTurn;
    }

    public void loadMoveHistoryFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            moveHistory.clear();  // Clear any existing history
            while ((line = reader.readLine()) != null) {
                moveHistory.add(line);  // Add each move to the history
            }
        }
    }
    // Get the move history
    public List<String> getMoveHistory() {
        return moveHistory;
    }

//transform piece for Xor and Tor
    private void transformPieces() { 
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 5; col++) {
                Piece piece = board[row][col];//get the piece to swap

                if (piece instanceof Tor) {
                    board[row][col] = new Xor(piece.isBlue());//replace Tor with Xor
                } else if (piece instanceof Xor) {
                    board[row][col] = new Tor(piece.isBlue());//replace Xor with Tor
                }
            }
        }
    }

  // validate whether a move is valid
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol) { 
        if (endRow < 0 || endRow >= 8 || endCol < 0 || endCol >= 5) return false; // Check bounds & ensure the piece exists & moves to a valid target
        Piece piece = board[startRow][startCol];
        if (piece == null || (board[endRow][endCol] != null && board[endRow][endCol].isBlue() == piece.isBlue())) return false;

       // validate the path for non-Biz pieces
        if (!(piece instanceof Biz)) {
            int rowStep = Integer.signum(endRow - startRow);
            int colStep = Integer.signum(endCol - startCol);
            int currentRow = startRow + rowStep;
            int currentCol = startCol + colStep;
            while (currentRow != endRow || currentCol != endCol) {
                if (board[currentRow][currentCol] != null) return false;
                currentRow += rowStep;
                currentCol += colStep;
            }
        }
        return piece.isValidMove(startRow, startCol, endRow, endCol, board);
    }

//return if piece is blue
    public boolean isBlueTurn() {
        return isBlueTurn;
    }


    public void saveGame(File file) throws IOException {
// Save the board, turn info, and turn counter to a file.
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(board);
            out.writeBoolean(isBlueTurn);
            out.writeInt(turnCounter);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadGame(File file) throws IOException, ClassNotFoundException {
// Load the board, turn info, and turn counter to a file.
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            board = (Piece[][]) in.readObject();
            isBlueTurn = in.readBoolean();
            turnCounter = in.readInt();
        }
    }
}

abstract class Piece implements Serializable {
    protected boolean isBlue;

    public Piece(boolean isBlue) {
        this.isBlue = isBlue;
    }

    public boolean isBlue() {
        return isBlue;
    }

    public abstract boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board);
}
// Each piece type has unique movement rules
// Each subclass overrides the isValidMove method to enforce its specific movement rules
class Ram extends Piece { // Low Mun Kit
    private int direction;
    private boolean isFlipped;

    public Ram(boolean isBlue) {
        super(isBlue);
        this.direction = isBlue ? -1 : 1;
        this.isFlipped = false;
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        int direction = this.isFlipped ? 1 : (this.isBlue ? -1 : 1);
        int targetRow = startRow + direction;

        // Ensure the target is within bounds
        if (endRow < 0 || endRow >= board.length || endCol < 0 || endCol >= board[0].length) {
            return false;
        }

        // Straight movement (forward advance)
        if (endRow == targetRow && endCol == startCol) {
            Piece targetPiece = board[endRow][endCol];
            return targetPiece == null || targetPiece.isBlue() != this.isBlue; // Valid if empty or opponent's piece
        }

        return false; // All other moves are invalid for Ram
    }

    public void handleEndOfBoard(int currentRow) { 
        // Flip the Ram's direction and state when it reaches the end of the board
        if ((isBlue && (currentRow == 0 || currentRow == 7)) || (!isBlue && (currentRow == 7 || currentRow == 0))) {
            direction = -direction; // Reverse direction
            isFlipped = !isFlipped; // Toggle the flipped state
        }
    }

    public boolean isFlipped() {
        return isFlipped;
    }
}

class Biz extends Piece { 
    public Biz(boolean isBlue) {
        super(isBlue);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);
        return rowDiff * colDiff == 2;
    }
}

class Tor extends Piece {
    public Tor(boolean isBlue) {
        super(isBlue);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        return (startRow == endRow || startCol == endCol) && isPathClear(startRow, startCol, endRow, endCol, board);
    }

    private boolean isPathClear(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        int rowStep = Integer.signum(endRow - startRow);
        int colStep = Integer.signum(endCol - startCol);
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;
        while (currentRow != endRow || currentCol != endCol) {
            if (board[currentRow][currentCol] != null) return false;
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }
}

class Xor extends Piece { 
    public Xor(boolean isBlue) {
        super(isBlue);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        return (Math.abs(startRow - endRow) == Math.abs(startCol - endCol)) && isPathClear(startRow, startCol, endRow, endCol, board);
    }

    private boolean isPathClear(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        int rowStep = Integer.signum(endRow - startRow);
        int colStep = Integer.signum(endCol - startCol);
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;
        while (currentRow != endRow || currentCol != endCol) {
            if (board[currentRow][currentCol] != null) return false;
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }
}

class Sau extends Piece { 
    public Sau(boolean isBlue) {
        super(isBlue);
    }

    @Override
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol, Piece[][] board) {
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);

        // Sau can move one square in any direction
        if (rowDiff <= 1 && colDiff <= 1) {
            Piece targetPiece = board[endRow][endCol];
            return targetPiece == null || targetPiece.isBlue() != this.isBlue; // Valid if empty or opponent's piece
        }

        return false;
    }

}

// Exception for game over
class GameOverException extends RuntimeException { // Low Mun Kit
    public GameOverException(String winner) {
        super(winner);
    }
}


// View. This class is responsible for the game interface.

class KwazamView extends JFrame {
    private JButton[][] boardSquares = new JButton[8][5];
    private JLabel turnIndicator;
    private JButton saveButton;
    private JButton loadButton;
    private Map<String, ImageIcon> pieceIcons;
    private JTextArea moveHistoryArea;  // To display move history

    public KwazamView() {
        // Frame of the game
        setTitle("Kwazam Chess");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize turn indicator
        turnIndicator = new JLabel("Blue's Turn", SwingConstants.CENTER);
        turnIndicator.setFont(new Font("Arial", Font.BOLD, 16));
        add(turnIndicator, BorderLayout.NORTH);

        // Initialize chessboard
        JPanel boardPanel = new JPanel(new GridLayout(8, 5));
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 5; col++) {
                JButton button = new JButton();
                button.setBackground(Color.WHITE);
                button.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                boardSquares[row][col] = button;
                boardPanel.add(button);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        // Initialize control panel, for save and load game
        JPanel controlPanel = new JPanel();
        saveButton = new JButton("Save Game");
        loadButton = new JButton("Load Game");
        controlPanel.add(saveButton);
        controlPanel.add(loadButton);
        add(controlPanel, BorderLayout.SOUTH);

        // Initialize move history display
        moveHistoryArea = new JTextArea(10, 30);
        moveHistoryArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(moveHistoryArea);
        add(scrollPane, BorderLayout.EAST);
        //Load piece icons
        loadPieceIcons();
    }

    //Load piece icons from resources folder
    private void loadPieceIcons() {
        pieceIcons = new HashMap<>(); // Store the association between a piece's name (as a string) and its corresponding ImageIcon. Like a search table.
        String[] pieceNames = {"Xor", "Biz", "Sau", "Tor", "Ram"};
        for (String pieceName : pieceNames) {
            pieceIcons.put("Blue_" + pieceName, new ImageIcon("resources/Blue_" + pieceName + ".png"));
            pieceIcons.put("Red_" + pieceName, new ImageIcon("resources/Red_" + pieceName + ".png"));
        }
    }

    //Rotate an image by 180 degrees. Used to fit with the flipped board.
    private ImageIcon rotateImage(ImageIcon icon) {
        Image image = icon.getImage();
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics(); // Allows images to be rotated
        AffineTransform transform = AffineTransform.getRotateInstance(Math.PI, image.getWidth(null) / 2.0, image.getHeight(null) / 2.0);
        g2d.drawImage(image, transform, null); // Image is converted
        g2d.dispose();
        return new ImageIcon(bufferedImage);
    }

// Logs for all piece movements done.
    public void updateMoveHistory(List<String> moveHistory) {
        moveHistoryArea.setText("");  // Clear the existing text
        for (String move : moveHistory) {
            moveHistoryArea.append(move + "\n");  // Append each move in the history
        }
    }

    //Update the board to reflect the model's state
    public void updateBoard(KwazamModel model, boolean flipped) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 5; col++) {
                int displayRow = flipped ? 7 - row : row;
                int displayCol = flipped ? 4 - col : col;

                Piece piece = model.getPieceAt(displayRow, displayCol);
                JButton button = boardSquares[row][col];

                if (piece != null) {
                    String key = (piece.isBlue() ? "Blue_" : "Red_") + piece.getClass().getSimpleName();
                    ImageIcon icon = pieceIcons.get(key);

                    if (piece instanceof Ram) {
                        if (((Ram) piece).isFlipped()) {
                            icon = rotateImage(icon);
                        }
                    }

                    if (flipped) {
                        icon = rotateImage(icon);
                    }
                    button.setIcon(icon);
                } else {
                    button.setIcon(null);
                }
                button.setBackground(Color.WHITE);
            }
        }

        turnIndicator.setText(model.isBlueTurn() ? "Blue's Turn" : "Red's Turn");
        updateMoveHistory(model.getMoveHistory());
    }

    // Getter methods
    public JButton getSquare(int row, int col) {
        return boardSquares[row][col];
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getLoadButton() {
        return loadButton;
    }
}

// Controller
// Listens for user actions, processes them, and updates the model and view accordingly.
class KwazamController {
    private KwazamModel model;
    private KwazamView view;
    private int[] selectedSquare = null;
    private List<int[]> highlightedSquares = new ArrayList<>();

    public KwazamController(KwazamModel model, KwazamView view) {
        this.model = model;
        this.view = view;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 5; col++) {
                final int r = row;
                final int c = col;
                view.getSquare(row, col).addActionListener(e -> handleSquareClick(r, c));
            }
        }

        view.getSaveButton().addActionListener(e -> handleSave());
        view.getLoadButton().addActionListener(e -> handleLoad());

        view.updateBoard(model, false);
    }

    // Handles user interactions when a player clicks on a square on the chessboard
    private void handleSquareClick(int row, int col) {
        boolean flipped = !model.isBlueTurn();
        int logicalRow = flipped ? 7 - row : row;
        int logicalCol = flipped ? 4 - col : col;

        if (selectedSquare == null) {
            Piece piece = model.getPieceAt(logicalRow, logicalCol);
            if (piece != null && piece.isBlue() == model.isBlueTurn()) {
                selectedSquare = new int[]{logicalRow, logicalCol};
                highlightPossibleMoves(logicalRow, logicalCol); // Highlight possible moves
            }
        } else {
            clearHighlights(); // Clear previous highlights

            int startRow = selectedSquare[0];
            int startCol = selectedSquare[1];

            try {
                if (model.isValidMove(startRow, startCol, logicalRow, logicalCol)) {
                    model.movePiece(startRow, startCol, logicalRow, logicalCol);
                    selectedSquare = null;
                    view.updateBoard(model, !model.isBlueTurn());
                } else {
                    JOptionPane.showMessageDialog(view, "Invalid move!");
                    selectedSquare = null;
                }
            } catch (GameOverException e) {
                JOptionPane.showMessageDialog(view, "Game Over! Winner: " + e.getMessage());
                System.exit(0);
            }
        }
    }

// Show possible moves for selected chess piece
    private void highlightPossibleMoves(int row, int col) {
        Piece piece = model.getPieceAt(row, col); // Get the piece at destination
        if (piece == null) return; // No piece is selected

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 5; c++) {
                try {
                    if (model.isValidMove(row, col, r, c)) {
                        highlightedSquares.add(new int[]{r, c}); // Adjust display for flipped board
                        boolean flipped = !model.isBlueTurn();
                        int displayRow = flipped ? 7 - r : r;
                        int displayCol = flipped ? 4 - c : c;
                        view.getSquare(displayRow, displayCol).setBackground(Color.YELLOW); // Highlighting
                    }
                } catch (Exception e) {
                    // Catch unexpected exceptions to prevent UI issues
                    e.printStackTrace();
                }
            }
        }
    }

// Clear the highlighting of possible moves
    private void clearHighlights() {
// Loop through all highlighted squares stored in list
        for (int[] square : highlightedSquares) {
            int row = square[0];
            int col = square[1]; 
            boolean flipped = !model.isBlueTurn(); // Adjust display for flipped board
            int displayRow = flipped ? 7 - row : row;
            int displayCol = flipped ? 4 - col : col;
            view.getSquare(displayRow, displayCol).setBackground(Color.WHITE); // Reset background to white
        }
        highlightedSquares.clear();
    }

//Allows the user to choose a location to save the current game state
    private void handleSave() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(view) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                model.saveGame(file);
                File historyFile = new File(file.getParent(), "move_history.txt");
                model.saveMoveHistoryToFile(historyFile);
                JOptionPane.showMessageDialog(view, "Game saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(view, "Failed to save game: " + e.getMessage());
            }
        }
    }

    private void handleLoad() {
    // Prompt the user to save the current game first before continue
    int choice = JOptionPane.showConfirmDialog(
        view, 
        "Do you want to save the current game before loading?", 
        "Load Game", 
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
        // User canceled or closed the dialog
        return;
    }

    if (choice == JOptionPane.YES_OPTION) {
        // User chose to save the game
        handleSave();
    }

    // Proceed with loading the game
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try {
            model.loadGame(file);
            File historyFile = new File(file.getParent(), "move_history.txt");
            if (historyFile.exists()) {
                model.loadMoveHistoryFromFile(historyFile);
            }
            view.updateBoard(model, !model.isBlueTurn());
            JOptionPane.showMessageDialog(view, "Game loaded successfully.");
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(view, "Failed to load game: " + e.getMessage());
        }
    }
}

}

// Main
public class KwazamChess {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainMenu());
    }
}


