package com.team.name.bestrestservice;
import java.util.*;

public class SpaceshipAI {

    // Class-level variables
    private final int[] dx = { -1, 0, 1, 0 }; // North, East, South, West
    private final int[] dy = { 0, 1, 0, -1 }; // North, East, South, West
    private final String[] directions = { "N", "E", "S", "W" };
    private final String[] fullDirections = { "North", "East", "South", "West" };
    private final int BOARD_SIZE = 13;
    private final int CENTER = BOARD_SIZE / 2;

    public String decideMove(GameStatus gameStatus) {
        String[][] field = gameStatus.field;

        int myX = -1, myY = -1;
        String myDirection = "";

        // Find our position and direction
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                String cell = field[i][j];
                if (cell != null && cell.startsWith("P")) {
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

        // Check for enemies in all directions except behind within radius 4
        for (int i = 0; i < 4; i++) {
            if (isOppositeDirection(dirIndex, i)) {
                continue; // Skip the direction opposite to current direction (enemy behind)
            }

            boolean enemyInRange = false;
            for (int distance = 1; distance <= 4; distance++) {
                int newX = myX + dx[i] * distance;
                int newY = myY + dy[i] * distance;

                if (isOutOfBounds(newX, newY)) {
                    break; // Can't look beyond the board
                }

                if (isAsteroid(field[newX][newY])) {
                    break; // Path is blocked by asteroid
                }

                if (isEnemy(field[newX][newY])) {
                    enemyInRange = true;
                    break;
                }
            }

            if (enemyInRange) {
                // Rotate towards the enemy direction if not already facing it
                String rotation = getMinimalRotationForEnemyRotation(dirIndex, i);
                if (rotation != null) {
                    return rotation;
                } else {
                    // Already facing the enemy, fire!
                    return "F";
                }
            }
        }

        // Check if we are outside the center area
        if (findNearestCoin(field, myX, myY) == null && !isWithinCenterArea(myX, myY)) {
            // Move towards the center
            String moveToCenter = moveToCenter(field, myX, myY, dirIndex);
            if (moveToCenter != null) {
                return moveToCenter;
            }
        } else {
            // Try to collect coins
            int[] coinTarget = findNearestCoin(field, myX, myY);
            if (coinTarget != null) {
                String nextMove = getNextMoveTowardsTarget(field, myX, myY, dirIndex, coinTarget[0], coinTarget[1]);
                if (nextMove != null) {
                    return nextMove;
                }
            }
        }

        // Try to move forward if possible
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

    private boolean isOppositeDirection(int currentIndex, int targetIndex) {
        return (currentIndex + 2) % 4 == targetIndex;
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
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    private boolean isOutOfBounds(int x, int y) {
        return !isWithinBounds(x, y);
    }

    private boolean isWithinCenterArea(int x, int y) {
        return Math.abs(x - CENTER) + Math.abs(y - CENTER) <= 3;
    }

    private int[] findNearestCoin(String[][] field, int myX, int myY) {
        int minDistance = Integer.MAX_VALUE;
        int[] target = null;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
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
            String move = attemptMoveInDirection(field, myX, myY, dirIndex, desiredDirIndex);
            if (move != null) {
                return move;
            }
        }
        return null; // No valid moves towards target
    }

    private String moveToCenter(String[][] field, int myX, int myY, int dirIndex) {
        // Determine the direction towards the center
        int deltaX = CENTER - myX;
        int deltaY = CENTER - myY;

        List<Integer> possibleDirections = new ArrayList<>();
        if (deltaX < 0) possibleDirections.add(0); // North
        if (deltaX > 0) possibleDirections.add(2); // South
        if (deltaY > 0) possibleDirections.add(1); // East
        if (deltaY < 0) possibleDirections.add(3); // West

        // Prioritize directions that move closer to the center
        possibleDirections.sort((a, b) -> {
            int distanceA = Math.abs((myX + dx[a]) - CENTER) + Math.abs((myY + dy[a]) - CENTER);
            int distanceB = Math.abs((myX + dx[b]) - CENTER) + Math.abs((myY + dy[b]) - CENTER);
            return Integer.compare(distanceA, distanceB);
        });

        // Try each possible direction
        for (int desiredDirIndex : possibleDirections) {
            String move = attemptMoveInDirection(field, myX, myY, dirIndex, desiredDirIndex);
            if (move != null) {
                return move;
            }
        }
        return null;
    }

    private String attemptMoveInDirection(String[][] field, int myX, int myY, int dirIndex, int desiredDirIndex) {
        int forwardX = myX + dx[desiredDirIndex];
        int forwardY = myY + dy[desiredDirIndex];

        if (isOutOfBounds(forwardX, forwardY)) {
            return null; // Can't move off the board
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
        return null;
    }

    private String getMinimalRotation(int currentIndex, int desiredIndex) {
        int diff = (desiredIndex - currentIndex + 4) % 4;
        if (diff == 1) {
            return "R";
        } else if (diff == 3) {
            return "L";
        } else if (diff == 2) {
            // Turn around
            return "R";
        } else {
            return null; // Already facing the desired direction
        }
    }

    private String getMinimalRotationForEnemyRotation(int currentIndex, int desiredIndex) {
        int diff = (desiredIndex - currentIndex + 4) % 4;
        if (diff == 1) {
            return "R";
        } else if (diff == 3) {
            return "L";
        } else if (diff == 2) {
            // Turn around
            return null;
        } else {
            return null; // Already facing the desired direction
        }
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
