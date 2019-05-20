package cn.edu.nchu.stu.tsuro;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Scanner;

class Block implements Cloneable {

    /**
     * 拼图的图像
     */
    Image image;

    /**
     * 拼图包含的路径
     */
    Path[] paths;

    /**
     * 旋转次数
     */
    int rotate90degNumber = 0;

    /**
     * 拼图所在位置横坐标
     */
    int blockX;

    /**
     * 拼图所在位置纵坐标
     */
    int blockY;

    private static class BlockInfo {

        Image image;

        Path[] paths;

        BlockInfo(Image image, Path[] paths) {
            this.image = image;
            this.paths = paths;
        }
    }

    /**
     * 所有拼图信息
     */
    private static ArrayList<BlockInfo> allBlockInfo = new ArrayList<>();

    /**
     * 读取拼图
     */
    private static void loadBlockData() {
        Scanner scanner = new Scanner(Block.class.getResourceAsStream("/block.txt"));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Scanner lineScanner = new Scanner(line);
            String imageFilename = lineScanner.next();
            Path[] paths = new Path[4];
            boolean[] added = new boolean[8];
            boolean ok = true;
            for (int i = 0; i < 4; i++) {
                paths[i] = new Path();
                paths[i].start = lineScanner.nextInt() - 1;
                paths[i].end = lineScanner.nextInt() - 1;
                if (added[paths[i].start] || added[paths[i].end]) {
                    System.out.println("Error: line " + (allBlockInfo.size() + 1));
                }
                added[paths[i].start] = added[paths[i].end] = true;
            }
            allBlockInfo.add(new BlockInfo(new Image(Block.class.getResourceAsStream("/img/" + imageFilename)), paths));
        }
    }

    static {
        loadBlockData();
    }

    /**
     * 工厂模式方法，创建拼图
     * 
     * @param type 拼图种类
     */
    static Block create(int type) {
        if (type >= allBlockInfo.size()) {
            return null;
        }
        BlockInfo blockInfo = allBlockInfo.get(type);
        return new Block(blockInfo.image, blockInfo.paths);
    }

    /**
     * 获取拼图种类数量
     */
    static int getBlockNumbers() {
        return allBlockInfo.size();
    }

    /**
     * 构造方法，初始化成员
     */
    private Block(Image image, Path[] paths) {
        this.image = image;
        this.paths = paths;
    }

    /**
     * 使拼图旋转 90 度
     */
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

}
