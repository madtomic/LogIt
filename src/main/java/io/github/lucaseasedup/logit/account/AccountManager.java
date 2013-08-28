/*
 * AccountManager.java
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
package io.github.lucaseasedup.logit.account;

import static io.github.lucaseasedup.logit.LogItPlugin.getMessage;
import io.github.lucaseasedup.logit.CancelledState;
import io.github.lucaseasedup.logit.LogItCore.HashingAlgorithm;
import io.github.lucaseasedup.logit.LogItCore.IntegrationType;
import io.github.lucaseasedup.logit.LogItCoreObject;
import io.github.lucaseasedup.logit.ReportedException;
import io.github.lucaseasedup.logit.db.Table;
import io.github.lucaseasedup.logit.hash.HashGenerator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.DatatypeConverter;
import org.bukkit.Bukkit;

public final class AccountManager extends LogItCoreObject
{
    public AccountManager(Table accountTable)
    {
        this.accountTable = accountTable;
    }
    
    public Set<String> getRegisteredUsernames()
    {
        return accountMap.keySet();
    }
    
    public boolean isRegistered(String username)
    {
        return accountMap.containsKey(username);
    }
    
    /**
     * Creates a new account with the given username and password.
     * 
     * <p> The given password will be hashed using the default algorithm.
     * 
     * @param username username.
     * @param password password.
     */
    public CancelledState createAccount(String username, String password)
    {
        if (isRegistered(username))
            throw new RuntimeException("Account already exists.");
        
        HashingAlgorithm algorithm = getCore().getDefaultHashingAlgorithm();
        String salt = HashGenerator.generateSalt(algorithm);
        String hash = getCore().hash(password, salt, algorithm);
        String now = String.valueOf(System.currentTimeMillis() / 1000L);
        
        Map<String, String> properties = new HashMap<>();
        
        properties.put("logit.accounts.username", username.toLowerCase());
        properties.put("logit.accounts.salt", salt);
        properties.put("logit.accounts.password", hash);
        properties.put("logit.accounts.hashing_algorithm", algorithm.encode());
        properties.put("logit.accounts.last_active", now);
        properties.put("logit.accounts.reg_date", now);
        
        AccountEvent evt = new AccountCreateEvent(properties);
        
        Bukkit.getPluginManager().callEvent(evt);
        
        if (evt.isCancelled())
            return CancelledState.CANCELLED;
        
        try
        {
            accountMap.put(username, new Account(accountTable, properties));
            
            log(Level.FINE, getMessage("CREATE_ACCOUNT_SUCCESS_LOG").replace("%player%", username));
        }
        catch (SQLException ex)
        {
            log(Level.WARNING,
                    getMessage("CREATE_ACCOUNT_FAIL_LOG").replace("%player%", username), ex);
            
            ReportedException.throwNew(ex);
        }
        
        return CancelledState.NOT_CANCELLED;
    }
    
    /**
     * Removes an account with the specified username.
     * 
     * <p> Removing an account does not entail logging out.
     * 
     * @param username username.
     * @throws AccountNotFoundException if account does not exist.
     */
    public CancelledState removeAccount(String username)
    {
        if (!isRegistered(username))
            throw new AccountNotFoundException();
        
        Account account = accountMap.get(username);
        AccountEvent evt = new AccountRemoveEvent(account);
        
        Bukkit.getPluginManager().callEvent(evt);
        
        if (evt.isCancelled())
            return CancelledState.CANCELLED;
        
        try
        {
            accountMap.remove(username);
            
            log(Level.FINE, getMessage("REMOVE_ACCOUNT_SUCCESS_LOG").replace("%player%", username));
        }
        catch (SQLException ex)
        {
            log(Level.WARNING,
                    getMessage("REMOVE_ACCOUNT_FAIL_LOG").replace("%player%", username), ex);
            
            ReportedException.throwNew(ex);
        }
        
        return CancelledState.NOT_CANCELLED;
    }
    
    /**
     * Checks if the given password matches that of account with the specified username.
     * 
     * <p> The given password will be hashed using an algorithm specified
     * in the database or in the config.
     * 
     * @param username username.
     * @param password the password to check.
     * @throws AccountNotFoundException if no such account exists.
     * @return {@code true} if they match.
     */
    public boolean checkAccountPassword(String username, String password)
    {
        if (accountTable.isColumnDisabled("logit.accounts.password"))
            return true;
        
        Account account = accountMap.get(username);
        
        if (account == null)
            throw new AccountNotFoundException();
        
        String actualHashedPassword = account.getString("logit.accounts.password");
        String actualSalt           = account.getString("logit.accounts.salt");
        
        HashingAlgorithm algorithm = getCore().getDefaultHashingAlgorithm();
        String userAlgorithm = account.getString("logit.accounts.hashing_algorithm");
        
        if (userAlgorithm != null)
        {
            algorithm = HashingAlgorithm.decode(userAlgorithm);
        }
        
        if (!accountTable.isColumnDisabled("logit.accounts.salt"))
        {
            return getCore().checkPassword(password, actualHashedPassword, actualSalt, algorithm);
        }
        else
        {
            return getCore().checkPassword(password, actualHashedPassword, algorithm);
        }
    }
    
    /**
     * Changes password of an account with the given username.
     * 
     * <p> The given password will be hashed using the default algorithm.
     * 
     * @param username    username.
     * @param newPassword a new password.
     */
    public CancelledState changeAccountPassword(String username, String newPassword)
    {
        if (!isRegistered(username))
            throw new AccountNotFoundException();
        
        Account account = accountMap.get(username);
        AccountEvent evt = new AccountChangePasswordEvent(account, newPassword);
        
        Bukkit.getPluginManager().callEvent(evt);
        
        if (evt.isCancelled())
            return CancelledState.CANCELLED;
        
        HashingAlgorithm algorithm = getCore().getDefaultHashingAlgorithm();
        String newSalt = HashGenerator.generateSalt(algorithm);
        String newHash = getCore().hash(newPassword, newSalt, algorithm);
        
        try
        {
            account.updateString("logit.accounts.salt", newSalt);
            account.updateString("logit.accounts.password", newHash);
            account.updateString("logit.accounts.hashing_algorithm", algorithm.encode());
            
            log(Level.FINE,
                    getMessage("CHANGE_PASSWORD_SUCCESS_LOG").replace("%player%", username));
        }
        catch (SQLException ex)
        {
            log(Level.WARNING,
                    getMessage("CHANGE_PASSWORD_FAIL_LOG").replace("%player%", username), ex);
            
            ReportedException.throwNew(ex);
        }
        
        return CancelledState.NOT_CANCELLED;
    }
    
    /**
     * Changes e-mail address of an account with the given username.
     * 
     * @param username username.
     * @param newEmail a new e-mail address.
     */
    public CancelledState changeEmail(String username, String newEmail)
    {
        if (!isRegistered(username))
            throw new AccountNotFoundException();
        
        Account account = accountMap.get(username);
        AccountEvent evt = new AccountChangeEmailEvent(account, newEmail);
        
        Bukkit.getPluginManager().callEvent(evt);
        
        if (evt.isCancelled())
            return CancelledState.CANCELLED;
        
        try
        {
            account.updateString("logit.accounts.email", newEmail);
            
            log(Level.FINE, getMessage("CHANGE_EMAIL_SUCCESS_LOG").replace("%player%", username));
        }
        catch (SQLException ex)
        {
            log(Level.WARNING,
                    getMessage("CHANGE_EMAIL_FAIL_LOG").replace("%player%", username), ex);
            
            ReportedException.throwNew(ex);
        }
        
        return CancelledState.NOT_CANCELLED;
    }
    
    public String getEmail(String username)
    {
        return accountMap.get(username).getString("logit.accounts.email");
    }
    
    /**
     * Attaches IP address to an account with the specified username.
     * 
     * @param username username.
     * @param ip       an IP address.
     * @throws AccountNotFoundException if the account does not exist.
     */
    public CancelledState attachIp(String username, String ip)
    {
        if (!isRegistered(username))
            throw new AccountNotFoundException();
        
        Account account = accountMap.get(username);
        AccountEvent evt = new AccountAttachIpEvent(account, ip);
        
        Bukkit.getPluginManager().callEvent(evt);
        
        if (evt.isCancelled())
            return CancelledState.CANCELLED;
        
        try
        {
            if (getCore().getIntegration() == IntegrationType.PHPBB2)
            {
                ip = DatatypeConverter.printHexBinary(InetAddress.getByName(ip).getAddress())
                        .toLowerCase();
            }
            
            account.updateString("logit.accounts.ip", ip);
            
            log(Level.FINE, getMessage("ATTACH_IP_SUCCESS_LOG").replace("%player%", username)
                    .replace("%ip%", ip));
        }
        catch (SQLException | UnknownHostException ex)
        {
            log(Level.WARNING, getMessage("ATTACH_IP_FAIL_LOG").replace("%player%", username)
                    .replace("%ip%", ip), ex);
            
            ReportedException.throwNew(ex);
        }
        
        return CancelledState.NOT_CANCELLED;
    }
    
    /**
     * Returns number of accounts with the given IP address.
     * 
     * <p> If {@code ip} is {@code null} or an empty string, the returned value is {@code 0}.
     * 
     * @param ip the IP address.
     * 
     * @return number of accounts with the given IP.
     */
    public int countAccountsWithIp(String ip)
    {
        if (ip == null || ip.isEmpty())
            return 0;
        
        int count = 0;
        
        for (Account account : accountMap.values())
        {
            if (ip.equalsIgnoreCase(account.getString("logit.accounts.ip")))
            {
                count++;
            }
        }
        
        return count;
    }
    
    public int countUniqueIps()
    {
        List<String> ips = new ArrayList<>(accountMap.size());
        
        for (Account account : accountMap.values())
        {
            String accountIp = account.getString("logit.accounts.ip");
            
            if (accountIp != null)
            {
                ips.add(accountIp);
            }
        }
        
        return new HashSet<>(ips).size();
    }
    
    public String getAccountPersistence(String username, String key)
    {
        Account account = accountMap.get(username);
        
        if (account == null)
            return null;
        
        return account.getPersistence(key);
    }
    
    public void updateAccountPersistence(String username, String key, String value)
    {
        Account account = accountMap.get(username);
        
        if (account == null)
            return;
        
        try
        {
            account.updatePersistence(key, value);
        }
        catch (SQLException ex)
        {
            log(Level.WARNING, "Could not update account persistance: " + key + ".", ex);
            
            ReportedException.throwNew(ex);
        }
    }
    
    public Account getAccount(String username)
    {
        return accountMap.get(username);
    }
    
    public Collection<Account> getAllAccounts()
    {
        return accountMap.values();
    }
    
    public int getAccountCount()
    {
        return accountMap.size();
    }
    
    public Table getTable()
    {
        return accountTable;
    }
    
    /**
     * Loads accounts from the database.
     */
    public void loadAccounts()
    {
        Map<String, Account> loadedAccounts = new HashMap<>();
        
        try
        {
            List<Map<String, String>> rs = accountTable.select();
            
            for (Map<String, String> row : rs)
            {
                String username = row.get("logit.accounts.username");
                
                if (username == null || loadedAccounts.containsKey(username))
                    continue;
                
                username = username.toLowerCase();
                
                if (getCore().getIntegration() == IntegrationType.PHPBB2)
                {
                    if (username.equals("anonymous"))
                    {
                        continue;
                    }
                }
                
                Map<String, String> account = new HashMap<>();
                
                for (Entry<String, String> column : row.entrySet())
                {
                    account.put(column.getKey(), column.getValue());
                }
                
                loadedAccounts.put(username, new Account(accountTable, account));
            }
            
            accountMap = new AccountMap(accountTable, loadedAccounts);
            
            log(Level.FINE, getMessage("LOAD_ACCOUNTS_SUCCESS")
                    .replace("%num%", String.valueOf(accountMap.size())));
        }
        catch (SQLException ex)
        {
            log(Level.WARNING, getMessage("LOAD_ACCOUNTS_FAIL"), ex);
            
            ReportedException.throwNew(ex);
        }
    }
    
    private final Table accountTable;
    private AccountMap accountMap = null;
}
