package thut.essentials.util;

import java.lang.reflect.Field;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ServerPauser
{
    public static final Field f          = ReflectionHelper.findField(MinecraftServer.class, "currentTime",
            "field_175591_ab", "ab");
    public static boolean     paused     = false;
    public static int         pauseTimer = 10;

    @SubscribeEvent
    public static void clientContact(ServerConnectionFromClientEvent event)
    {
        paused = false;
        pauseTimer = 100;
    }

    @SubscribeEvent
    public static void tickEnd(TickEvent.ServerTickEvent event)
    {
        if (event.phase == Phase.START && event.side == Dist.DEDICATED_SERVER)
        {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            paused = pauseTimer-- < 0 && server.isDedicatedServer() && server.getPlayerList().getPlayers().isEmpty();
            if (paused)
            {
                try
                {
                    server.sendMessage(new StringTextComponent(TextFormatting.DARK_GREEN + "zzzZZZzzz"));
                    Thread.sleep(ConfigManager.INSTANCE.pauseTime);
                    f.set(server, ((long) f.get(server)) + ConfigManager.INSTANCE.pauseTime);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
