package com.zimbra.cs.offline.common;

import com.zimbra.common.util.Constants;


public interface OfflineConstants {
	
	public static enum SyncStatus {
		unknown, offline, online, running, authfail, error
	}	
	
    public static final String A_offlineAccountsOrder = "offlineAccountsOrder";
    
    public static final String A_offlineRemoteServerVersion = "offlineRemoteServerVersion";
    public static final String A_offlineRemotePassword = "offlineRemotePassword";
    public static final String A_offlineRemoteServerUri = "offlineRemoteServerUri";
    
    public static final String A_offlineProxyHost = "offlineProxyHost";
    public static final String A_offlineProxyPort = "offlineProxyPort";
    public static final String A_offlineProxyUser = "offlineProxyUser";
    public static final String A_offlineProxyPass = "offlineProxyPass";

    public static final String A_offlineDataSourceType = "offlineDataSourceType";
    public static final String A_offlineDataSourceName = "offlineDataSourceName";
    public static final String A_offlineAccountName = "offlineAccountName";
    
    public static final String A_offlineSyncFreq = "offlineSyncFreq";
    public static final String A_offlineSyncStatus = "offlineSyncStatus";
    public static final String A_offlineLastSync = "offlineLastSync";
    
    public static final String A_zimbraDataSourceDomain = "zimbraDataSourceDomain";
    public static final String A_zimbraDataSourceSmtpHost = "zimbraDataSourceSmtpHost";
    public static final String A_zimbraDataSourceSmtpPort = "zimbraDataSourceSmtpPort";
    public static final String A_zimbraDataSourceSmtpConnectionType = "zimbraDataSourceSmtpConnectionType";
    public static final String A_zimbraDataSourceSmtpAuthRequired = "zimbraDataSourceSmtpAuthRequired";
    public static final String A_zimbraDataSourceSmtpAuthUsername = "zimbraDataSourceSmtpAuthUsername";
    public static final String A_zimbraDataSourceSmtpAuthPassword = "zimbraDataSourceSmtpAuthPassword";
    
    public static final String A_zimbraDataSourceUseProxy = "zimbraDataSourceUseProxy";
    public static final String A_zimbraDataSourceProxyHost = "zimbraDataSourceProxyHost";
    public static final String A_zimbraDataSourceProxyPort = "zimbraDataSourceProxyPort";
    
    public static final String A_zimbraDataSourceSyncFreq = "zimbraDataSourceSyncFreq";
    public static final String A_zimbraDataSourceSyncStatus = "zimbraDataSourceSyncStatus";
    public static final String A_zimbraDataSourceLastSync = "zimbraDataSourceLastSync";
    
    public static final long DEFAULT_SYNC_FREQ = 5 * Constants.MILLIS_PER_MINUTE;
    
    public static final String LOCAL_ACCOUNT_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";
}
