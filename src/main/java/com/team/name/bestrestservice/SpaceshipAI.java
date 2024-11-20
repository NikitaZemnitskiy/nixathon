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
            // Move towards the center
            String move = moveTowards(shipX, shipY, shipDirection, centerX, centerY, field);
            return move;
        } else {
            // At the center, target the nearest enemy
            String move = attackNearestEnemy(shipX, shipY, shipDirection, field);
            return move;
        }
    }

    private String moveTowards(int shipX, int shipY, char shipDirection, int targetX, int targetY, String[][] field) {
        int dx = targetX - shipX;
        int dy = targetY - shipY;

        char desiredDirection;

        // Decide whether to move in X or Y direction
        if (Math.abs(dx) > Math.abs(dy)) {
            desiredDirection = dx > 0 ? 'E' : 'W';
        } else {
            desiredDirection = dy > 0 ? 'S' : 'N';
        }

        if (shipDirection == desiredDirection) {
            // Attempt to move forward if the next cell is empty or contains a coin
            int[] nextPos = getNextPosition(shipX, shipY, shipDirection);
            if (isCellFree(nextPos[0], nextPos[1], field)) {
                return "M";
            } else {
                // Cannot move forward, try rotating
                return "L";
            }
        } else {
            // Rotate towards the desired direction
            return rotateTowards(shipDirection, desiredDirection);
        }
    }

    private String attackNearestEnemy(int shipX, int shipY, char shipDirection, String[][] field) {
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
            // No enemies found, default to move
            return "M";
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

        if (dx == 0 && dy == 0) {
            // Same position (collision), default to move
            return "M";
        } else if (dx == 0) {
            desiredDirection = dy > 0 ? 'S' : 'N';
        } else if (dy == 0) {
            desiredDirection = dx > 0 ? 'E' : 'W';
        } else {
            // Enemy not in line, prefer vertical rotation
            desiredDirection = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 'E' : 'W') : (dy > 0 ? 'S' : 'N');
        }

        if (shipDirection == desiredDirection) {
            // Check if the enemy is within firing range
            if (isEnemyInFiringRange(shipX, shipY, shipDirection, nearestEnemy, field)) {
                return "F";
            } else {
                // Wait or move
                return "M";
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

        if (leftTurns <= rightTurns) {
            return "L";
        } else {
            return "R";
        }
    }

    private int[] getNextPosition(int x, int y, char direction) {
        switch (direction) {
            case 'N': return new int[]{x, y - 1};
            case 'S': return new int[]{x, y + 1};
            case 'E': return new int[]{x + 1, y};
            case 'W': return new int[]{x - 1, y};
            default: return new int[]{x, y};
        }
    }

    private boolean isCellFree(int x, int y, String[][] field) {
        if (x < 0 || x >= 13 || y < 0 || y >= 13) {
            return false; // Out of bounds
        }
        String cell = field[y][x];
        return cell == null || cell.equals("_") || cell.equals("C");
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