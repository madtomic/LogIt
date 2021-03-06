package io.github.lucaseasedup.logit.command.hub;

import static io.github.lucaseasedup.logit.message.MessageHelper.sendMsg;
import static io.github.lucaseasedup.logit.message.MessageHelper.t;
import io.github.lucaseasedup.logit.command.CommandAccess;
import io.github.lucaseasedup.logit.command.CommandHelpLine;
import io.github.lucaseasedup.logit.config.Property;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class ConfigListHubCommand extends HubCommand
{
    public ConfigListHubCommand()
    {
        super("config list", new String[] {},
                new CommandAccess.Builder()
                        .permission("logit.config.list")
                        .playerOnly(false)
                        .runningCoreRequired(true)
                        .build(),
                new CommandHelpLine.Builder()
                        .command("logit config list")
                        .descriptionLabel("subCmdDesc.config.list")
                        .optionalParam("page")
                        .build());
    }
    
    @Override
    public void execute(CommandSender sender, String[] args)
    {
        Map<String, Property> properties = getConfig("config.yml").getProperties();
        
        int page = 1;
        int pages = (properties.size() / PROPERTIES_PER_PAGE) + 1;
        int i = 0, j = 0;
        
        if (args.length > 0)
        {
            try
            {
                page = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException ex)
            {
                sendMsg(sender, t("invalidParam")
                        .replace("{0}", "page"));
                
                return;
            }
        }
        
        if (page <= 0)
        {
            page = 1;
        }
        
        sendMsg(sender, "");
        sendMsg(sender, t("config.list.header1")
                .replace("{0}", String.valueOf(page))
                .replace("{1}", String.valueOf(pages)));
        sendMsg(sender, t("config.list.header2"));
        sendMsg(sender, t("config.list.header3"));
        
        for (Map.Entry<String, Property> e : properties.entrySet())
        {
            if ((i > ((PROPERTIES_PER_PAGE * (page - 1)) - 1)) && (j < PROPERTIES_PER_PAGE))
            {
                sendMsg(sender, t("config.list.property")
                        .replace("{0}", e.getValue().getPath())
                        .replace("{1}", e.getValue().toString()));
                
                j++;
            }
            
            i++;
        }
        
        if (page > pages)
        {
            sendMsg(sender, t("config.list.noProperties"));
        }
    }
    
    private static final int PROPERTIES_PER_PAGE = 15;
}
