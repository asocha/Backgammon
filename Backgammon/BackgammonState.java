package Backgammon;

import java.util.Random;

/**
 * A state of the Backgammon game is characterized by a board containing a
 * grid of spaces for disks, the next player to move, and a dice roll.
 * The dice can be used or unused, and one of them will be selected.
 * The utility is also stored as part of the state.
 *
 * @author Andrew Socha
 *
 */
public class BackgammonState implements Cloneable {
    
    /*
     * Board uses special bit coding. First bit: disk of player 1, Second bit: disk of
     * player 2, remaining bits: number of disks at that location.
     * This is done in an attempt to speed up State cloning.
     */
    private byte[] board;
    
    private final int[] dice;
    private final int[] usedDice;
    private int selectedDie;

    private int moveCount;
    
    private double utility; //Indicates the utility of the state. 1: win for current player

    /**
     * BackgammonState constructor to create a board state
     * 
     * @param first True if this is the initial state of the game, false otherwise.
     *              If true, we set up the game according to Backgammon's rules.
     *              Otherwise, this state is being used by the AI to clone another state,
     *                  so we don't need to set up the board.
     */
    public BackgammonState(boolean first) {
        dice = new int[2];
        usedDice = new int[2];
        board = new byte[26];
        
        if (first){
            utility = 0.5;

            rollDice();

            board[1] = (byte)(1 + 2*4);
            board[6] = (byte)(2 + 5*4);
            board[8] = (byte)(2 + 3*4);
            board[12] = (byte)(1 + 5*4);
            board[13] = (byte)(2 + 5*4);
            board[17] = (byte)(1 + 3*4);
            board[19] = (byte)(1 + 5*4);
            board[24] = (byte)(2 + 2*4);
        }
    }

    /**
     * Gets the utility of the state
     * 
     * @return the utility (0-1)
     */
    public double getUtility() {
        return utility;
    }
    
    /**
     * Get the roll of one of the dice
     * 
     * @param die the die to get the roll of (0 or 1)
     * @return    the die's roll (1-6)
     */
    public int getDieRoll(int die) {
        return dice[die];
    }
    
    /**
     * Gets the index of the selected die
     * 
     * @return the index of the selected die (0 or 1)
     */
    public int getSelectedDie() {
        return selectedDie;
    }
    
    /**
     * Changes which die is selected if the unselected die hasn't been used this turn.
     * 
     * @return true if the selected die was changed, false otherwise
     */
    public boolean changeSelectedDie() {
        if (usedDice[1-selectedDie] == 0 || (dice[0] == dice[1] && usedDice[1-selectedDie] == 1)){
            selectedDie = 1-selectedDie;
            return true;
        }
        return false;
    }

    /**
     * Gets the number of the player who owns the pieces on a space
     * 
     * @param space a space on the board (0-25)
     * @return the number or the player who owns the pieces (1 or 2)
     *         or 0 if no pieces are in that space
     */
    public int getPlayerNum(int space) {
        return board[space] & 3;
    }
    
    /**
     * Gets the amount of pieces on a space of the board
     * 
     * @param space a space on the board (0-25)
     * @return the number of pieces on that space in the board (0-15)
     */
    public int getCount(int space) {
        return board[space] / (byte)4;
    }

    /**
     * Gets the number of the player whose turn it is
     * 
     * @return the current player's number (1 or 2)
     */
    public int getPlayerToMove() {
        return moveCount % 2 + 1;
    }

    /**
     * Used by the AI player to ignore the second die on a doubles roll.
     * This eliminates some duplicate states in the AI's search tree.
     * 
     * @return true if the AI should ignore the second die roll (doubles were rolled)
     */
    public boolean shouldIgnoreDoubles(){
        return (dice[0] == dice[1]);
    }

    /**
     * Move the piece at the indicated space if it is a valid move
     * 
     * @param space         the space whose piece we are moving (0-25)
     *                      50-75 if the AI is playing with the unselected die
     *                      -1 if the AI is playing and there are no valid moves
     * @param isComputer    True if the AI is playing, false otherwise.
     *                      This is solely used to speed up the move since the AI
     *                      already knows its move is valid, but we have to check
     *                      if a human player's move is valid.
     */
    public void moveDisk(int space, boolean isComputer) {
        if (space > 40){ //computer adds 50 to actions with the unselected die to differentiate them from
                         //actions with the currently selected die
            space -= 50;
            selectedDie = 1 - selectedDie;
        }
        
        if (space != -1 && (isComputer || isValid(space) != 0)) {
            int playerNum = getPlayerToMove();
            usedDice[selectedDie]++;
            
            boolean endTurn = (dice[0] != dice[1] && usedDice[1-selectedDie] == 1) ||
                (usedDice[0] == 2 && usedDice[1] == 2);
            
            //remove the moving piece
            board[space]-=(byte)4;
            if (getCount(space) == 0) board[space] = (byte)0;
            
            int space2 = space - (int)Math.pow(-1, playerNum) * dice[selectedDie];
            
            if (space2 <= 0){ //player piece reaches the end (it is removed from play)
                utility += .00001 + space / 500.0; //add .00001 as a small incentive for the AI to remove pieces
                if (isTerminal()) utility = 1; //victory
            }
            else if (space2 >= 25){ //player piece reaches the end (it is removed from play)
                utility += .00001 + (25 - space) / 500.0;
                if (isTerminal()) utility = 1; //victory
            }
            else if (getPlayerNum(space2) == 3 - playerNum){ // player captures piece
                if (playerNum == 2){ //player 2 is moving
                    if (board[0] == (byte) 0) board[0] = (byte) 1;
                    board[0]+=(byte)4;
                    utility += space2 / 500.0;
                }
                else{ //player 1 is moving
                    if (board[25] == (byte) 0) board[25] = (byte) 2;
                    board[25]+=(byte)4;
                    utility += (25 - space2) / 500.0;
                }
                board[space2] = (byte) (playerNum + 4);
                utility += dice[selectedDie] / 500.0;
            }
            else{ // player moves piece
                if (board[space2] == (byte) 0) board[space2] = (byte) playerNum;
                board[space2]+=(byte)4;
                utility += dice[selectedDie] / 500.0;
            }
            
            if (endTurn){
                moveCount++;
                rollDice();
            }
            else selectedDie = 1 - selectedDie;
        }
        else if (space == -1 || noValidMoves()){ //no valid moves... skip turn
            moveCount++;
            rollDice();
        }
    }
    
    /**
     * Rolls the dice at the start of a new player's turn.
     *      Sets each die to a random value.
     *      Sets the each die to unused.
     *      Sets the selected die to die index 0.
     *      Inverts the utility of the state.
     */
    public final void rollDice(){
        Random rand = new Random();
        dice[0] = rand.nextInt(6) + 1;
        dice[1] = rand.nextInt(6) + 1;
        usedDice[0] = 0;
        usedDice[1] = 0;
        selectedDie = 0;
        utility = 1-utility;
    }

    /**
     * Sets the dice to specific rolls
     * Used by the AI to consider specific future dice rolls
     * 
     * @param x the first die's value (1-6)
     * @param y the second die's value (1-6)
     */
    public void setDice(int x, int y){
        dice[0] = x;
        dice[1] = y;
    }

    /**
     * Checks if a player can move his/her pieces off the board, which requires all of his/her
     * pieces to be in the final 6 spaces.
     * 
     * @param space the space the player is trying to move out from (1-6 or 19-24)
     * @return      true if the piece can be moved out, false otherwise
     */
    private boolean canMoveOut(int space){
        if (getPlayerToMove() == 1){
            //check that all our pieces are in the last 6 spaces
            for (int i = 0; i < 19; i++){
                if ((board[i] & 1) == 1) return false;
            }
            
            //check if we're moving our exact die amount
            if (space == 25 - dice[selectedDie]) return true;
            
            //if we can move the exact dice amount, force that
            for (int i = 19; i < space; i++){
                if ((board[i] & 1) == 1) return false;
            }
        }
        else{
            for (int i = 7; i < 26; i++){
                if ((board[i] & 2) == 2) return false;
            }
            
            if (space == dice[selectedDie]) return true;
            
            for (int i = 6; i > space; i--){
                if ((board[i] & 2) == 2) return false;
            }
        }
        return true;
    }

    /**
     * Checks if a given piece can be moved with the currently selected die
     * 
     * @param space the space of the piece that we are checking (0-25)
     * @return      2 if the piece can move and either captures or moves out.
     *              1 if the piece can simply move.
     *              0 if the piece cannot move.
     */
    public int isValid(int space) {
        int playerNum = getPlayerToMove();
        int space2 = space - (int)Math.pow(-1, playerNum) * dice[selectedDie];
        if (space2 > 25) space2 = 25;
        else if (space2 < 0) space2 = 0;
        
        int player2 = getPlayerNum(space2);
        boolean valid = getPlayerNum(space) == playerNum && //moving our own piece
                (space == 0 || space == 25 || getCount((playerNum - 1) * 25) == 0) && //no captured piece or moving captured piece
                
                //valid move
                (player2 == playerNum || //move to our own space
                (player2 == 0 && space2 != 0 && space2 != 25) || //move to empty space
                (player2 == 3 - playerNum && //capture a piece
                        getCount(space2) == 1 && space2 != 0 && space2 != 25) ||
                ((space2 == 0 || space2 == 25) && canMoveOut(space))); //move out
        if (valid){
            if (player2 == 3 - playerNum || space2 == 0 || space2 == 25) //capture or move out
                return 2;
            return 1;
        }
        return 0;
    }
    
    /**
     * Determines if any piece can be moved by the current player (with either die)
     * 
     * @return true if no piece can be moved
     *         false if any piece can be moved
     */
    private boolean noValidMoves(){
        int capturedLoc = (getPlayerToMove() - 1) * 25;
        if (getCount(capturedLoc) != 0){ //player has a captured piece
            if (isValid(capturedLoc) != 0) return false;
            if (changeSelectedDie()){
                if (isValid(capturedLoc) != 0){
                    changeSelectedDie();
                    return false;
                }
                changeSelectedDie();
            }
        }
        else{
            for (int i = 1; i < 25; i++){
                if (isValid(i) != 0) return false;
            }
            if (changeSelectedDie()){
                for (int i = 1; i < 25; i++){
                    if (isValid(i) != 0){
                        changeSelectedDie();
                        return false;
                    }
                }
                changeSelectedDie();
            }
        }
        return true;
    }

    /**
     * Determines if the current player has won by checking if he has no pieces left.
     * Assumes that all of the player's pieces are in his/her home board (final 6 spaces).
     * 
     * @return true if the current player has won, false otherwise
     */
    public boolean isTerminal(){
        if (getPlayerToMove() == 1){
            for (int i = 19; i < 26; i++){
                if ((board[i] & 1) == 1) return false;
            }
        }
        else{
            for (int i = 1; i < 7; i++){
                if ((board[i] & 2) == 2) return false;
            }
        }
        return true;
    }

    /**
     * Gets the index of the winning player.
     * Assumes one player has no tiles remaining.
     * 
     * @return the winning player's index
     */
    public int getWinner(){
        for (int i = 0; i < 26; i++){
            if ((board[i] & 3) == 1) return 1;
            if ((board[i] & 3) == 2) return 0;
        }
        return -1;
    }

    /**
     * Clones the BackgammonState
     * 
     * @return the cloned state
     */
    @Override
    public BackgammonState clone() {
        BackgammonState result = new BackgammonState(false);
        result.utility = utility;
        result.moveCount = moveCount;
        result.selectedDie = selectedDie;
        
        result.board = board.clone();
        result.dice[0] = dice[0];
        result.dice[1] = dice[1];
        result.usedDice[0] = usedDice[0];
        result.usedDice[1] = usedDice[1];
        return result;
    }
}
