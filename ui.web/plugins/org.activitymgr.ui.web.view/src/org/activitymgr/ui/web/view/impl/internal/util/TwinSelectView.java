/*
 * Copyright (c) 2004-2025, Jean-Francois Brazeau and Obeo.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.activitymgr.ui.web.view.impl.internal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic.View;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TwinSelectView extends HorizontalLayout implements View {

	private ITwinSelectFieldLogic logic;
	private ListSelect leftSelect;
	private ListSelect rightSelect;
	private Button moveAllRightButton;
	private Button moveRightButton;
	private Button moveLeftButton;
	private Button moveAllLeftButton;
	private List<String> availableItemIds = new ArrayList<String>();
	Map<String, String> labels = new HashMap<String, String>();
	private Button moveUpButton;
	private Button moveDownButton;
	private boolean orderMode;
	private boolean logicNotificationEnabled = false;

	public TwinSelectView() {
		super();
		// Left select
		leftSelect = newSelect();


		// Middle buttons
		VerticalLayout selectButtons = new VerticalLayout();
		selectButtons.setMargin(new MarginInfo(false, true));
		addComponent(selectButtons);
		moveAllRightButton = addSelectButton(selectButtons, ">>", event -> selectAll());
		moveRightButton = addSelectButton(selectButtons, ">",
				event -> moveSelectedItems(leftSelect, rightSelect));
		moveLeftButton = addSelectButton(selectButtons, "<",
				event -> moveSelectedItems(rightSelect, leftSelect));
		moveAllLeftButton = addSelectButton(selectButtons, "<<", event -> deselectAll());

		// Create order buttons (but don't add it to the UI)
		moveUpButton = createRightMoveButton("Up", 1, (ids, selects) -> {
			String first = selects.get(0);
			return ids.indexOf(first) - 1;
		});
		
		moveDownButton = createRightMoveButton("Down", -1, (ids, selects) -> {
			String last = selects.get(selects.size() - 1);
			return ids.indexOf(last) + 1;
		});
		
		// Right select
		rightSelect = newSelect();

		logicNotificationEnabled = true;
	}
	
	private Button createRightMoveButton(String label, int direction, 
			BiFunction<List<Object>, List<String>, Integer> indexToMove) {
		Button result = new Button(label);
		result.setImmediate(true);
		result.setWidth("75px");
		result.addClickListener(event -> {
			// Moving down a joined group of items is equivalent to move the
			// item that is just after the group, before the group
			List<Object> rightItemIds = new ArrayList<>(rightSelect.getItemIds());
			List<String> rightSelectedItems = getRightSelectedItemIds();

			// Retrieve the item that is after the group
			int idx = indexToMove.apply(rightItemIds, rightSelectedItems);
			String itemIdToMove = (String) rightItemIds.get(idx);

			// Move the item after the group
			int newIdx = idx + direction * rightSelectedItems.size();
			moveRightSelectItemUpOrDown(itemIdToMove, newIdx);
		});
		return result;
	}

	private void moveRightSelectItemUpOrDown(String itemId, int newIdx) {
		IndexedContainer container = ((IndexedContainer) rightSelect
				.getContainerDataSource());
		container.removeItem(itemId);
		container.addItemAt(newIdx, itemId);
		rightSelect.setItemCaption(itemId, labels.get(itemId));

		// Update buttons
		updateButtonsEnablement();

		notifyChangeToLogic();
	}

	@Override
	public void showOrderButton() {
		this.orderMode = true;
		// Right buttons
		VerticalLayout orderButtons = new VerticalLayout(moveUpButton, moveDownButton);
		orderButtons.setMargin(new MarginInfo(false, true));
		addComponent(orderButtons);
	}

	@Override
	public void addAvailableEntry(String id, String label) {
		availableItemIds.add(id);
		labels.put(id, label);
		leftSelect.addItem(id);
		leftSelect.setItemCaption(id, label);
		updateButtonsEnablement();
	}

	private void enableForContent(Button action, Collection<?> content) {
		action.setEnabled(!content.isEmpty());
	}
	
	private void updateButtonsEnablement() {
		List<Object> rightItemIds = new ArrayList<Object>(
				rightSelect.getItemIds());
		List<String> selectedItems = getRightSelectedItemIds();

		// Middle buttons enablement
		enableForContent(moveAllRightButton, leftSelect.getItemIds());
		enableForContent(moveRightButton, (Collection<?>) leftSelect.getValue());
		
		enableForContent(moveLeftButton, selectedItems);
		enableForContent(moveAllLeftButton, rightItemIds);

		if (!orderMode) {
			return;
		}
		// Up and down buttons are enabled only if at least one item is selected
		// and if all selected items are joined and if it's not the last items
		// for down button and not the first items for up button
		moveUpButton.setEnabled(false);
		moveDownButton.setEnabled(false);
		if (!selectedItems.isEmpty()) {
			Iterator<String> iSelect = selectedItems
					.iterator();
			String firstSelectedItemId = iSelect.next();
			int firstIdx = rightItemIds.indexOf(firstSelectedItemId);
			boolean joined = true;
			int idx = firstIdx + 1;
			while (iSelect.hasNext()) {
				String next = iSelect.next();
				String nextItemId = (String) rightItemIds.get(idx++);
				if (!next.equals(nextItemId)) {
					joined = false;
					break;
				}
			}
			if (joined) {
				moveUpButton.setEnabled(firstIdx != 0);
				boolean isAtLast = firstIdx + selectedItems.size() 
						== rightItemIds.size();
				moveDownButton.setEnabled(!isAtLast);
			}
		}
	}

	private List<String> getRightSelectedItemIds() {
		final List<Object> rightItemIds = new ArrayList<>(
				rightSelect.getItemIds());
		
		@SuppressWarnings("unchecked")
		List<String> rightSelectedItems = new ArrayList<>(
				(Collection<String>) rightSelect.getValue());
		
		
		Collections.sort(rightSelectedItems, Comparator.comparingInt(rightItemIds::indexOf));
		return rightSelectedItems;
	}

	private Button addSelectButton(VerticalLayout container, String title, Button.ClickListener task) {
		Button result = new Button(title);
		result.setImmediate(true);
		result.setWidth("50px");
		// result.setWidth("100%");
		container.addComponent(result);
		container.setComponentAlignment(result, Alignment.MIDDLE_CENTER);
		result.setEnabled(false);
		
		return result;
	}

	private ListSelect newSelect() {
		ListSelect select = new ListSelect();
		select.setImmediate(true);
		select.setMultiSelect(true);
		select.setWidth("130px");
		
		select.setHeight("100px");
		select.addValueChangeListener(event -> updateButtonsEnablement());
		addComponent(select);
		return select;
	}

	@Override
	public void registerLogic(ITwinSelectFieldLogic logic) {
		this.logic = logic;
	}

	private void moveAllItemTo(ListSelect select) {
		// Clear both selects
		leftSelect.removeAllItems();
		rightSelect.removeAllItems();
		// Add all entries to the given select
		for (String id : availableItemIds) {
			select.addItem(id);
			select.setItemCaption(id, labels.get(id));
		}
		// Update buttons
		updateButtonsEnablement();
		notifyChangeToLogic();
	}

	@SuppressWarnings("unchecked")
	private void moveSelectedItems(ListSelect from, ListSelect destination) {
		// If we move from left to right, and if up and down buttons are shown,
		// simply append the items at the end. Otherwise, preserve the original
		// order
		boolean doNotPreserveOriginalOrder = (destination == rightSelect && orderMode);
		IndexedContainer destinationContainer = ((IndexedContainer) destination
				.getContainerDataSource());
		Collection<String> itemIdsToMove = (Collection<String>) from.getValue();
		for (String itemIdToMove : itemIdsToMove) {
			if (!doNotPreserveOriginalOrder) {
				int globalItemIdToMoveIndex = availableItemIds
						.indexOf(itemIdToMove);
				int idx = 0;
				Collection<String> actualItemIds = (Collection<String>) destination
						.getItemIds();
				for (String actualItemId : actualItemIds) {
					int globalActualItemId = availableItemIds
							.indexOf(actualItemId);
					if (globalItemIdToMoveIndex < globalActualItemId) {
						break;
					} else {
						idx++;
					}
				}
				destinationContainer.addItemAt(idx, itemIdToMove);
			} else {
				destinationContainer.addItem(itemIdToMove);
			}
			destination.setItemCaption(itemIdToMove, labels.get(itemIdToMove));
			from.removeItem(itemIdToMove);
		}
		// Update buttons
		updateButtonsEnablement();
		// Notify the logic
		notifyChangeToLogic();
	}

	@Override
	public void selectAll() {
		moveAllItemTo(rightSelect);
	}

	private void deselectAll() {
		moveAllItemTo(leftSelect);
	}

	@Override
	public void setValue(Collection<String> value) {
		logicNotificationEnabled = false;
		try {
			if (orderMode) {
				// Remove elements from the right
				List<Object> itemsToRemove = new ArrayList<Object>();
				for (Object itemId : rightSelect.getItemIds()) {
					if (!value.contains(itemId)) {
						itemsToRemove.add(itemId);
					}
				}
				for (Object itemId : itemsToRemove) {
					rightSelect.removeItem(itemId);
					leftSelect.addItem(itemId);
					leftSelect.setItemCaption(itemId, labels.get(itemId));
				}

				// In order mode, simply move elements
				// (don't use move function, or value order
				// will not be preserved)
				for (String itemId : value) {
					if (leftSelect.containsId(itemId)) {
						leftSelect.removeItem(itemId);
						rightSelect.addItem(itemId);
						rightSelect.setItemCaption(itemId, labels.get(itemId));
					}
				}
				// Notify update
				notifyChangeToLogic();
			} else {
				// Remove elements from the right
				for (Object itemId : rightSelect.getItemIds()) {
					if (!value.contains(itemId)) {
						rightSelect.select(itemId);
					}
				}

				moveSelectedItems(rightSelect, leftSelect);
				// In non order mode, reuse move function
				// (so that elements will be sorted)
				for (String itemId : value) {
					if (leftSelect.containsId(itemId)) {
						leftSelect.select(itemId);
					}
				}
				moveSelectedItems(leftSelect, rightSelect);
			}
		} finally {
			logicNotificationEnabled = true;
		}
	}

	private void notifyChangeToLogic() {
		if (logicNotificationEnabled) {
			@SuppressWarnings("unchecked")
			Collection<String> itemIds = (Collection<String>) rightSelect
					.getItemIds();
			logic.onValueChanged(itemIds);
		}
	}

	@Override
	public void focus() {
		super.focus();
	}
}
