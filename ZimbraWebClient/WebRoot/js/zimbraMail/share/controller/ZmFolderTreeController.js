/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
 * Creates a folder tree controller.
 * @constructor
 * @class
 * This class controls a tree display of folders.
 *
 * @author Conrad Damon
 * 
 * @param type		[constant]		type of organizer we are displaying/controlling (folder or search)
 * @param dropTgt	[DwtDropTgt]*	drop target for this type
 */
ZmFolderTreeController = function(type, dropTgt) {

	if (arguments.length == 0) { return; }

	type = type ? type : ZmOrganizer.FOLDER;
	ZmTreeController.call(this, type);

	this._listeners[ZmOperation.NEW_FOLDER] = new AjxListener(this, this._newListener);
	this._listeners[ZmOperation.RENAME_FOLDER] = new AjxListener(this, this._renameListener);
	this._listeners[ZmOperation.SHARE_FOLDER] = new AjxListener(this, this._shareAddrBookListener);
	this._listeners[ZmOperation.MOUNT_FOLDER] = new AjxListener(this, this._mountAddrBookListener);
	this._listeners[ZmOperation.EMPTY_FOLDER] = new AjxListener(this, this._emptyListener);
	this._listeners[ZmOperation.SYNC_OFFLINE_FOLDER] = new AjxListener(this, this._syncOfflineFolderListener);
	this._listeners[ZmOperation.BROWSE] = new AjxListener(this, this._browseListener);
};

ZmFolderTreeController.prototype = new ZmTreeController;
ZmFolderTreeController.prototype.constructor = ZmFolderTreeController;

// Public methods

ZmFolderTreeController.prototype.toString =
function() {
	return "ZmFolderTreeController";
};

/**
* Displays a folder tree. Certain folders are hidden.
*/
ZmFolderTreeController.prototype.show =
function(params) {
	var omit = params.omit || {};
	for (var id in ZmFolder.HIDE_ID) {
		omit[id] = true;
	}
	var dataTree = this.getDataTree(params.account);
	if (dataTree) {
	    for (var name in ZmFolder.HIDE_NAME) {
			var folder = dataTree.getByName(name);
			if (folder) {
				omit[folder.id] = true;
			}
		}
    }
	params.omit = omit;
	return ZmTreeController.prototype.show.call(this, params);
};

/**
* Enables/disables operations based on context.
*
* @param parent		[DwtControl]	the widget that contains the operations
* @param id			[int]			ID of the currently selected/activated organizer
*/
ZmFolderTreeController.prototype.resetOperations =
function(parent, type, id) {

	var emptyText = ZmMsg.emptyFolder; //ZmMsg.empty + (ZmFolder.MSG_KEY[id]?" "+ZmFolder.MSG_KEY[id] : "");
	var folder = appCtxt.getById(id);
	var hasContent = ((folder.numTotal > 0) || (folder.children && (folder.children.size() > 0)));

	// user folder or Folders header
	var nId = ZmOrganizer.normalizeId(id, this.type);
	if (nId == ZmOrganizer.ID_ROOT || ((!folder.isSystem()) && !folder.isSyncIssuesFolder())) {
		parent.enableAll(true);
		parent.enable(ZmOperation.SYNC, folder.isFeed()/* || folder.hasFeeds()*/);
		parent.enable(ZmOperation.SYNC_ALL, folder.isFeed() || folder.hasFeeds());
		parent.enable([ZmOperation.SHARE_FOLDER, ZmOperation.MOUNT_FOLDER], (!folder.link || folder.isAdmin()));
		parent.enable(ZmOperation.EMPTY_FOLDER, (hasContent || folder.link));	// numTotal is not set for shared folders
		parent.enable(ZmOperation.RENAME_FOLDER, !folder.isDataSource());		// dont allow datasource'd folder to be renamed via overview
		parent.enable(ZmOperation.NEW_FOLDER, !folder.disallowSubFolder);

		if (folder.isRemote() && folder.isReadOnly()) {
			parent.enable([ZmOperation.NEW_FOLDER, ZmOperation.MARK_ALL_READ, ZmOperation.EMPTY_FOLDER], false);
		}
	}
	// system folder
	else {
		parent.enableAll(false);
		// can't create folders under Drafts or Junk
		if (!folder.disallowSubFolder &&
			(nId == ZmFolder.ID_INBOX ||
			 nId == ZmFolder.ID_SENT  ||
			 nId == ZmFolder.ID_TRASH ||
			 nId == ZmFolder.ID_ARCHIVE))
		{
			parent.enable(ZmOperation.NEW_FOLDER, true);
		}
		// "Empty" for Chats, Junk and Trash
		if (nId == ZmFolder.ID_SPAM ||
			nId == ZmFolder.ID_TRASH ||
			nId == ZmFolder.ID_CHATS)
		{
			if (nId == ZmFolder.ID_SPAM) {
				emptyText = ZmMsg.emptyJunk;
			} else if (nId == ZmFolder.ID_TRASH) {
				 emptyText = ZmMsg.emptyTrash;
			}
			parent.enable(ZmOperation.EMPTY_FOLDER, hasContent);
		}
		// only allow Inbox and Sent system folders to be share-able for now
		if (!folder.link && (nId == ZmFolder.ID_INBOX || nId == ZmFolder.ID_SENT)) {
			parent.enable([ZmOperation.SHARE_FOLDER, ZmOperation.MOUNT_FOLDER, ZmOperation.EDIT_PROPS], true);
		}
		// bug fix #30435 - enable empty folder for sync failures folder
		if (appCtxt.isOffline && nId == ZmOrganizer.ID_SYNC_FAILURES && hasContent) {
			parent.enable(ZmOperation.EMPTY_FOLDER, true);
		}
//		parent.enable(ZmOperation.EXPORT_FOLDER, true);
//		parent.enable(ZmOperation.IMPORT_FOLDER, true);
	}

	parent.enable(ZmOperation.EXPAND_ALL, (folder.size() > 0));
	if (nId != ZmOrganizer.ID_ROOT && !folder.isReadOnly()) {
		// always enable for shared folders since we dont get this info from server
		parent.enable(ZmOperation.MARK_ALL_READ, (folder.numUnread > 0 || folder.link));
	}

	var op = parent.getOp(ZmOperation.EMPTY_FOLDER);
	if (op) {
		op.setText(emptyText);
	}

	// are there any external accounts associated to this folder?
	var button = parent.getOp(ZmOperation.SYNC);
	if (button) {
		var syncAllButton = parent.getOp(ZmOperation.SYNC_ALL);
		var hasFeeds = folder.hasFeeds();
		if (folder.isFeed()) {
			button.setEnabled(true);
			button.setVisible(true);
			button.setText(ZmMsg.checkFeed);
			if (syncAllButton) {
				syncAllButton.setEnabled(true);
				syncAllButton.setVisible(true);
				syncAllButton.setText(ZmMsg.checkAllFeed);
			}
		}
		else if (hasFeeds) {
			if (syncAllButton){
				syncAllButton.setEnabled(true);
				syncAllButton.setVisible(true);
				syncAllButton.setText(ZmMsg.checkAllFeed);
			}
		}
		else {
			var isEnabled = appCtxt.get(ZmSetting.POP_ACCOUNTS_ENABLED) || appCtxt.get(ZmSetting.IMAP_ACCOUNTS_ENABLED);
			if (!appCtxt.isOffline && isEnabled) {
				var dsCollection = AjxDispatcher.run("GetDataSourceCollection");
				var dataSources = dsCollection.getItemsFor(ZmOrganizer.normalizeId(folder.id));
				if (dataSources.length > 0) {
					button.setText(ZmMsg.checkExternalMail);
					button.setEnabled(true);
					button.setVisible(true);
				} else {
					button.setVisible(false);
				}
			}
			else {
				button.setVisible(false);
			}

			if (!hasFeeds && syncAllButton) {
				syncAllButton.setVisible(false);
			}
		}
	}

	button = parent.getOp(ZmOperation.SYNC_OFFLINE_FOLDER);
	if (button) {
		if (!folder.isOfflineSyncable) {
			button.setVisible(false);
		} else {
			button.setVisible(true);
			button.setEnabled(true);
			var text = (folder.isOfflineSyncing)
				? ZmMsg.syncOfflineFolderOff : ZmMsg.syncOfflineFolderOn;
			button.setText(text);
		}
	}
	parent.enable(ZmOperation.BROWSE, true);

	// we always enable sharing in case we're in multi-mbox mode
	this._setupSharingButton(parent, ZmOperation.SHARE_FOLDER);
	this._setupSharingButton(parent, ZmOperation.MOUNT_FOLDER);
};

ZmFolderTreeController.prototype._setupSharingButton =
function(parent, op) {
	button = parent.getOp(op);
	if (button) {
		if (appCtxt.get(ZmSetting.SHARING_ENABLED)) {
			button.setVisible(true);
			if (appCtxt.isOffline && !appCtxt.getActiveAccount().isZimbraAccount) {
				button.setEnabled(false);
			}
		} else {
			button.setVisible(false);
		}
	}
};

// Private methods

/*
* Returns ops available for "Folders" container.
*/
ZmFolderTreeController.prototype._getHeaderActionMenuOps =
function() {
	return [
		ZmOperation.NEW_FOLDER,
		ZmOperation.MOUNT_FOLDER,
		ZmOperation.EXPAND_ALL,
		ZmOperation.SYNC,
		ZmOperation.BROWSE
	];
};

/*
* Returns ops available for folder items.
*/
ZmFolderTreeController.prototype._getActionMenuOps =
function() {
	return [
		ZmOperation.NEW_FOLDER,
		ZmOperation.MARK_ALL_READ,
		ZmOperation.DELETE,
		ZmOperation.RENAME_FOLDER,
		ZmOperation.MOVE,
		ZmOperation.SHARE_FOLDER,
		ZmOperation.EDIT_PROPS,
		ZmOperation.EXPAND_ALL,
		ZmOperation.SYNC,
		ZmOperation.SYNC_ALL,
		ZmOperation.EMPTY_FOLDER,
		ZmOperation.SYNC_OFFLINE_FOLDER
	];
};

ZmFolderTreeController.prototype._getAllowedSubTypes =
function() {
	var types = {};
	types[ZmOrganizer.FOLDER] = true;
	types[ZmOrganizer.SEARCH] = true;
	return types;
};

/*
* Returns a "New Folder" dialog.
*/
ZmFolderTreeController.prototype._getNewDialog =
function() {
	return appCtxt.getNewFolderDialog();
};

/*
* Returns a "Rename Folder" dialog.
*/
ZmFolderTreeController.prototype._getRenameDialog =
function() {
	return appCtxt.getRenameFolderDialog();
};

/*
* Called when a left click occurs (by the tree view listener). The folder that
* was clicked may be a search, since those can appear in the folder tree. The
* appropriate search will be performed.
*
* @param folder		ZmOrganizer		folder or search that was clicked
*/
ZmFolderTreeController.prototype._itemClicked =
function(folder) {
	if (folder.type == ZmOrganizer.SEARCH) {
		// if the clicked item is a search (within the folder tree), hand
		// it off to the search tree controller
		var stc = this._opc.getTreeController(ZmOrganizer.SEARCH);
		stc._itemClicked(folder);
	} else {
		if (folder._showFoldersCallback) {
			folder._showFoldersCallback.run();
			return;
		}
		var searchFor = ZmId.SEARCH_MAIL;
		if (folder.isInTrash()) {
			var app = appCtxt.getCurrentAppName();
			// if other apps add Trash to their folder tree, set appropriate type here:
			if (app == ZmApp.CONTACTS) {
				searchFor = ZmItem.CONTACT;
			}
		}
		var getHtml = appCtxt.get(ZmSetting.VIEW_AS_HTML);
		// for Sync Failures folder, always show in traditional view
		var types = (folder.nId == ZmOrganizer.ID_SYNC_FAILURES) ? [ZmItem.MSG] : null;
		appCtxt.getSearchController().search({query:folder.createQuery(), searchFor:searchFor, getHtml:getHtml, types:types});
	}
};


// Actions

ZmFolderTreeController.prototype._doSync =
function(folder) {
	var dsc = AjxDispatcher.run("GetDataSourceCollection");
	var nFid = ZmOrganizer.normalizeId(folder.id);
	var dataSources = dsc.getItemsFor(nFid);

	if (dataSources.length > 0) {
		dsc.importMailFor(nFid);
	}
	else {
		ZmTreeController.prototype._doSync.call(this, folder);
	}
};

ZmFolderTreeController.prototype._syncFeeds =
function(folder) {
	if (!appCtxt.isOffline && folder && !folder.isFeed()) {
		var dataSources = (appCtxt.get(ZmSetting.POP_ACCOUNTS_ENABLED) || appCtxt.get(ZmSetting.IMAP_ACCOUNTS_ENABLED))
			? folder.getDataSources(null, true) : null;

		if (dataSources) {
			var dsc = AjxDispatcher.run("GetDataSourceCollection");
			dsc.importMail(dataSources);
			return;
		}
	}

	ZmTreeController.prototype._syncFeeds.call(this, folder);
};

/*
* Makes a request to add a new item to the tree.
*
* @param treeView	[ZmTreeView]	a tree view
* @param parentNode	[DwtTreeItem]	node under which to add the new one
* @param organizer	[ZmOrganizer]	organizer for the new node
* @param index		[int]*			position at which to add the new node
 */
ZmFolderTreeController.prototype._addNew =
function(treeView, parentNode, organizer, idx) {
	if (ZmFolder.HIDE_ID[organizer.id]) {
		return false;
	}
	return treeView._addNew(parentNode, organizer, idx);
};

// Listeners

/*
* Deletes a folder. If the folder is in Trash, it is hard-deleted. Otherwise, it
* is moved to Trash (soft-delete). If the folder is Trash or Junk, it is emptied.
* A warning dialog will be shown before the Junk folder is emptied.
*
* @param ev		[DwtUiEvent]	the UI event
*/
ZmFolderTreeController.prototype._deleteListener =
function(ev) {
	var organizer = this._getActionedOrganizer(ev);
	if (organizer.nId == ZmFolder.ID_SPAM || organizer.isInTrash()) {
		this._pendingActionData = organizer;
		var ds = this._deleteShield = appCtxt.getOkCancelMsgDialog();
		ds.reset();
		ds.registerCallback(DwtDialog.OK_BUTTON, this._deleteShieldYesCallback, this, organizer);
		ds.registerCallback(DwtDialog.CANCEL_BUTTON, this._clearDialog, this, this._deleteShield);
		var confirm = (organizer.type == ZmOrganizer.SEARCH) ? ZmMsg.confirmDeleteSavedSearch : (organizer.nId==ZmFolder.ID_TRASH)?ZmMsg.confirmEmptyTrashFolder:ZmMsg.confirmEmptyFolder;
		var msg = AjxMessageFormat.format(confirm, organizer.getName());
		ds.setMessage(msg, DwtMessageDialog.WARNING_STYLE);
		ds.popup();
	} else {
		this._doMove(organizer, appCtxt.getById(ZmFolder.ID_TRASH));
	}
};

/*
* Empties a folder.
* It removes all the items in the folder except sub-folders.
* If the folder is Trash, it empties even the sub-folders.
* A warning dialog will be shown before any folder is emptied.
*
* @param ev		[DwtUiEvent]	the UI event
*/
ZmFolderTreeController.prototype._emptyListener =
function(ev) {
	var organizer = this._pendingActionData = this._getActionedOrganizer(ev);
	var ds = this._emptyShield = appCtxt.getOkCancelMsgDialog();
	ds.reset();
	ds.registerCallback(DwtDialog.OK_BUTTON, this._emptyShieldYesCallback, this, organizer);
	ds.registerCallback(DwtDialog.CANCEL_BUTTON, this._clearDialog, this, this._emptyShield);
	var msg = (organizer.nId != ZmFolder.ID_TRASH)
		? (AjxMessageFormat.format(ZmMsg.confirmEmptyFolder, organizer.getName()))
		: ZmMsg.confirmEmptyTrashFolder;
	ds.setMessage(msg, DwtMessageDialog.WARNING_STYLE);

    var focusButtonId = (organizer.nId == ZmFolder.ID_TRASH || organizer.nId == ZmFolder.ID_SPAM) ?  DwtDialog.OK_BUTTON : DwtDialog.CANCEL_BUTTON;
    ds.associateEnterWithButton(focusButtonId);
    ds.popup(null, focusButtonId);


	if (!(organizer.nId == ZmFolder.ID_SPAM || organizer.isInTrash())) {
		var cancelButton = ds.getButton(DwtDialog.CANCEL_BUTTON);
		cancelButton.focus();
	}
};



/*
* Toggles on/off flag for syncing IMAP folder with server. Only for offline use.
*
* @param ev		[DwtUiEvent]	the UI event
*/
ZmFolderTreeController.prototype._syncOfflineFolderListener =
function(ev) {
	var folder = this._getActionedOrganizer(ev);
	if (folder) {
		folder.toggleSyncOffline();
	}
};
ZmFolderTreeController.prototype._browseListener =
function(ev){
	var folder = this._getActionedOrganizer(ev);
	if (folder) {
		AjxDispatcher.require("Browse");
		appCtxt.getSearchController().showBrowsePickers([ZmPicker.FOLDER]);
		//appCtxt.getSearchController()._browseViewController.addPicker(ZmPicker.FOLDER);
	}
}
/*
* Don't allow dragging of system folders.
*
* @param ev		[DwtDragEvent]		the drag event
*/
ZmFolderTreeController.prototype._dragListener =
function(ev) {
	if (ev.action == DwtDragEvent.DRAG_START) {
		var folder = ev.srcControl.getData(Dwt.KEY_OBJECT);
		ev.srcData = {data:folder, controller:this};
		if (!(folder instanceof ZmFolder) || folder.isSystem() || folder.isSyncIssuesFolder()) {
			ev.operation = Dwt.DND_DROP_NONE;
		}
	}
};

/*
* Handles the potential drop of something onto a folder. When something is dragged over
* a folder, returns true if a drop would be allowed. When something is actually dropped,
* performs the move. If items are being dropped, the source data is not the items
* themselves, but an object with the items (data) and their controller, so they can be
* moved appropriately.
*
* @param ev		[DwtDropEvent]		the drop event
*/
ZmFolderTreeController.prototype._dropListener =
function(ev) {
	var dropFolder = ev.targetControl.getData(Dwt.KEY_OBJECT);
	var data = ev.srcData.data;
	var isShiftKey = (ev.shiftKey || ev.uiEvent.shiftKey);

	if (ev.action == DwtDropEvent.DRAG_ENTER) {
		var type = ev.targetControl.getData(ZmTreeView.KEY_TYPE);
		if (data instanceof ZmFolder) {
			ev.doIt = dropFolder.mayContain(data, type) && !dropFolder.disallowSubFolder;
		} else if (data instanceof ZmTag) {
			ev.doIt = false; // tags cannot be moved
		} else {
			if (this._dropTgt.isValidTarget(data)) {
				ev.doIt = dropFolder.mayContain(data, type);

				var action;
				var actionData = (!(data instanceof Array)) ? [data] : data;

				// walk thru the array and find out what action is allowed
				for (var i = 0; i < actionData.length; i++) {
					if (actionData[i] instanceof ZmItem) {
						action |= actionData[i].getDefaultDndAction(isShiftKey);
					}
				}

				var plusDiv = (actionData.length == 1)
					? ev.dndProxy.firstChild.nextSibling
					: ev.dndProxy.firstChild.nextSibling.nextSibling;

				if (action && plusDiv) {
					// TODO - what if action is ZmItem.DND_ACTION_BOTH ??
					var isCopy = ((action & ZmItem.DND_ACTION_COPY) != 0);
					Dwt.setVisibility(plusDiv, isCopy);
				}
			} else {
				ev.doIt = false;
			}
		}
	} else if (ev.action == DwtDropEvent.DRAG_DROP) {
		if (data instanceof ZmFolder) {
			this._doMove(data, dropFolder);
		} else {
			var ctlr = ev.srcData.controller;
			var items = (data instanceof Array) ? data : [data];
			ctlr._doMove(items, dropFolder, null, !isShiftKey);
		}
	}
};

ZmFolderTreeController.prototype._shareAddrBookListener =
function(ev) {
	this._pendingActionData = this._getActionedOrganizer(ev);
	appCtxt.getSharePropsDialog().popup(ZmSharePropsDialog.NEW, this._pendingActionData);
};

ZmFolderTreeController.prototype._mountAddrBookListener =
function(ev) {
	appCtxt.getMountFolderDialog().popup(ZmOrganizer.FOLDER);
};


// Miscellaneous

/*
* Returns a title for moving a folder.
*/
ZmFolderTreeController.prototype._getMoveDialogTitle =
function() {
	return AjxMessageFormat.format(ZmMsg.moveFolder, this._pendingActionData.name);
};
