import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Player {

    static boolean DEBUG = false;
    static final int CURRENT_VERSION = 3;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        GameLoop gameLoop = new GameLoop(new IInput() {
            @Override
            public String next() {
                return in.next();
            }

            @Override
            public int nextInt() {
                return in.nextInt();
            }

            @Override
            public boolean hasNextLine() {
                return in.hasNextLine();
            }

            @Override
            public String nextLine() {
                return in.nextLine();
            }
        });
        gameLoop.loop();
    }

    interface IInput {
        String next();

        int nextInt();

        boolean hasNextLine();

        String nextLine();
    }

    static class GameLoop {

        private Game game;
        private int round;
        private IInput in;

        public GameLoop(IInput in) {
            this.in = in;
            game = new Game(CURRENT_VERSION);
            round = 1;
        }

        void loop() {
            // game loop
            while (true) {
                if (round == 1) {
                    createGame();
                }

                game.newRound(in);

                System.out.println(game.computeOutput());

                round++;
            }
        }

        void createGame() {
            game.createFromInputLines(in);
        }

        public void setGame(Game game) {
            this.game = game;
        }

        public Game getGame() {
            return game;
        }
    }

    static class Game {

        private static int GAME_VERSION = 1;

        private Labyrinth labyrinth;
        private GameConfig gameConfig;
        private Round previousRound;
        private Round currentRound;

        Game(int gameVersion) {
            GAME_VERSION = gameVersion;
        }

        void createFromInputLines(IInput in) {
            labyrinth = new Labyrinth(in);
            gameConfig = new GameConfig(in);
        }

        void newRound(IInput in) {
            previousRound = currentRound;
            currentRound = new Round(in);
        }

        String computeOutput() {
            // strategy :
            // - if (wanderer within 3) escape from closest wanderer
            // - else go to closest explorer
            String decisionToEscapeWanderer = escapeWanderer(currentRound, 1);
            if (decisionToEscapeWanderer == null) {
                String goToExplorer = goToClosestExplorer(currentRound);
                if (goToExplorer != null) {
                    return goToExplorer;
                }
            } else {
                return decisionToEscapeWanderer;
            }

            return "WAIT";
        }

        private String escapeWanderer(Round round, int distanceWhenTooClose) {
            Wanderer wanderer = round.findClosestWandererWithin(distanceWhenTooClose);
            if (wanderer != null) {
                String move = labyrinth.escapeFrom(round.me.position, wanderer.position, round.wanderers);
                System.err.println("Escape from wanderer " + wanderer.id + " (" + wanderer.position.r + "/" + wanderer.position.c + ") with " + move);
                return move;
            }
            return null; // no wanderer within the critical distance
        }

        private String goToClosestExplorer(Round round) {
            Explorer explorer = round.findClosestExplorer();
            if (explorer != null) {
                return "MOVE " + explorer.position.c + " " + explorer.position.r;
            }

            return null;
        }
    }

    static class Labyrinth {
        private static int CELL_EMPTY = 0;
        private static int CELL_WALL = 1;
        private static int CELL_PORTAL = 2;

        private int width;
        private int height;
        private int[][] board;

        Labyrinth(IInput in) {
            width = in.nextInt();
            height = in.nextInt();
            board = new int[width][height];
            System.err.println("Board w=" + width + "/h=" + height);

            if (in.hasNextLine()) {
                String line = in.nextLine();
                System.err.println("Skip line : " + line);
            }
            for (int i = 0; i < height; i++) {
                addBoardLine(i, in.nextLine());
            }
        }

        private void addBoardLine(int rowIndex, String rawLine) {
            System.err.println(rawLine);
            char[] chars = rawLine.toCharArray();
            for (int col = 0; col < chars.length; col++) {
                board[col][rowIndex] = resolveCell(chars[col]);
            }
        }

        private int resolveCell(char cellAsAChar) {
            return cellAsAChar == '.' ? CELL_EMPTY :
                    cellAsAChar == '#' ? CELL_WALL :
                            CELL_PORTAL;
        }

        public String escapeFrom(Position myPos, Position opponentPos, List<Wanderer> wanderers) {
            // keep the available position that is the farest from the opponent
            List<CellDistance> cellDistances = new ArrayList<>();

            // above
            if (myPos.r > 0 && !isWall(myPos.r - 1, myPos.c) && noWandererAtPos(myPos.r - 1, myPos.c, wanderers)) {
                cellDistances.add(new CellDistance("A", new Position(myPos.c, myPos.r - 1).distanceTo(opponentPos)));
            }

            // below
            if (myPos.r < height - 1 && !isWall(myPos.r + 1, myPos.c) && noWandererAtPos(myPos.r + 1, myPos.c, wanderers)) {
                cellDistances.add(new CellDistance("B", new Position(myPos.c, myPos.r + 1).distanceTo(opponentPos)));
            }

            // left
            if (myPos.c > 0 && !isWall(myPos.r, myPos.c - 1) && noWandererAtPos(myPos.r, myPos.c - 1, wanderers)) {
                cellDistances.add(new CellDistance("L", new Position(myPos.c - 1, myPos.r).distanceTo(opponentPos)));
            }

            // right
            if (myPos.c < width - 1 && !isWall(myPos.r, myPos.c + 1) && noWandererAtPos(myPos.r, myPos.c + 1, wanderers)) {
                cellDistances.add(new CellDistance("R", new Position(myPos.c + 1, myPos.r).distanceTo(opponentPos)));
            }

            // sort by decreasing distances
            cellDistances.sort((c1, c2) -> c1.distance < c2.distance ? 1 : -1);

            if (!cellDistances.isEmpty()) {
                String target = cellDistances.get(0).which;
                int r = target.equals("A") ? myPos.r - 1 :
                        target.equals("B") ? myPos.r + 1 :
                                myPos.r;
                int c = target.equals("L") ? myPos.c - 1 :
                        target.equals("R") ? myPos.c + 1 :
                                myPos.c;
                return "MOVE " + c + " " + r;
            }

            return "WAIT";
        }

        private boolean noWandererAtPos(int r, int c, List<Wanderer> wanderers) {
            return wanderers.stream().noneMatch(w -> w.position.r == r && w.position.c == c);
        }

        private boolean isWall(int r, int c) {
            return board[c][r] == CELL_WALL;
        }

        private static class CellDistance {
            String which;
            int distance;

            public CellDistance(String which, int distance) {
                this.which = which;
                this.distance = distance;
            }
        }
    }

    static class Round {
        Me me;
        List<Explorer> explorers = new ArrayList<>();
        List<Wanderer> wanderers = new ArrayList<>();

        Round() {
        }

        Round(IInput in) {
            int entityCount = in.nextInt(); // the first given entity corresponds to your explorer
            for (int i = 0; i < entityCount; i++) {
                String entityType = in.next();
                int id = in.nextInt();
                int x = in.nextInt();
                int y = in.nextInt();
                int param0 = in.nextInt();
                int param1 = in.nextInt();
                int param2 = in.nextInt();

                if (i == 0) {
                    me = new Me(id, new Position(x, y), param0, param1, param2);
                } else if (entityType.equals("EXPLORER")) {
                    Explorer explorer = new Explorer(id, new Position(x, y), param0, param1, param2);
                    explorers.add(explorer);
                } else {
                    Wanderer wanderer = new Wanderer(id, new Position(x, y), param0, param1, param2);
                    wanderers.add(wanderer);
                }
            }
        }

        public Explorer findClosestExplorer() {
            return explorers.stream()
                    .sorted((e1, e2) -> {
                        int d1 = e1.position.distanceTo(me.position);
                        int d2 = e2.position.distanceTo(me.position);
                        return d1 < d2 ? -1 : 1;
                    })
                    .findFirst()
                    .orElse(null);
        }

        public Wanderer findClosestWandererWithin(int distance) {
            return wanderers.stream()
                    .filter(w -> me.position.distanceTo(w.position) <= distance)
                    .sorted((w1, w2) -> {
                        int d1 = w1.position.distanceTo(me.position);
                        int d2 = w2.position.distanceTo(me.position);
                        return d1 < d2 ? -1 : 1;
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    static class GameConfig {

        GameConfig(IInput in) {
            int sanityLossLonely = in.nextInt(); // how much sanity you lose every turn when alone, always 3 until wood 1
            int sanityLossGroup = in.nextInt(); // how much sanity you lose every turn when near another player, always 1 until wood 1
            int wandererSpawnTime = in.nextInt(); // how many turns the wanderer take to spawn, always 3 until wood 1
            int wandererLifeTime = in.nextInt(); // how many turns the wanderer is on map after spawning, always 40 until wood 1
        }
    }

    static class Position {
        int c;
        int r;

        Position(int c, int r) {
            this.c = c;
            this.r = r;
        }

        int distanceTo(Position other) {
            return Math.abs(other.r - this.r) + Math.abs(other.c - this.c);
        }
    }

    static abstract class Entity {
        int id;
        Position position;

        Entity(int id, Position position) {
            this.id = id;
            this.position = position;
        }
    }

    static class Explorer extends Entity {
        int mentalHealth;
        int notUsed1;
        int notUsed2;

        Explorer(int id, Position position, int mentalHealth, int notUsed1, int notUsed2) {
            super(id, position);
            this.mentalHealth = mentalHealth;
            this.notUsed1 = notUsed1;
            this.notUsed2 = notUsed2;
        }

        public boolean isMe() {
            return false;
        }
    }

    static class Me extends Explorer {

        Me(int id, Position position, int mentalHealth, int notUsed1, int notUsed2) {
            super(id, position, mentalHealth, notUsed1, notUsed2);
        }

        public boolean isMe() {
            return true;
        }
    }

    static class Wanderer extends Entity {

        public static final int SPAWNING = 0;
        public static final int WANDERING = 1;

        int timeBeforeInvocation;
        int actualState;
        int targetExplorerId;

        Wanderer(int id, Position position, int timeBeforeInvocation, int actualState, int targetExplorerId) {
            super(id, position);
            this.timeBeforeInvocation = timeBeforeInvocation;
            this.actualState = actualState;
            this.targetExplorerId = targetExplorerId;
        }
    }
}