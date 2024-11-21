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
            // At the center, rotate to face the direction of the nearest enemy's movement
            String move = rotateToFaceEnemyMovement(shipX, shipY, shipDirection, field);
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

    private String rotateToFaceEnemyMovement(int shipX, int shipY, char shipDirection, String[][] field) {
        // Find the nearest enemy ship
        EnemyShip nearestEnemy = null;
        int minDistance = Integer.MAX_VALUE;

        for (int y = 0; y < 13; y++) {
            for (int x = 0; x < 13; x++) {
                String cell = field[y][x];
                if (cell != null && cell.startsWith("E")) {
                    int distance = Math.abs(x - shipX) + Math.abs(y - shipY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        char enemyDirection = cell.charAt(1);
                        nearestEnemy = new EnemyShip(x, y, enemyDirection);
                    }
                }
            }
        }

        if (nearestEnemy == null) {
            // No enemies found
            return null;
        }

        // Calculate the enemy's next position based on their current direction
        int enemyNextX = nearestEnemy.x;
        int enemyNextY = nearestEnemy.y;

        switch (nearestEnemy.direction) {
            case 'N':
                enemyNextY -= 1;
                break;
            case 'E':
                enemyNextX += 1;
                break;
            case 'S':
                enemyNextY += 1;
                break;
            case 'W':
                enemyNextX -= 1;
                break;
        }

        // Ensure the next position is within bounds
        if (enemyNextX < 0 || enemyNextX >= 13 || enemyNextY < 0 || enemyNextY >= 13) {
            // If out of bounds, use the enemy's current position
            enemyNextX = nearestEnemy.x;
            enemyNextY = nearestEnemy.y;
        }

        // Calculate the desired direction to face
        char desiredDirection = calculateDesiredDirection(shipX, shipY, enemyNextX, enemyNextY);

        if (shipDirection != desiredDirection) {
            return rotateTowards(shipDirection, desiredDirection);
        } else {
            // Already facing the correct direction, skip turn
            return null;
        }
    }

    private char calculateDesiredDirection(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;

        if (dx == 0 && dy == 0) {
            // Same position, arbitrary direction
            return 'N';
        }

        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx > 0 ? 'E' : 'W';
        } else {
            return dy > 0 ? 'S' : 'N';
        }
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

        EnemyShip(int x, int y, char direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
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