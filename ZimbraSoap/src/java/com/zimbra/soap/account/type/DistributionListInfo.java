/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.account.type;

import java.util.List;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.DistributionListGranteeInfoInterface;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_DL)
public class DistributionListInfo extends ObjectInfo {

    @XmlAttribute(name=AccountConstants.A_IS_OWNER, required=false)
    ZmBoolean isOwner;
    @XmlAttribute(name=AccountConstants.A_IS_MEMBER, required=false)
    ZmBoolean isMember;
    
    @XmlAttribute(name=AccountConstants.A_DYNAMIC, required=false)
    ZmBoolean dynamic;
    @XmlElement(name=AccountConstants.E_DLM, required=false)
    private List<String> members;

    @XmlElementWrapper(name=AccountConstants.E_DL_OWNERS, required=false)
    @XmlElement(name=AccountConstants.E_DL_OWNER, required=false)
    private List<DistributionListGranteeInfo> owners = Lists.newArrayList();
    
    @XmlElementWrapper(name=AccountConstants.E_RIGHTS, required=false)
    @XmlElement(name=AccountConstants.E_RIGHT, required=false)
    private List<DistributionListRightInfo> rights = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListInfo() {
        this((String) null, (String) null,
            (Collection <String>) null, (Collection <KeyValuePair>) null);
    }

    public DistributionListInfo(String id, String name) {
        this((String)id, (String)name,
            (Collection <String>) null, (Collection <KeyValuePair>) null);
    }

    public DistributionListInfo(String id, String name,
            Collection <String> members, Collection <KeyValuePair> attrs) {
        super(id, name, attrs);
        setMembers(members);
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = ZmBoolean.fromBool(isOwner);
    }
    
    public Boolean isOwner() {
        return ZmBoolean.toBool(isOwner, false);
    }
    
    public void setIsMember(Boolean isMember) {
        this.isMember = ZmBoolean.fromBool(isMember);
    }
    
    public Boolean isMember() {
        return ZmBoolean.toBool(isMember, false);
    }
    
    public void setMembers(Collection <String> members) {
        this.members = Lists.newArrayList();
        if (members != null) {
            this.members.addAll(members);
        }
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = ZmBoolean.fromBool(dynamic);
    }

    public Boolean isDynamic() {
        return ZmBoolean.toBool(dynamic, false);
    }

    public void setOwners(List<DistributionListGranteeInfo> owners) {
        this.owners = owners;
    }

    public void addOwner(DistributionListGranteeInfo owner) {
        owners.add(owner);
    }

    public List<? extends DistributionListGranteeInfoInterface> getOwners() {
        return Collections.unmodifiableList(owners);
    }
    
    public void setRights(List<DistributionListRightInfo> rights) {
        this.rights = rights;
    }

    public void addRight(DistributionListRightInfo right) {
        rights.add(right);
    }

    public List<? extends DistributionListRightInfo> getRights() {
        return Collections.unmodifiableList(rights);
    }
}
