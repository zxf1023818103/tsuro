package cn.edu.nchu.stu.tsuro;

import javafx.animation.Animation;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Main extends Application {

    /**
     * 棋子半径
     */
    private static final double PIECE_RADIUS = 5.0;

    /**
     * 移动时长
     */
    private static final double MOVE_DURATION = 1000.0;

    /**
     * 游戏菜单面板
     */
    public HBox startPanel;

    /**
     * 游戏人数下拉框
     */
    public ComboBox<Integer> playerNumberBox;

    /**
     * 游戏主界面面板
     */
    public StackPane mainPanel;

    /**
     * 棋盘图层
     */
    public GridPane boardPanel;

    /**
     * 棋子图层
     */
    public AnchorPane piecePanel;

    /**
     * 当前玩家标签
     */
    public Label playerLabel;

    /**
     * 当前路程长度标签
     */
    public Label totalLengthLabel;

    /**
     * 牌堆拼图位 1
     */
    public ImageView blockView1;

    /**
     * 牌堆拼图位 2
     */
    public ImageView blockView2;

    /**
     * 牌堆拼图位 3
     */
    public ImageView blockView3;

    /**
     * 游戏类型单选框
     */
    public ToggleGroup gamingTypeGroup;

    /**
     * 棋盘大小单选框
     */
    public ToggleGroup boardSizeGroup;

    /**
     * 牌堆
     */
    public GridPane blockPilePanel;

    /**
     * 计分板面板
     */
    public GridPane scorePanel;

    /**
     * 游戏结果面板
     */
    public VBox resultPanel;

    /**
     * 获胜者标签
     */
    public Label winnerLabel;

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

    /**
     * 牌堆
     */
    private ImageView[] blockViews = new ImageView[3];

    /**
     * 当前轮到的玩家的位于 allPlayers 数组的下标
     */
    private int currentPlayerIndex;

    /**
     * 棋子的颜色
     */
    private final Color[] pieceColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN};

    /**
     * 存储编号对应的节点坐标
     * 给每个点编号，若为 n x n 的棋盘，则每块拼图的一条边有 2 个点，每行（列）有 2 x n 个点，共有 2 x (n + 1) 行（列），总计 2 x n x (n + 1) x 2 个点
     */
    private Point2D[] availablePoints;

    /**
     * 存储已走过通路的邻接矩阵，矩阵元素为两点间的距离，为 0 则没有通路
     */
    private boolean[][] adjacentMatrix;

    /**
     * 标记已经走过的块
     */
    private boolean[][] visitedBlocks;

    /**
     * 存储已经走过的点
     */
    private Map<Integer, Boolean> visitedPositions = new HashMap<>();

    /**
     * 入口点，加载界面文件
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Tsuro");
        /// 读取界面文件
        Parent root = FXMLLoader.load(getClass().getResource("/board.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }


    /**
     * 初始化牌堆
     */
    private void initializeBlockViewArray() {
        blockViews[0] = blockView1;
        blockViews[1] = blockView2;
        blockViews[2] = blockView3;
    }

    /**
     * 读取界面选项
     */
    private void getGameOptions() {
        playerNumber = playerNumberBox.getSelectionModel().getSelectedIndex() + 1;
        sideSize = Integer.parseInt(boardSizeGroup.getSelectedToggle().getUserData().toString());
        gamingType = GamingType.valueOf(gamingTypeGroup.getSelectedToggle().getUserData().toString());
    }

    /**
     * 初始化所有可到达点
     */
    private void initializeAvailablePoints(double width, double height) {

        double blockWidth = width / sideSize;

        double blockHeight = height / sideSize;

        availablePoints = new Point2D[sideSize * 2 * (sideSize + 1) * 2];

        int currentPointIndex = 0;

        for (int row = 0; row < sideSize + 1; row++) {
            for (int blockX = 0; blockX < sideSize; blockX++) {
                for (int i = 1; i < 3; i++) {
                    double x = blockX * blockWidth + (i * blockWidth / 3);
                    double y = row * blockHeight;
                    availablePoints[currentPointIndex++] = new Point2D(x, y);
                }
            }
        }

        for (int column = 0; column < sideSize + 1; column++) {
            for (int blockY = 0; blockY < sideSize; blockY++) {
                for (int i = 1; i < 3; i++) {
                    double x = column * blockWidth;
                    double y = blockY * blockHeight + (i * blockHeight / 3);
                    availablePoints[currentPointIndex++] = new Point2D(x, y);
                }
            }
        }
    }

    /**
     * 绘制棋盘
     */
    private void drawBoard(double width, double height) {
        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPrefWidth(width / sideSize);
        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPrefHeight(height / sideSize);
        for (int i = 0; i < sideSize; i++) {
            boardPanel.getColumnConstraints().add(i, columnConstraints);
            boardPanel.getRowConstraints().add(i, rowConstraints);
        }

    }

    /**
     * 绘制棋子选择位
     */
    private void drawPieceSelector() {
        allPlayers = new Player[playerNumber];
        Circle[] edgePieces = new Circle[sideSize * 4 * 2];
        PieceInfo[] pieceInfos = new PieceInfo[sideSize * 4 * 2];
        for (int i = 0; i < pieceInfos.length; i++) {
            pieceInfos[i] = new PieceInfo();
        }

        int currentPieceIndex = 0;
        for (int i = 0; i < sideSize * 2; i++) {
            PieceInfo pieceInfo = pieceInfos[currentPieceIndex];
            pieceInfo.globalPosition = i;
            pieceInfo.blockX = i / 2;
            pieceInfo.blockY = 0;
            //System.out.printf("globalPosition=%d blockX=%d blockY=%d\n", pieceInfo.globalPosition, pieceInfo.blockX, pieceInfo.blockY);
            currentPieceIndex++;
        }
        for (int i = sideSize * 2 * sideSize; i < sideSize * 2 * (sideSize + 1); i++) {
            PieceInfo pieceInfo = pieceInfos[currentPieceIndex];
            pieceInfo.globalPosition = i;
            pieceInfo.blockY = sideSize - 1;
            pieceInfo.blockX = (currentPieceIndex - (sideSize * 2)) / 2;
            //System.out.printf("globalPosition=%d blockX=%d blockY=%d\n", pieceInfo.globalPosition, pieceInfo.blockX, pieceInfo.blockY);
            currentPieceIndex++;
        }
        for (int i = sideSize * 2 * (sideSize + 1); i < sideSize * 2 * (sideSize + 2); i++) {
            PieceInfo pieceInfo = pieceInfos[currentPieceIndex];
            pieceInfo.globalPosition = i;
            pieceInfo.blockY = (currentPieceIndex - (sideSize * 2) * 2) / 2;
            pieceInfo.blockX = 0;
            //System.out.printf("globalPosition=%d blockX=%d blockY=%d\n", pieceInfo.globalPosition, pieceInfo.blockX, pieceInfo.blockY);
            currentPieceIndex++;
        }
        for (int i = sideSize * 2 * (2 * sideSize + 1); i < sideSize * 2 * (sideSize + 1) * 2; i++) {
            PieceInfo pieceInfo = pieceInfos[currentPieceIndex];
            pieceInfo.globalPosition = i;
            pieceInfo.blockX = sideSize - 1;
            pieceInfo.blockY = (currentPieceIndex - (sideSize * 2) * 3) / 2;
            //System.out.printf("globalPosition=%d blockX=%d blockY=%d\n", pieceInfo.globalPosition, pieceInfo.blockX, pieceInfo.blockY);
            currentPieceIndex++;
        }
        for (int i = 0; i < pieceInfos.length; i++) {
            Circle piece = new Circle(PIECE_RADIUS);
            piece.setCenterX(availablePoints[pieceInfos[i].globalPosition].getX());
            piece.setCenterY(availablePoints[pieceInfos[i].globalPosition].getY());
            piece.setUserData(pieceInfos[i]);
            piece.setOnMouseClicked(e -> {
                Circle target = (Circle) e.getTarget();
                target.setDisable(true);
                PieceInfo pieceInfo = (PieceInfo) target.getUserData();
                target.setFill(pieceColors[currentPlayerIndex]);
                Player player = new Player();
                player.currentPosition = pieceInfo.globalPosition;
                player.blockX = pieceInfo.blockX;
                player.blockY = pieceInfo.blockY;
                player.index = currentPlayerIndex;
                player.piece = target;
                player.totalLength = 0;
                //player.visitedPositions = new HashMap<>();
                player.visitedPositions = visitedPositions;
                allPlayers[currentPlayerIndex] = player;
                currentPlayerIndex++;

                System.out.println(String.format("Player %d selected piece %d.", player.index, player.currentPosition));

                if (currentPlayerIndex >= playerNumber) {
                    piecePanel.setVisible(false);
                    currentPlayerIndex = 0;
                    piecePanel.getChildren().clear();
                    debugGlobalPosition();
                    for (Player _player : allPlayers) {
                        piecePanel.getChildren().add(_player.piece);
                    }
                    piecePanel.setVisible(true);
                    blockPilePanel.setVisible(true);
                    scorePanel.setVisible(true);
                }
            });
            edgePieces[i] = piece;
        }
        piecePanel.getChildren().addAll(edgePieces);
    }

    /**
     * 获取下一位轮到的玩家
     */
    private Player nextPlayer() {
        Player player = null;
        while (!(player = allPlayers[currentPlayerIndex++]).alive) {
            currentPlayerIndex %= playerNumber;
        }
        currentPlayerIndex %= playerNumber;
        return player;
    }

    /**
     * 选择拼图并放在棋盘上
     */
    private void selectBlock(Block block) {
        Player player = nextPlayer();

        blockPilePanel.setDisable(true);
        scorePanel.setVisible(true);
        playerLabel.setTextFill(pieceColors[player.index]);

        ImageView imageView = new ImageView(block.image);
        imageView.setFitHeight(boardPanel.getHeight() / sideSize);
        imageView.setFitWidth(boardPanel.getWidth() / sideSize);
        imageView.setRotate(block.rotate90degNumber * 90);
        boardPanel.add(imageView, player.blockX, player.blockY);

        visitedBlocks[player.blockY][player.blockX] = true;

        for (Path path : block.paths) {
            System.out.printf("%d - %d\t", path.start, path.end);
            Integer start = convertLocalPositionToGlobalPosition(player.blockX, player.blockY, path.start);
            Integer end = convertLocalPositionToGlobalPosition(player.blockX, player.blockY, path.end);
            System.out.printf("%d - %d\n", start, end);
            adjacentMatrix[start][end] = adjacentMatrix[end][start] = true;
        }

        step(player);
    }

    /**
     * 调试选项，在棋盘上显示全局位置
     */
    private void debugGlobalPosition() {
//        for (int i = 0; i < availablePoints.length; i++) {
//            Label label = new Label(Integer.toString(i));
//            label.setStyle("-fx-text-fill: red");
//            label.setLayoutX(availablePoints[i].getX());
//            label.setLayoutY(availablePoints[i].getY());
//            piecePanel.getChildren().add(label);
//        }
    }

    /**
     * 选择完游戏选项后，开始游戏
     */
    public void startGame(ActionEvent event) {
        /// 开始界面设置成禁用状态，表示正在加载
        startPanel.setDisable(true);

        initializeBlockViewArray();

        startPanel.setVisible(false);

        getGameOptions();

        /// 绘制棋盘
        double width = boardPanel.getWidth();
        double height = boardPanel.getHeight();

        drawBoard(width, height);

        initializeAvailablePoints(width, height);

        /// 初始化邻接矩阵数组
        adjacentMatrix = new boolean[availablePoints.length][availablePoints.length];
        /// 初始化用于标记是否铺上拼图的数组
        visitedBlocks = new boolean[sideSize][sideSize];

        drawPieceSelector();

        playerLabel.setTextFill(pieceColors[0]);

        mainPanel.setVisible(true);

        debugGlobalPosition();

        deal();
    }

    /**
     * 退出游戏
     */
    public void exit(ActionEvent event) {
        System.exit(0);
    }

    /**
     * 牌堆的鼠标点击事件处理函数
     */
    public void rotate90degOrSelectBlock(MouseEvent e) {
        ImageView target = (ImageView) e.getTarget();
        Block block = (Block) target.getUserData();
        switch (e.getButton()) {
            case PRIMARY:
                block.rotate90deg();
                target.setRotate((target.getRotate() + 90) % 360);
                break;
            case SECONDARY:
                selectBlock(block);
                break;
        }
    }

    /**
     * 根据棋盘内棋子位置获取棋子全局编号
     *
     * @param blockX        拼图所在行
     * @param blockY        拼图所在列
     * @param localPosition 棋子在拼图内的编号
     * @return 棋子的全局编号
     */
    private int convertLocalPositionToGlobalPosition(int blockX, int blockY, int localPosition) {
        switch (localPosition) {
            case 0:
            case 1:
                return blockY * (sideSize * 2) + (blockX * 2) + localPosition;
            case 6:
            case 7:
                return blockX * (sideSize * 2) + (blockY * 2) + (1 - (localPosition - 6)) + (2 * sideSize * (sideSize + 1));
            case 2:
            case 3:
                return convertLocalPositionToGlobalPosition(blockX + 1, blockY, localPosition == 2 ? 7 : 6);
            default:
                return convertLocalPositionToGlobalPosition(blockX, blockY + 1, localPosition == 5 ? 0 : 1);
        }
    }

    /**
     * 发牌
     */
    private void deal() {
        Random random = new Random(System.nanoTime());
        for (ImageView blockView : blockViews) {
            Block block = Block.create(random.nextInt(Block.getBlockNumbers()));
            assert (block != null);
            blockView.setUserData(block);
            blockView.setImage(block.image);
        }
        blockPilePanel.setDisable(false);
    }

    /**
     * 获取棋子下一步要到达的位置编号。
     *
     * @param player 玩家
     * @return 棋子下一步要到达的位置编号
     */
    private Integer getNextPosition(Player player) {
        Integer result = null;

        for (int i = 0; i < adjacentMatrix.length; i++) {
            Boolean visited = player.visitedPositions.get(i);
            visited = visited == null ? false : visited;
            if ((adjacentMatrix[player.currentPosition][i] || adjacentMatrix[i][player.currentPosition]) && !visited) {
                result = i;
                break;
            }
        }

        return result;
    }

    /**
     * 判断点是否在边缘
     *
     * @param globalPosition 点的全局编号
     */
    private boolean isPositionOnEdge(int globalPosition) {
        return (0 <= globalPosition && globalPosition < (sideSize * 2))
                || ((sideSize * 2 * sideSize) <= globalPosition && globalPosition < (sideSize * 2 * (sideSize + 2)))
                || ((sideSize * 2 * (2 * sideSize + 1)) <= globalPosition && globalPosition < (sideSize * 2 * (sideSize + 1) * 2));
    }

    /**
     * 根据全局位置编号获取该点相邻的两块格子的位置信息
     *
     * @param globalPosition 全局位置
     */
    private BlockPair getNearBlockPairByGlobalPosition(int globalPosition) {

        BlockPair result = new BlockPair();

        if (globalPosition < 0) {
            return null;
        }
        /// 上边
        else if (globalPosition < 2 * sideSize) {
            result.firstBlockX = null;
            result.firstBlockY = null;
            result.secondBlockX = globalPosition % (sideSize * 2) / 2;
            result.secondBlockY = 0;
            result.type = BlockPair.VERTICAL;
        }
        /// 中间
        else if (globalPosition < 2 * sideSize * sideSize) {
            result.firstBlockX = globalPosition % (sideSize * 2) / 2;
            result.firstBlockY = globalPosition / (sideSize * 2) - 1;
            result.secondBlockX = result.firstBlockX;
            result.secondBlockY = result.firstBlockY + 1;
            result.type = BlockPair.VERTICAL;
        }
        /// 下边
        else if (globalPosition < 2 * sideSize * (sideSize + 1)) {
            result.firstBlockX = globalPosition % (sideSize * 2) / 2;
            result.firstBlockY = sideSize - 1;
            result.secondBlockX = null;
            result.secondBlockY = null;
            result.type = BlockPair.VERTICAL;
        }
        /// 左边
        else if (globalPosition < 2 * sideSize * (sideSize + 2)) {
            result.firstBlockX = null;
            result.firstBlockY = null;
            result.secondBlockX = 0;
            result.secondBlockY = globalPosition % (sideSize * 2) / 2;
            result.type = BlockPair.HORIZONTAL;
        }
        /// 中间
        else if (globalPosition < 2 * sideSize * (2 * sideSize + 1)) {
            result.firstBlockX = globalPosition / (sideSize * 2) - sideSize - 2;
            result.firstBlockY = globalPosition % (sideSize * 2) / 2;
            result.secondBlockX = result.firstBlockX + 1;
            result.secondBlockY = result.firstBlockY;
            result.type = BlockPair.HORIZONTAL;
        }
        /// 下边
        else if (globalPosition < 4 * sideSize * (sideSize + 1)) {
            result.firstBlockX = sideSize - 1;
            result.firstBlockY = globalPosition % (sideSize * 2) / 2;
            result.secondBlockX = null;
            result.secondBlockY = null;
            result.type = BlockPair.HORIZONTAL;
        }
        else {
            return null;
        }
        return result;
    }

    /**
     * 设置某位玩家已经结束游戏
     *
     * @param player 该玩家
     */
    private void kill(Player player) {
        /// 标记该棋子已死亡
        player.piece.setOpacity(0.3);
        player.alive = false;
        /// 若所有棋子均已死亡，则游戏结束
        boolean gameOver = true;
        int alive = 0;
        for (Player _player : allPlayers) {
            if (_player.alive)
                alive++;
        }
        gameOver = gamingType == GamingType.LONGEST_LENGTH ? alive == 0 : alive <= 1;
        if (gameOver) {
            blockPilePanel.setDisable(true);
            mainPanel.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
            displayResult();
        }
    }

    /**
     * 显示游戏获胜者
     */
    private void displayResult() {
        double winnerLength = 0;
        Player winner = null;
        if (gamingType == GamingType.LONGEST_LENGTH) {
            for (Player player : allPlayers) {
                winnerLength = Math.max(player.totalLength, winnerLength);
            }
            for (Player player : allPlayers) {
                if (player.totalLength == winnerLength) {
                    winner = player;
                    break;
                }
            }
        }
        else {
            winner = allPlayers[(currentPlayerIndex - 1 + playerNumber) % playerNumber];
        }
        assert (winner != null);
        winnerLabel.setTextFill(winner.piece.getFill());
        resultPanel.setVisible(true);
    }

    /**
     * 更新计分板
     */
    private void updateScoreBoard(Player player) {
        /// 更新标签颜色
        playerLabel.setTextFill(pieceColors[player.index]);
        /// 更新总路程显示
        totalLengthLabel.setText(String.format("%.1f cm", player.totalLength / (piecePanel.getWidth() / sideSize) * 5.0));
    }

    /**
     * 创建棋子移动动画
     */
    private Animation createAnimation(Player player, int nextPosition) {
        Point2D start = availablePoints[player.currentPosition];
        Point2D end = availablePoints[nextPosition];
        /// 更新走过的总路程
        player.totalLength += start.distance(end);
        /// 初始化动画
        PathTransition animation = new PathTransition(new Duration(MOVE_DURATION), new Line(start.getX(), start.getY(), end.getX(), end.getY()), player.piece);
        Line trace = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        trace.setFill(player.piece.getFill());
        piecePanel.getChildren().add(trace);
        /// 递归调用该函数，直到移动到终点
        animation.setOnFinished(e -> step(player));
        return animation;
    }

    /**
     * 更新玩家位置信息
     */
    private void updatePlayerPosition(Player player, int globalPosition) {
        if (!player.alive) {
            return;
        }
        BlockPair nearBlockPair = getNearBlockPairByGlobalPosition(globalPosition);
        System.out.println(nearBlockPair);
        assert (nearBlockPair != null);
        if (nearBlockPair.firstBlockX != null && nearBlockPair.firstBlockY != null && visitedBlocks[nearBlockPair.firstBlockY][nearBlockPair.firstBlockX]) {
            if (nearBlockPair.secondBlockX != null && nearBlockPair.secondBlockY != null) {
                player.blockX = nearBlockPair.secondBlockX;
                player.blockY = nearBlockPair.secondBlockY;
            }
        } else {
            if (nearBlockPair.firstBlockX != null && nearBlockPair.firstBlockY != null) {
                player.blockX = nearBlockPair.firstBlockX;
                player.blockY = nearBlockPair.firstBlockY;
            }
        }
        player.currentPosition = globalPosition;
    }

    /**
     * 选择完拼图后，让当前玩家的棋子向前移动至终点。
     *
     * @param player 当前玩家
     */
    private void step(Player player) {

        if (!player.alive) {
            return;
        }

        updateScoreBoard(player);

        if (player.totalLength == 0) {
            player.visitedPositions.put(player.currentPosition, true);
        }
        else if (isPositionOnEdge(player.currentPosition)) {
            kill(player);
            deal();
            return;
        }

        Integer nextPosition = getNextPosition(player);

        if (nextPosition != null) {
            //Boolean visited = player.visitedPositions.get(nextPosition);
            player.visitedPositions.put(nextPosition, true);
            System.out.println("next position: " + nextPosition);
            //if (visited == null || !visited) {
                System.out.println(String.format("Player %d: %d => %d", player.index, player.currentPosition, nextPosition));

                Animation animation = createAnimation(player, nextPosition);
                updatePlayerPosition(player, nextPosition);
                animation.play();
            //}
            //else {
            //    deal();
            //}
        }
        else {
            deal();
        }
    }
}
