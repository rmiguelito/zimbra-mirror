/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import java.util.Map;

import org.dom4j.DocumentException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;

import com.zimbra.cs.im.IMAddr;
import com.zimbra.cs.im.IMMessage;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMUtils;
import com.zimbra.cs.im.IMMessage.TextPart;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.soap.ZimbraSoapContext;

public class IMSendMessage extends IMDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        OperationContext octxt = getOperationContext(zsc, context);

        Element response = zsc.createElement(IMConstants.IM_SEND_MESSAGE_RESPONSE);

        Element msgElt = request.getElement(IMConstants.E_MESSAGE);

        String threadId = msgElt.getAttribute(IMConstants.A_THREAD_ID, null);
        String addr = IMUtils.resolveAddress(msgElt.getAttribute(IMConstants.A_ADDRESS));

        String subject = null;
        TextPart bodyPart = null;

        Element subjElt = msgElt.getOptionalElement(IMConstants.E_SUBJECT);
        if (subjElt != null) {
            subject = subjElt.getText();
        }

        Element bodyElt = msgElt.getOptionalElement(IMConstants.E_BODY);
        if (bodyElt != null) {
            // 
            // FIXME - temp hack
            //
            try {
                String s = bodyElt.toString();
//                s = s.replaceAll("<p", "<span").replaceAll("</p", "</span" );
//                s = "<span style=\"font-weight: bold;\">aaa</span>";
//                s = "<span style='text-decoration: underline;'>hihi</span>";
//                s=s.replaceAll("style='.*'", "style='font-weight: bold;'");
//                s=s.replaceAll("font-family.*0\\);","");
//                s=s.replaceAll("font-style.*none;","");
//                s=s.replaceAll("font-weight:bold", "font-weight: bold");
//                s=s.replaceAll(":", ": ");
                Element e = Element.parseXML(s);
                org.dom4j.Element root = org.dom4j.DocumentHelper.createElement("root");
                org.dom4j.Element xhtmlBody = root.addElement("body", "http://www.w3.org/1999/xhtml");
                xhtmlBody.add(e.toXML());
                xhtmlBody.detach();
                bodyPart = new TextPart(xhtmlBody);
            } catch (DocumentException e) {
                throw ServiceException.FAILURE("Error parsing message body: "+bodyElt.toXML().toString(), e);
            }
        }
//        if (bodyElt != null) {
//            bodyPart = new TextPart(bodyElt);
//        }

        boolean isTyping = false;
        if (msgElt.getOptionalElement(IMConstants.E_TYPING) != null)
            isTyping = true;

        IMMessage msg = new IMMessage(subject==null?null:new TextPart(subject),
            bodyPart, isTyping);

        Object lock = super.getLock(zsc);
        synchronized(lock) {
            IMPersona persona = super.getRequestedPersona(zsc, context, lock);
            persona.sendMessage(octxt, new IMAddr(addr), threadId, msg);
        }

        response.addAttribute(IMConstants.A_THREAD_ID, threadId);

        return response;        
    }
}
