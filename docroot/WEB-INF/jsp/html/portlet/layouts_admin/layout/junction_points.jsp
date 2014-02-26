<%@ include file="/html/portlet/layouts_admin/init.jsp" %>

<%--
Die Ausführung udn Kompilierung einer Custom-JSP-Seite erfolgt im Context der 
ROOT Anwendung und verwendet auch deren ClassLoader. Daher sind Klassen aus dem
Hook nicht sichtbar. Um diese Einschränkung zu umgehen erfolgt hier ein Include
einer JSP-Seite aus dem Hook. Dabei wird der Kontext gewechselt udn somit auch der
ClassLoader.
--%>
<liferay-util:include page="/edit_junction_points.jsp" servletContext="<%=this.getServletContext().getContext(\"/junction-point-hook\") %>">
</liferay-util:include>