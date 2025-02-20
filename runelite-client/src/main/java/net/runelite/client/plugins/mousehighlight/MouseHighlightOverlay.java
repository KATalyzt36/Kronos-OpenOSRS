/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.mousehighlight;

import com.google.common.base.Strings;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Singleton
@Slf4j
class MouseHighlightOverlay extends Overlay
{
	private final TooltipManager tooltipManager;
	private final Client client;
	private final MouseHighlightPlugin plugin;
	private final OverlayManager overlayManager;

	@Inject
	MouseHighlightOverlay(final Client client, final TooltipManager tooltipManager, final MouseHighlightPlugin plugin, final OverlayManager overlayManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.plugin = plugin;
		this.overlayManager = overlayManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.isMenuOpen())
		{
			return null;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		int last = menuEntries.length - 1;

		if (last < 0)
		{
			return null;
		}

		MenuEntry menuEntry = menuEntries[last];
		String target = menuEntry.getTarget();
		String option = menuEntry.getOption();
		int type = menuEntry.getOpcode();

		if (shouldNotRenderMenuAction(type))
		{
			return null;
		}

		if (Strings.isNullOrEmpty(option))
		{
			return null;
		}

		// Trivial options that don't need to be highlighted, add more as they appear.
		switch (option)
		{
			case "Walk here":
			case "Cancel":
			case "Continue":
				return null;
			case "Move":
				// Hide overlay on sliding puzzle boxes
				if (target.contains("Sliding piece"))
				{
					return null;
				}
		}

		final int widgetId = menuEntry.getParam1();
		final int groupId = WidgetInfo.TO_GROUP(widgetId);
		final int childId = WidgetInfo.TO_CHILD(widgetId);
		final Widget widget = client.getWidget(groupId, childId);

		StringBuilder sb = new StringBuilder();
		sb.append(option).append(Strings.isNullOrEmpty(target) ? "" : " " + target).append("</col>");

		if (!plugin.isUiTooltip() && widget != null)
		{
			return null;
		}

		if (!plugin.isChatboxTooltip() && groupId == WidgetInfo.CHATBOX.getGroupId())
		{
			return null;
		}

		if (widget != null)
		{
			// If this varc is set, some CS is showing tooltip
			int tooltipTimeout = client.getVar(VarClientInt.TOOLTIP_TIMEOUT);
			if (tooltipTimeout > client.getGameCycle())
			{
				return null;
			}
		}

		if (widget == null && !plugin.isMainTooltip())
		{
			return null;
		}

		// If this varc is set, a tooltip is already being displayed
		int tooltipDisplayed = client.getVar(VarClientInt.TOOLTIP_VISIBLE);
		if (tooltipDisplayed == 1)
		{
			return null;
		}

		if (widget != null) {
			int slot = menuEntry.getParam0();
			WidgetItem widgetItem = widget.getWidgetItem(slot);
			if (widgetItem != null) {
				String[] attributes = widgetItem.getAttributes();
				if (attributes != null) {
					sb.append("</br>");
					for (String attribute : attributes) {
						if (attribute != null && !attribute.equalsIgnoreCase("empty")) {
							sb.append("</br>").
							append("Upgrade: ").
							append("<col=").
							append(attribute.charAt(0) == '-' ? "ff0000" : "00ff00").
							append(">").
							append(attribute.replace("-", "").replace("+", "")).
							append("</col>");
						}
					}
				}
			}
		}

		tooltipManager.addFront(new Tooltip(sb.toString()));

		return null;
	}

	private boolean shouldNotRenderMenuAction(int type)
	{
		return type == MenuOpcode.RUNELITE_OVERLAY.getId()
				|| (!plugin.isRightClickTooltipEnabled() && isMenuActionRightClickOnly(type));
	}

	private boolean isMenuActionRightClickOnly(int type)
	{
		return type == MenuOpcode.EXAMINE_ITEM_BANK_EQ.getId();
	}
}
