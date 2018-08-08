package thut.essentials.commands.names;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.PlayerDataHandler;
import thut.essentials.util.RuleManager;

public class Prefix extends BaseCommand
{

    public Prefix()
    {
        super("prefix", 2);
    }

    /** Return whether the specified command parameter index is a username
     * parameter. */
    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player;
        int start = 1;
        try
        {
            player = getPlayer(server, sender, args[0]);
        }
        catch (Exception e)
        {
            player = getPlayerBySender(sender);
            start = 0;
        }
        String arg = args.length == start ? "" : args[start];
        NBTTagCompound tag = PlayerDataHandler.getCustomDataTag(player);
        NBTTagCompound nametag = tag.getCompoundTag("name");
        if (!nametag.hasKey("original")) nametag.setString("original", player.getDisplayNameString());
        for (int i = start + 1; i < args.length; i++)
        {
            arg = arg + " " + args[i];
        }
        arg = RuleManager.format(arg);
        if (!arg.isEmpty())
        {
            sender.sendMessage(
                    new TextComponentString("Set Prefix for " + player.getDisplayNameString() + " to " + arg));
        }
        else
        {
            sender.sendMessage(new TextComponentString("Reset Set Prefix for " + player.getDisplayNameString()));
        }
        nametag.setString("prefix", arg);
        tag.setTag("name", nametag);
        PlayerDataHandler.saveCustomData(player);
        player.refreshDisplayName();
    }

}
