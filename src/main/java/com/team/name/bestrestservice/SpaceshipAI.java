package com.team.name.bestrestservice;

import com.team.name.bestrestservice.controller.TaskResolveController;

import java.util.*;

public class SpaceshipAI {

    // Class-level variables
    private final int[] dx = { -1, 0, 1, 0 }; // North, East, South, West
    private final int[] dy = { 0, 1, 0, -1 }; // North, East, South, West
    private final String[] directions = { "N", "E", "S", "W" };
    private final String[] fullDirections = { "North", "East", "South", "West" };

    public String decideMove(GameStatus gameStatus) {
        String[][] field = gameStatus.field;

        int myX = -1, myY = -1;
        String myDirection = "";

        // Find our position and direction
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                String cell = field[i][j];
                if (cell.startsWith("P")) {
                    myX = i;
                    myY = j;
                    if (cell.length() > 1) {
                        myDirection = cell.substring(1); // Extract direction string
                    }
                    break;
                }
            }
            if (myX != -1) break;
        }

        if (myX == -1 || myY == -1 || myDirection.isEmpty()) {
            // Could not find our ship or direction
            return "M"; // Default move
        }

        // Map direction to index
        int dirIndex = getDirectionIndex(myDirection);
        if (dirIndex == -1) {
            // Invalid direction
            return "M"; // Default move
        }

        // Check if an enemy is in firing range + 1
        boolean enemyInRangePlusOne = false;
        for (int distance = 1; distance <= 5; distance++) {
            int newX = myX + dx[dirIndex] * distance;
            int newY = myY + dy[dirIndex] * distance;

            if (isOutOfBounds(newX, newY)) {
                break; // Can't look beyond the board
            }

            if (isAsteroid(field[newX][newY])) {
                break; // Path is blocked by asteroid
            }

            if (isEnemy(field[newX][newY])) {
                if (distance <= 4) {
                    // Enemy is within firing range
                    return "F"; // Fire at enemy
                } else {
                    enemyInRangePlusOne = true; // Enemy is within firing range + 1
                    break;
                }
            }
        }

        // If enemy is within firing range + 1, move forward if possible
        if (enemyInRangePlusOne) {
            String moveForward = tryMoveForward(field, myX, myY, dirIndex);
            if (moveForward != null) {
                return moveForward;
            } else {
                // Cannot move forward, try to rotate towards the enemy
                String rotateMove = rotateTowardsEnemy(field, myX, myY, dirIndex);
                if (rotateMove != null) {
                    return rotateMove;
                }
            }
        }

        // Try to move towards the nearest coin
        int[] coinTarget = findNearestCoin(field, myX, myY);

        if (coinTarget != null) {
            String nextMove = getNextMoveTowardsTarget(field, myX, myY, dirIndex, coinTarget[0], coinTarget[1]);
            if (nextMove != null) {
                return nextMove;
            }
        }

        // If no coins or cannot move towards coin, move forward if possible
        String moveForward = tryMoveForward(field, myX, myY, dirIndex);
        if (moveForward != null) {
            return moveForward;
        }

        // Rotate to find a possible move
        String rotateMove = findRotationToMove(field, myX, myY, dirIndex);
        if (rotateMove != null) {
            return rotateMove;
        }

        // If no possible move, attempt to stay in place
        return "M"; // May result in staying in place if blocked
    }

    private int getDirectionIndex(String direction) {
        for (int i = 0; i < directions.length; i++) {
            if (direction.equals(directions[i]) || direction.equals(fullDirections[i])) {
                return i;
            }
        }
        return -1; // Direction not found
    }

    private boolean isEnemy(String cell) {
        return cell != null && cell.startsWith("E");
    }

    private boolean isAsteroid(String cell) {
        return "A".equals(cell);
    }

    private boolean isEmpty(String cell) {
        return "_".equals(cell) || "C".equals(cell);
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 13 && y >= 0 && y < 13;
    }

    private boolean isOutOfBounds(int x, int y) {
        return !isWithinBounds(x, y);
    }

    private int[] findNearestCoin(String[][] field, int myX, int myY) {
        int minDistance = Integer.MAX_VALUE;
        int[] target = null;

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if ("C".equals(field[i][j])) {
                    int distance = Math.abs(myX - i) + Math.abs(myY - j);
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = new int[] { i, j };
                    }
                }
            }
        }
        return target;
    }

    private String getNextMoveTowardsTarget(String[][] field, int myX, int myY, int dirIndex, int targetX, int targetY) {
        // Determine possible directions towards the target
        int deltaX = targetX - myX;
        int deltaY = targetY - myY;

        List<Integer> possibleDirections = new ArrayList<>();
        if (deltaX < 0) possibleDirections.add(0); // North
        if (deltaX > 0) possibleDirections.add(2); // South
        if (deltaY > 0) possibleDirections.add(1); // East
        if (deltaY < 0) possibleDirections.add(3); // West

        // Try each possible direction
        for (int desiredDirIndex : possibleDirections) {
            int forwardX = myX + dx[desiredDirIndex];
            int forwardY = myY + dy[desiredDirIndex];

            if (isOutOfBounds(forwardX, forwardY)) {
                continue; // Can't move off the board
            }

            if (isEmpty(field[forwardX][forwardY])) {
                String rotation = getMinimalRotation(dirIndex, desiredDirIndex);
                if (rotation != null) {
                    return rotation;
                } else {
                    // Already facing the desired direction
                    return "M";
                }
            }
        }
        return null; // No valid moves towards target
    }

    private String getMinimalRotation(int currentIndex, int desiredIndex) {
        int diff = (desiredIndex - currentIndex + 4) % 4;
        if (diff == 1) {
            return "R";
        } else if (diff == 3) {
            return "L";
        } else if (diff == 2) {
            // Do not turn around unless necessary
            return null;
        } else {
            return null; // Already facing the desired direction
        }
    }

    private String rotateTowardsEnemy(String[][] field, int myX, int myY, int currentDirIndex) {
        // Rotate towards the direction where the enemy is located within range + 1
        for (int i = 0; i < 4; i++) {
            int dirIndex = i;

            if (dirIndex == currentDirIndex) {
                continue; // Already checked this direction
            }

            boolean enemyInRangePlusOne = false;
            for (int distance = 1; distance <= 5; distance++) {
                int newX = myX + dx[dirIndex] * distance;
                int newY = myY + dy[dirIndex] * distance;

                if (isOutOfBounds(newX, newY)) {
                    break; // Can't look beyond the board
                }

                if (isAsteroid(field[newX][newY])) {
                    break; // Path is blocked by asteroid
                }

                if (isEnemy(field[newX][newY])) {
                    enemyInRangePlusOne = true;
                    break;
                }
            }

            if (enemyInRangePlusOne) {
                String rotation = getMinimalRotation(currentDirIndex, dirIndex);
                if (rotation != null) {
                    return rotation;
                } else {
                    // Already facing the desired direction
                    String moveForward = tryMoveForward(field, myX, myY, dirIndex);
                    if (moveForward != null) {
                        return moveForward;
                    }
                }
            }
        }
        return null;
    }

    private String tryMoveForward(String[][] field, int myX, int myY, int dirIndex) {
        int forwardX = myX + dx[dirIndex];
        int forwardY = myY + dy[dirIndex];

        if (isOutOfBounds(forwardX, forwardY)) {
            return null; // Can't move off the board
        }

        if (isEmpty(field[forwardX][forwardY])) {
            return "M";
        } else {
            return null;
        }
    }

    private String findRotationToMove(String[][] field, int myX, int myY, int currentDirIndex) {
        for (int i = 1; i <= 3; i++) {
            int newIndex = (currentDirIndex + i) % 4;

            int forwardX = myX + dx[newIndex];
            int forwardY = myY + dy[newIndex];

            if (isOutOfBounds(forwardX, forwardY)) {
                continue; // Can't move off the board
            }

            if (isEmpty(field[forwardX][forwardY])) {
                // Decide whether to rotate left or right
                int diff = (newIndex - currentDirIndex + 4) % 4;
                if (diff == 1) {
                    return "R";
                } else if (diff == 3) {
                    return "L";
                }
            }
        }
        return null;
    }

    public static class GameStatus {
        String[][] field = new String[13][13];
        int narrowingIn;
        int gameId;

        public GameStatus(String[][] field, int narrowingIn, int gameId) {
            this.field = field;
            this.narrowingIn = narrowingIn;
            this.gameId = gameId;
        }

        public String[][] getField() {
            return field;
        }

        public void setField(String[][] field) {
            this.field = field;
        }

        public int getNarrowingIn() {
            return narrowingIn;
        }

        public void setNarrowingIn(int narrowingIn) {
            this.narrowingIn = narrowingIn;
        }

        public int getGameId() {
            return gameId;
        }

        public void setGameId(int gameId) {
            this.gameId = gameId;
        }
    }
}
