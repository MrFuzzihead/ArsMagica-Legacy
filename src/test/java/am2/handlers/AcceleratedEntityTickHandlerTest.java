package am2.handlers;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMEventHandler;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AcceleratedEntityTickHandlerTest {

    @Before
    @After
    public void clearEntities() {
        AMEventHandler.acceleratedEntitiesUUIDs.clear();
    }

    @Test
    public void startPhaseIsIgnored() {
        AMEventHandler.acceleratedEntitiesUUIDs.put(UUID.randomUUID(), 2);

        new AcceleratedEntityTickHandler().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
    }

    @Test
    public void emptyMapSkipsWorldIteration() {
        new AcceleratedEntityTickHandler().onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }
}
