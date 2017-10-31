import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Stack;

// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      TeamSweetFlips
//					(Anastasia Miles, 90039862)
//
// DESCRIPTION: This file contains my agent class.
//
// NOTES:       
// ======================================================================

public class MyAI extends Agent
{
	private final int INIT_ROW_CAP = 4;
	private final int COL_CAP = 10;
	private final Direction INIT_DIR = Direction.EAST;
	
	// For tracking whether a Peril is present in a Cell
	private enum Presence {
		TRUE,
		FALSE,
		POTENTIAL,
		UNKNOWN
	}
	
	// General class for a Wumpus or a Pit
	private class Peril {
		public Presence present;
		
		public Peril() {
			this.present = Presence.UNKNOWN;
		}
		
	}
	
	// A single Cell in the internal map
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
	
	// Map of the cave
	private class CaveMap {
		public List<List<Cell>> map;
		
		public CaveMap() {
			this.map = new ArrayList<List<Cell>>(INIT_ROW_CAP);
			for(int i = 0; i < INIT_ROW_CAP; i ++) {
				this.map.add(new ArrayList<Cell>());
				for(int j = 0; j < COL_CAP; j++) {
					this.map.get(i).add(new Cell());
				}
			}
			// Starting Cell is safe
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
			this.map.add(new ArrayList<Cell>(COL_CAP));
			for(int i = 0; i < COL_CAP; i++) {
				this.map.get(index).add(new Cell());
			}
		}
	}
	// Keep track of which way the agent is facing
	private enum Direction {
		// Each Direction has a number representation
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
	
	// Keeps track of row-column pairs
	private class Pair {
		public int row;
		public int col;
		
		public Pair(int row, int col) {
			this.row = row;
			this.col = col;
		}
		
		// For future use
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
			return (that.row == this.row && that.col == this.col);
		}
		
		// Returns true if p is adjacent to this
		public boolean isAdjacent(Pair p) {
			if(this.row == p.row) {
				return ((this.col - 1 == p.col) || (this.col + 1 == p.col));
			}
			if(this.col == p.col) {
				return ((this.row - 1 == p.row) || (this.row + 1 == p.row));
			}
			return false;
		}
		
		// Returns true if p is in direction d of this
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
	
	private boolean arrowShot;
	private boolean goldFound;
	// Signifies backtracking
	private boolean retreating;
	private boolean wumpusKilled;
	private CaveMap map;
	private Direction direction;
	// So we can figure out where the Wumpus is
	private HashSet<Pair> wumpusSpottings;
	// Bounds of the map, initially unknown
	private int lastRow;
	private int lastCol;
	//So we can backtrack
	private Stack<Pair> pathOut;
	private Pair currentPosition;
	// Where we want to move to, needs to carry if we must rotate first. Initially null
	private Pair targetDestination;
	private Pair wumpusLocation;
	private Random rand;
	
	public MyAI ( )
	{
		this.arrowShot = false;
		this.goldFound = false;
		this.retreating = false;
		this.wumpusKilled = false;
		this.map = new CaveMap();
		this.direction = INIT_DIR;
		this.wumpusSpottings = new HashSet<Pair>();
		this.lastRow = -1;
		this.lastCol = -1;
		this.pathOut = new Stack<Pair>();
		this.currentPosition = new Pair(0, 0);
		this.rand = new Random();
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
		// If there's gold, grab it. Start backtracking to (0,0).
		if(glitter) {
			this.retreating = true;
			this.goldFound = true;
			return Action.GRAB;
		}
		
		// The Wumpus has been killed.
		if(scream) {
			// Update his location if it was a lucky shot from (0,0); we shot East
			if(this.wumpusLocation == null) {
				this.wumpusLocation = new Pair(0, 1);
				
			}
			// Set Wumpus Presence to false so we can move there
			this.map.getCell(this.wumpusLocation).setWumpusPresence(Presence.FALSE);
			// If we were currently backtracking, push that target Cell to the path
			if(this.targetDestination != null && this.map.getCellVisited(this.targetDestination)) {
				this.pathOut.push(this.targetDestination);
			}
			this.wumpusKilled = true;
			// Continue exploring, moving to the ex-Wumpus Cell if it's safe
			this.retreating = false;
			if(this.map.getCell(this.wumpusLocation).getPitPresence() == Presence.FALSE) {
				this.targetDestination = this.wumpusLocation;
			}
		}
		
		// If we are at the start Cell (0,0)
		if(this.currentPosition.equals(new Pair(0, 0))) {
			// If there's a breeze here or we've found the gold, leave
			if(this.goldFound || breeze) {
				return Action.CLIMB;
			// If there's a stench, take a random shot
			} else if (!this.wumpusKilled && stench){
				if(!this.arrowShot) {
					this.arrowShot = true;
					return Action.SHOOT;
				}
				return Action.CLIMB;
			}
		}
		
		// If there's a bump, update this.lastRow and/or this.lastCol
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
		
		// If we've found the Wumpus and are next to it, turn to face it and then shoot
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
		
		// Update the status of adjacent cells based on the percepts in this one
		updateAdjacentCells(breeze, stench);
		
		// If we don't have a scheduled destination Cell
		if(this.targetDestination == null) {
			// Get the adjacent Cells we can move to
			ArrayList<Pair> openMoves = getAvailableCells();
			// Randomly choose one
			this.targetDestination = getTargetDestination(openMoves);
		}
		// Get the required Action to move to the Cell
		Action move = getMove(this.targetDestination);
		// Update the internal position information with the Action
		updateCurrentPosition(move, this.targetDestination);
		return move;
	}
	
	// Figures out the Direction to turn to face the target Cell, limiting moves
	private Action rotateToFace(Pair target) {
		for(Direction dir : Direction.values()) {
			if(dir != this.direction && this.currentPosition.isFacing(target, dir)) {
				if(this.direction.rightNumRep == dir.numRep) {
					return Action.TURN_RIGHT;
				}
				return Action.TURN_LEFT;
			}
		}
		// If we need to turn 180*, just turn left
		return Action.TURN_LEFT;
	}
	
	// Updates the internal position information
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
	
	// Gets the appropriate move to travel to the target Cell
	private Action getMove(Pair target) {
		// target is only null if we are in (0,0) with no available moves
		if(target == null) {
			return Action.CLIMB;
		}
		if(this.currentPosition.isFacing(target, this.direction)) {
			return Action.FORWARD;
		}
		return rotateToFace(target);
	}
	
	// Randomly chooses a target Cell from among the available moves
	private Pair getTargetDestination(ArrayList<Pair> openMoves) {
		// If there are no available moves or the gold is found, backtrack
		if(openMoves.size() == 0 || this.goldFound) {
			this.retreating = true;
			// pathOut will only be empty at (0,0)
			if(this.pathOut.isEmpty()) {
				return null;
			}
			return this.pathOut.pop();
		} 
		// If there are moves, stop backtracking and randomly explore one
		this.retreating = false;
		int  n = this.rand.nextInt(openMoves.size());
		return openMoves.get(n);
		
	}
	
	// Gets the available Cells to move to from the current position
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
	
	// If a Cell is in bounds, safe, and unvisited, we can move there
	private boolean isPotentialCell(int row, int col) {
		Pair pair = new Pair(row, col);
		return inBounds(row, col) && this.map.getCell(pair).isSafe() && !this.map.getCellVisited(pair);
	}
	
	// Checks if a cell is in bounds
	private boolean inBounds(int row, int col) {
		return !(row < 0 || col < 0 || col > COL_CAP - 1 || 
				(this.lastRow != -1 && row > this.lastRow) ||
				(this.lastCol != -1 && col > this.lastCol));
	}
	
	// Updates adjacent Cells with information obtained from the current Cell
	private void updateAdjacentCells(boolean breeze, boolean stench) {
		// If we've already been there, we know it's safe
		if(this.map.getCellVisited(this.currentPosition)) {
			return;
		}
		int row = this.currentPosition.row;
		int col = this.currentPosition.col;
		updateCell(new Pair(row - 1, col), breeze, stench);
		updateCell(new Pair(row + 1, col), breeze, stench);
		updateCell(new Pair(row, col - 1), breeze, stench);
		updateCell(new Pair(row, col + 1), breeze, stench);
		// If we've isolated the Wumpus, update the location
		if(this.wumpusSpottings.size() == 1) {
			this.wumpusLocation = this.wumpusSpottings.iterator().next();
		}
		
	}
	
	// Update a single Cell's information
	private void updateCell(Pair p, boolean breeze, boolean stench) {
		if(!inBounds(p.row, p.col)) {
			return;
		}
		// If we need to expand the internal map, do so
		if(p.row >= this.map.getRowSize()) {
			this.map.addRow();
		}
		
		Cell c = this.map.getCell(p);

		// If the Cell is already safe, skip the processing
		if(c.isSafe()) {
			return;
		}
		// If there was no breeze or stench, the Cell is safe
		if(!breeze && !stench) {
			c.setWumpusPresence(Presence.FALSE);
			c.setPitPresence(Presence.FALSE);
			return;
		}
		// If there was no breeze, there is no Pit
		if(!breeze) {
			c.setPitPresence(Presence.FALSE);
		// If there was a breeze and we didn't already mark the Cell safe, 
	    // mark a potential Pit
		} else if(breeze && 
				c.getPitPresence() != Presence.FALSE) {
			c.setPitPresence(Presence.POTENTIAL);
		}
		// If there was no stench, there is no Wumpus
		if(this.wumpusKilled || !stench) {
			c.setWumpusPresence(Presence.FALSE);
			// If we thought this could have been a Wumpus, remove it from the list
			if(wumpusSpottings.contains(p)) {
				wumpusSpottings.remove(p);
			}
		// If the Wumpus is alive, there was a stench, and we didn't already mark
		// the Cell safe, mark it as a potential Wumpus and add a sighting
		} else if(!this.wumpusKilled && stench && 
				c.getWumpusPresence() != Presence.FALSE) {
			c.setWumpusPresence(Presence.POTENTIAL);
			if(!this.wumpusSpottings.add(p)) {
				this.wumpusLocation = p;
			}
		}
	}
}