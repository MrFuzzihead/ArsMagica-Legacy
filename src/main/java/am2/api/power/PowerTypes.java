package am2.api.power;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import am2.LogHelper;
import am2.api.ArsMagicaApi;

public final class PowerTypes {

    public static final PowerTypes NONE = new PowerTypes(0, "None", "§f");
    public static final PowerTypes LIGHT = new PowerTypes(1, "Light", "§b");
    public static final PowerTypes NEUTRAL = new PowerTypes(2, "Neutral", "§1");
    public static final PowerTypes DARK = new PowerTypes(4, "Dark", "§4");

    private static final ArrayList<PowerTypes> allPowerTypes = new ArrayList<PowerTypes>() {

        {
            add(LIGHT);
            add(NEUTRAL);
            add(DARK);
        }
    };

    private static final Map<Integer, PowerTypes> byId = new ConcurrentHashMap<Integer, PowerTypes>();
    private static volatile PowerTypes[] allCache;

    static {
        for (PowerTypes type : allPowerTypes) byId.put(type.ID(), type);
        // NONE is the fallback for unknown IDs and was never part of all(); keep that contract explicit.
        byId.put(NONE.ID(), NONE);
        allCache = allPowerTypes.toArray(new PowerTypes[allPowerTypes.size()]);
    }

    private int _id;
    private String _name;
    private String _chatColor;

    private PowerTypes(int ID, String name, String chatColor) {
        if ((ID & -ID) != ID) {
            throw new InvalidParameterException(
                String.format("ID must be a bitflag that is a power of 2!  (You used %d)", ID));
        }
        _id = ID;
        _name = name;
        _chatColor = chatColor;
    }

    public static synchronized void RegisterPowerType(int id, String name, String chatColor) {
        if (getByID(id) != NONE) {
            LogHelper.info(
                "Attempted to register power type %s with ID of %d, but that ID is already taken!  The type was NOT registered!",
                name,
                id);
        } else {
            PowerTypes type = new PowerTypes(id, name, chatColor);
            allPowerTypes.add(type);
            byId.put(id, type);
            allCache = allPowerTypes.toArray(new PowerTypes[allPowerTypes.size()]);
            LogHelper.info("Registered new power type %s with ID %d", name, id);
        }
    }

    public int ID() {
        return _id;
    }

    public String name() {
        return _name;
    }

    public String chatColor() {
        if (ArsMagicaApi.instance.getColourblindMode()) {
            return "";
        }
        return _chatColor;
    }

    /**
     * Returns the shared, cached array of registered power types. Callers must not modify it; it is rebuilt when a
     * type is registered.
     */
    public static PowerTypes[] all() {
        PowerTypes[] cache = allCache;
        if (cache == null) {
            cache = allPowerTypes.toArray(new PowerTypes[allPowerTypes.size()]);
            allCache = cache;
        }
        return cache;
    }

    public static PowerTypes getByID(int id) {
        PowerTypes type = byId.get(id);
        return type != null ? type : NONE;
    }
}
