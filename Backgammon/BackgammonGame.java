package Backgammon;

import aima.core.search.adversarial.Game;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a game of Backgammon.
 *
 * @author Andrew Socha
 *
 */
public class BackgammonGame implements Game<BackgammonState, Integer, String> {

    String[] players = new String[]{"Black (Clockwise)", "White (Counterclockwise)"};
    BackgammonState initialState = new BackgammonState(true);

    /**
     * Gets the first board state of a game
     * 
     * @return the board state
     */
    @Override
    public BackgammonState getInitialState() {
        return initialState;
    }

    /**
     * Not used but forced to Override this
     */
    @Override
    public String[] getPlayers() {
        return players;
    }

    /**
     * Gets the name of the player whose turn it is.
     * 
     * @param state any game state
     * @return      the name of the player who is playing this turn
     */
    @Override
    public String getPlayer(BackgammonState state) {
        return getPlayer(state.getPlayerToMove());
    }

    /**
     * Returns the player corresponding to the specified player number. For
     * efficiency reasons, BackgammonStates use numbers instead of
     * strings to identify players.
     * 
     * @param playerNum the player number: either 1 or 2
     * @return          the player's name
     */
    public String getPlayer(int playerNum) {
        return players[playerNum-1];
    }

    /** Creates a list of all the actions that the AI considers for a given state
     * 
     * @param state any game state
     * @return      A list of valid moves that the AI will consider playing for that game state.
     *              To reduce duplicate game states in the AI's search tree,
     *                  the AI does not consider all valid moves.
     *              If there are no valid moves, the list contains only -1.
     */
    @Override
    public List<Integer> getActions(BackgammonState state) {
        List<Integer> result = new ArrayList<>();
        int valid;
        boolean couldCapture = false;

        for (int i = 0; i < 26; i++) {
            valid = state.isValid(i);
            if (valid == 1) result.add(i);
            else if (valid == 2){ //prioritize capturing and moving out in list
                result.add(0, i);
                couldCapture = true;
            }
        }
        
        if (!state.shouldIgnoreDoubles() && state.changeSelectedDie()){ //check moves with other die
            int initialSize = result.size();
            boolean couldCapture2 = false;
            
            for (int i = 0; i < 26; i++) {
                valid = state.isValid(i);
                if (valid == 2){
                    result.add(0, i + 50); //add 50 to differentiate between dice
                    couldCapture2 = true;
                }
                else if (valid == 1 && (!couldCapture || !result.contains(i) || i == 0 || i == 25)) result.add(i + 50);
                //if we could capture/move out with the first die,
                    //only consider the 2nd die's moves if they capture or move out a piece (prioritized by placing first in list)
                    //or the moving piece couldn't be moved with the first die
                    //or the moving piece is captured
                //otherwise, add all valid moves
            }
            state.changeSelectedDie();
            
            //try to prevent duplicate sets of moves
            if (!couldCapture){
                int mostResults = 50; //die 1 has more moves
                if (couldCapture2 || result.size() > 2*initialSize) mostResults = -50; //die two has more moves
                
                //eliminate some moves of the die with the least moves,
                //keeping captures, move outs, and moves blocked with the other die
                Integer pairedInt;
                for (int i = 0; i < result.size(); i++){
                    pairedInt = result.get(i) + mostResults;
                    if (pairedInt != 0 && pairedInt != 25 && result.contains(pairedInt)){
                        result.remove(pairedInt);
                        i--;
                    }
                }
            }
        }
        
        if (result.isEmpty()) result.add(-1); //no valid move, attempting any move will pass turn
        return result;
    }

    /**
     * Clones a state, performs a move on that state, and returns the result.
     * The state must be cloned because the AI analyzes multiple possible moves on the state.
     * 
     * @param state         the initial game state
     * @param action        the location of the disc to be moved:
     *                      0-25 indicate game spaces;
     *                      50-75 indicate that the game should swap the selected die, then use
     *                          the associated space in 0-2;
     *                      -1 indicates no valid move for the AI (the player can use 0-25 for no valid move)
     * @param isComputer    true if the AI is using this method, false if the player is using it
     * @return              the resulting game state
     */
    public BackgammonState getResult(BackgammonState state, Integer action, boolean isComputer) {
        BackgammonState result = state.clone();
        result.moveDisk(action, isComputer);
        return result;
    }
    
    /**
     * Not used, but I'm forced to Override it 
     * See public BackgammonState getResult(BackgammonState state, Integer action, boolean isComputer)
     *      instead
     */
    @Override
    public BackgammonState getResult(BackgammonState state, Integer action) {
        BackgammonState result = state.clone();
        return result;
    }

    /**
     * Checks if someone has won the game
     * 
     * @param state the current game state
     * @return      true if someone has won, false otherwise
     */
    @Override
    public boolean isTerminal(BackgammonState state) {
        return (state.getUtility() == 1.0 || state.getUtility() == 0);
    }

    /**
     * Gets the utility of a state for a given player
     * 
     * @param state     any game state
     * @param player    either player's name
     * @return          the utility value of the state for the player
     */
    @Override
    public double getUtility(BackgammonState state, String player) {
        //swap the utility value if the state isn't the current player's turn
        if (player.length() != players[state.getPlayerToMove() - 1].length()) return 1.0-state.getUtility();
        return state.getUtility();
    }
    
    /**
     * Gets the winning player of a state.
     * Assumes that one of the players has won (isTerminal() is true).
     * 
     * @param state any game state
     * @return      the name of the winning player
     */
    public String getWinner(BackgammonState state){
        return players[state.getWinner()];
    }
}
