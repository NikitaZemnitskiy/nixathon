package com.team.name.bestrestservice;

import java.util.*;

public class SpaceshipAI {

    public String decideMove(GameStatus gameStatus) {
        String[][] field = gameStatus.getField();

        int shipX = -1;
        int shipY = -1;
        char shipDirection = ' ';

        // Find our ship
        outerLoop:
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                String cell = field[y][x];
                if (cell != null && cell.startsWith("P")) {
                    shipX = x;
                    shipY = y;
                    shipDirection = cell.charAt(1);
                    break outerLoop;
                }
            }
        }

        if (shipX == -1 || shipY == -1) {
            // Ship not found, default to do nothing
            return null;
        }

        // Coordinates of the center
        int centerX = 6;
        int centerY = 6;

        if (shipX != centerX || shipY != centerY) {
            // Move towards the center using pathfinding
            String move = moveTowards(shipX, shipY, shipDirection, centerX, centerY, field);
            return move;
        } else {
            // At the target cell, rotate and fire at predicted enemy positions
            String move = rotateAndFireAtIncomingEnemy(shipX, shipY, shipDirection, field);
            return move;
        }
    }

    private String moveTowards(int shipX, int shipY, char shipDirection, int targetX, int targetY, String[][] field) {
        // Use BFS to find the next step towards the target avoiding obstacles
        int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        char[] dirChars = {'N', 'E', 'S', 'W'};
        boolean[][] visited = new boolean[13][13];
        int[][] prevX = new int[13][13];
        int[][] prevY = new int[13][13];

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{shipX, shipY});
        visited[shipY][shipX] = true;

        boolean found = false;
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int curX = current[0];
            int curY = current[1];

            if (curX == targetX && curY == targetY) {
                found = true;
                break;
            }

            for (int i = 0; i < directions.length; i++) {
                int newX = curX + directions[i][0];
                int newY = curY + directions[i][1];

                if (isValidCell(newX, newY, field) && !visited[newY][newX]) {
                    visited[newY][newX] = true;
                    queue.add(new int[]{newX, newY});
                    prevX[newY][newX] = curX;
                    prevY[newY][newX] = curY;
                }
            }
        }

        if (!found) {
            // No path found, default to rotate
            return "L";
        }

        // Reconstruct path
        List<int[]> path = new ArrayList<>();
        int pathX = targetX;
        int pathY = targetY;
        while (pathX != shipX || pathY != shipY) {
            path.add(new int[]{pathX, pathY});
            int tempX = prevX[pathY][pathX];
            int tempY = prevY[pathY][pathX];
            pathX = tempX;
            pathY = tempY;
        }
        Collections.reverse(path);

        if (path.isEmpty()) {
            // Already at the target
            return null; // Skip turn
        }

        int nextX = path.get(0)[0];
        int nextY = path.get(0)[1];
        char desiredDirection = getDirection(shipX, shipY, nextX, nextY);

        if (shipDirection == desiredDirection) {
            // Attempt to move forward
            return "M";
        } else {
            // Rotate towards the desired direction
            return rotateTowards(shipDirection, desiredDirection);
        }
    }

    private char getDirection(int fromX, int fromY, int toX, int toY) {
        if (toX - fromX == 1) return 'E';
        if (toX - fromX == -1) return 'W';
        if (toY - fromY == 1) return 'S';
        if (toY - fromY == -1) return 'N';
        return 'N'; // Default
    }

    private boolean isValidCell(int x, int y, String[][] field) {
        if (x < 0 || x >= 13 || y < 0 || y >= 13) {
            return false;
        }
        String cell = field[y][x];
        return cell == null || cell.equals("_") || cell.equals("C");
    }

    private String rotateAndFireAtIncomingEnemy(int shipX, int shipY, char shipDirection, String[][] field) {
        // Find all enemies
        List<EnemyShip> enemies = new ArrayList<>();
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                String cell = field[y][x];
                if (cell != null && cell.startsWith("E")) {
                    char enemyDirection = cell.charAt(1);
                    enemies.add(new EnemyShip(x, y, enemyDirection));
                }
            }
        }

        if (enemies.isEmpty()) {
            // No enemies found, skip turn
            return null;
        }

        // Include current position in predicted positions
        for (EnemyShip enemy : enemies) {
            enemy.predictPositions(field);
        }

        // Prioritize enemies moving towards us and entering our firing range
        EnemyShip targetEnemy = null;
        int minTurnsNeeded = Integer.MAX_VALUE;
        char desiredDirection = shipDirection;

        for (EnemyShip enemy : enemies) {
            // Check if the enemy is moving towards us
            if (enemy.isMovingTowards(shipX, shipY)) {
                // Predict the position where the enemy will be within our firing range
                for (PredictedPosition pos : enemy.predictedPositions) {
                    char dirToPosition = calculateDesiredDirection(shipX, shipY, pos.x, pos.y);
                    int turnsNeeded = calculateTurnsNeeded(shipDirection, dirToPosition);

                    if (turnsNeeded < minTurnsNeeded) {
                        minTurnsNeeded = turnsNeeded;
                        desiredDirection = dirToPosition;
                        targetEnemy = enemy;
                    }
                    break; // Found a valid position, no need to check further
                }
            }
        }

        if (targetEnemy != null) {
            if (shipDirection != desiredDirection) {
                // Rotate towards the predicted position
                return rotateTowards(shipDirection, desiredDirection);
            } else {
                // Fire if the enemy is within firing range
                return "F";
            }
        } else {
            // No imminent threats, proceed with previous logic
            return rotateAndFireAtNearestEnemy(shipX, shipY, shipDirection, field);
        }
    }

    private boolean isPositionInFiringRange(int shipX, int shipY, char direction, int posX, int posY, String[][] field) {
        int range = 4;
        int dx = 0, dy = 0;

        switch (direction) {
            case 'N': dy = -1; break;
            case 'S': dy = 1; break;
            case 'E': dx = 1; break;
            case 'W': dx = -1; break;
        }

        int currentX = shipX;
        int currentY = shipY;
        for (int i = 1; i <= range; i++) {
            currentX += dx;
            currentY += dy;

            if (currentX < 0 || currentX >= 13 || currentY < 0 || currentY >= 13) {
                break; // Out of bounds
            }

            String cell = field[currentY][currentX];

            if (cell != null && cell.startsWith("A")) {
                break; // Asteroid blocks the blast
            }

            if (currentX == posX && currentY == posY) {
                return true; // Position is within firing range
            }
        }

        return false;
    }

    private String rotateAndFireAtNearestEnemy(int shipX, int shipY, char shipDirection, String[][] field) {
        // Find the nearest enemy
        EnemyShip nearestEnemy = null;
        int minDistance = Integer.MAX_VALUE;
        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                String cell = field[y][x];
                if (cell != null && cell.startsWith("E")) {
                    int distance = Math.abs(x - shipX) + Math.abs(y - shipY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestEnemy = new EnemyShip(x, y, cell.charAt(1));
                    }
                }
            }
        }

        if (nearestEnemy == null) {
            // No enemies found
            return null;
        }

        char desiredDirection = calculateDesiredDirection(shipX, shipY, nearestEnemy.x, nearestEnemy.y);

        if (shipDirection != desiredDirection) {
            return rotateTowards(shipDirection, desiredDirection);
        } else {
            // Check if the enemy is within firing range
            if (isEnemyInFiringRange(shipX, shipY, shipDirection, nearestEnemy, field)) {
                return "F";
            } else {
                // Enemy not in range, skip turn
                return null;
            }
        }
    }

    private boolean isEnemyInFiringRange(int shipX, int shipY, char direction, EnemyShip enemy, String[][] field) {
        return isPositionInFiringRange(shipX, shipY, direction, enemy.x, enemy.y, field);
    }

    private char calculateDesiredDirection(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;

        if (dx == 0 && dy == 0) {
            // Same position, arbitrary direction
            return 'N';
        } else if (Math.abs(dx) >= Math.abs(dy)) {
            return dx > 0 ? 'E' : 'W';
        } else {
            return dy > 0 ? 'S' : 'N';
        }
    }

    private int calculateTurnsNeeded(char currentDirection, char desiredDirection) {
        String directions = "NESW";
        int currentIndex = directions.indexOf(currentDirection);
        int desiredIndex = directions.indexOf(desiredDirection);

        int leftTurns = (currentIndex - desiredIndex + 4) % 4;
        int rightTurns = (desiredIndex - currentIndex + 4) % 4;

        return Math.min(leftTurns, rightTurns);
    }

    private String rotateTowards(char currentDirection, char desiredDirection) {
        String directions = "NESW";
        int currentIndex = directions.indexOf(currentDirection);
        int desiredIndex = directions.indexOf(desiredDirection);

        int leftTurns = (currentIndex - desiredIndex + 4) % 4;
        int rightTurns = (desiredIndex - currentIndex + 4) % 4;

        if (leftTurns <= rightTurns) {
            return "L";
        } else {
            return "R";
        }
    }

    class EnemyShip {
        int x, y;
        char direction;
        List<PredictedPosition> predictedPositions;

        EnemyShip(int x, int y, char direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.predictedPositions = new ArrayList<>();
        }

        void predictPositions(String[][] field) {
            // Include the current position
            predictedPositions.add(new PredictedPosition(x, y, 0));

            // Predict future positions
            int dx = 0, dy = 0;
            switch (direction) {
                case 'N': dy = -1; break;
                case 'S': dy = 1; break;
                case 'E': dx = 1; break;
                case 'W': dx = -1; break;
            }

            int currentX = x;
            int currentY = y;
            for (int i = 1; i <= 5; i++) { // Predict up to 5 steps ahead
                currentX += dx;
                currentY += dy;

                if (currentX < 0 || currentX >= 13 || currentY < 0 || currentY >= 13) {
                    break; // Out of bounds
                }

                String cell = field[currentY][currentX];

                if (cell != null && cell.startsWith("A")) {
                    break; // Asteroid blocks movement
                }

                predictedPositions.add(new PredictedPosition(currentX, currentY, i));
            }
        }

        boolean isMovingTowards(int shipX, int shipY) {
            // Determine if the enemy is moving towards our ship
            int dx = 0, dy = 0;
            switch (direction) {
                case 'N': dy = -1; break;
                case 'S': dy = 1; break;
                case 'E': dx = 1; break;
                case 'W': dx = -1; break;
            }

            int nextX = x + dx;
            int nextY = y + dy;

            // Check if next position is closer to our ship
            int currentDistance = Math.abs(x - shipX) + Math.abs(y - shipY);
            int nextDistance = Math.abs(nextX - shipX) + Math.abs(nextY - shipY);

            return nextDistance < currentDistance;
        }
    }

    class PredictedPosition {
        int x, y;
        int turnsAhead; // Number of turns ahead this position is predicted

        PredictedPosition(int x, int y, int turnsAhead) {
            this.x = x;
            this.y = y;
            this.turnsAhead = turnsAhead;
        }
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