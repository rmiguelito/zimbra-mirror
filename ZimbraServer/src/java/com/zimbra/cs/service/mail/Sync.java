/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Aug 31, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class Sync extends DocumentHandler {

    private static final int DEFAULT_FOLDER_ID = Mailbox.ID_FOLDER_ROOT;

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);

        long begin = request.getAttributeLong(MailService.A_TOKEN, 0);

        Element response = lc.createElement(MailService.SYNC_RESPONSE);
        synchronized (mbox) {
            mbox.beginTrackingSync();

            long token = mbox.getLastChangeID();
            response.addAttribute(MailService.A_TOKEN, token);
            if (begin <= 0) {
                Folder folder = mbox.getFolderById(DEFAULT_FOLDER_ID);
                if (folder == null)
                    throw MailServiceException.NO_SUCH_FOLDER(DEFAULT_FOLDER_ID);
                initialFolderSync(response, mbox, folder);
            } else
                deltaSync(response, mbox, begin);
        }
        return response;
    }

    private void initialFolderSync(Element response, Mailbox mbox, Folder folder) throws ServiceException {
        boolean isSearch = folder instanceof SearchFolder;
        Element f = ToXML.encodeFolder(response, folder);
        if (!isSearch) {
            if (folder.getId() == Mailbox.ID_FOLDER_TAGS)
                initialTagSync(mbox, f);
            else {
                initialItemSync(f, MailService.E_MSG, mbox.listItemIds(MailItem.TYPE_MESSAGE, folder.getId()));
                initialItemSync(f, MailService.E_CONTACT, mbox.listItemIds(MailItem.TYPE_CONTACT, folder.getId()));
                initialItemSync(f, MailService.E_NOTE, mbox.listItemIds(MailItem.TYPE_NOTE, folder.getId()));
            }
        } else {
            // anything else to be done for searchfolders?
        }

        List subfolders = folder.getSubfolders();
        if (subfolders != null)
            for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
                Folder subfolder = (Folder) it.next();
                if (subfolder != null)
                    initialFolderSync(f, mbox, subfolder);
        }
    }

    private void initialTagSync(Mailbox mbox, Element response) throws ServiceException {
        List tags = mbox.getTagList();
        if (tags != null)
            for (Iterator it = tags.iterator(); it.hasNext(); ) {
                Tag tag = (Tag) it.next();
                if (tag != null && !(tag instanceof Flag))
                    ToXML.encodeTag(response, tag);
            }
    }

    private void initialItemSync(Element f, String ename, int[] items) {
        if (items == null || items.length == 0)
            return;
        Element e = f.addElement(ename);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < items.length; i++)
            sb.append(i == 0 ? "" : ",").append(items[i]);
        e.addAttribute(MailService.A_IDS, sb.toString());
    }

    private static final byte[] SYNC_ORDER = new byte[] { MailItem.TYPE_FOLDER, MailItem.TYPE_TAG, MailItem.TYPE_MESSAGE,
                                                          MailItem.TYPE_CONTACT, MailItem.TYPE_NOTE };
    private static final int MUTABLE_FIELDS = Change.MODIFIED_FLAGS  | Change.MODIFIED_TAGS |
                                              Change.MODIFIED_FOLDER | Change.MODIFIED_PARENT |
                                              Change.MODIFIED_NAME   | Change.MODIFIED_QUERY |
                                              Change.MODIFIED_COLOR  | Change.MODIFIED_POSITION;

    private void deltaSync(Element response, Mailbox mbox, long begin) throws ServiceException {
        // first, handle deleted items
        String deleted = mbox.getTombstones(begin);
        if (deleted != null && !deleted.equals(""))
            response.addElement(MailService.E_DELETED).addAttribute(MailService.A_IDS, deleted);

        // now, handle created/modified items
        if (begin >= mbox.getLastChangeID())
            return;
        for (int i = 0; i < SYNC_ORDER.length; i++) {
            byte type = SYNC_ORDER[i];
            List changed = mbox.getModifiedItems(type, begin);
            for (Iterator it = changed.iterator(); it.hasNext(); ) {
                MailItem item = (MailItem) it.next();
                if (item.getSavedSequence() > begin && type != MailItem.TYPE_FOLDER && type != MailItem.TYPE_TAG)
                    ToXML.encodeItem(response, item, Change.MODIFIED_FOLDER | Change.MODIFIED_DATE | Change.MODIFIED_SIZE);
                else
                    ToXML.encodeItem(response, item, MUTABLE_FIELDS);
            }
        }
    }
}