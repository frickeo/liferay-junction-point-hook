/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Layout;

/**
 * Schnittstelle für die Junction-Point Methoden.
 * @author frickeo
 *
 */
public interface JunctionPoint {

    /**
     * Name des Expando-Attributs für die Kennzeichnung einer Seite als JunctionPoint
     */
    public static final String JUNCTION_POINT_LAYOUT = "junction-point-layout";

    /**
     * Name des Expando-Attributs für die Zuordnung eines Navigationsastes zu einer JunctionPoint Seite.
     */
    public static final String JUNCTION_POINT_CONNECTION = "junction-point-connection";

	/**
	 * Liefert zu einem Junction Point Layout die Liste aller verbundenen Layouts
	 * @param layout Das Junction Point Layout
	 * @return Eine (sortierte) Liste mit allen verbundenen Layouts
	 * @throws SystemException bei einem Fehler
	 * @throws PortalException bei einem Fehler
	 */
	public abstract List<Layout> getConnectedLayouts(Layout layout)
			throws SystemException, PortalException;

    /**
     * Liefert die Liste aller Junction Point Layouts, die in der Site des Layouts oder einer
     * übergeordneten Site liegen
     * 
     * @param layout Das aktuelle Layout
     * @return die Liste aller passenden Junction Point Layouts
     * @throws SystemException bei einem Fehler
     */
    public abstract List<Layout> getUsableJunctionPoints(Layout layout) throws SystemException, PortalException;

	/**
	 * Prüft, ob das Layout als Junction Point Layout konfiguriert ist.
	 * @param layout Das Layout
	 * @return true, falls das Expando-Attribute 'Junction Point Layout' gesetzt ist.
	 */
	public abstract boolean isJunctionPointLayout(Layout layout);

    /**
     * Lookup the target of a junction point by inspecting the session.
     * <p>
     * This lookup uses the stored session values to support multiple used junction points.
     * </p>
     * 
     * @param layout The requested layout
     * @param request The request
     * @param ignoreHiddenValue A target layout should be searched even for hidden junction points.
     * @return the junction target layout for a junction point or the original layout, if no
     *         junction point is requested or no target is known.
     * @throws PortalException
     * @throws SystemException
     */
    public abstract Layout getJunctionTarget(Layout layout, HttpServletRequest request, boolean ignoreHiddenValue) throws SystemException;

    public abstract Layout getConfiguredJunctionPointLayout(Layout layout);

    public abstract List<Layout> getJunctionedAncestors(Layout layout, HttpServletRequest req);

    public abstract Layout getJunctionedAncestor(Layout layout, HttpServletRequest req);

}
