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

function ZmContactController(appCtxt, container, abApp) {

	ZmListController.call(this, appCtxt, container, abApp);

	this._listeners[ZmOperation.SAVE] = new AjxListener(this, this._saveListener);
	this._listeners[ZmOperation.CANCEL] = new AjxListener(this, this._cancelListener);
};

ZmContactController.prototype = new ZmListController();
ZmContactController.prototype.constructor = ZmContactController;

ZmContactController.prototype.toString =
function() {
	return "ZmContactController";
};

ZmContactController.prototype.show =
function(contact, isDirty) {
	this._contact = contact;
	this._currentView = this._getViewType();
	this._contactDirty = isDirty === true;
	this._list = contact.list;

	// re-enable input fields if list view exists
	if (this._listView[this._currentView])
		this._listView[this._currentView].enableInputs(true);

	this._setup(this._currentView);
	this._resetOperations(this._toolbar[this._currentView], 1); // enable all buttons

	var elements = new Object();
	elements[ZmAppViewMgr.C_TOOLBAR_TOP] = this._toolbar[this._currentView];
	elements[ZmAppViewMgr.C_APP_CONTENT] = this._listView[this._currentView];

	this._setView(this._currentView, elements, null, null, null, true);
};

// Private methods (mostly overrides of ZmListController protected methods)

ZmContactController.prototype._getToolBarOps = 
function() {
	var list = [ZmOperation.SAVE];
	list.push(ZmOperation.CANCEL);
	list.push(ZmOperation.SEP);
	if (this._appCtxt.get(ZmSetting.TAGGING_ENABLED))
		list.push(ZmOperation.TAG_MENU);
	if (this._appCtxt.get(ZmSetting.PRINT_ENABLED))
		list.push(ZmOperation.PRINT);
	list.push(ZmOperation.DELETE);
	return list;
};

ZmContactController.prototype._getActionMenuOps =
function() {
	return null;
};

ZmContactController.prototype._getViewType = 
function() {
	return this._contact.subType == ZmContact.SUBTYPE_CONTACT
		? ZmController.CONTACT_VIEW
		: ZmController.CONTACT_GROUP_VIEW;
};

ZmContactController.prototype._initializeListView = 
function(view) {
	if (!this._listView[view]) {
		this._listView[view] = view == ZmController.CONTACT_VIEW
			? new ZmContactView(this._container, this._appCtxt, this)
			: new ZmContactGroupView(this._container, this._appCtxt, this);
	}
};

ZmContactController.prototype._initializeToolBar = 
function(view) {
	ZmListController.prototype._initializeToolBar.call(this, view);

	// change the cancel button to "close" if editing existing contact
	var cancelButton = this._toolbar[view].getButton(ZmOperation.CANCEL);
	if (this._contact.id == undefined || this._contact.isGal) {
		cancelButton.setText(ZmMsg.cancel);
		cancelButton.setImage("Cancel");
	} else {
		cancelButton.setText(ZmMsg.close);
		cancelButton.setImage("Close");
	}
};

ZmContactController.prototype._getTagMenuMsg = 
function() {
	return ZmMsg.tagContact;
};

ZmContactController.prototype._setViewContents =
function(view) {
	this._listView[view].set(this._contact, this._contactDirty);
	if (this._contactDirty) delete this._contactDirty;
};

ZmContactController.prototype._paginate = 
function(view, bPageForward) {
	// TODO
	DBG.println("TODO - page to next/previous contact");
};

ZmContactController.prototype._resetOperations = 
function(parent, num) {
	if (!parent) return;
	if (this._contact.id == undefined || this._contact.isGal) {
		// disble all buttons except SAVE and CANCEL
		parent.enableAll(false);
		parent.enable([ZmOperation.SAVE, ZmOperation.CANCEL], true);
	} else if (this._contact.isShared()) {
		parent.enableAll(true);
		// XXX: let's disable DELETE until we figure out how to handle it
		parent.enable([ZmOperation.TAG_MENU, ZmOperation.DELETE], false);
	} else {
		ZmListController.prototype._resetOperations.call(this, parent, num);
	}
};

ZmContactController.prototype._saveListener =
function(ev, bIsPopCallback) {
	var view = this._listView[this._currentView];
	if (!view.isValid()) {
		// TODO - get proper error message
		this._msgDialog.setMessage("Cannot save. You are missing some required fields or have not entered valid values.", DwtMessageDialog.CRITICAL_STYLE);
		this._msgDialog.popup();
		return;
	}

	try {
		var mods = view.getModifiedAttrs();
		view.enableInputs(false);

		if (!bIsPopCallback)
			this._app.popView(true);

		if (mods) {
			// Make sure at least one form field has a value (otherwise, delete the contact). The call
			// to getModifiedAttrs() above populates _attrs with form field values.
			var doDelete = true;
			var formAttrs = view._attr;
			for (var i in formAttrs) {
				if (i == ZmContact.F_fileAs || i == ZmContact.X_fullName) continue;
				if (formAttrs[i]) {
					doDelete = false;
					break;
				}
			}

			var contact = view.getContact();

			if (doDelete) {
				this._doDelete([contact], null, null, true);
			} else {
				if (contact.id == undefined || contact.isGal) {
					var list = this._app.getContactList();
					this._doCreate(list, mods);
				} else {
					this._doModify(contact, mods);
				}
			}
		} else {
			// bug fix #5829 - differentiate betw. an empty contact and saving 
			//                 an existing contact w/o editing
			if (this._contact.isEmpty()) {
				var msg = this._contact.subType == ZmContact.SUBTYPE_GROUP
					? ZmMsg.emptyGroup
					: ZmMsg.emptyContact;
				this._appCtxt.setStatusMsg(msg, ZmStatusView.LEVEL_WARNING);
			} else {
				var msg = this._contact.subType == ZmContact.SUBTYPE_GROUP
					? ZmMsg.groupSaved
					: ZmMsg.contactSaved;
				this._appCtxt.setStatusMsg(msg, ZmStatusView.LEVEL_INFO);
			}
		}
	} catch (ex) {
		this._handleException(ex, this._saveListener, ev, false);
	}
};

ZmContactController.prototype._cancelListener = 
function(ev) {
	this._app.popView();
};

ZmContactController.prototype._doDelete = 
function(items, hardDelete, attrs, skipPostProcessing) {
	ZmListController.prototype._doDelete.call(this, items, hardDelete, attrs);

	if (!skipPostProcessing) {
		// XXX: async
		// disable input fields (to prevent blinking cursor from bleeding through)
		this._listView[this._currentView].enableInputs(false);
		this._app.popView();
	}
};

ZmContactController.prototype._preHideCallback =
function() {
	var view = this._listView[this._currentView];
	if (!view.isDirty())
		return true;

	var ps = this._popShield = this._appCtxt.getYesNoCancelMsgDialog();
	ps.reset();
	ps.setMessage(ZmMsg.askToSave, DwtMessageDialog.WARNING_STYLE);
	ps.registerCallback(DwtDialog.YES_BUTTON, this._popShieldYesCallback, this);
	ps.registerCallback(DwtDialog.NO_BUTTON, this._popShieldNoCallback, this);
	ps.popup(view._getDialogXY());
	
	return false;
};

ZmContactController.prototype._popShieldYesCallback =
function() {
	this._saveListener(null, true);
	this._popShield.popdown();

	// bug fix #5282
	// check if the pending view is poppable - if so, force-pop this view first!
	var avm = this._app.getAppViewMgr();
	if (avm.isPoppable(avm.getPendingViewId()))
		this._app.popView(true);

	this._app.getAppViewMgr().showPendingView(true);
};

ZmContactController.prototype._popShieldNoCallback =
function() {
	this._popShield.popdown();

	// bug fix #5282
	// check if the pending view is poppable - if so, force-pop this view first!
	var avm = this._app.getAppViewMgr();
	if (avm.isPoppable(avm.getPendingViewId()))
		this._app.popView(true);

	this._app.getAppViewMgr().showPendingView(true);
};

ZmContactController.prototype._popdownActionListener = 
function(ev) {
	// bug fix #3719 - do nothing
};
