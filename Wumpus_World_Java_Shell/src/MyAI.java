import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

// TO DO:
// 2. Choose move and save it into target move variable, rotate until correct
// 3. when moving, make sure map size is updated correctly

// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      Abdullah Younis
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================

//MAKE THE MAP INIT_CAP A GLOBAL VAR

public class MyAI extends Agent
{
	private enum Presence {
		TRUE,
		FALSE,
		POTENTIAL,
		UNKNOWN
	}
	//consolidate into a Peril
	private class Peril {
		public Presence present;
		
		public Peril() {
			this.present = Presence.UNKNOWN;
		}
		
	}
	
	private class Cell {
		public Peril pit;
		public Peril wumpus;
		public boolean visited;
		
		public Cell() {
			this.pit = new Peril();
			this.wumpus = new Peril();
			this.visited = false;
		}
		
		public Presence getWumpusPresence() {
			return this.wumpus.present;
		}
		
		public void setWumpusPresence(Presence p) {
			this.wumpus.present = p;
		}
		
		public Presence getPitPresence() {
			return this.pit.present;
		}
		
		public void setPitPresence(Presence p) {
			this.pit.present = p;
		}
		
		public boolean isSafe() {
			return (this.pit.present == Presence.FALSE && this.wumpus.present == Presence.FALSE);
		}
	}
	
	private class CaveMap {
		//need an add row and add column
		public List<List<Cell>> map;
		
		public CaveMap() {
			this.map = new ArrayList<List<Cell>>(4);
			for(int i = 0; i < 4; i ++) {
				this.map.add(new ArrayList<Cell>());
				for(int j = 0; j < 10; j++) {
					this.map.get(i).add(new Cell());
				}
			}
			this.map.get(0).get(0).wumpus.present = Presence.FALSE;
			this.map.get(0).get(0).pit.present = Presence.FALSE;
		}
		
		public Cell getCell(Pair p) {
			return this.map.get(p.row).get(p.col);
		}
		
		public void setCellVisited(Pair p, boolean visited) {
			this.map.get(p.row).get(p.col).visited = visited;
		}
		
		public boolean getCellVisited(Pair p) {
			return this.map.get(p.row).get(p.col).visited;
		}
		
		public int getRowSize() {
			return this.map.size();
		}
		
		public int getColSize() {
			return this.map.get(0).size();
		}
		
		public void addRow() {
			int index = this.map.size();
			this.map.add(new ArrayList<Cell>(10));
			for(int i = 0; i < 10; i++) {
				this.map.get(index).add(new Cell());
			}
		}
	}
	
	private enum Direction {
		NORTH(0, 1, 3),
		EAST(1, 2, 0),
		SOUTH(2, 3, 1),
		WEST(3, 0, 2);
		
		private final int numRep;
		private final int rightNumRep;
		private final int leftNumRep;
		private Direction(int num, int rightNum, int leftNum) {
			this.numRep = num;
			this.rightNumRep = rightNum;
			this.leftNumRep = leftNum;
		}
		
		public static Direction getDirFromNum(int num) {
			switch (num) {
            case 0: return Direction.NORTH;
            case 1: return Direction.EAST;
            case 2: return Direction.SOUTH;
            default: return Direction.WEST;
			}
		}
	}
	//own file
	private class Pair {
		public Pair(int row, int col) {
			this.row = row;
			this.col = col;
		}
		public int row;
		public int col;
		
		@Override
		public int hashCode() {
			return this.col*10 + this.row;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Pair)) {
				return false;
			}
			Pair that = (Pair) obj;
			if((that.row == this.row && that.col == this.col)) {
				return true;
			}
			return false;
		}
		
		public boolean isAdjacent(Pair p) {
			if(this.row == p.row) {
				return ((this.col - 1 == p.col) || (this.col + 1 == p.col));
			}
			if(this.col == p.col) {
				return ((this.row - 1 == p.row) || (this.row + 1 == p.row));
			}
			return false;
		}
		//maybe move out of pair?
		public boolean isFacing(Pair p, Direction d) {
			if(d == Direction.NORTH) {
				return ((this.row + 1 == p.row) && (this.col == p.col));
			} else if(d == Direction.SOUTH) {
				return ((this.row - 1 == p.row) && (this.col == p.col));
			} else if(d == Direction.EAST) {
				return ((this.row == p.row) && (this.col + 1 == p.col));
			} else if(d == Direction.WEST) {
				return ((this.row == p.row) && (this.col - 1 == p.col));
			}
			return false;
		}
	}
	
	private CaveMap map;
	private HashSet<Pair> wumpusSpottings;
	private Stack<Pair> pathOut;
	private Pair currentPosition;
	private int lastRow;
	private int lastCol;
	private Direction direction;
	private boolean goldFound;
	private boolean retreating;
	private Pair wumpusLocation;
	private Pair targetDestination;
	private boolean wumpusKilled;
	private boolean arrowShot;
	private Random rand;
	
	public MyAI ( )
	{
		this.map = new CaveMap();
		this.wumpusKilled = false;
		this.pathOut = new Stack<Pair>();
		this.currentPosition = new Pair(0, 0);
		this.lastRow = -1;
		this.lastCol = -1;
		this.direction = Direction.EAST;
		this.goldFound = false;
		this.retreating = false;
		//consider making its own object
		this.wumpusSpottings = new HashSet<Pair>();
		this.arrowShot = false;
		long seed = System.currentTimeMillis();
		this.rand = new Random();
		//1509336230875L
		//1509333782394L
		System.out.println(seed);
		
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================
		
		// ======================================================================
		// YOUR CODE ENDS
		// ======================================================================
	}
	
	public Action getAction
	(
		boolean stench,
		boolean breeze,
		boolean glitter,
		boolean bump,
		boolean scream
	)
	{
		//If there's gold, grab it
		if(glitter) {
			this.retreating = true;
			this.goldFound = true;
			return Action.GRAB;
		}
		//if we killed the wumpus, mark its cell as safe and set its location to null
		if(scream) {
			if(this.wumpusLocation != null) {
				this.map.getCell(this.wumpusLocation).setWumpusPresence(Presence.FALSE);
			}
			if(this.targetDestination != null) {
				this.pathOut.push(this.targetDestination);
			}
			this.wumpusKilled = true;
			this.retreating = false;
			this.targetDestination = this.wumpusLocation;
		}
		
		//If we are in the start cell and
		// 	gold has been found
		//OR
		//  there's a stench or breeze
		//OR
		//  we're retreating
		//climb out
		
		//NOTE: don't turn if the cell in front of you is safe and unexplored

		if(this.currentPosition.equals(new Pair(0, 0))) {
			if(this.goldFound || breeze) {
				return Action.CLIMB;
			}
			//want to make sure we have explored both paths first
			else if(this.retreating) {
				Pair northCell = new Pair(1, 0);
				Pair eastCell = new Pair(0, 1);
				if(!this.map.getCellVisited(northCell) && this.map.getCell(northCell).isSafe()) {
					this.targetDestination = northCell;
					this.retreating = false;
					
				} else if(!this.map.getCellVisited(eastCell) && this.map.getCell(eastCell).isSafe()) {
					this.targetDestination = eastCell;
					this.retreating = false;
				} else {
					return Action.CLIMB;
				}
			} else if (!this.wumpusKilled && stench){
				if(!this.arrowShot) {
					this.arrowShot = true;
					return Action.SHOOT;
				}
				return Action.CLIMB;
			}
		}
		
		//If there's a bump, update the furthest row or column
		if(bump) {
			if(this.direction == Direction.NORTH) {
				this.lastRow = this.currentPosition.row - 1;
				if(this.direction == Direction.NORTH) {
					this.currentPosition = new Pair(this.currentPosition.row - 1, this.currentPosition.col);
				}
			}
			else {
				if(this.direction == Direction.EAST) {
					this.currentPosition = new Pair(this.currentPosition.row, this.currentPosition.col - 1);
				}
				this.lastCol = this.currentPosition.col - 1;
			}
			if(!this.pathOut.isEmpty()) {
			this.pathOut.pop();
			}
		}
		//if the wumpus has been found and we're next to the wumpus, shoot it or turn to face it
		
		if(!this.arrowShot && !this.wumpusKilled && this.wumpusLocation != null && this.wumpusLocation.isAdjacent(this.currentPosition)) {
			if(this.currentPosition.isFacing(this.wumpusLocation, this.direction)) {
				this.arrowShot = true;
				return Action.SHOOT;
			} else {
				Action move = rotateToFace(this.wumpusLocation);
				if(move == Action.TURN_LEFT) {
					this.direction = Direction.getDirFromNum(this.direction.leftNumRep);
				} else if(move == Action.TURN_RIGHT) {
					this.direction = Direction.getDirFromNum(this.direction.rightNumRep);
				}
				return move;
		}
		}
		updateAdjacentCells(breeze, stench);
		if(this.targetDestination == null || this.targetDestination == this.currentPosition || !isReachable(this.targetDestination)) {
			//else, continue exploring
			
			//get safe adjacent cells; will put the "visited/origin" cell in the first index
			ArrayList<Pair> openMoves = getAvailableCells();
//			for(Pair p: openMoves) {
//				System.out.println("(" + p.row + "," + p.col + ")");
//			}
			//randomly choose from those
			this.targetDestination = getTargetDestination(openMoves);
			//give an action, turning left / right if necessary
		}
		Action move = getMove(this.targetDestination);
//		System.out.println("starting from: (" + this.currentPosition.row + "," + this.currentPosition.col + ")");
		updateCurrentPosition(move, this.targetDestination);
//		System.out.println("moving to:(" + this.currentPosition.row + "," + this.currentPosition.col + ")");
//		for(Pair p : this.pathOut) {
//			System.out.print("(" + p.row + "," + p.col + ") ");
//		}
//		System.out.println();
		return move;

		
//		public enum Action
//		{
//			TURN_LEFT,
//			TURN_RIGHT,
//			FORWARD,
//			SHOOT,
//			GRAB,
//			CLIMB
//		}
		
		// ======================================================================
		// YOUR CODE BEGINS
		// ======================================================================
		// ======================================================================
		// YOUR CODE ENDS
		// ======================================================================
	}
	
	private boolean isReachable(Pair p) {
		int row = this.currentPosition.row;
		int col = this.currentPosition.col;
		return (p.row == row - 1 && p.col == col) ||
				(p.row == row + 1 && p.col == col) ||
				(p.row == row && p.col == col + 1) ||
				(p.row == row && p.col == col - 1);
	}
	
	private Action rotateToFace(Pair target) {
		for(Direction dir : Direction.values()) {
			if(dir != this.direction && this.currentPosition.isFacing(target, dir)) {
				if(this.direction.rightNumRep == dir.numRep) {
					return Action.TURN_RIGHT;
				}
				//else you'll either turn left, or it's directly across
				return Action.TURN_LEFT;
			}
		}
		//if it's directly across, just choose left
		return Action.TURN_LEFT;
	}
	
	private void updateCurrentPosition(Action move, Pair target) {
		this.map.setCellVisited(this.currentPosition, true);
		if(move == Action.FORWARD) {
			if(!this.retreating) {
				this.pathOut.push(this.currentPosition);
			}
			this.currentPosition = target;
			this.targetDestination = null;
			
			
		} else if(move == Action.TURN_LEFT) {
			this.direction = Direction.getDirFromNum(this.direction.leftNumRep);
		} else if(move == Action.TURN_RIGHT) {
			this.direction = Direction.getDirFromNum(this.direction.rightNumRep);
		}
	}
	
	private Action getMove(Pair target) {
		if(target == null) {
			return Action.CLIMB;
		}
		if(this.currentPosition.isFacing(target, this.direction)) {
			return Action.FORWARD;
		}
		return rotateToFace(target);
	}
	
	private Pair getTargetDestination(ArrayList<Pair> openMoves) {
		if(openMoves.size() == 0 || this.goldFound) {
			this.retreating = true;
			if(this.pathOut.isEmpty()) {
				return null;
			}
			return this.pathOut.pop();
		} 
		
//		if(openMoves.size() > 0 && !this.goldFound) {
//			this.retreating = false;
//		}
//		if(openMoves.size() == 1) {
//			return openMoves.get(0);
//		}
//		if(this.retreating && !this.currentPosition.equals(new Pair(0, 0))) {
//			return this.pathOut.pop();
//		}
		this.retreating = false;
		int  n = this.rand.nextInt(openMoves.size());
		return openMoves.get(n);
		
	}
	
	private ArrayList<Pair> getAvailableCells() {
		ArrayList<Pair> result = new ArrayList<Pair>();
			int row = this.currentPosition.row;
			int col = this.currentPosition.col;
			if(isPotentialCell(row - 1, col)) { result.add(new Pair(row - 1, col)); }
			if(isPotentialCell(row + 1, col)) { result.add(new Pair(row + 1, col)); }
			if(isPotentialCell(row, col - 1)) { result.add(new Pair(row, col - 1)); }
			if(isPotentialCell(row, col + 1)) { result.add(new Pair(row, col + 1)); }
		return result;
	}
	
	private boolean isPotentialCell(int row, int col) {
		Pair cell = new Pair(row, col);
		boolean temp = inBounds(row, col);
		temp = temp && this.map.getCell(cell).isSafe();
		temp = temp && !this.map.getCellVisited(cell);
		return temp;
	}
	
	private boolean inBounds(int row, int col) {
		if(row < 0 || col < 0 || col > 9 || 
				(this.lastRow != -1 && row > this.lastRow) ||
				(this.lastCol != -1 && col > this.lastCol)) {
			return false;
		}
		return true;
	}
	//change argument from pair to cell
	private void updateAdjacentCells(boolean breeze, boolean stench) {
		if(this.map.getCellVisited(this.currentPosition)) {
			return;
		}
		int row = this.currentPosition.row;
		int col = this.currentPosition.col;
		updateCell(new Pair(row - 1, col), breeze, stench);
		updateCell(new Pair(row + 1, col), breeze, stench);
		updateCell(new Pair(row, col - 1), breeze, stench);
		updateCell(new Pair(row, col + 1), breeze, stench);
		if(wumpusSpottings.size() == 1) {
			wumpusLocation = wumpusSpottings.iterator().next();
		}
		
	}
	//idea: mark each potential set with a key so if a cell is ruled out to be
	// a pit or wumpus, the other is set to be true
	private void updateCell(Pair p, boolean breeze, boolean stench) {
		if(!inBounds(p.row, p.col)) {
			return;
		}
		
		if(p.row >= this.map.getRowSize()) {
			this.map.addRow();
		}
		
		Cell c = this.map.getCell(p);

		//if already safe, just skip all of it
		if(c.isSafe()) {
			return;
		}
		//if the currently occupied cell has no percepts
		if(!breeze && !stench) {
			c.setWumpusPresence(Presence.FALSE);
			c.setPitPresence(Presence.FALSE);
			return;
		//if there was no breeze
		}
		if(!breeze) {
			c.setPitPresence(Presence.FALSE);
		//if there was a breeze and we didn't already rule out this cell having a pit
		} else if(breeze && 
				c.getPitPresence() != Presence.FALSE) {
			//add this cell to the pit's keys
			c.setPitPresence(Presence.POTENTIAL);
		//if there was no stench
		}
		if(this.wumpusKilled || !stench) {
			c.setWumpusPresence(Presence.FALSE);
			if(wumpusSpottings.contains(p)) {
				wumpusSpottings.remove(p);
			}
		//if there was a stench and we didn't already rule out this cell having the wumpus
		} else if(!this.wumpusKilled && stench && 
				c.getWumpusPresence() != Presence.FALSE) {
			c.setWumpusPresence(Presence.POTENTIAL);
			if(!this.wumpusSpottings.add(p)) {
				this.wumpusLocation = p;
			}
		}
	}
	
	// ======================================================================
	// YOUR CODE BEGINS
	// ======================================================================


	// ======================================================================
	// YOUR CODE ENDS
	// ======================================================================
}