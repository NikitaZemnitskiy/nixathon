package com.team.name.bestrestservice;

import com.team.name.bestrestservice.controller.TaskResolveController;

import java.util.*;

public class SpaceshipAI {

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

        // Define movement deltas based on direction
        int[] dx = { -1, 1, 0, 0 }; // N, S, W, E
        int[] dy = { 0, 0, -1, 1 }; // N, S, W, E
        char[] directions = { 'N', 'S', 'W', 'E' };

        // Map direction to index
        int dirIndex = "NSWE".indexOf(myDirection);

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

        // Try to move towards the nearest coin
        int[] coinTarget = findNearestCoin(field, myX, myY);

        if (coinTarget != null) {
            String nextMove = getNextMoveTowardsTarget(field, myX, myY, myDirection, coinTarget[0], coinTarget[1]);
            if (nextMove != null) {
                return nextMove;
            }
        }

        // If no coins, move forward if possible
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

        // If no possible move, stay in place (or move forward if blocked)
        return "M"; // Attempt to move forward even if blocked
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

    private int[] findNearestCoin(String[][] field, int myX, int myY) {
        int minDistance = Integer.MAX_VALUE;
        int[] target = null;

        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field[i].length; j++) {
                if (field[i][j].equals("C")) {
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

    private String getNextMoveTowardsTarget(String[][] field, int myX, int myY, char myDirection, int targetX, int targetY) {
        // Determine the direction to the target
        int deltaX = targetX - myX;
        int deltaY = targetY - myY;

        char desiredDirection;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            desiredDirection = deltaX < 0 ? 'N' : 'S';
        } else {
            desiredDirection = deltaY < 0 ? 'W' : 'E';
        }

        if (myDirection == desiredDirection) {
            // Try to move forward
            int[] dx = { -1, 1, 0, 0 }; // N, S, W, E
            int[] dy = { 0, 0, -1, 1 }; // N, S, W, E
            int dirIndex = "NSWE".indexOf(myDirection);

            int forwardX = myX + dx[dirIndex];
            int forwardY = myY + dy[dirIndex];

            if (isWithinBounds(forwardX, forwardY) && isEmpty(field[forwardX][forwardY])) {
                return "M";
            } else {
                return null;
            }
        } else {
            // Determine the minimal rotation
            String rotation = getMinimalRotation(myDirection, desiredDirection);
            return rotation;
        }
    }

    private String getMinimalRotation(char currentDirection, char desiredDirection) {
        String directions = "NESW";
        int currentIndex = directions.indexOf(currentDirection);
        int desiredIndex = directions.indexOf(desiredDirection);

        int diff = (desiredIndex - currentIndex + 4) % 4;
        if (diff == 1) {
            return "R";
        } else if (diff == 3) {
            return "L";
        } else if (diff == 2) {
            // Do not turn around yourself; choose to rotate right
            return "R";
        } else {
            return null; // Already facing the desired direction
        }
    }

    private String findRotationToMove(String[][] field, int myX, int myY, char myDirection) {
        String directions = "NESW";
        int currentIndex = directions.indexOf(myDirection);

        for (int i = 1; i <= 3; i++) {
            int newIndex = (currentIndex + i) % 4;
            char newDirection = directions.charAt(newIndex);

            int[] dx = { -1, 0, 1, 0 }; // N, E, S, W
            int[] dy = { 0, 1, 0, -1 }; // N, E, S, W
            int dirIndex = newIndex;

            int forwardX = myX + dx[dirIndex];
            int forwardY = myY + dy[dirIndex];

            if (isWithinBounds(forwardX, forwardY) && isEmpty(field[forwardX][forwardY])) {
                // Decide whether to rotate left or right
                if (i == 1) {
                    return "R";
                } else if (i == 3) {
                    return "L";
                } else {
                    // i == 2, do not turn around yourself
                    return "R";
                }
            }
        }
        return null;
    }

    // GameStatus class as provided
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
