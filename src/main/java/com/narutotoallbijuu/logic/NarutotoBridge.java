package com.narutotoallbijuu.logic;

import com.narutotoallbijuu.AllBijuuMod;
import com.narutotoallbijuu.bijuu.Bijuu;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Integracao com o Narutoto original por reflexao (sem dependencia de compilacao).
 * Nomes verificados no JAR 0.4.1: classe net.mcreator.narutoto.network.NarutotoModVariables,
 * campo PLAYER_VARIABLES_CAPABILITY; em PlayerVariables os campos double chakra, chakra_maximum,
 * ninja_level, ninja_experience, jinchuriki e flags if_show_wind/fire/water/earth/lightning,
 * alem do metodo syncPlayerVariables(Entity). Qualquer falha desativa a integracao silenciosamente.
 */
public final class NarutotoBridge {
    private static final String VARS = "net.mcreator.narutoto.network.NarutotoModVariables";
    private static boolean initialized;
    private static boolean available;
    private static Capability<?> capability;
    private static Class<?> playerVarsClass;
    private static Method syncMethod;
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private NarutotoBridge() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> vars = Class.forName(VARS);
            Field capField = vars.getField("PLAYER_VARIABLES_CAPABILITY");
            capability = (Capability<?>) capField.get(null);
            playerVarsClass = Class.forName(VARS + "$PlayerVariables");
            syncMethod = playerVarsClass.getMethod("syncPlayerVariables", Entity.class);
            available = capability != null;
        } catch (Throwable t) {
            available = false;
            AllBijuuMod.LOGGER.warn("Integracao com Narutoto indisponivel: {}", t.toString());
        }
    }

    public static boolean isAvailable() {
        init();
        return available;
    }

    private static Object vars(Player p) {
        init();
        if (!available) {
            return null;
        }
        try {
            return p.getCapability(capability).resolve().orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field field(String name) {
        return FIELDS.computeIfAbsent(name, n -> {
            try {
                Field f = playerVarsClass.getField(n);
                f.setAccessible(true);
                return f;
            } catch (Throwable t) {
                return null;
            }
        });
    }

    public static double getDouble(Player p, String name, double def) {
        Object v = vars(p);
        Field f = v == null ? null : field(name);
        if (f == null) {
            return def;
        }
        try {
            return f.getDouble(v);
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean getBool(Player p, String name) {
        Object v = vars(p);
        Field f = v == null ? null : field(name);
        if (f == null) {
            return false;
        }
        try {
            return f.getBoolean(v);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void setDouble(Player p, String name, double value) {
        Object v = vars(p);
        Field f = v == null ? null : field(name);
        if (f == null) {
            return;
        }
        try {
            f.setDouble(v, value);
            syncMethod.invoke(v, p);
        } catch (Throwable t) {
            // integracao opcional
        }
    }

    public static double chakra(Player p) {
        return getDouble(p, "chakra", -1);
    }

    public static double chakraMax(Player p) {
        return getDouble(p, "chakra_maximum", -1);
    }

    public static void addChakra(Player p, double delta) {
        double cur = chakra(p);
        if (cur < 0) {
            return;
        }
        double max = chakraMax(p);
        double next = Math.max(0, cur + delta);
        if (max > 0) {
            next = Math.min(max, next);
        }
        setDouble(p, "chakra", next);
    }

    public static void addNinjaExperience(Player p, double amount) {
        double cur = getDouble(p, "ninja_experience", Double.NaN);
        if (!Double.isNaN(cur)) {
            setDouble(p, "ninja_experience", cur + amount);
        }
    }

    public static boolean isKuramaJinchuuriki(Player p) {
        return getDouble(p, "jinchuriki", 0) > 0;
    }

    public static double ninjaLevel(Player p) {
        return Math.max(0, getDouble(p, "ninja_level", 0));
    }

    public static int elementBonus(Player p, Bijuu b) {
        boolean wind = getBool(p, "if_show_wind");
        boolean fire = getBool(p, "if_show_fire");
        boolean water = getBool(p, "if_show_water");
        boolean earth = getBool(p, "if_show_earth");
        switch (b.element) {
            case WIND:
                return wind ? 25 : 0;
            case FIRE:
                return fire ? 25 : 0;
            case WATER:
                return water ? 25 : 0;
            case LAVA:
                return fire && earth ? 25 : (fire || earth ? 12 : 0);
            case STEAM:
                return fire && water ? 25 : (fire || water ? 12 : 0);
            case ACID:
                return water ? 15 : (earth ? 8 : 0);
            case INSECT:
                return earth ? 15 : 0;
            default:
                return 5;
        }
    }
}
