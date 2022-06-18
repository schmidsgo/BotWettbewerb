package de.hsa.games.fatsquirrel.botimpls.JannikBot;

import de.hsa.games.fatsquirrel.core.bot.BotController;
import de.hsa.games.fatsquirrel.core.bot.BotControllerFactory;
import de.hsa.games.fatsquirrel.core.bot.ControllerContext;
import de.hsa.games. fatsquirrel . utilities .XY;
public class BotControllerFactoryImpl implements BotControllerFactory {
    @Override
    public BotController createMasterBotController () {
        return new BotController () { int counter = 0;
            @Override
            public void nextStep(ControllerContext context) {
                if (counter++%25==0)
                    context.spawnMiniBot(XY.randomDirection (), 100);
                else
                    context .move(XY.randomDirection());
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