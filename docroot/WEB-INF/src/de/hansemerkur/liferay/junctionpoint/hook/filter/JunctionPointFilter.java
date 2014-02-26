/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.hook.filter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.BaseFilter;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;

import de.hansemerkur.liferay.junctionpoint.util.JunctionPointUtil;

/**
 * Dieser ServletFilter dient dazu, für einen JunctionPoint die Ziel-Site anzupassen. Dazu wird zum
 * einen ermittelt, ob das aktuelle Layout ein Junction Point Layout ist. Falls ja, so wird
 * ermittelt, welches Ziel Layout verbunden ist bzw. welches Ziel Layout zuletzt besucht wurde. Die
 * plid des Ziel Layouts wird die neue plid. Zum anderen wird ermittelt, ob das aktuelle Layout
 * selber in einem Navigationsbaum liegt, der mit einem Junction Point verbudne ist. Falls ja, so
 * wird die GroupId des obersten verbundenen Junction Points ermittelt und als neue SourceGrupId
 * verwendet.
 * <p>
 * Der Filter verwendet die neue Funktionalität <code>VirtualLayout</code> von Liferay, die es
 * erlaubt, eine Seite im Kontext einer anderen Gruppe anzuzeigen. Umgesetzt hat Liferay diese
 * Funktionalität in der Klasse <code>ServicePreAction</code>.
 * 
 * @author FRICKEO
 * @author $LastChangedBy: $
 * @version $LastChangedRevision: $
 */
public class JunctionPointFilter extends BaseFilter {

    private Log _log = LogFactoryUtil.getLog(getClass());

    protected Log getLog() {
        return _log;
    }

    @Override
    protected void processFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws Exception {

        HttpServletRequest jpRequest = getJunctionPointRequest(request);

        super.processFilter(JunctionPointFilter.class, jpRequest, response, filterChain);
    }

    protected HttpServletRequest getJunctionPointRequest(HttpServletRequest request) throws SystemException, PortalException {
        long plid = ParamUtil.getLong(request, "p_l_id");
        long sourceGroupId = ParamUtil.getLong(request, "p_v_l_s_g_id");

        // falls keine layoutId angegeben ist oder aber bereits eine abweichende GroupId für ein
        // VirtualLayout so wird nichts angepasst.
        if (plid == GetterUtil.DEFAULT_LONG || sourceGroupId != GetterUtil.DEFAULT_LONG) {
            return request;
        }

        Layout layout = LayoutLocalServiceUtil.fetchLayout(plid);
        // Falls das Layout nicht existiert : nichts anpassen
        if (layout == null) {
            return request;
        }

        // Das Ziellayout anpassen, fass es ein JunctionPoint-Layout ist
        Layout targetLayout = JunctionPointUtil.getJunctionTarget(layout, request, true);

        // Das Top-Level-Layout unter Einbeziehung von JunctionPoints ermitteln
        Layout ancestorLayout = JunctionPointUtil.getJunctionedAncestor(layout, request);
        sourceGroupId = ancestorLayout.getGroupId();

        // Zurück, falls es keine Anpassungen gibt
        if (layout.getPlid() == targetLayout.getPlid() && layout.getGroupId() == sourceGroupId) {
            return request;
        }

        return new JunctionPointRequest(request, targetLayout.getPlid(), sourceGroupId);
    }
}
