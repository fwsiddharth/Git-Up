package com.gitup.app.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gitup.app.data.model.Account
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AccountManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gitup_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val gson = Gson()
    
    fun saveAccount(account: Account) {
        // Trim the token to remove any whitespace or newlines
        val cleanedAccount = account.copy(token = account.token.trim())
        
        val accounts = getAccounts().toMutableList()
        
        // Remove existing account with same id
        accounts.removeAll { it.id == cleanedAccount.id }
        
        // If this is the active account, deactivate others
        val updatedAccounts = if (cleanedAccount.isActive) {
            accounts.map { it.copy(isActive = false) }.toMutableList()
        } else {
            accounts
        }
        
        updatedAccounts.add(cleanedAccount)
        
        val json = gson.toJson(updatedAccounts)
        sharedPreferences.edit().putString("accounts", json).apply()
    }
    
    fun getAccounts(): List<Account> {
        val json = sharedPreferences.getString("accounts", null) ?: return emptyList()
        val type = object : TypeToken<List<Account>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun getActiveAccount(): Account? {
        return getAccounts().firstOrNull { it.isActive }
    }
    
    fun setActiveAccount(accountId: String) {
        val accounts = getAccounts().map {
            it.copy(isActive = it.id == accountId)
        }
        val json = gson.toJson(accounts)
        sharedPreferences.edit().putString("accounts", json).apply()
    }
    
    fun removeAccount(accountId: String) {
        val accounts = getAccounts().filter { it.id != accountId }
        val json = gson.toJson(accounts)
        sharedPreferences.edit().putString("accounts", json).apply()
    }
    
    fun hasAccounts(): Boolean {
        return getAccounts().isNotEmpty()
    }
}
