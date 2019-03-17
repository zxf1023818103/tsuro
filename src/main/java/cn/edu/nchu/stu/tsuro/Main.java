package cn.edu.nchu.stu.tsuro;

import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Main extends Application {

    private static final double PIECE_RADIUS = 5.0;

    private static final double MOVE_DURATION = 1000.0;

    public HBox startPanel;

    public ComboBox<Integer> playerNumberBox;

    public StackPane mainPanel;

    public GridPane boardPanel;

    public AnchorPane piecePanel;

    public Label playerLabel;

    public Label totalLengthLabel;

    public ImageView blockView1;

    public ImageView blockView2;

    public ImageView blockView3;

    public ToggleGroup gamingTypeGroup;

    public ToggleGroup boardSizeGroup;

    public GridPane blockPilePanel;

    public GridPane scorePanel;

    /**
     * 游戏人数
     */
    private int playerNumber;

    /**
     * 棋盘边长
     */
    private int sideSize;

    /**
     * 获胜方式
     */
    private GamingType gamingType;

    /**
     * 所有玩家
     */
    private Player[] allPlayers;

    private ImageView[] blockViews = new ImageView[3];

    private Circle[] allPieces;

    /**
     * 当前轮到的玩家的位于 allPlayers 数组的下标
     */
    private int currentPlayerIndex;

    /**
     * 棋子的颜色
     */
    private final Color[] pieceColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN};

    /**
     * 所有拼图
     */
    private ArrayList<Block> allBlocks = new ArrayList<>();

    /**
     * 存储编号对应的节点坐标
     * 给每个点编号，若为 n x n 的棋盘，则每块拼图的一条边有 2 个点，每行（列）有 2 x n 个点，共有 2 x (n + 1) 行（列），总计 2 x n x (n + 1) x 2 个点
     */
    private Point2D[] allPoints;

    /**
     * 存储已走过通路的邻接矩阵，矩阵元素为两点间的距离，为 0 则没有通路
     */
    private boolean[][] adjacentMatrix;

    /**
     * 标记已经走过的块
     */
    private boolean[][] visitedBlocks;

    @Override
    public void start(Stage primaryStage) throws IOException {
        /// 读取界面文件
        Parent root = FXMLLoader.load(getClass().getResource("/board.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void startGame(MouseEvent event) {
        /// 开始界面设置成禁用状态，表示正在加载
        startPanel.setDisable(true);
        /// 读取拼图
        Scanner scanner = new Scanner(getClass().getResourceAsStream("/block.txt"));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Scanner lineScanner = new Scanner(line);
            String imageFilename = lineScanner.next();
            Path[] paths = new Path[4];
            for (int i = 0; i < 4; i++) {
                paths[i] = new Path();
                paths[i].start = lineScanner.nextInt();
                paths[i].end = lineScanner.nextInt();
            }
            allBlocks.add(new Block(new Image(getClass().getResourceAsStream("/img/" + imageFilename)), paths));
        }
        blockViews[0] = blockView1;
        blockViews[1] = blockView2;
        blockViews[2] = blockView3;
        startPanel.setVisible(false);
        /// 读取界面选项
        playerNumber = playerNumberBox.getSelectionModel().getSelectedIndex() + 1;
        sideSize = Integer.parseInt(boardSizeGroup.getSelectedToggle().getUserData().toString());
        gamingType = GamingType.valueOf(gamingTypeGroup.getSelectedToggle().getUserData().toString());
        /// 初始化玩家
        allPlayers = new Player[playerNumber];
        /// 初始化邻接矩阵
        adjacentMatrix = new boolean[2 * (sideSize + 1) * sideSize * 2][2 * (sideSize + 1) * sideSize * 2];
        /// 初始化用于标记是否铺上拼图的数组
        visitedBlocks = new boolean[sideSize][sideSize];
        /// 初始化棋盘
        double width = boardPanel.getWidth();
        double height = boardPanel.getHeight();
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPrefWidth(width / sideSize);
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPrefHeight(height / sideSize);
        for (int i = 0; i < sideSize; i++) {
            boardPanel.getColumnConstraints().add(i, columnConstraints);
            boardPanel.getRowConstraints().add(i, rowConstraints);
        }
        /// 初始化所有可到达的点
        allPoints = new Point2D[2 * (sideSize + 1) * sideSize * 2];
        int curIndex = 0;
        double blockWidth = width / sideSize;
        double blockHeight = height / sideSize;
        /// 计算所有行的所有点坐标
        /// n + 1 行点
        for (int line = 0; line < sideSize + 1; line++) {
            /// 每行有 n 块拼图
            for (int blockX = 0; blockX < sideSize; blockX++) {
                /// 每块拼图的一条边有 2 个点
                for (int i = 1; i < 3; i++) {
                    double y = line * blockHeight;
                    double x = (blockX + (i / 3.0)) * blockWidth;
                    allPoints[curIndex++] = new Point2D(x, y);
                    //piecePanel.getChildren().add(new Circle(x, y, PIECE_RADIUS));
                }
            }
        }
        /// 计算所有列的所有点坐标
        /// 共有 n + 1 列点
        for (int line = 0; line < sideSize + 1; line++) {
            /// 每列有 n 块拼图
            for (int blockY = 0; blockY < sideSize; blockY++) {
                /// 每块拼图的一条边有 2 个点
                for (int i = 1; i < 3; i++) {
                    double x = line * blockWidth;
                    double y = (blockY + (i / 3.0)) * blockHeight;
                    allPoints[curIndex++] = new Point2D(x, y);
                    //piecePanel.getChildren().add(new Circle(x, y, PIECE_RADIUS));
                }
            }
        }
        /// 初始化棋子图层
        allPieces = new Circle[sideSize * 4 * 2];
        /// 绘制上边的棋子选择位
        int currentPieceIndex = 0;
        for (int block = 0; block < sideSize; block++) {
            for (int i = 1; i < 3; i++) {
                Circle piece = new Circle((width * block / sideSize) + (width * i / sideSize / 3.0), 0, PIECE_RADIUS);
                allPieces[currentPieceIndex++] = piece;
                piecePanel.getChildren().add(piece);
            }
        }
        /// 绘制下边的棋子选择位
        for (int block = 0; block < sideSize; block++) {
            for (int i = 1; i < 3; i++) {
                Circle piece = new Circle((width * block / sideSize) + (width * i / sideSize / 3.0), height, PIECE_RADIUS);
                allPieces[currentPieceIndex++] = piece;
                piecePanel.getChildren().add(piece);
            }
        }
        /// 绘制左边的棋子选择位
        for (int block = 0; block < sideSize; block++) {
            for (int i = 1; i < 3; i++) {
                Circle piece = new Circle(0, (width * block / sideSize) + (width * i / sideSize / 3.0), PIECE_RADIUS);
                allPieces[currentPieceIndex++] = piece;
                piecePanel.getChildren().add(piece);
            }
        }
        /// 绘制右边的棋子选择位
        for (int block = 0; block < sideSize; block++) {
            for (int i = 1; i < 3; i++) {
                Circle piece = new Circle(width, (width * block / sideSize) + (width * i / sideSize / 3.0), PIECE_RADIUS);
                allPieces[currentPieceIndex++] = piece;
                piecePanel.getChildren().add(piece);
            }
        }
        /// 注册事件处理函数用于棋子选择
        for (currentPieceIndex = 0; currentPieceIndex < allPieces.length; currentPieceIndex++) {
            Player player = new Player();
            player.piece = allPieces[currentPieceIndex];
            /// 记录起始点的位置是在 blockX 行 blockY 列的拼图的第 currentPointIndex 个点上。
            switch (currentPieceIndex / (sideSize * 2)) {
                case 0: /// 上边
                    player.blockX = currentPieceIndex / 2;
                    player.blockY = 0;
                    player.currentPointIndex = currentPieceIndex % 2;
                    break;
                case 1: /// 右边
                    player.blockX = sideSize - 1;
                    player.blockY = currentPieceIndex / 2 - sideSize;
                    player.currentPointIndex = currentPieceIndex % 2 + 2;
                    break;
                case 2: /// 下边
                    player.blockX = sideSize - (currentPieceIndex / 2 - (sideSize * 2)) - 1;
                    player.blockY = sideSize - 1;
                    player.currentPointIndex = 1 - (currentPieceIndex % 2) + 4;
                    break;
                default: /// 左边
                    player.blockX = 0;
                    player.blockY = sideSize - (currentPieceIndex / 2 - (sideSize * 3)) - 1;
                    player.currentPointIndex = 1 - (currentPieceIndex % 2) + 6;
                    break;
            }
            player.currentPointIndex = block2BoardPieceIndex(player.blockX, player.blockY, player.currentPointIndex);
            /// 由于 Java 的 lambda 函数不能捕获局部变量，所以需要将该棋子信息存入按钮的用户数据。
            allPieces[currentPieceIndex].setUserData(player);
            allPieces[currentPieceIndex].setOnMouseClicked(e -> {
                Circle piece = (Circle)e.getTarget();
                Player currentPlayer = (Player)piece.getUserData();
                piece.setDisable(true);
                currentPlayer.index = currentPlayerIndex;
                piece.setFill(pieceColors[currentPlayerIndex]);
                allPlayers[currentPlayerIndex++] = currentPlayer;
                /// 棋子选择完毕，开始游戏。
                if (currentPlayerIndex >= playerNumber) {
                    piecePanel.setVisible(false);
                    /// 用于记录当前轮到的玩家
                    currentPlayerIndex = 0;
                    /// 隐藏未选中的棋子，只留下已经被选择的棋子
                    for (Circle _piece : allPieces) {
                        boolean shouldRemove = true;
                        for (Player _player : allPlayers) {
                            if (_piece == _player.piece) {
                                shouldRemove = false;
                            }
                        }
                        _piece.setVisible(!shouldRemove);
                    }
                    piecePanel.setVisible(true);
                    blockPilePanel.setVisible(true);
                }
            });
            mainPanel.setVisible(true);
            deal();
        }
    }

    /**
     * 退出游戏
     */
    public void exit(MouseEvent event) {
        System.exit(0);
    }

    public void rotate90degOrSelectBlock(MouseEvent e) {
        ImageView target = (ImageView) e.getTarget();
        Block block = (Block) target.getUserData();
        /// 左键旋转拼图
        //if (e.isPrimaryButtonDown()) {
            block.rotate90deg();
            target.setRotate((target.getRotate() + 90) % 360);
        //}
        /// 右键选择拼图
        if (e.isSecondaryButtonDown()) {
            blockPilePanel.setDisable(true);
            Player player = allPlayers[currentPlayerIndex];
            scorePanel.setVisible(true);
            playerLabel.setTextFill(pieceColors[player.index]);
            /// 将选中拼图的路径转换坐标后存入邻接矩阵中，并记录该点对应的拼图位置
            for (Path path : block.paths) {
                int start = block2BoardPieceIndex(player.blockX, player.blockY, path.start);
                int end = block2BoardPieceIndex(player.blockX, player.blockY, path.end);
                adjacentMatrix[start][end] = true;
            }
            /// 标记该拼图已经走过
            visitedBlocks[block.blockY][block.blockX] = true;
            ImageView imageView = new ImageView(target.getImage());
            imageView.setFitHeight(boardPanel.getHeight() / sideSize);
            imageView.setFitWidth(boardPanel.getWidth() / sideSize);
            boardPanel.add(imageView, player.blockY, player.blockX);
            moveToEnd(player);
            currentPlayerIndex++;
            currentPlayerIndex %= playerNumber;
        }
    }

    /**
     * 根据棋盘内棋子位置获取棋子全局编号
     * @param blockX 拼图所在行
     * @param blockY 拼图所在列
     * @param blockPieceIndex 棋子在拼图内的编号
     * @return 棋子的全局编号
     */
    private int block2BoardPieceIndex(int blockX, int blockY, int blockPieceIndex) {
        switch (blockPieceIndex / 2) {
            case 0: /// 拼图上边
                return (sideSize * blockY) + (blockX * 2) + (blockPieceIndex % 2);
            case 1: /// 拼图右边
                return (2 * sideSize * (sideSize + 1)) + ((sideSize + 1) * blockX) + (blockY * 2) + (blockPieceIndex % 2);
            case 2: /// 拼图下边
                return ((sideSize + 1) * blockY) + (blockX * 2) + (1 - (blockPieceIndex % 2));
            default: /// 拼图左边
                return (2 * sideSize * (sideSize + 1)) + (sideSize * blockX) + (blockY * 2) + (1 - (blockPieceIndex % 2));
        }
    }

    /**
     * 发牌
     */
    public void deal() {
        Collections.shuffle(allBlocks);
        for (int i = 0; i < blockViews.length; i++) {
            Block block = allBlocks.get(i);
            blockViews[i].setImage(block.image);
            blockViews[i].setUserData(block);
        }
    }

    /**
     * 获取棋子下一步要到达的位置编号。
     * @param currentIndex 当前棋子的位置编号
     * @return 棋子下一步要到达的位置编号
     */
    private int getNextPointIndex(int currentIndex) {
        for (int i = 0; i < adjacentMatrix.length; i++) {
            if (adjacentMatrix[currentIndex][i])
                return i;
        }
        return -1;
    }

    /**
     * 判断点是否在边缘
     * @param index 点的全局编号
     */
    private boolean isEdgePointIndex(int index) {
        /// 上边
        if (0 <= index && index < sideSize * 2)
            return true;
        /// 下边
        else if (sideSize * 2 * sideSize <= index && index < sideSize * 2 * (sideSize + 1))
            return true;
        /// 左边
        else if (sideSize * 2 * (sideSize + 1) <= index && index < sideSize * 2 * (sideSize + 2))
            return true;
        /// 右边
        else if (sideSize * 2 * (sideSize * 2 + 1) <= index && index < sideSize * 2 * (sideSize + 1) * 2)
            return true;
        else return false;
    }

    /**
     * 选择完拼图后，让当前玩家的棋子向前移动至终点。
     * @param player 当前玩家
     */
    private void moveToEnd(Player player) {
        /// 更新标签颜色
        playerLabel.setTextFill(pieceColors[player.index]);
        /// 更新总路程显示
        totalLengthLabel.setText(String.format("%.1f cm", player.totalLength / (piecePanel.getWidth() / sideSize) * 5.0));
        int nextPointIndex = getNextPointIndex(player.currentPointIndex);
        if (nextPointIndex == -1 && player.totalLength != 0) {
            /// 如果棋子到达棋盘边缘则游戏结束
            if (isEdgePointIndex(player.currentPointIndex)) {
                /// 标记该棋子已死亡
                player.piece.setOpacity(0.3);
                player.alive = false;
                /// 若所有棋子均已死亡，则游戏结束
                boolean gameOver = true;
                for (Player _player : allPlayers) {
                    if (_player.alive)
                        gameOver = false;
                }
                if (gameOver) {
                    blockPilePanel.setDisable(true);
                    mainPanel.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
                }
            }
            else {
                /// 切换下一位玩家并发牌
                deal();
                blockPilePanel.setDisable(false);
            }
        }
        else {
            /// 更新棋子当前位置
            Point2D currentPoint = allPoints[player.currentPointIndex];
            player.currentPointIndex = nextPointIndex;
            Point2D nextPoint = allPoints[nextPointIndex];
            /// 更新走过的总路程
            player.totalLength += currentPoint.distance(nextPoint);
            /// 初始化动画
            PathTransition animation = new PathTransition(new Duration(MOVE_DURATION), new Line(currentPoint.getX(), currentPoint.getY(), nextPoint.getX(), nextPoint.getY()), player.piece);
            Line trace = new Line(currentPoint.getX(), currentPoint.getY(), nextPoint.getX(), nextPoint.getY());
            trace.setFill(player.piece.getFill());
            piecePanel.getChildren().add(trace);
            /// 递归调用该函数，直到移动到终点
            animation.setOnFinished(e -> moveToEnd(player));
            animation.play();
        }
    }

}
