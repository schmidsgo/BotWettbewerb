package de.hsa.games.fatsquirrel.botimpls.JannikBot;

import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.BotControllerFactory;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games.fatsquirrel.core.entities.Entity;
import de.hsa.games.fatsquirrel.core.entities.EntityType;
import de.hsa.games. fatsquirrel . utilities .XY;

import java.util.*;

public class BotControllerFactoryImpl implements BotControllerFactory {
    @Override
    public BotController createMasterBotController () {
        return new BotController () { int counter = 0;
            private int newTargetLock = 0;
            private Entity closestGoodPlant;
            @Override
            public void nextStep(ControllerContext view) {

                LinkedList<XY> path = new LinkedList<>();

                // Find new Target if cool down is 0
                if (newTargetLock <= 0) {
                    closestGoodPlant = getClosestFood(view);
                    newTargetLock = 5;
                }

                // if Target was found get the path to it
                if (closestGoodPlant != null) {
                    Pathfinder pathfinder = new Pathfinder(view, closestGoodPlant.locate());
                    path = pathfinder.findPathToClosestGoodPlant();

                } else {
                    standardMove(view);
                }

                // if no path is available move with the standard move
                // else move to the next Point in the path
                if (path == null || path.isEmpty()) {
                    standardMove(view);
                } else {
                    if (path.size() == 1) {
                        newTargetLock = 0;
                    }
                    pathNodeToMoveCommand(view, path.poll());
                }


                newTargetLock--;
            }

            private void pathNodeToMoveCommand(ControllerContext view, XY xy) {
                view.move(xy);
            }

            private void standardMove(ControllerContext view) {
                //TODO: Sinvolles Standardmovement
                view.move(view.locate().plus(XY.RIGHT));
            }

            private ArrayList<Entity> listAllEntities(ControllerContext view) {
                ArrayList<Entity> entities = new ArrayList<>();
                XY upperLeft = view.getViewUpperLeft();
                XY lowerRight = view.getViewLowerRight();
                try {
                    for (int x = Math.max(upperLeft.x, 0); x <= lowerRight.x; x++) {
                        try {
                            for (int y = Math.max(upperLeft.y, 0); y <= lowerRight.y; y++) {
                                EntityType entity = view.getEntityAt(new XY(x, y));
                                if (entity != EntityType.NONE) {
                                    entities.add(entity);
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException ignore) {

                        }
                    }
                } catch (ArrayIndexOutOfBoundsException ignore) {

                }
                return entities;
            }

            private Entity getClosestGoodPlant(ControllerContext view) {
                ArrayList<Entity> entities = listAllEntities(view);
                HashMap<Entity, Double> results = new HashMap<>();

                for (Entity entity : entities) {
                    if (entity.getEntityType() == EntityType.GOOD_PLANT) {
                        XY distanceVector = entity.getXY().minus(view.locate());
                        double distance = Math.sqrt(Math.pow(distanceVector.x, 2) + Math.pow(distanceVector.y, 2));
                        results.put(entity, distance);
                    }
                }
                if (results.isEmpty()) {
                    return null;
                } else {
                    return Collections.min(results.entrySet(), Map.Entry.comparingByValue()).getKey();
                }
            }

            private Entity getClosestFood(ControllerContext view) {
                ArrayList<Entity> entities = listAllEntities(view);
                HashMap<Entity, Double> results = new HashMap<>();

                for (Entity entity : entities) {
                    if (entity.getEntityType() == EntityType.GOOD_PLANT || entity.getEntityType() == EntityType.GOOD_BEAST) {
                        XY distanceVector = entity.getXY().minus(view.locate());
                        double distance = Math.sqrt(Math.pow(distanceVector.x, 2) + Math.pow(distanceVector.y, 2));
                        results.put(entity, distance);
                    }
                }
                if (results.isEmpty()) {
                    return null;
                } else {
                    return Collections.min(results.entrySet(), Map.Entry.comparingByValue()).getKey();
                }
            }

            private class Pathfinder {
                private final ControllerContext controllerContext;

                private final PriorityQueue<QueueElement> openList = new PriorityQueue<>(new NodeComparator());
                private final ArrayList<QueueElement> closedList = new ArrayList<>();

                private QueueElement start;
                private XY end;

                Pathfinder(ControllerContext controllerContext, XY end) {
                    this.controllerContext = controllerContext;
                    this.start = new QueueElement(controllerContext.locate(), null, 0,
                            controllerContext.locate().distanceFrom(end));
                    this.end = end;
                    openList.add(this.start);
                }

                public LinkedList<XY> findPathToClosestGoodPlant() {
                    while (!openList.isEmpty()) {
                        QueueElement current = openList.poll();
                        try {
                            if (current.xy.equals(this.end)) {
                                LinkedList<XY> path = new LinkedList<>();
                                QueueElement element = current;

                                while (element.hasPrevious()) {
                                    path.addFirst(element.xy);
                                    element = element.previous;
                                }
                                return path;
                            }
                        } catch (NullPointerException ignore) {

                        }

                        XY[] directions = {XY.LEFT, XY.LEFT_UP, XY.UP, XY.RIGHT_UP, XY.RIGHT, XY.RIGHT_DOWN, XY.DOWN, XY.LEFT_DOWN};
                        for (XY direction : directions) {
                            XY neighbour = current.xy.plus(direction);
                            Entity test = controllerContext.getEntityAt(neighbour);
                            if (controllerContext.getEntityAt(neighbour) == null ||
                                    (controllerContext.getEntityAt(neighbour).getEntityType() != EntityType.WALL &&
                                            controllerContext.getEntityAt(neighbour).getEntityType() != EntityType.BAD_BEAST &&
                                            controllerContext.getEntityAt(neighbour).getEntityType() != EntityType.BAD_PLANT)) {

                                if (closedList.stream().noneMatch(o -> o.xy == neighbour)) {
                                    int gScore = current.gScore + 1;
                                    double fScore = gScore + neighbour.distanceFrom(end);
                                    openList.add(new QueueElement(neighbour, current, gScore, fScore));
                                }
                            }

                        }
                        closedList.add(current);
                    }
                    return null;
                }

                class NodeComparator implements Comparator<QueueElement> {
                    /**
                     * Sorts the Queue ascending to F-Score
                     */
                    @Override
                    public int compare(QueueElement n1, QueueElement n2) {
                        return Double.compare(n1.fScore, n2.fScore);
                    }
                }

                private class QueueElement {
                    protected XY xy;
                    protected QueueElement previous;
                    protected int gScore;
                    protected double fScore;

                    QueueElement(XY xy, QueueElement previous, int gScore, double fScore) {
                        this.xy = xy;
                        this.previous = previous;
                        this.fScore = fScore;
                        this.gScore = gScore;
                    }

                    protected boolean hasPrevious() {
                        return !(previous == null);
                    }
                }

            }
        };
    }
    @Override
    public BotController createMiniBotController () {
        return new BotController () {
            @Override
            public void nextStep(ControllerContext context) {
                context.move(XY.randomDirection());
            }
        };
    }
}