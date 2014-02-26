/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;

/**
 * Implementierung für die Junction-Point Methoden.
 * 
 * @author frickeo
 */
public class JunctionPointImpl implements JunctionPoint {

    private static final String CURRENT_JUNCTIONED_ANCESTORS = JunctionPointUtil.class.getName() + "_JUNCTIONED_ANCESTORS_";

    @Override
    public boolean isJunctionPointLayout(Layout layout) {
        Serializable value = layout.getExpandoBridge().getAttribute(JUNCTION_POINT_LAYOUT, false);
        return Boolean.TRUE.equals(value);
    }

    @Override
    public List<Layout> getUsableJunctionPoints(Layout layout) throws SystemException, PortalException {
        long companyId = CompanyThreadLocal.getCompanyId();
        long classNameId = ClassNameLocalServiceUtil.getClassNameId(Layout.class);
        // Ermittle die Menge aller erlaubten GroupIds
        Set<Long> usableGroupIds = getHierarchicalGroupIds(layout);

        List<Layout> layouts = new ArrayList<Layout>();
        List<ExpandoValue> values = ExpandoValueLocalServiceUtil.getColumnValues(companyId, classNameId,
                ExpandoTableConstants.DEFAULT_TABLE_NAME, JUNCTION_POINT_LAYOUT, Boolean.TRUE.toString(), -1, -1);
        for (ExpandoValue expandoValue : values) {
            long classPK = expandoValue.getClassPK();
            Layout jpLayout = LayoutLocalServiceUtil.fetchLayout(classPK);
            if (jpLayout != null && usableGroupIds.contains(jpLayout.getGroupId())) {
                layouts.add(jpLayout);
            }
        }

        sortLayouts(layouts);
        return layouts;
    }

    /**
     * Ermittelt eine Menge mit den GroupIds der Layout-Site sowie aller übnergeordneter Sites.
     * 
     * @param layout Das Einstiegslayout
     * @return Eine Menge von GroupIds
     * @throws SystemException
     */
    protected Set<Long> getHierarchicalGroupIds(Layout layout) throws SystemException {
        // Ermittle die Menge aller erlaubten GroupIds
        Set<Long> usableGroupIds;
        try {
            Group group = layout.getGroup();
            usableGroupIds = new HashSet<Long>();
            while (group != null) {
                usableGroupIds.add(group.getGroupId());
                group = group.getParentGroup();
            }
        }
        catch (PortalException e) {
            // Behandlung einer PortalException
            throw new RuntimeException(e);
        }
        return usableGroupIds;
    }

    @Override
    public List<Layout> getConnectedLayouts(Layout layout) throws SystemException {
        long companyId = layout.getCompanyId();
        Group group;
        try {
            group = layout.getGroup();
        }
        catch (PortalException e) {
            // Behandlung einer PortalException
            throw new RuntimeException(e);
        }
        String junctionPointKey = group.getUuid() + "/" + layout.getLayoutId();

        long classNameId = ClassNameLocalServiceUtil.getClassNameId(Layout.class);
        List<Layout> layouts = new ArrayList<Layout>();
        List<ExpandoValue> values = ExpandoValueLocalServiceUtil.getColumnValues(companyId, classNameId,
                ExpandoTableConstants.DEFAULT_TABLE_NAME, JUNCTION_POINT_CONNECTION, junctionPointKey, -1, -1);
        for (ExpandoValue expandoValue : values) {
            long classPK = expandoValue.getClassPK();
            Layout connectedLayout = LayoutLocalServiceUtil.fetchLayout(classPK);
            if (connectedLayout != null) {
                layouts.add(connectedLayout);
            }
        }

        sortLayouts(layouts);
        return layouts;
    }

    protected List<Layout> getAllowedConnectedLayouts(Layout layout) throws SystemException {
        // Ermittle alle verbundenen JunctionPoints
        List<Layout> layouts = getConnectedLayouts(layout);

        // nicht erlaubte Layouts entfernen
        Iterator<Layout> layoutIterator = layouts.iterator();
        while (layoutIterator.hasNext()) {
            Layout connectedLayout = layoutIterator.next();
            // Ermittle die Menge aller erlaubten GroupIds
            Set<Long> usableGroupIds = getHierarchicalGroupIds(connectedLayout);
            if (!usableGroupIds.contains(layout.getGroupId())) {
                layoutIterator.remove();
            }
        }

        return layouts;
    }

    /**
     * Calculates the key that is used to store the usage of a junction target in the session.
     * 
     * @param layout The junction point layout
     * @return An attribute name for storing the target layout for a junction point
     */
    protected String getAttributeName(long plid) {

        return "junction_target_" + plid;
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
    @Override
    public Layout getJunctionTarget(Layout layout, HttpServletRequest request, boolean ignoreHiddenValue) throws SystemException {

        if (layout == null) {
            return null;
        }

        Layout targetLayout = layout;
        HttpSession session = (request == null) ? null : request.getSession(false);

        boolean isJunctionPoint = isJunctionPointLayout(layout);

        while (session != null && isJunctionPoint) {

            // Check if a junction point is defined for the top ancestor
            String attributeName = getAttributeName(targetLayout.getPlid());
            Object targetPlid = session.getAttribute(attributeName);
            // if no target is known yet for the current junction point AND
            // exactly one target is
            // configured, that target is used and stored as junction target.
            // This automatism is
            // only done for visible layouts, unless overridden by the caller.
            if (targetPlid == null && (!targetLayout.getHidden() || ignoreHiddenValue)) {
                List<Layout> targets = getAllowedConnectedLayouts(targetLayout);
                if (targets.size() == 1) {
                    targetPlid = new Long((targets.get(0)).getPlid());
                    session.setAttribute(attributeName, targetPlid);
                } else {
                    // andernfalls automatisch zum "ersten" Target springen,
                    // falls alle Targets zur gleichen Gruppe gehören
                    long minTargetPlid = 0;
                    int minPriority = Integer.MAX_VALUE;
                    for (Layout target : targets) {
                        // Überprüfung, ob alle JunctionTarget die selbe GroupId
                        // haben wie der JunctionPoint
                        if (targetLayout.getGroupId() != target.getGroupId()) {
                            minTargetPlid = 0;
                            break;
                        }

                        // Bestimmung des Layouts mit der kleinesten Priorität
                        if (target.getPriority() < minPriority) {
                            minPriority = target.getPriority();
                            minTargetPlid = target.getPlid();
                        }
                    }
                    // Falls eine Seite gefunden wurde, wird diese in der
                    // Session vermerkt.
                    if (minTargetPlid != 0) {
                        targetPlid = Long.valueOf(minTargetPlid);
                        session.setAttribute(attributeName, targetPlid);
                    } else {
                        session.setAttribute(attributeName, Boolean.FALSE);
                    }
                }
            }

            // Continue the lookup if the junction target is another junction
            // point itself
            if (targetPlid instanceof Long) {
                long plid = ((Long) targetPlid).longValue();
                if (plid != targetLayout.getPlid()) {
                    try {
                        targetLayout = LayoutLocalServiceUtil.getLayout(plid);
                        isJunctionPoint = isJunctionPointLayout(targetLayout);
                        continue;
                    }
                    catch (NoSuchLayoutException e) {
                        // Session value is invalid; remove it
                        session.removeAttribute(attributeName);
                    }
                    catch (PortalException e) {
                        throw new RuntimeException(e);
                    }
                    catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            break;
        }

        return targetLayout;
    }

    /**
     * Lookup the configured junction point to be used instead of the given layout or null, if no
     * junction point is configured at all.
     * 
     * @param layout The layout that may have configured a junction point to be used
     * @return The connected Layout or null
     */
    @Override
    public Layout getConfiguredJunctionPointLayout(Layout layout) {

        // only top level layouts may use a junction point
        if (layout.getParentLayoutId() != LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
            return null;
        }

        Layout junctionPointLayout = null;

        // lookup the junction point configuration value
        String configValue = (String) layout.getExpandoBridge().getAttribute(JUNCTION_POINT_CONNECTION, false);
        if (Validator.isNotNull(configValue)) {
            // the configuration consists of the group uuid and the layout id.
            String[] configParts = configValue.split("/");
            if (configParts.length == 2) {
                Group group;
                try {
                    group = GroupLocalServiceUtil.fetchGroupByUuidAndCompanyId(configParts[0], layout.getCompanyId());
                    long layoutId = GetterUtil.getLong(configParts[1]);
                    if (group != null && layoutId != GetterUtil.DEFAULT_LONG) {
                        junctionPointLayout = LayoutLocalServiceUtil.fetchLayout(group.getGroupId(), layout.getPrivateLayout(), layoutId);
                    }
                }
                catch (SystemException e) {
                    ; // ignore (the config is outdated)
                }
            }
        }

        return junctionPointLayout;
    }

    /**
     * Lookup the top level layout of a given layout under consideration of junction points. If the
     * top level layout is a junction point layout, that layout is returned.
     * 
     * @param layout The layout to be shown
     * @param req The request
     * @return The top level layout under consideration of junction point layout pages
     */
    @Override
    public Layout getJunctionedAncestor(Layout layout, HttpServletRequest req) {

        List<Layout> ancestors = getJunctionedAncestors(layout, req);
        if (ancestors != null && ancestors.size() > 0) {
            return ancestors.get(ancestors.size() - 1);
        } else {
            return layout;
        }
    }

    /**
     * Lookup all ancestors of a given layout the top level layout under consideration of junction
     * points. Please note that the list does not include the layout itself.
     * 
     * @param layout The layout to be shown
     * @param req The request
     * @return The ancestor layouts under consideration of junction point layout pages
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Layout> getJunctionedAncestors(Layout layout, HttpServletRequest req) {

        HttpSession session = req.getSession();
        long junctionPointPlid = (layout != null) ? layout.getPlid() : GetterUtil.DEFAULT_LONG;
        String ancestorsKey = CURRENT_JUNCTIONED_ANCESTORS + junctionPointPlid;

        // check if the top level layout has already been calculated for the current request
        List<Layout> ancestors = (List<Layout>) req.getAttribute(ancestorsKey);
        if (ancestors != null) {
            return ancestors;
        }

        ancestors = new ArrayList<Layout>();

        Layout curLayout = layout;

        // we need to make sure that we don't loop forever if a community
        // junctions itself or any other kind of junction cycle exists
        Set<Long> visitedPlids = new HashSet<Long>();

        // Calculate all the ancestors with respect to junction points
        while (junctionPointPlid != GetterUtil.DEFAULT_LONG) {
            // always add the current layout as the first one
            ancestors.add(curLayout);
            // get the ancestors for the current layout
            // (getAncestors will return an empty list for root layouts)
            try {
                List<Layout> curAncestors = curLayout.getAncestors();
                ancestors.addAll(curAncestors);
            }
            catch (SystemException e) {
                throw new RuntimeException(e);
            }
            catch (PortalException e) {
                throw new RuntimeException(e);
            }

            // Check if a junction point is defined for the top ancestor
            Layout topLevelLayout = (Layout) ancestors.get(ancestors.size() - 1);
            Layout junctionPoint = getConfiguredJunctionPointLayout(topLevelLayout);
            junctionPointPlid = (junctionPoint != null) ? junctionPoint.getPlid() : GetterUtil.DEFAULT_LONG;

            // If a target is defined go to that community and search for the next top ancestor
            if (junctionPointPlid != GetterUtil.DEFAULT_LONG) {
                // exit the loop if the junction point layout has been visited earlier
                if (!visitedPlids.add(new Long(junctionPointPlid))) {
                    break;
                }

                // make the junction point layout the new current layout
                curLayout = junctionPoint;

                // set a session attribute to remember the junction layout used last.
                Long junctionTargetPlid = new Long(topLevelLayout.getPlid());
                String lastJunctionTarget = getAttributeName(junctionPointPlid);
                session.setAttribute(lastJunctionTarget, junctionTargetPlid);
            }
        }

        // store the calculated ancestors list
        req.setAttribute(ancestorsKey, ancestors);

        return ancestors;
    }

    protected void sortLayouts(List<Layout> layouts) {
        Comparator<Layout> comparator = new Comparator<Layout>() {

            @Override
            public int compare(Layout o1, Layout o2) {
                if (o1.getGroupId() == o2.getGroupId()) {
                    try {
                        return o1.getGroup().getFriendlyURL().compareTo(o2.getGroup().getFriendlyURL());
                    }
                    catch (PortalException e) {
                        throw new RuntimeException(e);
                    }
                    catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return (int) (o1.getLayoutId() - o2.getLayoutId());
                }
            }
        };

        Collections.sort(layouts, comparator);
    }
}
