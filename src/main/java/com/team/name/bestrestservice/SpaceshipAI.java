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
            // At the target cell, rotate and fire at nearest enemy considering their facing direction
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
            // No enemies found, skip turn
            return null;
        }

        // Include current position in predicted positions
        for (EnemyShip enemy : enemies) {
            enemy.predictPositions(field);
        }

        // Find the best enemy to target considering their facing direction
        EnemyShip targetEnemy = null;
        int minTurnsToFire = Integer.MAX_VALUE;
        char desiredDirection = shipDirection;

        for (EnemyShip enemy : enemies) {
            // Calculate the threat level based on enemy's facing direction
            int threatLevel = calculateThreatLevel(shipX, shipY, enemy);

            // For each predicted position, calculate the direction and turns needed
            for (PredictedPosition pos : enemy.predictedPositions) {
                char dirToEnemy = calculateDesiredDirection(shipX, shipY, pos.x, pos.y);
                int turnsNeeded = calculateTurnsNeeded(shipDirection, dirToEnemy);

                int totalScore = threatLevel + turnsNeeded;

                if (totalScore < minTurnsToFire) {
                    minTurnsToFire = totalScore;
                    desiredDirection = dirToEnemy;
                    targetEnemy = enemy;
                }
            }
        }

        if (shipDirection == desiredDirection) {
            // Check if the enemy is within firing range
            if (isEnemyInFiringRange(shipX, shipY, shipDirection, targetEnemy, field)) {
                return "F";
            } else {
                // Enemy is not in range or blocked, skip turn
                return null;
            }
        } else {
            // Rotate towards the enemy
            return rotateTowards(shipDirection, desiredDirection);
        }
    }

    private int calculateThreatLevel(int shipX, int shipY, EnemyShip enemy) {
        // Higher threat level if enemy is facing us
        int dx = shipX - enemy.x;
        int dy = shipY - enemy.y;

        char directionToUs = calculateDesiredDirection(enemy.x, enemy.y, shipX, shipY);

        if (enemy.direction == directionToUs) {
            return 0; // Highest priority
        } else {
            return 1; // Lower priority
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

            // Check if the enemy is at this position
            if (enemy.isAtPosition(currentX, currentY)) {
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
            // Already facing the desired direction
            return null; // Skip turn
        } else if (leftTurns <= rightTurns) {
            return "L";
        } else {
            return "R";
        }
    }

    // EnemyShip class with updated prediction logic
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

        boolean isAtPosition(int posX, int posY) {
            // Check if the enemy is at a predicted position
            for (PredictedPosition pos : predictedPositions) {
                if (pos.x == posX && pos.y == posY && pos.turnsAhead == 0) {
                    return true; // Enemy is currently at this position
                }
            }
            return false;
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

    // GameStatus class remains the same
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