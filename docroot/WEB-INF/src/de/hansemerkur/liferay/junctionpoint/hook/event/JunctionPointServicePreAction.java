/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.hook.event;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutWrapper;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.permission.GroupPermissionUtil;
import com.liferay.portal.theme.ThemeDisplay;

import de.hansemerkur.liferay.junctionpoint.hook.model.JunctionPointLayoutWrapper;

/**
 * Diese ServicePre-Action dient dazu, das von Liferay ServicePreAction gesetzte ZielLayout sowie
 * die ermittelten TopLevel-Layouts so zu verpacken, dass bei der Ermittlung übergordneter oder
 * untergeordneter Seiten die konfigurierten JunctionPoints berücksichtigt werden. Die passiert
 * durch Verpackung der Layouts in einer <code>JunctionPointLayoutWrapper</code> Instanz.
 * 
 * @author frickeo
 */
public class JunctionPointServicePreAction extends Action {

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {

        StopWatch stopWatch = null;

        if (_log.isDebugEnabled()) {
            stopWatch = new StopWatch();

            stopWatch.start();
        }

        try {
            servicePre(request, response);
        }
        catch (Exception e) {
            throw new ActionException(e);
        }

        if (_log.isDebugEnabled()) {
            _log.debug("Running takes " + stopWatch.getTime() + " ms");
        }
    }

    @SuppressWarnings("unchecked")
    private void servicePre(HttpServletRequest request, HttpServletResponse response) throws PortalException, SystemException {
        // get the top-level-layouts and wrap them
        List<Layout> layouts = (List<Layout>) request.getAttribute(WebKeys.LAYOUTS);
        for (int i = 0; i < layouts.size(); i++) {
            Layout layout = layouts.get(i);
            Layout wrappedLayout = new JunctionPointLayoutWrapper(layout);
            layouts.set(i, wrappedLayout);
        }

        // get the target layout and wrap it
        Layout layout = (Layout) request.getAttribute(WebKeys.LAYOUT);
        // umwrap it
        if (layout instanceof LayoutWrapper) {
            layout = ((LayoutWrapper) layout).getWrappedModel();
        }
        // and wrap it again
        if (layout != null) {
            Layout wrappedLayout = new JunctionPointLayoutWrapper(layout);
            request.setAttribute(WebKeys.LAYOUT, wrappedLayout);
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            themeDisplay.setLayout(wrappedLayout);
            
            // TRIL-176: Site-Administration erlauben, falls die Berechtigung existiert
            PermissionChecker permissionChecker = PermissionThreadLocal.getPermissionChecker();
            if (permissionChecker != null && permissionChecker.isSignedIn() && GroupPermissionUtil.contains(
    				permissionChecker, layout.getGroup(),
    				ActionKeys.VIEW_SITE_ADMINISTRATION)) {

            	themeDisplay.setShowSiteAdministrationIcon(true);
            	String urlSiteAdministration = themeDisplay.getURLSiteAdministration();
            	urlSiteAdministration = HttpUtil.removeParameter(urlSiteAdministration, "doAsGroupId");
            	urlSiteAdministration = HttpUtil.addParameter(urlSiteAdministration, "doAsGroupId", layout.getGroupId());
            	themeDisplay.setURLSiteAdministration(urlSiteAdministration);
    		}
            
            
        }
    }

    private static Log _log = LogFactoryUtil.getLog(JunctionPointServicePreAction.class);

}
