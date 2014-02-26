/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ReferenceRegistry;
import com.liferay.portal.model.Layout;

/**
 * Util-Klasse mit statischen Methden für den JunctionPoint. Die Verdrahtung passiert per
 * Spring-Konfiguration in ext-spring.xml. Dies erfolgt wie unter
 * <code>http://www.liferay.com/de/community/wiki/-/wiki/Main/Adding+Spring+Capabilitites+to+Hook</code>
 * beschrieben.
 * 
 * @author frickeo
 */
public class JunctionPointUtil {

    private static JunctionPoint junctionPoint;

    public static JunctionPoint getJunctionPoint() {
        if (junctionPoint == null) {
            junctionPoint = (JunctionPoint) PortalBeanLocatorUtil.locate(JunctionPoint.class.getName());

            ReferenceRegistry.registerReference(JunctionPointUtil.class, "junctionPoint");
        }

        return junctionPoint;
    }

    public void setJunctionPoint(JunctionPoint junctionPoint) {
        JunctionPointUtil.junctionPoint = junctionPoint;
    }

    /**
     * Liefert zu einem Junction Point Layout die Liste aller verbundenen Layouts
     * 
     * @param layout Das Junction Point Layout
     * @return Eine (sortierte) Liste mit allen verbundenen Layouts
     * @throws SystemException bei einem Fehler
     * @throws PortalException bei einem Fehler
     */
    public static List<Layout> getConnectedLayouts(Layout layout) throws SystemException, PortalException {
        return getJunctionPoint().getConnectedLayouts(layout);
    }

    /**
     * Liefert die Liste aller Junction Point Layouts, die in der Site des Layouts oder einer
     * übergeordneten Site liegen
     * 
     * @param layout Das aktuelle Layout
     * @return die Liste aller passenden Junction Point Layouts
     * @throws SystemException bei einem Fehler
     */
    public static List<Layout> getUsableJunctionPoints(Layout layout) throws SystemException, PortalException {
        return getJunctionPoint().getUsableJunctionPoints(layout);
    }

    /**
     * Prüft, ob das Layout als Junction Point Layout konfiguriert ist.
     * 
     * @param layout Das Layout
     * @return true, falls das Expando-Attribute 'Junction Point Layout' gesetzt ist.
     */
    public static boolean isJunctionPointLayout(Layout layout) {
        return getJunctionPoint().isJunctionPointLayout(layout);
    }

    /**
     * Lookup the configured junction point to be used instead of the given
     * layout or null, if no junction point is configured at all.
     * 
     * @param layout The layout that may have configured a junction point to be used
     * @return The connected Layout or null
     */
    public static Layout getConfiguredJunctionPointLayout(Layout layout) {
        return getJunctionPoint().getConfiguredJunctionPointLayout(layout);
    }

    /**
     * Lookup the top level layout of a given layout under consideration of junction points. If the
     * top level layout is a junction point layout, that layout is returned.
     * 
     * @param layout The layout to be shown
     * @param req The request
     * @return The top level layout under consideration of junction point layout pages
     */
    public static Layout getJunctionedAncestor(Layout layout, HttpServletRequest req) {
        return getJunctionPoint().getJunctionedAncestor(layout, req);
    }
    /**
     * Lookup all ancestors of a given layout the top level layout under consideration of junction
     * points. Unlike the getAncestors method of the LayoutImpl class the list includes the given
     * layout as first layout.
     * 
     * @param layout The layout to be shown
     * @param req The request
     * @return The ancestor layouts under consideration of junction point layout pages
     */
    public static List<Layout> getJunctionedAncestors(Layout layout, HttpServletRequest req) {
        return getJunctionPoint().getJunctionedAncestors(layout, req);
    }

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
    public static Layout getJunctionTarget(Layout layout, HttpServletRequest request, boolean ignoreHiddenValue) throws SystemException {
        return getJunctionPoint().getJunctionTarget(layout, request, ignoreHiddenValue);
    }
}
