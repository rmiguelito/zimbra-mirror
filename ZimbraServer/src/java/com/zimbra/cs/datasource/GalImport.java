/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mime.ParsedContact;

public class GalImport extends MailItemImport {

	public GalImport(DataSource ds) throws ServiceException {
    	super(ds);
    }
    
	public void importData(List<Integer> folderIds, boolean fullSync)
			throws ServiceException {
    	if (folderIds == null)
    		importGal(dataSource.getFolderId(), fullSync, false);
    	else
    		for (int fid : folderIds)
    			importGal(fid, fullSync, false);
	}

	public void test() throws ServiceException {
		try {
			searchGal(null);
		} catch (NamingException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		} catch (IOException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		}
	}

	private static final String TYPE = "t";
	private static final String FOLDER = "f";
	private static final String SYNCTOKEN = "st";
	
	private void setStatus(boolean success) throws ServiceException {
		Date now = new Date();
		DataSource ds = getDataSource();
		Map<String,Object> attrs = new HashMap<String,Object>();
		String attr = success ? 
				Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp :
				Provisioning.A_zimbraGalLastFailedSyncTimestamp;
		attrs.put(attr, DateUtil.toGeneralizedTime(now));
		Provisioning.getInstance().modifyAttrs(ds, attrs);
	}
	
	public void importGal(int fid, boolean fullSync, boolean force) throws ServiceException {
    	mbox.beginTrackingSync();
		DataSource ds = getDataSource();
		DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
		if (folderMapping.md == null) {
			folderMapping.itemId = fid;
			folderMapping.md = new Metadata();
			folderMapping.md.put(TYPE, FOLDER);
			DbDataSource.addMapping(ds, folderMapping);
		}
		String syncToken = fullSync ? null : folderMapping.md.get(SYNCTOKEN, null);
    	OperationContext octxt = new Mailbox.OperationContext(mbox);
    	SearchGalResult result = null;
    	try {
    		result = searchGal(syncToken);
    	} catch (Exception e) {
    		setStatus(false);
    		ZimbraLog.gal.error("Error executing gal search", e);
    		return;
    	}
        HashMap<String,DataSourceItem> allMappings = new HashMap<String,DataSourceItem>();
        for (DataSourceItem dsItem : DbDataSource.getAllMappings(ds))
        	if (dsItem.md == null || dsItem.md.get(TYPE, null) == null)
        		allMappings.put(dsItem.remoteId, dsItem);
        for (GalContact contact : result.getMatches()) {
        	try {
                processContact(octxt, contact, fid, force);
                allMappings.remove(contact.getId());
        	} catch (Exception e) {
        		setStatus(false);
        		ZimbraLog.gal.warn("Ignoring error importing gal contact "+contact.getId(), e);
        	}
        }
        folderMapping.md.put(SYNCTOKEN, result.getToken());
        DbDataSource.updateMapping(ds, folderMapping);
        if (allMappings.size() == 0 || !fullSync) {
    		setStatus(true);
        	return;
        }
        
        ArrayList<Integer> deleted = new ArrayList<Integer>();
        int[] deletedIds = new int[allMappings.size()];
        int i = 0;
        for (DataSourceItem dsItem : allMappings.values()) {
        	deleted.add(dsItem.itemId);
        	deletedIds[i++] = dsItem.itemId;
        }
    	try {
        	mbox.delete(octxt, deletedIds, MailItem.TYPE_CONTACT, null);
    	} catch (ServiceException e) {
    		ZimbraLog.gal.warn("Ignoring error deleting gal contacts", e);
    	}
        DbDataSource.deleteMappings(getDataSource(), deleted);
		setStatus(true);
	}
	
	private SearchGalResult searchGal(String syncToken) throws ServiceException, NamingException, IOException  {
		if (getDataSource().getAttr(Provisioning.A_zimbraGalType).compareTo("zimbra") == 0)
			return searchZimbraGal(syncToken);
		else
			return searchExternalGal(syncToken);
	}
	private static String[] ZIMBRA_ATTRS = {
		"zimbraAccountCalendarUserType",
		"zimbraCalResType",
		"zimbraCalResLocationDisplayName",
		"zimbraCalResCapacity",
		"zimbraCalResContactEmail"
	};
	private LdapGalMapRules getGalMapRules() throws ServiceException {
		DataSource ds = getDataSource();
        String[] attrs = ds.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        if (attrs.length == 0)
        	attrs = Provisioning.getInstance().getDomainByName(ds.getAccount().getDomainName()).getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        ArrayList<String> attrList = new ArrayList<String>();
        java.util.Collections.addAll(attrList, attrs);
        for (String attr : ZIMBRA_ATTRS)
        	attrList.add(attr+"="+attr);
        return new LdapGalMapRules(attrList.toArray(new String[0]));
	}
	
	private static final int MAX_GAL_SEARCH_RESULT = 65535;
	
	private SearchGalResult searchExternalGal(String syncToken) throws ServiceException, NamingException, IOException  {
		ZimbraLog.gal.debug("searchExternalGal: "+syncToken);
		DataSource ds = getDataSource();
		GalOp galOp = GalOp.sync;
        GalParams.ExternalGalParams galParams = new GalParams.ExternalGalParams(ds, galOp);
        LdapGalMapRules rules = getGalMapRules();
        return LdapUtil.searchLdapGal(galParams, galOp, "", MAX_GAL_SEARCH_RESULT, rules, syncToken, null);
	}
	
	private SearchGalResult searchZimbraGal(String syncToken) throws ServiceException {
		ZimbraLog.gal.debug("searchZimbraGal "+syncToken);
        SearchGalResult result = SearchGalResult.newSearchGalResult(null);
        String filter = getDataSource().getAttr(Provisioning.A_zimbraGalLdapFilter);
        String query = filter;
        if (syncToken != null) {
            String arg = LdapUtil.escapeSearchFilterArg(syncToken);
            query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+"))"+filter+")";
        }
        LdapGalMapRules rules = getGalMapRules();
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(false);
            LdapUtil.searchGal(zlc, 0, "", query, 0, rules, syncToken, result);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        return result;
	}
	
	private void processContact(OperationContext octxt, GalContact contact, int fid, boolean force) throws ServiceException {
		Map<String,Object> attrs = contact.getAttrs();
		String id = contact.getId();
		ZimbraLog.gal.debug("processing gal contact "+id);
		DataSourceItem dsItem = DbDataSource.getReverseMapping(getDataSource(), id);
        String modifiedDate = (String) contact.getAttrs().get("modifyTimeStamp");
    	if (dsItem.itemId == 0) {
    		ZimbraLog.gal.debug("creating new contact "+id);
    		dsItem.remoteId = id;
            ParsedContact pc = new ParsedContact(attrs);
            dsItem.itemId = mbox.createContact(octxt, pc, fid, null).getId();
    		DbDataSource.addMapping(getDataSource(), dsItem);
    	} else {
    		String syncDate = mbox.getContactById(octxt, dsItem.itemId).get("modifyTimeStamp");
            if (force || syncDate == null || !syncDate.equals(modifiedDate)) {
        		ZimbraLog.gal.debug("modifying contact "+id);
                ParsedContact pc = new ParsedContact(attrs);
                mbox.modifyContact(octxt, dsItem.itemId, pc);
            }
    	}
	}
	
	public String getUrl() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapURL);
	}
	public String getFilter() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapFilter);
	}
	public String getSearchBase() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapSearchBase);
	}
	public String getAuthType() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapAuthMech);
	}
	public String getBindDN() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapBindDn);
	}
	public String getPassword() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapBindPassword);
	}
	public String getKerberos5Principal() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
	}
	public String getKerberos5Keytab() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
	}
}
