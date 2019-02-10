// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");
        HashMap<EntityId, Boolean> shipToShipyard = new HashMap<>();
        for (;;) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            for (final Ship ship : me.ships.values()) {

                if(ship.position.equals(me.shipyard.position)){
                    Log.log("removing id: " + ship.id + " from the map");
                    logStatus(ship, shipToShipyard, "RemovingShip From map");
                    shipToShipyard.remove(ship.id);
                    final Direction randomDirection = Direction.ALL_CARDINALS.get(rng.nextInt(4));
                    commandQueue.add(ship.move(randomDirection));
                }
                else if(shipToShipyard.containsKey(ship.id) && shipToShipyard.get(ship.id) ){
                    Log.log("ship should be heading home");
                    logStatus(ship, shipToShipyard, "ShipHeadingHome");
                    Command command = ship.move(gameMap.naiveNavigate(ship, me.shipyard.position));
                    commandQueue.add(command);
                }
                else if(ship.isFull()) {
                    shipToShipyard.put(ship.id, true);
                    logStatus(ship, shipToShipyard, "ShipFull");
                    Log.log("I think the ship is full. Adding id: " + ship.id + " to map");
                    Command command = ship.move(gameMap.naiveNavigate(ship, me.shipyard.position));
                    commandQueue.add(command);
                }
                else if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10 ) {
                    logStatus(ship, shipToShipyard, "Ship Moving Random");
                    final Direction randomDirection = Direction.ALL_CARDINALS.get(rng.nextInt(4));
                    commandQueue.add(ship.move(randomDirection));
                } else {
                    logStatus(ship, shipToShipyard, "Ship Staying");
                    commandQueue.add(ship.stayStill());
                }
            }

            if (
                game.turnNumber <= 200 &&
                me.halite >= Constants.SHIP_COST &&
                !gameMap.at(me.shipyard).isOccupied())
            {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }

    private static void logStatus(Ship ship, HashMap<EntityId, Boolean> shipToShipyard, String shipFull) {
        boolean shipyardStatus = false;
        if(shipToShipyard.containsKey(ship.id)){
            shipyardStatus = shipToShipyard.get(ship.id);
        }
        Log.log("ShipID: " + ship.id + " ShipHalite: " + ship.halite + " shipyardStatus: " + shipyardStatus + " Event: " + shipFull);
    }
}
