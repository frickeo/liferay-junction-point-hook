/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.hook.model;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutWrapper;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.ServiceContextThreadLocal;

import de.hansemerkur.liferay.junctionpoint.util.JunctionPointUtil;

/**
 * Diese Klasse dient dazu, bei der Ermittlung übergeordneter oder untergeordneter Layout die
 * verwendeten Junction Points zu berücksichtigen. Dazu werden die Methoden <code>getChildren</code>
 * , <code>getParentLayoutId</code>, <code>getAncestorPlid</code> sowie <code>getAncestors</code>
 * angepasst. Für die korrekte Darstellung in der Top-Level Navigation und im Navigationsportlet
 * wird zudem eine angepasste Methode <code>isSelected</code> benötigt (Aufruf aus NavItem).
 * 
 * @author FRICKEO
 * @author $LastChangedBy: $
 * @version $LastChangedRevision: $
 */
public class JunctionPointLayoutWrapper extends LayoutWrapper {

    /**
     * <code>serialVersionUID</code>.
     */
    private static final long serialVersionUID = -5288479677168349603L;

    public JunctionPointLayoutWrapper(Layout layout) {
        super(layout);
        if (layout == null) {
            throw new NullPointerException("The wrapped layout must not be null!");
        }
    }

    @Override
    public long getAncestorPlid() throws PortalException, SystemException {

        HttpServletRequest request = ServiceContextThreadLocal.getServiceContext().getRequest();
        Layout junctionedAncestor = JunctionPointUtil.getJunctionedAncestor(getWrappedModel(), request);

        return junctionedAncestor.getPlid();
    }

    @Override
    public List<Layout> getAncestors() throws PortalException, SystemException {

        HttpServletRequest request = ServiceContextThreadLocal.getServiceContext().getRequest();
        // alle übergeordneten Seiten einschließlich der aktuellen Seite holen
        List<Layout> junctionedAncestors = JunctionPointUtil.getJunctionedAncestors(getWrappedModel(), request);
        // Eine neue Menge aufbauen ohne die aktuelle Seite sowie die per Junction Point verknüpften Seiten
        List<Layout> layouts = new ArrayList<Layout>();
        for (int i = 1; i < junctionedAncestors.size(); i++) {
            Layout layout = junctionedAncestors.get(i);
            if (JunctionPointUtil.getConfiguredJunctionPointLayout(layout) != null) {
                continue;
            }

            layouts.add(layout);
        }

        wrapLayouts(layouts);

        return layouts;
    }

    @Override
    public List<Layout> getChildren(PermissionChecker permissionChecker) throws SystemException, PortalException {

        HttpServletRequest request = ServiceContextThreadLocal.getServiceContext().getRequest();
        List<Layout> children;

        Layout junctionTarget = JunctionPointUtil.getJunctionTarget(this, request, false);
        if (junctionTarget == this) {
            children = super.getChildren(permissionChecker);
        } else {
            children = junctionTarget.getChildren(permissionChecker);
        }

        wrapLayouts(children);

        return children;
    }

    @Override
    public List<Layout> getChildren() throws SystemException {

        HttpServletRequest request = ServiceContextThreadLocal.getServiceContext().getRequest();
        List<Layout> children;

        Layout junctionTarget = JunctionPointUtil.getJunctionTarget(this, request, false);
        if (junctionTarget == this) {
            children = super.getChildren();
        } else {
            children = junctionTarget.getChildren();
        }
        children = new ArrayList<Layout>(children);

        wrapLayouts(children);

        return children;
    }

    @Override
    public long getParentLayoutId() {

        long parentLayoutId = super.getParentLayoutId();
        if (parentLayoutId == LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
            Layout junctionPointLayout = JunctionPointUtil.getConfiguredJunctionPointLayout(getWrappedModel());
            if (junctionPointLayout != null && junctionPointLayout.getParentLayoutId() != LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
            	// HACK! Das Breadcrumb-Portlet fragt erst die ParentLayoutId ab und ruft danach getParentLayout, falls die ID != 0 ist.
            	// andere Klassen machen dies aber leider ganz anders.
            	StackTraceElement stackTraceElement = new Exception().getStackTrace()[1];
            	if ("com.liferay.taglib.ui.BreadcrumbTag".equals(stackTraceElement.getClassName())) {
            		return -1;
            	}
            }
        }

        return parentLayoutId;
    }

    /**
     * @param layouts
     */
    protected void wrapLayouts(List<Layout> layouts) {
        for (int i = 0; i < layouts.size(); i++) {
            Layout layout = layouts.get(i);
            if (layout instanceof JunctionPointLayoutWrapper == false) {
                Layout wrappedLayout = new JunctionPointLayoutWrapper(layout);
                layouts.set(i, wrappedLayout);
            }
        }
    }

    @Override
    public boolean isSelected(boolean selectable, Layout layout, long ancestorPlid) {
    	// In dieser Methode wird geprüft, ob das vorliegende Layout ('this') als selektiert
    	// markiert werden soll. Der Parameter 'layout' stellt dabei das anzuzeigende Layout dar,
    	// die ancestorPlid ist die ID des zum anzuzeigenden Layout gehörenden Top-Level-Layout

        return super.isSelected(selectable, layout, ancestorPlid);
    }
    
    @Override
    public boolean equals(Object obj) {
        return getWrappedModel().equals(obj);
    }
}
