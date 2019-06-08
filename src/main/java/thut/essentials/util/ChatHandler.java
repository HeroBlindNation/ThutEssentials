package thut.essentials.util;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.PlayerContext;
import thut.essentials.events.NameEvent;
import thut.essentials.land.LandManager;
import thut.essentials.land.LandManager.LandTeam;
import thut.essentials.land.LandManager.PlayerRank;

public class ChatHandler
{
    public static final String CANFORMAT = "thutessentials.chat.format";

    public ChatHandler()
    {
        PermissionAPI.registerNode(CANFORMAT, DefaultPermissionLevel.OP,
                "Will messages sent by the player using & codes be converted to text formats, use \\& to bypass. This requires forceChatFormat true in configs.");
    }

    @SubscribeEvent
    public void chatEvent(ServerChatEvent event)
    {
        if (!ConfigManager.INSTANCE.forceChatFormat) return;
        String format = ConfigManager.INSTANCE.chatFormat;
        String userName = event.getPlayer().getDisplayNameString();

        Iterator<ITextComponent> iter = event.getComponent().iterator();
        ITextComponent name = null;
        boolean found = false;
        List<String> text = Lists.newArrayList();
        List<Style> styles = Lists.newArrayList();
        while (iter.hasNext())
        {
            ITextComponent temp = iter.next();
            text.add(temp.getUnformattedComponentText());
            styles.add(temp.getStyle());
            if (temp.getUnformattedText().equals(userName))
            {
                Style style = temp.getStyle();
                name = new StringTextComponent(userName);
                name.setStyle(style);
                found = true;
            }
        }
        IPermissionHandler manager = PermissionAPI.getPermissionHandler();
        boolean canFormat = manager.hasPermission(event.getPlayer().getGameProfile(), CANFORMAT,
                new PlayerContext(event.getPlayer()));
        // If we find the component for the name, re-build chat based on that.
        if (found && text.size() > 3)
        {
            ITextComponent message = new StringTextComponent("");
            // Here we assume that the username and formatting are first 3
            // values of the formatted username section.
            for (int i = 4; i < text.size(); i++)
            {
                String segment = text.get(i);
                if (manager.hasPermission(event.getPlayer().getGameProfile(), CANFORMAT,
                        new PlayerContext(event.getPlayer())))
                {
                    segment = RuleManager.format(segment);
                }
                StringTextComponent part = new StringTextComponent(segment);
                part.setStyle(styles.get(i));
                message.appendSibling(part);
            }
            String[] vars = format.split("\\[name\\]");
            if (vars.length == 2)
            {
                if (canFormat)
                {
                    vars[0] = RuleManager.format(vars[0]);
                }
                ITextComponent toSend = new StringTextComponent(vars[0]);
                toSend.appendSibling(name);
                vars = vars[1].split("\\[message\\]");
                if (canFormat) for (int i = 0; i < vars.length; i++)
                {
                    vars[i] = RuleManager.format(vars[i]);
                }
                ITextComponent newMessage = new StringTextComponent(vars[0]);
                newMessage.appendSibling(message);
                if (vars.length == 2) newMessage.appendSibling(new StringTextComponent(vars[1]));
                toSend.appendSibling(newMessage);
                event.setComponent(toSend);
                return;
            }

        }
        // Otherwise just make a simple compnent for this.
        String message = format.replace("[name]", TextFormatting.RESET + userName + TextFormatting.RESET);
        String mess = event.getMessage();
        if (canFormat)
        {
            mess = RuleManager.format(mess);
        }
        message = message.replace("[message]", mess);
        event.setComponent(new StringTextComponent(message));

    }

    @SubscribeEvent
    public void getDisplayNameEvent(PlayerEvent.NameFormat event)
    {
        String displayName = event.getDisplayname();
        CompoundNBT tag = PlayerDataHandler.getCustomDataTag(event.getPlayerEntity());
        CompoundNBT nametag = tag.getCompound("name");
        if (nametag.hasKey("name") && ConfigManager.INSTANCE.name)
        {
            displayName = nametag.getString("name");
        }
        if (nametag.hasKey("prefix") && ConfigManager.INSTANCE.prefix)
        {
            if (!nametag.getString("prefix").trim().isEmpty())
                displayName = nametag.getString("prefix") + " " + displayName;
        }
        if (nametag.hasKey("suffix") && ConfigManager.INSTANCE.suffix)
        {
            if (!nametag.getString("suffix").trim().isEmpty())
                displayName = displayName + " " + nametag.getString("suffix");
        }
        if (ConfigManager.INSTANCE.landEnabled)
        {
            LandTeam team = LandManager.getTeam(event.getEntity());
            PlayerRank rank = team._ranksMembers.get(event.getEntity().getUniqueID());
            if (rank != null)
            {
                String rankPrefix = rank.prefix;
                if (rankPrefix != null)
                {
                    displayName = rankPrefix + TextFormatting.RESET + " " + displayName;
                }
            }
            if (!team.prefix.isEmpty()) displayName = team.prefix + TextFormatting.RESET + " " + displayName;
        }
        NameEvent event1 = new NameEvent(event.getPlayerEntity(), displayName);
        MinecraftForge.EVENT_BUS.post(event1);
        event.setDisplayname(event1.getName());
    }
}
