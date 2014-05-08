package Backgammon;

/**
 * Creates a Backgammon game involving human and/or AI players.
 * @author Andrew Socha
 *
 * Created using the AIMA-Java libraries at https://code.google.com/p/aima-java/
 * Also used the Connect Four example from http://aima-java.googlecode.com/svn/trunk/aima-all/release/aima3ejavademos.html
 * as a starting place, converting it into this Backgammon game.
 */

import aima.core.search.adversarial.AdversarialSearch;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;


/**
 * Implements the GUI to play the Backgammon game
 * 
 * @author Andrew
 */
public class Backgammon {
    
    /** Application starter. */
    public static void main(String[] args) {
        JFrame frame = new Backgammon().constructApplicationFrame();
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    /**
     * Creates the game window
     * 
     * @return the game window's JFrame
     */
    public JFrame constructApplicationFrame() {
        JFrame frame = new JFrame();
        JPanel panel = new BackgammonPanel();
        frame.add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    /** Panel in which the game is played. */
    private static class BackgammonPanel extends JPanel implements ActionListener {
        JComboBox timeCombo;
        JComboBox metrics;
        JButton clearButton;
        JButton proposeButton;
        JLabel statusBar;

        BackgammonGame game;
        BackgammonState currState;

        BackgammonPanel() {
            game = new BackgammonGame();
            currState = game.getInitialState();
            setLayout(new BorderLayout());
            setBackground(Color.BLUE);

            //add top toolbar
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            
            timeCombo = new JComboBox(new String[] { "0.2 sec", "1 sec", "5 sec", "25 sec" });
            toolBar.add(timeCombo);
            timeCombo.setMaximumSize(new Dimension(100,30));
            
            metrics = new JComboBox();
            toolBar.add(metrics);
            metrics.setPreferredSize(new Dimension(1,1));

            clearButton = new JButton("Reset");
            clearButton.addActionListener(this);
            toolBar.add(clearButton);
            
            proposeButton = new JButton("Propose Move");
            proposeButton.addActionListener(this);
            toolBar.add(proposeButton);
            
            add(toolBar, BorderLayout.NORTH);

            JPanel boardPanel = new JPanel();
            boardPanel.setLayout(new GridLayout(2, 15, 5, 5));
            boardPanel.setBorder(BorderFactory.createEtchedBorder());
            boardPanel.setBackground(Color.GREEN);
            for (int i = 13; i < 26; i++) { //add board tiles (first row)
                GridElement element = new GridElement(i);
                boardPanel.add(element);
                element.addActionListener(this);
                if (i == 18 || i == 24){
                    JButton fakeButton = new JButton();
                    fakeButton.setBackground(Color.BLACK);
                    boardPanel.add(fakeButton);
                }
            }
            for (int i = 12; i >= 0; i--) { //add board tiles (2nd row)
                GridElement element = new GridElement(i);
                boardPanel.add(element);
                element.addActionListener(this);
                if (i == 7 || i == 1){
                    JButton fakeButton = new JButton();
                    fakeButton.setBackground(Color.BLACK);
                    boardPanel.add(fakeButton);
                }
            }
            
            add(boardPanel, BorderLayout.CENTER);
            
            //add bottom toolbar
            JPanel dicePanel = new JPanel();
            dicePanel.setLayout(new GridLayout(1, 3, 5, 5));
            dicePanel.setBorder(BorderFactory.createEtchedBorder());
            dicePanel.setBackground(Color.BLUE);
            
            statusBar = new JLabel("");
            statusBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            dicePanel.add(statusBar);
            
            for (int i = 0; i < 2; i++) { //add dice tiles
                DieElement element = new DieElement(i);
                dicePanel.add(element);
                element.addActionListener(this);
            }
            
            add(dicePanel, BorderLayout.SOUTH);

            updateStatus();
        }

        /** Handles all button events and updates the view. */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e == null || e.getSource() == clearButton) {
                currState = game.getInitialState();
                currState.rollDice();
            }
            else if (!game.isTerminal(currState)) {
                if (e.getSource() == proposeButton) { //AI moves piece
                    proposeMove();
                }
                else if (e.getSource() instanceof GridElement) { //try to move a piece
                    GridElement el = (GridElement) e.getSource();
                    currState = game.getResult(currState, el.location, false);
                }
                else if (e.getSource() instanceof DieElement) { //select a different Die
                    DieElement el = (DieElement) e.getSource();
                    
                    if (el.dieNumber != currState.getSelectedDie()) currState.changeSelectedDie();
                }
            }
            repaint();
            updateStatus();
        }

        /** Uses adversarial search to select the next action. */
        private void proposeMove() {
            int time = (int)(1000.0 * Math.pow(5, timeCombo.getSelectedIndex() - 1)); //time allotted to determine move (in ms)
            AdversarialSearch<BackgammonState, Integer> search = new BackgammonAIPlayer(game, time);
            Integer action = search.makeDecision(currState);
            metrics.removeAllItems();
            
            //display search info
            for (String element : search.getMetrics().getAll().values()){
                metrics.addItem(element);
            }
            
            currState = game.getResult(currState, action, true);
        }

        /** Updates the status bar. */
        private void updateStatus() {
            String statusText;
            if (!game.isTerminal(currState)) {
                String toMove = game.getPlayer(currState);
                statusText = "Next move: " + toMove;
                statusBar.setForeground(toMove.equals("Black (Clockwise)") ? Color.BLACK : Color.WHITE);
            }
            else {
                if (game.getUtility(currState, "") == 1 || game.getUtility(currState, "") == 0)
                    statusText = game.getWinner(currState) + " has won. Congratulations!";
                else statusText = "No winner."; //should never be reached
                
                statusBar.setForeground(Color.RED);
            }
            statusBar.setText(statusText);
        }

        /** Represents a space on the board where discs can be placed. */
        private class GridElement extends JButton {
            int location;

            GridElement(int location) {
                this.location = location;
                setBackground(location % 2 == 1 ? Color.RED : Color.LIGHT_GRAY);
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                int playerNum = currState.getPlayerNum(location);
                if (playerNum != 0) {
                    drawDisk(g, playerNum);
                    
                    //draw number of disks
                    g.setColor(Color.BLUE);
                    
                    int count = currState.getCount(location);
                    char[] countAsChars = String.valueOf(count).toCharArray();
                    g.drawChars(countAsChars, 0, countAsChars.length, getWidth() / 2 - 2, getHeight() / 2 + 4);
                }
            }

            /** Fills a simple oval. */
            void drawDisk(Graphics g, int playerNum) {
                int size = Math.min(getWidth(), getHeight());
                g.setColor(playerNum == 1 ? Color.BLACK : Color.WHITE);
                g.fillOval((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
            }
        }
        
        /** Represents a die which can be selected */
        private class DieElement extends JButton {
            int dieNumber;

            DieElement(int dieNumber) {
                this.dieNumber = dieNumber;
                setBackground(Color.WHITE);
            }

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                //draw dice
                g.setColor(Color.black);
                int roll = currState.getDieRoll(dieNumber);
                char[] countAsChars = String.valueOf(roll).toCharArray();
                g.drawChars(countAsChars, 0, countAsChars.length, getWidth() / 2 - 2, getHeight() / 2 + 4);
                
                if (dieNumber == currState.getSelectedDie()) drawSelected(g, dieNumber);
            }
            
            /** Draws a simple oval. */
            void drawSelected(Graphics g, int dieNum) {
                int size = Math.min(getWidth(), getHeight());
                g.setColor(Color.GREEN);
                g.drawOval((getWidth() - size) / 2 + dieNum, (getHeight() - size) / 2 + dieNum, size - 2
                    * dieNum, size - 2 * dieNum);
            }
        }
    }
}