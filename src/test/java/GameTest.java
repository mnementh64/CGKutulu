import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameTest {

    @Test
    public void test_round_findClosestExplorer_2_explorers() {
        Player.Round round = new Player.Round();
        round.me = new Player.Me(1, new Player.Position(0, 0), 1, 1, 1);
        round.explorers.add(new Player.Explorer(2, new Player.Position(1, 1), 1, 1, 1));
        round.explorers.add(new Player.Explorer(3, new Player.Position(10, 10), 1, 1, 1));

        Player.Explorer closest = round.findClosestExplorer();

        Assert.assertEquals(2, closest.id);
    }

    @Test
    public void test_round_findClosestExplorer_4_explorers() {
        Player.Round round = new Player.Round();
        round.me = new Player.Me(1, new Player.Position(0, 0), 1, 1, 1);
        round.explorers.add(new Player.Explorer(3, new Player.Position(1, 2), 1, 1, 1));
        round.explorers.add(new Player.Explorer(4, new Player.Position(2, 1), 1, 1, 1));
        round.explorers.add(new Player.Explorer(5, new Player.Position(10, 10), 1, 1, 1));
        round.explorers.add(new Player.Explorer(2, new Player.Position(1, 1), 1, 1, 1));

        Player.Explorer closest = round.findClosestExplorer();

        Assert.assertEquals(2, closest.id);
    }

    @Test
    public void labyrinth_pacman_escapeFrom_onlyOneCell() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-pacman.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();

        // When
        String move = labyrinth.escapeFrom(new Player.Position(1, 1), new Player.Position(4, 1), new ArrayList<>());

        // Then
        Assert.assertEquals("MOVE 1 2", move);
    }

    @Test
    public void labyrinth_pacman_escapeFrom_twoCells() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-pacman.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();

        // When
        String move = labyrinth.escapeFrom(new Player.Position(5, 2), new Player.Position(8, 2), new ArrayList<>());
        System.out.println(move);

        // Then
        Assert.assertTrue(move.equals("MOVE 5 1") || move.equals("MOVE 5 3"));
    }

    @Test
    public void labyrinth_pacman_escapeFrom_centralCase() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-pacman.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();

        // When
        String move = labyrinth.escapeFrom(new Player.Position(6, 5), new Player.Position(7, 6), new ArrayList<>());

        // Then
        Assert.assertEquals("MOVE 5 5", move);
    }

    @Test
    public void labyrinth_pacman_escapeFrom_topCase() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-pacman.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();

        // When
        String move = labyrinth.escapeFrom(new Player.Position(7, 2), new Player.Position(7, 3), new ArrayList<>());

        // Then
        Assert.assertTrue(move.equals("MOVE 6 2") || move.equals("MOVE 8 2"));
    }

    @Test
    public void labyrinth_corridors_escapeFrom_case1() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-corridors.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();

        // When
        String move = labyrinth.escapeFrom(new Player.Position(5, 8), new Player.Position(5, 7), new ArrayList<>());
        System.out.println(move);

        // Then
        Assert.assertTrue(move.equals("MOVE 4 8") || move.equals("MOVE 5 9"));
    }

    @Test
    public void labyrinth_corridors_escapeFrom_case1_2wanderers() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-corridors.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);
        Player.Labyrinth labyrinth = loaderForTest.createLabyrinth();
        List<Player.Wanderer> wanderers = Arrays.asList(
                new Player.Wanderer(1, new Player.Position(5, 7), 1, 1, 1),
                new Player.Wanderer(1, new Player.Position(5, 9), 1, 1, 1)
        );

        // When
        String move = labyrinth.escapeFrom(new Player.Position(5, 8), new Player.Position(5, 7), wanderers);
        System.out.println(move);

        // Then
        Assert.assertEquals("MOVE 4 8", move);
    }

    @Test
    public void game_pacman() throws IOException {
        // Given
        String rawBoard = loadResourceAsString(GameTest.class, "labyrinth-pacman.txt");
        GameBoardLoaderForTest loaderForTest = new GameBoardLoaderForTest(rawBoard);

        // When
        Player.GameLoop gameLoop = new Player.GameLoop(null) {
            @Override
            void loop() {
                createGame();
            }

            @Override
            void createGame() {
                setGame(loaderForTest.createGame());
            }
        };
        gameLoop.loop();

        // Then
        System.out.println("done");
    }

    private static byte[] loadResourceAsBytes(Class theClass, String filePath) throws IOException {
        try (InputStream is = theClass.getResourceAsStream(filePath)) {
            return IOUtils.toByteArray(is);
        }
    }

    private static String loadResourceAsString(Class theClass, String filePath) throws IOException {
        return new String(loadResourceAsBytes(theClass, filePath), Charset.forName("UTF-8"));
    }

    private class GameBoardLoaderForTest {
        private int rowIndex = 0;
        private int colIndex = 0;
        //        private String[][] board;
        private String[] rows;

        GameBoardLoaderForTest(String rawBoard) {
            rows = rawBoard.split("\n");
        }

        Player.Game createGame() {
            Player.Game game = new Player.Game(Player.CURRENT_VERSION);
            game.createFromInputLines(fakeScanner());

            return game;
        }

        public Player.Labyrinth createLabyrinth() {
            return new Player.Labyrinth(fakeScanner());
        }

        private Player.IInput fakeScanner() {
            return new Player.IInput() {
                @Override
                public String next() {
                    return String.valueOf(rows[rowIndex].charAt(colIndex++));
                }

                @Override
                public int nextInt() {
                    // integer must be alone on its line
                    colIndex = 0;
                    return Integer.parseInt(rows[rowIndex++]);
                }

                @Override
                public boolean hasNextLine() {
                    return rowIndex < (rows.length - 1);
                }

                @Override
                public String nextLine() {
                    colIndex = 0;
                    return rows[rowIndex++];
                }
            };
        }

    }
}