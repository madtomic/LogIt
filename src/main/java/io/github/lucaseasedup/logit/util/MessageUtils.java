/*
 * MessageUtils.java
 *
 * Copyright (C) 2012-2013 LucasEasedUp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.lucaseasedup.logit.util;

import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import static io.github.lucaseasedup.logit.util.PlayerUtils.getPlayer;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MessageUtils
{
    private MessageUtils()
    {
    }
    
    /**
     * Sends the given message to a player.
     * 
     * @param username Player's username.
     * @param message Message.
     */
    public static void sendMessage(String username, String message)
    {
        Player player = getPlayer(username);
        
        if (player != null)
        {
            player.sendMessage(message);
        }
    }
    
    /**
     * Sends the given message to all online players.
     * 
     * @param message Message.
     */
    public static void broadcastMessage(String message)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            p.sendMessage(message);
        }
    }
    
    /**
     * Sends the given message to all online players, except for a player specified as a first parameter.
     * 
     * @param message Message.
     * @param exceptPlayers Players to be omitted in broadcasting.
     */
    public static void broadcastMessageExcept(String message, List<Player> exceptPlayers)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (!exceptPlayers.contains(p))
            {
                p.sendMessage(message);
            }
        }
    }
    
    /**
     * Broadcasts a join message.
     * 
     * @param player Player who joined.
     * @param revealSpawnWorld Whether the spawn-world should be shown along with the join message.
     */
    public static void broadcastJoinMessage(Player player, boolean revealSpawnWorld)
    {
        String joinMessage = JoinMessageGenerator.generate(player, revealSpawnWorld);
        
        broadcastMessageExcept(joinMessage, Arrays.asList(player));
    }
    
    /**
     * Broadcasts a quit message.
     * 
     * @param player Player who quit.
     */
    public static void broadcastQuitMessage(Player player)
    {
        String quitMessage = getMessage("QUIT")
                .replace("%player%", player.getName());
        
        broadcastMessageExcept(quitMessage, Arrays.asList(player));
    }
}
