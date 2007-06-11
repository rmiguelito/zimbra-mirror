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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateSignature extends DocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        canModifyOptions(zsc, account);
        
        Element signatureEl = request.getElement(AccountConstants.E_SIGNATURE);
        String name = signatureEl.getAttribute(AccountConstants.A_NAME);
        Map<String,Object> attrs = AccountService.getAttrs(signatureEl, AccountConstants.A_NAME);
        Signature signature = Provisioning.getInstance().createSignature(account, name, attrs);
        
        Element response = zsc.createElement(AccountConstants.CREATE_SIGNATURE_RESPONSE);
        ToXML.encodeSignature(response, signature);
        return response;
    }
}
