import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;
    private static final int UNIT_SIZE = 10;
    private static final int DELAY = 100;

    private final ArrayList<Point> snake;
    private Point fruit;
    private boolean isGameOver;
    private boolean isGameStarted = false;
    private int score = 0;
    private int highScore = 0;

    private final Timer timer;
    private final JButton restartButton;
    private final JPanel scorePanel;
    private final JLabel scoreLabel;
    private final JLabel highScoreLabel;
    private JLabel directionLabel;

    private int direction;

    private Clip backgroundMusicClip;
    private Clip eatFruitClip;
    private Clip gameOverClip;

    private boolean isBackgroundMusicPlaying = false;
    private boolean isWaitingForDirection = false;

    public SnakeGame() {
        snake = new ArrayList<>();
        snake.add(new Point(WIDTH / 2, HEIGHT / 2));
        generateFruit();

        isGameOver = false;

        timer = new Timer(DELAY, this);

        restartButton = new JButton("Restart");
        restartButton.addActionListener(e -> restartGame());

        scorePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        scoreLabel = new JLabel("Score: " + score);
        highScoreLabel = new JLabel("High Score: " + highScore);
        directionLabel = new JLabel("Aperte para uma direção");

        scorePanel.add(scoreLabel);
        scorePanel.add(highScoreLabel);
        scorePanel.add(directionLabel);

        setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };
        gamePanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(restartButton);

        JPanel topPanel = new JPanel();
        topPanel.add(scorePanel);

        add(topPanel, BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isGameStarted) {
                    isGameStarted = true;
                    timer.start();
                    playBackgroundMusic();
                    directionLabel.setVisible(false);
                }
                int key = e.getKeyCode();
                if ((key == KeyEvent.VK_LEFT) && (direction != 1)) direction = 3;
                if ((key == KeyEvent.VK_RIGHT) && (direction != 3)) direction = 1;
                if ((key == KeyEvent.VK_UP) && (direction != 2)) direction = 0;
                if ((key == KeyEvent.VK_DOWN) && (direction != 0)) direction = 2;
            }
        });

        setFocusable(true);

        loadAudio();
    }

    private void loadAudio() {
        try {
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(AudioSystem.getAudioInputStream(new File("audio/background_music.wav")));

            eatFruitClip = AudioSystem.getClip();
            eatFruitClip.open(AudioSystem.getAudioInputStream(new File("audio/eat_fruit.wav")));

            gameOverClip = AudioSystem.getClip();
            gameOverClip.open(AudioSystem.getAudioInputStream(new File("audio/game_over.wav")));
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private void playBackgroundMusic() {
        if (backgroundMusicClip != null && !isBackgroundMusicPlaying) {
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            isBackgroundMusicPlaying = true;
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusicClip != null && isBackgroundMusicPlaying) {
            backgroundMusicClip.stop();
            isBackgroundMusicPlaying = false;
        }
    }

    private void playEatFruitSound() {
        if (eatFruitClip != null) {
            eatFruitClip.setFramePosition(0);
            FloatControl gainControl = (FloatControl) eatFruitClip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(5.0f);
            eatFruitClip.start();
        }
    }

    private void playGameOverSound() {
        if (gameOverClip != null) {
            gameOverClip.setFramePosition(0);
            gameOverClip.start();
        }
        stopBackgroundMusic();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGameOver) {
            move();
            checkCollision();
            checkFruit();
            repaint();
        }
    }

    private void generateFruit() {
        Random rand = new Random();
        int x = rand.nextInt((WIDTH / UNIT_SIZE)) * UNIT_SIZE;
        int y = rand.nextInt((HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
        fruit = new Point(x, y);
    }

    private void move() {
        Point head = snake.get(0);
        Point newHead = (Point) head.clone();
        switch (direction) {
            case 0:
                newHead.translate(0, -UNIT_SIZE);
                break;
            case 1:
                newHead.translate(UNIT_SIZE, 0);
                break;
            case 2:
                newHead.translate(0, UNIT_SIZE);
                break;
            case 3:
                newHead.translate(-UNIT_SIZE, 0);
                break;
        }
        snake.add(0, newHead);
        snake.remove(snake.size() - 1);
    }

    private void checkCollision() {
        Point head = snake.get(0);
        if (head.equals(fruit)) {
            snake.add(new Point(-1, -1));
            generateFruit();
            score++;
            if (score > highScore) {
                highScore = score;
                highScoreLabel.setText("High Score: " + highScore);
            }
            scoreLabel.setText("Score: " + score);

            playEatFruitSound();
        } else if (head.x < 0 || head.x >= WIDTH || head.y < 0 || head.y >= HEIGHT) {
            isGameOver = true;
            timer.stop();

            playGameOverSound();
        } else if (snake.subList(1, snake.size()).contains(head)) {
            isGameOver = true;
            timer.stop();

            playGameOverSound();
        }
    }

    private void checkFruit() {
        Point head = snake.get(0);
        if (head.equals(fruit)) {
            snake.add(new Point(-1, -1));
            generateFruit();
        }
    }

    private void restartGame() {
        snake.clear();
        snake.add(new Point(WIDTH / 2, HEIGHT / 2));
        generateFruit();
        isGameOver = false;
        score = 0;
        scoreLabel.setText("Score: " + score);
        direction = -1;
        isWaitingForDirection = true;
        timer.stop();

        directionLabel.setVisible(true);

        if (!isBackgroundMusicPlaying) {
            playBackgroundMusic();
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isWaitingForDirection) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_LEFT && direction != 1) {
                        direction = 3;
                        isWaitingForDirection = false;
                        timer.start();
                        playBackgroundMusic();
                        directionLabel.setVisible(false);
                    } else if (key == KeyEvent.VK_RIGHT && direction != 3) {
                        direction = 1;
                        isWaitingForDirection = false;
                        timer.start();
                        playBackgroundMusic();
                        directionLabel.setVisible(false);
                    } else if (key == KeyEvent.VK_UP && direction != 2) {
                        direction = 0;
                        isWaitingForDirection = false;
                        timer.start();
                        playBackgroundMusic();
                        directionLabel.setVisible(false);
                    } else if (key == KeyEvent.VK_DOWN && direction != 0) {
                        direction = 2;
                        isWaitingForDirection = false;
                        timer.start();
                        playBackgroundMusic();
                        directionLabel.setVisible(false); 
                    }
                }
            }
        });
        requestFocus();
    }

    private void drawGame(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.RED);
        g.fillRect(fruit.x, fruit.y, UNIT_SIZE, UNIT_SIZE);

        g.setColor(Color.GREEN);
        for (int i = 0; i < snake.size(); i++) {
            Point point = snake.get(i);
            if (i == 0) {
                g.setColor(Color.ORANGE);
            }
            if (point.equals(new Point(-1, -1)) && i != 0) {
                continue;
            }
            g.fillRect(point.x, point.y, UNIT_SIZE, UNIT_SIZE);
            g.setColor(Color.GREEN);
        }

        if (isGameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Game");
            SnakeGame snakeGame = new SnakeGame();
            frame.add(snakeGame);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            snakeGame.playBackgroundMusic();
        });
    }
}
