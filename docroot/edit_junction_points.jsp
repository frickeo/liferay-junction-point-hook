<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<%@ page import="de.hansemerkur.liferay.junctionpoint.util.JunctionPoint" %>
<%@ page import="de.hansemerkur.liferay.junctionpoint.util.JunctionPointUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PropsKeys" %>
<%@ page import="com.liferay.portal.kernel.util.PropsUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.model.Group" %>
<%@ page import="com.liferay.portal.model.Layout" %>
<%@ page import="com.liferay.portal.model.LayoutConstants" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="java.util.List" %>

<liferay-theme:defineObjects />

<liferay-ui:error-marker key="errorSection" value="junction-points" />

<h3><liferay-ui:message key="junction-points" /></h3>

<%
Layout selLayout = (Layout)request.getAttribute("edit_pages.jsp-selLayout");

boolean isTopLevelLayout = selLayout.isRootLayout();
boolean isJunctionPointLayout = JunctionPointUtil.isJunctionPointLayout(selLayout);
List<Layout> connectedLayouts = JunctionPointUtil.getConnectedLayouts(selLayout);
List<Layout> usableLayouts = JunctionPointUtil.getUsableJunctionPoints(selLayout);

String randomNamespace = PortalUtil.generateRandomKey(request, "edit_junction_point_page") + StringPool.UNDERLINE;
%>

<div class="alert alert-info">
  <liferay-ui:message key="junction-point-layout-help" />
</div>

<aui:input id='<%= randomNamespace + JunctionPoint.JUNCTION_POINT_LAYOUT + "_name" %>' type="hidden" name="ExpandoAttributeName--junction-point-layout--" value="<%=JunctionPoint.JUNCTION_POINT_LAYOUT %>" />
<aui:select id="<%= randomNamespace + JunctionPoint.JUNCTION_POINT_LAYOUT %>" name="ExpandoAttribute--junction-point-layout--">
  <aui:option label="true" selected="<%= isJunctionPointLayout %>" value="1" />
  <aui:option label="false" selected="<%= !isJunctionPointLayout %>" value="0" />
</aui:select>

<c:if test="<%= isJunctionPointLayout %>">
  <aui:field-wrapper label="junction-point-usage">
    <aui:nav>
      <%
      String prefix = PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING);
      if (selLayout.isPrivateLayout()) {
          if (selLayout.getGroup().isUser()) {
              prefix = PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING);
          } else {
              prefix = PropsUtil.get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING);
          }
      }

      for (int i = 0; i < connectedLayouts.size(); i++) {
          Layout junctionLayout = connectedLayouts.get(i);
          Group junctionGroup = junctionLayout.getGroup();
          String href = prefix + junctionGroup.getFriendlyURL() + junctionLayout.getFriendlyURL();
          String label = junctionGroup.getName() + " - " + junctionLayout.getName(locale);
        %>
        <aui:nav-item label="<%= label %>" href="<%= href %>"/>
        <%
      }
      %>
    </aui:nav>
  </aui:field-wrapper>
</c:if>

<c:if test="<%= isTopLevelLayout %>">
  <div class="alert alert-info">
    <liferay-ui:message key="junction-point-help" />
  </div>

  <aui:input id='<%= randomNamespace + JunctionPoint.JUNCTION_POINT_CONNECTION + "_name" %>' type="hidden" name="ExpandoAttributeName--junction-point-connection--" value="<%=JunctionPoint.JUNCTION_POINT_CONNECTION %>" />
  <aui:select name="ExpandoAttribute--junction-point-connection--">

    <%
    String junctionPointConnection = (String) selLayout.getExpandoBridge().getAttribute(JunctionPoint.JUNCTION_POINT_CONNECTION);
    %>
    <aui:option label="" selected="<%= Validator.isNull(junctionPointConnection) %>" value="" />
    <%
    for (int i = 0; i < usableLayouts.size(); i++) {
        Layout junctionLayout = usableLayouts.get(i);
        if (junctionLayout.getPlid() == selLayout.getPlid()) {
            continue;
        }
        String label = junctionLayout.getGroup().getName() + " - " + junctionLayout.getName(locale);
        String value = junctionLayout.getGroup().getUuid() + "/" + junctionLayout.getLayoutId();
    %>

      <aui:option label='<%= label %>' selected="<%= value.equals(junctionPointConnection) %>" value="<%= value %>" />

    <%
    }
    %>

  </aui:select>
</c:if>

<c:if test="<%= !isJunctionPointLayout && !isTopLevelLayout %>">
  <div class="alert alert-warn">
    <liferay-ui:message key="junction-point-warn" />
  </div>
</c:if>
