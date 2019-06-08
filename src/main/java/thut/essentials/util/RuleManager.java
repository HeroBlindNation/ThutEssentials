package thut.essentials.util;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import thut.essentials.ThutEssentials;

public class RuleManager
{
    final static Field                       rulesField;
    final static Map<String, TextFormatting> charCodeMap = Maps.newHashMap();

    static
    {
        Field temp = null;
        try
        {
            temp = ConfigManager.class.getDeclaredField("rules");
        }
        catch (SecurityException | NoSuchFieldException e)
        {
            e.printStackTrace();
        }
        rulesField = temp;
        try
        {
            temp = ReflectionHelper.findField(TextFormatting.class, "formattingCode", "field_96329_z", "z");
            temp.setAccessible(true);
            for (TextFormatting format : TextFormatting.values())
            {
                try
                {
                    char code = temp.getChar(format);
                    charCodeMap.put(code + "", format);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String format(String rule)
    {
        boolean done = false;
        int index = 0;
        String arg = "\\n";
        rule = rule.replace(arg, System.getProperty("line.separator"));
        index = rule.indexOf('&', index);
        while (!done && index < rule.length() && index >= 0)
        {
            try
            {
                done = !rule.contains("&");
                index = rule.indexOf('&', index);
                if (index < rule.length() - 1 && index >= 0)
                {
                    if (index > 0 && rule.substring(index - 1, index).equals("\\"))
                    {
                        index++;
                        continue;
                    }
                    String toReplace = rule.substring(index, index + 2);
                    String num = toReplace.replace("&", "");
                    TextFormatting format = charCodeMap.get(num);
                    if (format != null) rule = rule.replaceAll(toReplace, format + "");
                    else index++;
                }
                else
                {
                    done = true;
                }
            }
            catch (Exception e)
            {
                done = true;
                e.printStackTrace();
            }
        }
        return rule;
    }

    public static void addRule(ICommandSource sender, String rule) throws Exception
    {
        List<String> rulesList = getRules();
        rule = format(rule);
        rulesList.add(rule);
        sender.sendMessage(new StringTextComponent("Added rule: " + rule));
        ConfigManager.INSTANCE.updateField(rulesField, rulesList.toArray(new String[0]));
    }

    public static void delRule(ICommandSource sender, int rule) throws Exception
    {
        List<String> rulesList = getRules();
        String r = rulesList.remove(rule);
        sender.sendMessage(new StringTextComponent("Removed rule: " + r));
        ConfigManager.INSTANCE.updateField(rulesField, rulesList.toArray(new String[0]));
    }

    public static List<String> getRules()
    {
        return Lists.newArrayList(ThutEssentials.instance.config.rules);
    }
}
