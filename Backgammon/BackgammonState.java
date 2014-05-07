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
    
    /**
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

    public double getUtility() {
        return utility;
    }
    
    public int getDieRoll(int die) {
        return dice[die];
    }
    
    public int getSelectedDie() {
        return selectedDie;
    }
    
    public boolean changeSelectedDie() {
        if (usedDice[1-selectedDie] == 0 || (dice[0] == dice[1] && usedDice[1-selectedDie] == 1)){
            selectedDie = 1-selectedDie;
            return true;
        }
        return false;
    }

    public int getPlayerNum(int space) {
        return board[space] & 3;
    }
    
    public int getCount(int space) {
        return board[space] / (byte)4;
    }

    public int getPlayerToMove() {
        return moveCount % 2 + 1;
    }
    
    //used by the AI player to ignore the second die on a doubles roll (eliminates some duplicate states)
    public boolean shouldIgnoreDoubles(){
        return (dice[0] == dice[1] && usedDice[selectedDie] != 2);
    }

    //move the piece at the indicated space if it is a valid move
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
            if (space2 > 25) space2 = 25;
            else if (space2 < 0) space2 = 0;
            
            if (space2 == 0){ //player piece reaches the end (it is removed from play)
                utility += .00001 + space / 500.0; //add .00001 as a small incentive for the AI to remove pieces
                if (isTerminal()) utility = 1; //victory
            }
            else if (space2 == 25){ //player piece reaches the end (it is removed from play)
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
    
    public final void rollDice(){
        Random rand = new Random();
        dice[0] = rand.nextInt(6) + 1;
        dice[1] = rand.nextInt(6) + 1;
        usedDice[0] = 0;
        usedDice[1] = 0;
        selectedDie = 0;
        utility = 1-utility;
    }
    
    //used by the AI to consider specific future dice rolls
    public void setDice(int x, int y){
        dice[0] = x;
        dice[1] = y;
    }

    //checks if a player can move his/her pieces off the board... requires all of his/her
    //pieces to be in the final 6 spaces.
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
            for (int i = 7; i < 25; i++){
                if ((board[i] & 2) == 2) return false;
            }
            
            if (space == dice[selectedDie]) return true;
            
            for (int i = 6; i > space; i--){
                if ((board[i] & 2) == 2) return false;
            }
        }
        return true;
    }
    
    //Checks if a given piece can be moved
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
    
    //determines if any piece can be moved
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
    
    //determines if the current player has won by checking if he has no pieces left
    public boolean isTerminal(){
        int playerNum = getPlayerToMove();
        for (int i = 0; i < 26; i++){
            if ((board[i] & 3) == playerNum) return false;
        }
        return true;
    }
    
    //returns the winning player's number, assumes one player has no tiles remaining
    public int getWinner(){
        for (int i = 0; i < 26; i++){
            if ((board[i] & 3) == 1) return 1;
            if ((board[i] & 3) == 2) return 0;
        }
        return -1;
    }

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
