package cn.edu.nchu.stu.tsuro;

class BlockPair {

    static final int VERTICAL = 0;

    static final int HORIZONTAL = 1;

    int type;

    Integer firstBlockX;

    Integer firstBlockY;

    Integer secondBlockX;

    Integer secondBlockY;

    @Override
    public String toString() {
        return "BlockPair{" +
                "type=" + type +
                ", firstBlockX=" + firstBlockX +
                ", firstBlockY=" + firstBlockY +
                ", secondBlockX=" + secondBlockX +
                ", secondBlockY=" + secondBlockY +
                '}';
    }
}