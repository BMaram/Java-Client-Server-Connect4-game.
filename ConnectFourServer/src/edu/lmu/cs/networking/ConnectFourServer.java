package edu.lmu.cs.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A server for a network multi-player Connect Four game.  Modified and
 * extended from the class presented in Deitel and Deitel "Java How to
 * Program" book.  we made a bunch of enhancements and rewrote large sections
 * of the code.  The main change is instead of passing *data* between the
 * client and server, we made a CFP (Connect Four protocol) which is totally
 * plain text, so you can test the game with Telnet (always a good idea.)
 * The strings that are sent in CFP are:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 47)    WELCOME <String>  (String in {"RED", "BLUE"})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 * 
 * @author Maram Mahmoud AlBsisi , Deema Saeed AlGhamdi
 */

public class ConnectFourServer {

    /**
     * Runs the application. Pairs up clients that connect.
     */
    public static void main(String[] args) throws Exception {
        int port = 8901 , backlog = 5;
        String ip = "10.103.1.125";
        ServerSocket listener = new ServerSocket(port);

        System.out.println("Connect Four Server is Running");
        System.out.println("Adress: "+listener.getInetAddress());
        System.out.println("port: "+listener.getLocalSocketAddress());
        try {
            while (true) {
                Game game = new Game();
                Game.Player playerX = game.new Player(listener.accept(), "RED");
                Game.Player playerO = game.new Player(listener.accept(), "BLUE");
                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX;
                playerX.start();
                playerO.start();
            }
        } finally {
            listener.close();
        }
    }
}

/**
 * A two-player game.
 */
class Game {

    /**
     * A board has forty-eight squares. Each square is either unowned or
     * it is owned by a player.  So we use a simple array of player
     * references.  If null, the corresponding square is unowned,
     * otherwise the array cell stores a reference to the player that
     * owns it.
     */
    private Player[] board = {
        null, null, null, null, null, null,null, null,
        null, null, null, null, null, null,null, null,
        null, null, null, null, null, null,null, null,
        null, null, null, null, null, null,null, null,
        null, null, null, null, null, null,null, null,
        null, null, null, null, null, null,null, null};

    /**
     * The current player.
     */
    Player currentPlayer;

    /**
     * Returns whether the current state of the board is such that 
     * the given player is a winner.
     */
    public boolean isWinner() {
         // horizontalCheck 
        for (int j = 0 ; j< 9-4 ; j++){//column
            for (int i = 0 ; i < 48 ; i+=8){//row
                if (    board[i + j]!= null && board[i +j]== board[i +j+1] && board[i +j] == board[i+j+2] && board[i +j] ==  board[i+j+3]){
                return true;
                }
            }
        }
        // verticalCheck
        for (int i = 0 ; i< 24 ; i+=8){
            for (int j = 0 ; j < 9 ; j++){
                if (    board[i  + j]!= null && board[i +j]== board[i+8 +j] && board[i +j] == board[i+(16) +j] && board[i +j] ==  board[i+(24) +j]){
                return true;
                }
            }
        }
        // ascendingDiagonalCheck 
        for (int i = 24 ; i< 48 ; i+=8){
            for (int j = 0 ; j <4 ; j++){
                if (    board[i + j]!= null && board[i +j]== board[(i-8) +j+1] && board[(i-8) +j+1] == board[i-16 +j+2] && board[(i-16) +j+2] ==  board[(i-24) +j+3]){
                return true;
                }
            }
        }
        // descendingDiagonalCheck
        for (int i = 24 ; i< 48 ; i+=8){
            for (int j = 3 ; j < 8; j++){
                if (    board[i  + j]!= null && board[i +j]== board[(i-8) +j-1 ] && board[(i-8) +j-1 ] == board[(i-16) +j-2] && board[(i-16) +j-2] ==  board[(i-24) +j-3]){
                return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Returns whether there are no more empty squares.
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called by the player threads when a player tries to make a
     * move.  This method checks to see if the move is legal: that
     * is, the player requesting the move must be the current player
     * and the square in which she is trying to move must not already
     * be occupied.  If the move is legal the game state is updated
     * (the square is set and the next player becomes current) and
     * the other player is notified of the move so it can update its
     * client.
     */
    public synchronized int legalMove(int location, Player player) {
        int minlocation = (location % 8)+8*5;
        for(int i = minlocation ; i >= location ; i-= 8)
            if (player == currentPlayer && board[i] == null) {
                board[i] = currentPlayer;
                currentPlayer = currentPlayer.opponent;
                currentPlayer.otherPlayerMoved(i);
                return i;
            }
        return -1;
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either '1' or '2'.  For communication with the
     * client the player has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */
    class Player extends Thread {
        String mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        /**
         * Constructs a handler thread for a given socket and mark
         * initializes the stream fields, displays the first two
         * welcoming messages.
         */
        public Player(Socket socket, String mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            }
        }

        /**
         * Accepts notification of who the opponent is.
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Handles the otherPlayerMoved message.
         */
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                isWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        /**
         * The run method of this thread.
         */
        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE All players connected");

                // Tell the first player that it is her turn.
                if (mark.equals("RED")) {
                    output.println("MESSAGE Your move");
                }

                // Repeatedly get commands from the client and process them.
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        int validlocation = legalMove(location, this);
                        if (validlocation!= -1) {
                            output.println("VALID_MOVE"+validlocation);
                            output.println(isWinner() ? "VICTORY"
                                         : boardFilledUp() ? "TIE"
                                         : "");
                        } else {
                            output.println("MESSAGE ?");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}