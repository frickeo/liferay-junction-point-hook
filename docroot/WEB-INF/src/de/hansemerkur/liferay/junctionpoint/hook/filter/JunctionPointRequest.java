/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.hook.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Mit diesem RequestWrapper kann eine von den Requestdaten abweichende plid sowie sourceGroupId
 * vorgegeben werden.
 * 
 * @author FRICKEO
 * @author $LastChangedBy: $
 * @version $LastChangedRevision: $
 */
public class JunctionPointRequest extends HttpServletRequestWrapper {

    private final String plid;
    private final String pvlsgid;

    public JunctionPointRequest(HttpServletRequest request, long plid, long pvlsgid) {
        super(request);

        this.plid = String.valueOf(plid);
        this.pvlsgid = String.valueOf(pvlsgid);
    }

    @Override
    public String getParameter(String name) {

        if ("p_l_id".equals(name)) {
            return plid;
        }

        if ("p_v_l_s_g_id".equals(name)) {
            return pvlsgid;
        }

        return super.getParameter(name);
    }
}
