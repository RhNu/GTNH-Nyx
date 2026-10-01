package rhynia.nyx.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import static rhynia.nyx.MainKt.MOD_ID;
import static rhynia.nyx.MainKt.MOD_NAME;
import static rhynia.nyx.config.Definition.CATEGORY_MACHINE;

@Config(modid = MOD_ID, configSubDirectory = MOD_NAME, category = CATEGORY_MACHINE, filename = CATEGORY_MACHINE)
@Config.RequiresMcRestart
public class ConfigMachine {
    @Config.Comment("Offset for MTE IDs, using to solve conflicts with other custom mods, preserves OFFSET+1..OFFSET+50 range.")
    @Config.DefaultInt(17800)
    public static int MTE_ID_OFFSET;

    @Config.Comment("MTE Copier enable.")
    @Config.DefaultBoolean(true)
    public static boolean MTE_COPIER;

    @Config.Comment("MTE Copier tick rate, default is 100, lower value means faster.")
    @Config.DefaultInt(100)
    public static int MTE_COPIER_TICK;

    @Config.Comment("MTE Proxy enable.")
    @Config.DefaultBoolean(true)
    public static boolean MTE_PROXY;

    @Config.Comment("MTE Converter enable.")
    @Config.DefaultBoolean(true)
    public static boolean MTE_CONVERTER;

    @Config.Comment("MTE Injector enable.")
    @Config.DefaultBoolean(true)
    public static boolean MTE_INJECTOR;
}
