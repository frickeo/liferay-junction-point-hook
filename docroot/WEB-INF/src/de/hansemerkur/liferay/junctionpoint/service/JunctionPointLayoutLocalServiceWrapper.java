/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalService;
import com.liferay.portal.service.LayoutLocalServiceWrapper;

import de.hansemerkur.liferay.junctionpoint.hook.model.JunctionPointLayoutWrapper;
import de.hansemerkur.liferay.junctionpoint.util.JunctionPointUtil;

/**
 * Die Klasse passt die Methode
 * <code>getParentLayout</code> so an, dass JunctionPoints berücksichtigt werden.
 * 
 * @author FRICKEO
 * @author $LastChangedBy: $
 * @version $LastChangedRevision: $
 */
public class JunctionPointLayoutLocalServiceWrapper extends LayoutLocalServiceWrapper {

    public JunctionPointLayoutLocalServiceWrapper(LayoutLocalService layoutLocalService) {
        super(layoutLocalService);
    }

    @Override
    public Layout getParentLayout(Layout layout) throws PortalException, SystemException {
        Layout currentLayout = layout;
        // unwrap the layout
        boolean wrapped = false;
        if (currentLayout instanceof JunctionPointLayoutWrapper) {
            wrapped = true;
            currentLayout = ((JunctionPointLayoutWrapper) currentLayout).getWrappedModel();
        }

        // get the assigned JunctionPoint (if any)
        if (currentLayout.isRootLayout()) {
            Layout junctionedLayout = JunctionPointUtil.getConfiguredJunctionPointLayout(currentLayout);
            while (junctionedLayout != null) {
                currentLayout = junctionedLayout;
                junctionedLayout = JunctionPointUtil.getConfiguredJunctionPointLayout(junctionedLayout);
            }
        }

        // now get the parent layout
        Layout parentLayout = super.getParentLayout(currentLayout);

        // and rewrap it
        if (wrapped) {
            parentLayout = new JunctionPointLayoutWrapper(parentLayout);
        }

        return parentLayout;
    }
}
