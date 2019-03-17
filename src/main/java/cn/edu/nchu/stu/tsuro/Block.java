package cn.edu.nchu.stu.tsuro;

import javafx.scene.image.Image;

class Block {

    Image image;

    Path[] paths;

    Block[] adjacentBlocks = new Block[4];

    int rotate90degNumber = 0;

    int blockX;

    int blockY;

    Block(Image image, Path[] paths) {
        this.image = image;
        this.paths = paths;
    }

    void rotate90deg() {
        for (Path path : paths) {
            path.start += 2;
            path.start %= 8;
            path.end += 2;
            path.end %= 8;
        }
        rotate90degNumber += 1;
        rotate90degNumber %= 4;
    }

    void select(int blockX, int blockY) {

    }
}
