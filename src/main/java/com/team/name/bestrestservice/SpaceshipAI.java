package com.team.name.bestrestservice;
import com.team.name.bestrestservice.controller.TaskResolveController;

import java.util.*;

public class SpaceshipAI {

    public static String determineMove(TaskResolveController.GameStatus gameStatus) {
        String[][] board = gameStatus.field;
        int narrowingIn = gameStatus.narrowingIn;

        // Locate player's ship and game entities
        int playerX = -1, playerY = -1;
        char playerDirection = 'N';
        List<int[]> coins = new ArrayList<>();
        List<int[]> enemies = new ArrayList<>();
        int rows = board.length;
        int cols = board[0].length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String cell = board[i][j];
                if (cell.startsWith("P")) {
                    playerX = i;
                    playerY = j;
                    playerDirection = cell.charAt(1);
                } else if (cell.equals("C")) {
                    coins.add(new int[]{i, j});
                } else if (cell.startsWith("E")) {
                    enemies.add(new int[]{i, j, cell.charAt(1)});
                }
            }
        }

        // Decide action based on game state
        if (playerX == -1 || playerY == -1) return "M"; // Default fallback

        // 1. Check for enemies in firing range
        for (int[] enemy : enemies) {
            if (isInFiringRange(playerX, playerY, playerDirection, enemy[0], enemy[1])) {
                return "F";
            }
        }

        // 2. Move towards the nearest coin
        int[] targetCoin = findNearest(playerX, playerY, coins);
        if (targetCoin != null) {
            return navigateTo(playerX, playerY, playerDirection, targetCoin[0], targetCoin[1]);
        }

        // 3. Avoid collisions or narrowing zones
        if (narrowingIn <= 1 && isNearEdge(playerX, playerY, rows, cols)) {
            return avoidEdge(playerDirection);
        }

        // 4. Default movement
        return "M";
    }

    private static boolean isInFiringRange(int x, int y, char direction, int targetX, int targetY) {
        switch (direction) {
            case 'N': return x > targetX && x - targetX <= 4 && y == targetY;
            case 'S': return x < targetX && targetX - x <= 4 && y == targetY;
            case 'E': return y < targetY && targetY - y <= 4 && x == targetX;
            case 'W': return y > targetY && y - targetY <= 4 && x == targetX;
        }
        return false;
    }
    private static int[] findNearest(int x, int y, List<int[]> targets) {
        int[] nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (int[] target : targets) {
            int distance = Math.abs(x - target[0]) + Math.abs(y - target[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = target;
            }
        }
        return nearest;
    }

    private static String navigateTo(int x, int y, char direction, int targetX, int targetY) {
        if (x < targetX) {
            return direction == 'S' ? "M" : "R";
        } else if (x > targetX) {
            return direction == 'N' ? "M" : "L";
        } else if (y < targetY) {
            return direction == 'E' ? "M" : "R";
        } else if (y > targetY) {
            return direction == 'W' ? "M" : "L";
        }
        return "M";
    }

    private static boolean isNearEdge(int x, int y, int rows, int cols) {
        return x == 1 || x == rows - 2 || y == 1 || y == cols - 2;
    }

    private static String avoidEdge(char direction) {
        return "R";
    }

}
