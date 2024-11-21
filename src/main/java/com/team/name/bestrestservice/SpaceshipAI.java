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
            // Ship not found, default to move
            return "M";
        }

        // Coordinates of the center
        int centerX = 6;
        int centerY = 6;

        if (shipX != centerX || shipY != centerY) {
            // Move towards the center using pathfinding
            String move = moveTowards(shipX, shipY, shipDirection, centerX, centerY, field);
            return move;
        } else {
            // At the target cell, rotate and fire at nearest enemy
            String move = rotateAndFireAtNearestEnemy(shipX, shipY, shipDirection, field);
            return move;
        }
    }

    private String moveTowards(int shipX, int shipY, char shipDirection, int targetX, int targetY, String[][] field) {
        // Use BFS to find the next step towards the target avoiding obstacles
        int[][] directions = {{0, -1, 'N'}, {1, 0, 'E'}, {0, 1, 'S'}, {-1, 0, 'W'}};
        boolean[][] visited = new boolean[13][13];
        int[][] prevX = new int[13][13];
        int[][] prevY = new int[13][13];
        char[][] prevDirection = new char[13][13];

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

            for (int[] dir : directions) {
                int newX = curX + dir[0];
                int newY = curY + dir[1];

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
            return "M";
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

    private String rotateAndFireAtNearestEnemy(int shipX, int shipY, char shipDirection, String[][] field) {
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
            // No enemies found, default to rotate
            return "L";
        }

        // Find the nearest enemy
        EnemyShip nearestEnemy = null;
        int minDistance = Integer.MAX_VALUE;
        for (EnemyShip enemy : enemies) {
            int distance = Math.abs(enemy.x - shipX) + Math.abs(enemy.y - shipY);
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = enemy;
            }
        }

        // Determine the direction towards the nearest enemy
        int dx = nearestEnemy.x - shipX;
        int dy = nearestEnemy.y - shipY;

        char desiredDirection;

        if (Math.abs(dx) > Math.abs(dy)) {
            desiredDirection = dx > 0 ? 'E' : 'W';
        } else {
            desiredDirection = dy > 0 ? 'S' : 'N';
        }

        if (shipDirection == desiredDirection) {
            // Check if the enemy is within firing range
            if (isEnemyInFiringRange(shipX, shipY, shipDirection, nearestEnemy, field)) {
                return "F";
            } else {
                // Enemy not in range, continue rotating
                return "L";
            }
        } else {
            // Rotate towards the enemy
            return rotateTowards(shipDirection, desiredDirection);
        }
    }

    private boolean isEnemyInFiringRange(int shipX, int shipY, char shipDirection, EnemyShip enemy, String[][] field) {
        int range = 4;
        int dx = 0, dy = 0;

        switch (shipDirection) {
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

            if (currentX == enemy.x && currentY == enemy.y) {
                return true; // Enemy is in firing range
            }
        }

        return false;
    }

    private String rotateTowards(char currentDirection, char desiredDirection) {
        String directions = "NESW";
        int currentIndex = directions.indexOf(currentDirection);
        int desiredIndex = directions.indexOf(desiredDirection);

        int leftTurns = (currentIndex - desiredIndex + 4) % 4;
        int rightTurns = (desiredIndex - currentIndex + 4) % 4;

        if (leftTurns == 0) {
            return "F"; // Already facing the desired direction, try firing
        } else if (leftTurns <= rightTurns) {
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

    // Include the GameStatus class here if needed
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