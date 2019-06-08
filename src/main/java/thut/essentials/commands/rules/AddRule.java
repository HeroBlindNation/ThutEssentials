package thut.essentials.commands.rules;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import thut.essentials.util.BaseCommand;
import thut.essentials.util.RuleManager;

public class AddRule extends BaseCommand
{

    public AddRule()
    {
        super("addrule", 2);
    }

    @Override
    public String getUsage(ICommandSource sender)
    {
        return "/" + getName() + " <rule>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSource sender, String[] args) throws CommandException
    {
        String rule = args[0];
        for (int i = 1; i < args.length; i++)
        {
            rule = rule + " " + args[i];
        }
        try
        {
            RuleManager.addRule(sender, rule);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
