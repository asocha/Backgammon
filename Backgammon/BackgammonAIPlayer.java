package Backgammon;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import aima.core.search.adversarial.Game;
import aima.core.search.adversarial.IterativeDeepeningAlphaBetaSearch;
import aima.core.search.framework.Metrics;

/**
 * Implements an Iterative Deepening Adversarial Search with Alpha-Beta Pruning and chance nodes.
 *
 * @author Andrew Socha
 */
public class BackgammonAIPlayer extends IterativeDeepeningAlphaBetaSearch<BackgammonState, Integer, String> {
    Metrics metrics;

    /**
     * Creates the AI player for a Backgammon game
     * 
     * @param game the Backgammon game
     * @param time the time the AI takes to determine its moves (in milliseconds)
     */
    public BackgammonAIPlayer(Game<BackgammonState, Integer, String> game, int time) {
        super(game, 0.0, 1.0, time); //game, utilMin, utilMax, time
    }

    /**
     * Checks if one utility is significantly better than another.
     * Used by the AI to end its search early if it finds one move that is
     * way better than all others.
     * 
     * @param newUtility the newly found utility (currently the best one the AI has found) (0-1)
     * @param utility    the utility that was previously the best on the AI had found (0-1)
     * @return           true if the new utility is significantly better than the old one,
     *                   false otherwise
     */
    @Override
    protected boolean isSignificantlyBetter(double newUtility, double utility) {
        return newUtility - utility > 0.03;
    }

    /**
     * Not used, but forced to Override it
     */
    @Override
    protected boolean hasSafeWinner(double resultUtility) {
        return (resultUtility == 1);
    }

    /**
     * Determines the utility of a state for a certain player
     * 
     * @param state     any game state
     * @param player    the name of either player
     * @return          the utility of the state for the given player
     */
    @Override
    protected double eval(BackgammonState state, String player) {
        if (state.getUtility() != 1 && state.getUtility() != 0) maxDepthReached = true;
        return game.getUtility(state, player);
    }
    
    /**
     * Figures out what move the AI player will make using Iterative Deepening
     * Adversarial Search with Alpha-Beta Pruning
     * 
     * @param state the current game state
     * @return      the move that the AI choose (0-25 or 50-75, or -1 for no valid moves)
     */
    @Override
    public Integer makeDecision(BackgammonState state) {
        maxTime += System.currentTimeMillis();
        metrics = new Metrics();
        List<Integer> results = new ArrayList<>();
        double resultValue = Double.NEGATIVE_INFINITY;
        String player = game.getPlayer(state);
        StringBuffer logText;
        expandedNodes = 0;
        maxDepth = 0;
        currDepthLimit = 0;
        boolean exit = false;
        double newResultValue, secondBestValue, value, alpha;
        
        BackgammonState resultState;
        List<Integer> actions = game.getActions(state);
        do {
            incrementDepthLimit();
            maxDepthReached = false;
            List<Integer> newResults = new ArrayList<>();
            newResultValue = Double.NEGATIVE_INFINITY;
            secondBestValue = Double.NEGATIVE_INFINITY;
            alpha = Double.NEGATIVE_INFINITY;
            logText = new StringBuffer("depth " + currDepthLimit + ": ");
            
            for (Integer action : results){ //place the move(s) that were best in the previous iteration into the first spots in the list
                actions.remove(action);         //which should improve alpha-beta pruning
                actions.add(0, action);
            }
            
            //begin searching for the best action with the current depth
            for (Integer action : actions) {
                if (System.currentTimeMillis() > maxTime) {
                    exit = true;
                    break;
                }
                
                resultState = ((BackgammonGame)game).getResult(state, action, true);
                
                if (game.getPlayer(resultState).length() == player.length()){ //max is playing next
                    value = maxValue(resultState, player, alpha, Double.POSITIVE_INFINITY, 1);
                }
                else{
                    //the next state is a fresh turn, so we must consider all possible dice rolls
                    value = 0;
                    for (int i = 1; i < 7; i++){
                        for (int j = i; j < 7; j++){
                            resultState.setDice(i,j);
                            value += minValue(resultState, player, alpha, Double.POSITIVE_INFINITY, 1) / (i == j ? 36.0 : 18.0);
                        }
                    }
                }
                
                //round value to a few decimal places
                value = value * 1000000000;
                value = Math.round(value);
                value = value / 1000000000;
                
                logText.append(action).append("->").append(value).append(" ");
                if (value >= newResultValue) {
                    if (value > newResultValue) {
                        secondBestValue = newResultValue;
                        newResultValue = value;
                        alpha = value; //update alpha
                        newResults.clear();
                    }
                    newResults.add(action); //place all equally best results in arraylist
                }
                else if (value > secondBestValue) {
                    secondBestValue = value;
                }
            }

            metrics.set(String.valueOf(currDepthLimit),logText.toString());
            if (!exit || isSignificantlyBetter(newResultValue, resultValue)) {
                results = newResults;
                resultValue = newResultValue;
            }
            
            //end search at the current depth if we found a really good move
            if (results.size() == 1 && isSignificantlyBetter(resultValue, secondBestValue)) break;
        } while (!exit && maxDepthReached && resultValue != 1);
        
        //break ties by moving the piece that is closest to the end if we can move out,
            //or the piece furthest from the end if we cannot move out
        actions.clear();
        for (Integer result : results) {
            actions.add(result % 50);
        }
        Collections.sort(actions);
        Integer result;
        if ((player.equals("Black (Clockwise)") && actions.get(0) > 18) || (!player.equals("Black (Clockwise)") && actions.get(actions.size()-1) > 6)) result = actions.get(actions.size()-1);
        else result = actions.get(0);
        
        if (results.contains(result)) return result;
        return result + 50;
    }

    /**
     * Determines the maximum estimated utility that can be obtained from all of
     * the possible moves avaliable in a game state
     * 
     * @param state     any game state
     * @param player    the current player (of the game, not necessarily the state)
     * @param alpha     alpha for alpha-beta pruning
     * @param beta      beta for alpha-beta pruning
     * @param depth     the depth in the search tree
     * @return          the calculated utility
     */
    @Override
    public double maxValue(BackgammonState state, String player, double alpha, double beta, int depth) {
        expandedNodes++;
        maxDepth = Math.max(maxDepth, depth);
        if (depth >= currDepthLimit || state.getUtility() == 1 || state.getUtility() == 0) {
            return eval(state, player); //leaf node, return utility
        }
        else {
            double value = 0, newValue;
            BackgammonState resultState;
            for (Integer action : game.getActions(state)) {
                resultState = ((BackgammonGame)game).getResult(state, action, true);
                
                if (game.getPlayer(resultState).length() == player.length()){ //max is playing next
                    newValue = maxValue(resultState, player, alpha, beta, depth + 1);
                }
                else{
                    //the next state is a fresh turn, so we must consider all possible dice rolls
                    newValue = 0;
                    for (int i = 1; i < 7; i++){
                        for (int j = i; j < 7; j++){
                            resultState.setDice(i,j);
                            newValue += minValue(resultState, player, alpha, beta, depth + 1) / (i == j ? 36.0 : 18.0);
                        }
                    }
                }
                
                if (newValue >= beta) return newValue;
                
                value = Math.max(value, newValue);
                alpha = Math.max(alpha, value);
            }
            return value;
        }
    }
    
    /**
     * Determines the minimum estimated utility that can be obtained from all of
     * the possible moves avaliable in a game state
     * 
     * @param state     any game state
     * @param player    the current player (of the game, not necessarily the state)
     * @param alpha     alpha for alpha-beta pruning
     * @param beta      beta for alpha-beta pruning
     * @param depth     the depth in the search tree
     * @return          the calculated utility
     */
    @Override
    public double minValue(BackgammonState state, String player, double alpha, double beta, int depth) {
        expandedNodes++;
        maxDepth = Math.max(maxDepth, depth);
        if (depth >= currDepthLimit || state.getUtility() == 1 || state.getUtility() == 0) {
            return eval(state, player); //leaf node, return utility
        }
        else {
            double value = 1, newValue;
            BackgammonState resultState;
            for (Integer action : game.getActions(state)) {
                resultState = ((BackgammonGame)game).getResult(state, action, true);
                
                if (game.getPlayer(resultState).length() == player.length()){ //max is playing next
                    //the next state is a fresh turn, so we must consider all possible dice rolls
                    newValue = 0;
                    for (int i = 1; i < 7; i++){
                        for (int j = i; j < 7; j++){
                            resultState.setDice(i,j);
                            newValue += maxValue(resultState, player, alpha, beta, depth + 1) / (i == j ? 36.0 : 18.0);
                        }
                    }
                }
                else{
                    newValue = minValue(resultState, player, alpha, beta, depth + 1);
                }

                if (newValue <= alpha) return newValue;
                
                value = Math.min(newValue, value);
                beta = Math.min(beta, value);
            }
            return value;
        }
    }
    
    /**
     * Returns some statistical data from the last search.
     * 
     * @return the search data
     */
    @Override
    public Metrics getMetrics() {
        metrics.set(String.valueOf(-1), "Expanded Nodes: " + expandedNodes);
        metrics.set(String.valueOf(-2), "Max Depth: " + maxDepth);
        return metrics;
    }
}
