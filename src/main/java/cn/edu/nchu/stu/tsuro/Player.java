package cn.edu.nchu.stu.tsuro;

import javafx.scene.shape.Circle;

import java.util.Map;

class Player {

    int index;

    Circle piece;

    int blockX;

    int blockY;

    int currentPosition;

    Map<Integer, Boolean> visitedPositions;

    double totalLength = 0;

    boolean alive = true;
}
