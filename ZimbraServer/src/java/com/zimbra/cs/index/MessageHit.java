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
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.ArrayList;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.service.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 * @author tim
 * 
 * Efficient Read-access to a Message returned from a query. APIs mirror the
 * APIs on com.zimbra.cs.mailbox.Message, but are read-only. The real
 * archive.mailbox.Message can be retrieved, but this should only be done if
 * write-access is necessary.
 */
public class MessageHit extends ZimbraHit {

    private static Log mLog = LogFactory.getLog(MessageHit.class);
    
    private Document mDoc = null;

    private Message mMessage = null;

    private ArrayList mMatchedParts = null;

    private int mConversationId = 0;

    private int mMessageId = 0;

    private ConversationHit mConversationHit = null;

    protected MessageHit(ZimbraQueryResultsImpl results, Mailbox mbx, Document d, float score) {
        super(results, mbx, score);
        mDoc = d;
        assert (d != null);
    }

    protected MessageHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id, float score) {
        super(results, mbx, score);
        mMessageId = id;
        assert (id != 0);
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#inTrashOrSpam()
     */
    boolean inMailbox() throws ServiceException {
        return getMessage().inMailbox();
    }
    boolean inTrash() throws ServiceException {
        return getMessage().inTrash();
    }
    boolean inSpam() throws ServiceException {
        return getMessage().inSpam();
    }
    
    
    int getFolderId() throws ServiceException {
        return getMessage().getFolderId();
    }

    public int getConversationId() throws ServiceException {
        if (mConversationId == 0) {
            mConversationId = getMessage().getConversationId();
        }
        return mConversationId;
    }

    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            if (mMessage == null && mDoc != null) {
                String dateStr = mDoc.get(LuceneFields.L_DATE);
                if (dateStr != null) {
                    mCachedDate = DateField.stringToTime(dateStr);
                    
                    // Tim: 5/11/2005 - now that the DB has been changed to store dates in msec
                    // precision, we do NOT want to manually truncate the date here...
                       // fix for Bug 311 -- SQL truncates dates when it stores them
                       //mCachedDate = (mCachedDate /1000) * 1000;
                    return mCachedDate;
                }
            }
            mCachedDate = getMessage().getDate();
        }
        return mCachedDate;
    }

    public void addPart(MessagePartHit part) {
        if (mMatchedParts == null)
            mMatchedParts = new ArrayList();
        
        if (!mMatchedParts.contains(part)) {
            mMatchedParts.add(part);
        }
    }

    public ArrayList getMatchedMimePartNames() {
        return mMatchedParts;
    }

    public int getItemId() {
        if (mMessageId != 0) {
            return mMessageId;
        }
        String mbid = mDoc.get(LuceneFields.L_MAILBOX_BLOB_ID);
        try {
            if (mbid != null) {
                mMessageId = Integer.parseInt(mbid);
            }
            return mMessageId;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public byte getItemType() throws ServiceException {
        return MailItem.TYPE_MESSAGE;
    }
    

    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        int size = 0;
        try {
            size = getSize();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return "MS: " + super.toString() + " C" + convId + " M" + Integer.toString(getItemId()) + " S="+size;
    }

    public int getSize() throws ServiceException {
//        if (mDoc != null) {
        if (false) {
            String sizeStr = mDoc.get(LuceneFields.L_SIZE);
            return (int) ZimbraAnalyzer.SizeTokenFilter.DecodeSize(sizeStr);
        } else {
            return (int) getMessage().getSize();
        }
    }

    public boolean isTagged(Tag tag) throws ServiceException {
        return getMessage().isTagged(tag);
    }
    
    void setItem(MailItem item) {
        mMessage = (Message)item;
    }
    
    boolean itemIsLoaded() {
        return mMessage != null;
    }
    
    public Message getMessage() throws ServiceException {
        if (mMessage == null) {
            Mailbox mbox = Mailbox.getMailboxById(getMailbox().getId());
            int messageId = getItemId();
            try {
                mMessage = mbox.getMessageById(messageId);
            } catch (ServiceException e) {
                mLog.error("Error getting message id="+messageId+" from mailbox "+mbox.getId(),e);
                e.printStackTrace();
                throw e;
            }
        }
        return mMessage;
    }

    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            if (mConversationHit != null) { 
                mCachedSubj = getConversationResult().getSubject();
            } else {
                if (mDoc != null) {
                    mCachedSubj = mDoc.get(LuceneFields.L_SORT_SUBJECT);
                } else {
                    mCachedSubj = getMessage().getSubject();
                }
            }
        }
        return mCachedSubj;
    }
    
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getSender();
        }
        return mCachedName;
    }

    public long getDateHeader() throws ServiceException {
        if (mMessage == null && mDoc != null) {
            String dateStr = mDoc.get(LuceneFields.L_DATE);
            //                mLog.info(toString() + " " + dateStr);
            if (dateStr != null) {
                return DateField.stringToTime(dateStr);
            } else {
                return 0;
            }
        }
        //            mLog.info(toString() + " Called getMessage().getDate()");
        return getMessage().getDate();
    }

    public String getSender() throws ServiceException {
        //		    if (mMessage == null && mDoc != null) {
        //            if (false) {
        //		        return mDoc.get(LuceneFields.L_H_FROM);
        //		    }
        ParsedAddress cn = new ParsedAddress(getMessage().getSender());
        return cn.getSortString();
    }

    ////////////////////////////////////////////////////
    //
    // Hierarchy access:
    //

    /**
     * @return a ConversationResult corresponding to this message's
     *         conversation
     * @throws ServiceException
     */
    public ConversationHit getConversationResult() throws ServiceException {
        if (mConversationHit == null) {
            Integer cid = new Integer(getConversationId());
            mConversationHit = getResults().getConversationHit(getMailbox(), cid, getScore());
            mConversationHit.addMessageHit(this);
        }
        return mConversationHit;
    }
}