package com.team.name.bestrestservice;

import com.team.name.bestrestservice.controller.TaskResolveController;

import java.util.*;

public class SpaceshipAI {

    // Class-level variables
    private final int[] dx = {-1, 0, 1, 0}; // N, E, S, W
    private final int[] dy = {0, 1, 0, -1}; // N, E, S, W
    private final char[] directions = {'N', 'E', 'S', 'W'};

    public String decideMove(GameStatus gameStatus) {
        String[][] field = gameStatus.field;

        int myX = -1, myY = -1;
        char myDirection = ' ';

        // Find our position and direction
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                String cell = field[i][j];
                if (cell.startsWith("P")) {
                    myX = i;
                    myY = j;
                    if (cell.length() > 1) {
                        myDirection = cell.charAt(1);
                    }
                    break;
                }
            }
            if (myX != -1) break;
        }

        // Map direction to index
        int dirIndex = "NESW".indexOf(myDirection);

        // Check if an enemy is in firing range
        for (int distance = 1; distance <= 4; distance++) {
            int newX = myX + dx[dirIndex] * distance;
            int newY = myY + dy[dirIndex] * distance;

            if (isOutOfBounds(newX, newY) || isAsteroid(field[newX][newY])) {
                break; // Blast is blocked by asteroid or boundary
            }

            if (isEnemy(field[newX][newY])) {
                return "F"; // Fire at enemy
            }
        }

        // Try to move towards the nearest enemy
        int[] enemyTarget = findNearestEnemy(field, myX, myY);

        if (enemyTarget != null) {
            String nextMove = getNextMoveTowardsTarget(field, myX, myY, myDirection, enemyTarget[0], enemyTarget[1]);
            if (nextMove != null) {
                return nextMove;
            }
        }

        // Try to move towards the nearest coin
        int[] coinTarget = findNearestCoin(field, myX, myY);

        if (coinTarget != null) {
            String nextMove = getNextMoveTowardsTarget(field, myX, myY, myDirection, coinTarget[0], coinTarget[1]);
            if (nextMove != null) {
                return nextMove;
            }
        }

        // Try to move forward if possible
        int forwardX = myX + dx[dirIndex];
        int forwardY = myY + dy[dirIndex];
        if (isWithinBounds(forwardX, forwardY) && isEmpty(field[forwardX][forwardY])) {
            return "M";
        }

        // Rotate to find a possible move
        String rotateMove = findRotationToMove(field, myX, myY, myDirection);
        if (rotateMove != null) {
            return rotateMove;
        }

        // If no possible move, attempt to move forward
        return "M"; // May result in staying in place if blocked
    }

    private boolean isEnemy(String cell) {
        return cell.startsWith("E");
    }

    private boolean isAsteroid(String cell) {
        return cell.equals("A");
    }

    private boolean isEmpty(String cell) {
        return cell.equals("_") || cell.equals("C");
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 13 && y >= 0 && y < 13;
    }

    private boolean isOutOfBounds(int x, int y) {
        return x < 0 || x >= 13 || y < 0 || y >= 13;
    }

    private int[] findNearestEnemy(String[][] field, int myX, int myY) {
        int minDistance = Integer.MAX_VALUE;
        int[] target = null;

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if (isEnemy(field[i][j])) {
                    int distance = Math.abs(myX - i) + Math.abs(myY - j);
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = new int[]{i, j};
                    }
                }
            }
        }
        return target;
    }

    private int[] findNearestCoin(String[][] field, int myX, int myY) {
        int minDistance = Integer.MAX_VALUE;
        int[] target = null;

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j].equals("C")) {
                    int distance = Math.abs(myX - i) + Math.abs(myY - j);
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = new int[]{i, j};
                    }
                }
            }
        }
        return target;
    }

    private String getNextMoveTowardsTarget(String[][] field, int myX, int myY, char myDirection, int targetX, int targetY) {
        // Determine possible directions towards the target
        int deltaX = targetX - myX;
        int deltaY = targetY - myY;

        List<Character> possibleDirections = new ArrayList<>();
        if (deltaX < 0) possibleDirections.add('N');
        if (deltaX > 0) possibleDirections.add('S');
        if (deltaY > 0) possibleDirections.add('E');
        if (deltaY < 0) possibleDirections.add('W');

        // Try each possible direction
        for (char desiredDirection : possibleDirections) {
            int dirIndex = "NESW".indexOf(desiredDirection);
            int forwardX = myX + dx[dirIndex];
            int forwardY = myY + dy[dirIndex];

            if (isWithinBounds(forwardX, forwardY) && isEmpty(field[forwardX][forwardY])) {
                String rotation = getMinimalRotation(myDirection, desiredDirection);
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

    private String getMinimalRotation(char currentDirection, char desiredDirection) {
        String directionsStr = "NESW";
        int currentIndex = directionsStr.indexOf(currentDirection);
        int desiredIndex = directionsStr.indexOf(desiredDirection);

        int diff = (desiredIndex - currentIndex + 4) % 4;
        if (diff == 1) {
            return "R";
        } else if (diff == 3) {
            return "L";
        } else if (diff == 2) {
            // Only turn around if no other options are available
            return "R"; // or "L"
        } else {
            return null; // Already facing the desired direction
        }
    }

    private String findRotationToMove(String[][] field, int myX, int myY, char myDirection) {
        String directionsStr = "NESW";
        int currentIndex = directionsStr.indexOf(myDirection);

        for (int i = 1; i <= 3; i++) {
            int newIndex = (currentIndex + i) % 4;
            char newDirection = directionsStr.charAt(newIndex);

            int dirIndex = newIndex;

            int forwardX = myX + dx[dirIndex];
            int forwardY = myY + dy[dirIndex];

            if (isWithinBounds(forwardX, forwardY) && isEmpty(field[forwardX][forwardY])) {
                // Decide whether to rotate left or right
                int diff = (newIndex - currentIndex + 4) % 4;
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
