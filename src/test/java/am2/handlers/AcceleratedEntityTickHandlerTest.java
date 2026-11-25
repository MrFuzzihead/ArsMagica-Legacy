package am2.handlers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.world.WorldStateManager;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AcceleratedEntityTickHandlerTest {

    @Before
    @After
    public void clearEntities() {
        WorldStateManager.clearAll();
    }

    @Test
    public void startPhaseIsIgnored() {
        new AcceleratedEntityTickHandler().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
    }

    @Test
    public void emptyMapSkipsWorldIteration() {
        new AcceleratedEntityTickHandler().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }
}
