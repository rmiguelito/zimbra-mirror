/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Web Client
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

function ZmContactGroupView(parent, appCtxt, controller) {
	ZmContactView.call(this, parent, appCtxt, controller);

	this._createHtml();
};

ZmContactGroupView.prototype = new ZmContactView;
ZmContactGroupView.prototype.constructor = ZmContactGroupView;

// Public Methods

ZmContactGroupView.prototype.toString =
function() {
	return "ZmContactGroupView";
};

ZmContactGroupView.prototype.set =
function(contact, isDirty) {
	this._attr = new Object();
	for (var a in contact.getAttrs())
		this._attr[a] = contact.getAttr(a);

	if (this._contact)
		this._contact.removeChangeListener(this._changeListener);
	contact.addChangeListener(this._changeListener);

	this._contact = contact;
	this._setFields();
	this._isDirty = isDirty || false;

	var groupField = document.getElementById(this._groupNameId);
	groupField.focus();
};

ZmContactGroupView.prototype.getModifiedAttrs =
function() {
	this._attr[ZmContact.F_folderId] = this._folderSelect.getValue();

	var groupName = document.getElementById(this._groupNameId);
	this._attr[ZmContact.F_groupName] = AjxStringUtil.trim(groupName.value);

	var emails = [];
	for (var i = 0; i < this._addrs.good.size(); i++)
		emails.push(this._addrs.good.get(i).address);
	this._attr[ZmContact.F_email] = emails.join("; ");

	return this._attr;
};

ZmContactGroupView.prototype.enableInputs =
function(bEnable) {
	var groupName = document.getElementById(this._groupNameId);
	groupName.disabled = !bEnable;

	var members = document.getElementById(this._membersTxtId);
	members.disabled = !bEnable;
};

ZmContactGroupView.prototype.isValid =
function() {
	var isValid = false;

	var groupName = document.getElementById(this._groupNameId);
	var value = AjxStringUtil.trim(groupName.value);

	if (value.length > 0) {
		var members = document.getElementById(this._membersTxtId);
		this._addrs = ZmEmailAddress.parseEmailString(members.value);

		isValid = this._addrs.bad.size() == 0;
	}

	return isValid;
};


// Private Methods

ZmContactGroupView.prototype._createHtml =
function() {
	this._fieldIds = new Object();

	var titleId = Dwt.getNextId();
	this._fieldIds[ZmContactView.F_contactTitle] = titleId;

	var tagsId = Dwt.getNextId();
	this._fieldIds[ZmContactView.F_contactTags] = tagsId;

	this._contactHeaderId = Dwt.getNextId();
	this._contactHeaderRowId = Dwt.getNextId();
	this._groupNameId = Dwt.getNextId();
	this._membersTxtId = Dwt.getNextId();
	var folderCellId = Dwt.getNextId();
	var membersCellId = Dwt.getNextId();

	var html = [];
	var idx = 0;

	// Title bar
	html[idx++] = "<table border=0 width=99% cellspacing=0 cellpadding=0 id='";
	html[idx++] = this._contactHeaderId;
	html[idx++] = "'><tr class='contactHeaderRow' id='";
	html[idx++] = this._contactHeaderRowId;
	html[idx++] = "'><td width=20><center>";
	html[idx++] = AjxImg.getImageHtml("Group");
	html[idx++] = "</center></td><td><div id='";
	html[idx++] = titleId;
	html[idx++] = "' class='contactHeader'></div></td><td align='right' id='";
	html[idx++] = tagsId;
	html[idx++] = "'></td></tr></table><p>";
	html[idx++] = "<table border=0 width=100%><tr><td width=100 valign=top class='editLabel'>";
	html[idx++] = ZmMsg.groupName;
	html[idx++] = "</td><td valign=top><input type='text' autocomplete='off' size=35 id='";
	html[idx++] = this._groupNameId;
	html[idx++] = "'></td><td align=right valign=top id='";
	html[idx++] = folderCellId;
	html[idx++] = "'></td></tr><tr><td align=right valign=top id='";
	html[idx++] = membersCellId;
	html[idx++] = "'></td><td valign=top colspan=2><textarea style='width:100%;' id='";
	html[idx++] = this._membersTxtId;
	html[idx++] = "'></textarea></td></tr><tr><td></td><td valign=top colspan=2><center>";
	html[idx++] = ZmMsg.groupDirections;
	html[idx++] = "</center></td></tr></table>";

	this.getHtmlElement().innerHTML = html.join("");

	// add Dwt widgets now..
	this._folderSelect = new DwtSelect(this);
	this._folderSelect.reparentHtmlElement(folderCellId);

	this._membersButton = new DwtButton(this);
	this._membersButton.setText(ZmMsg.members);
	this._membersButton.setSize("75");
	this._membersButton.addSelectionListener(new AjxListener(this, this._membersListener));
	this._membersButton.reparentHtmlElement(membersCellId);

	// add key handler support
	var groupField = document.getElementById(this._groupNameId);
	Dwt.setHandler(groupField, DwtEvent.ONKEYUP, ZmContactGroupView._onKeyUp);
	Dwt.associateElementWithObject(groupField, this);
};

ZmContactGroupView.prototype._setFields =
function() {
	this._setHeaderColor();
	this._setTitle();
	this._setTags();
	this._setFolder();

	// TODO - populate these fields
//	if (this._contact.id) {
		// set group name
		var groupName = document.getElementById(this._groupNameId);
		groupName.value = "";

		// set group members
		var members = document.getElementById(this._membersTxtId);
		members.value = "";
//	}
};

ZmContactGroupView.prototype._sizeChildren =
function(width, height) {
	var membersText = document.getElementById(this._membersTxtId);
	Dwt.setSize(membersText, Dwt.DEFAULT, height-125); // XXX: hardcode for now :(
};


// Listeners

ZmContactGroupView.prototype._membersListener =
function(ev) {
	if (!this._contactPicker) {
		this._contactPicker = new ZmContactPicker(this._appCtxt);
		this._contactPicker.registerCallback(DwtDialog.OK_BUTTON, this._contactPickerOkCallback, this);
	}

	this._contactPicker.popup();
};

ZmContactGroupView.prototype._contactPickerOkCallback =
function(vec) {
	var addrs = vec.getArray();
	if (addrs.length) {
		var textarea = document.getElementById(this._membersTxtId);
		var emails = new Array();
		for (var i = 0; i < addrs.length; i++) {
			var addr = addrs[i].address;
			emails.push(addr);
		}
		var value = AjxStringUtil.trim(textarea.value);
		if (value.length)
			value += "; ";
		textarea.value += emails.join("; ");
	}

	this._contactPicker.popdown();
};


// Callbacks

ZmContactGroupView._onKeyUp =
function(ev) {
	var e = DwtUiEvent.getTarget(ev);
	if (e) {
		var view = Dwt.getObjectFromElement(e);
		view._isDirty = true;
		view._setTitle(e.value);
	}
	return true;
};
