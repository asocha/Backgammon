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

    @Override
    public BackgammonState getInitialState() {
        return initialState;
    }

    @Override
    public String[] getPlayers() {
        return players;
    }

    @Override
    public String getPlayer(BackgammonState state) {
        return getPlayer(state.getPlayerToMove());
    }

    /**
     * Returns the player corresponding to the specified player number. For
     * efficiency reasons, BackgammonStates use numbers instead of
     * strings to identify players.
     */
    public String getPlayer(int playerNum) {
        return players[playerNum-1];
    }

    //Creates a list of all the actions that the AI considers for a given state
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

    public BackgammonState getResult(BackgammonState state, Integer action, boolean isComputer) {
        BackgammonState result = state.clone();
        result.moveDisk(action, isComputer);
        return result;
    }
    
    @Override
    //not used, but I'm forced to Override it
    public BackgammonState getResult(BackgammonState state, Integer action) {
        BackgammonState result = state.clone();
        return result;
    }

    @Override
    public boolean isTerminal(BackgammonState state) {
        return (state.getUtility() == 1.0 || state.getUtility() == 0);
    }

    @Override
    public double getUtility(BackgammonState state, String player) {
        //swap the utility value if the state isn't the current player's turn
        if (!player.equals(players[state.getPlayerToMove() - 1])) return 1.0-state.getUtility();
        return state.getUtility();
    }
    
    public String getWinner(BackgammonState state){
        return players[state.getWinner()];
    }
}
