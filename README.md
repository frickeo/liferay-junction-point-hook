liferay-junction-point-hook
===========================

Introduction
------------
The junction-point-hook allow an administrator to merge any top-level navigation tree of some site into an arbitrary place of a parent site. The configuration is stored in custom attributes only, which allows changes without coding. We are using this feature to manage the intranet of our company. The parent site defines the overall navigation tree with one layout page for each organisational unit. We then define a set of sub sites with different site administrators, one site for each organisational unit. The sub sites are merged into the parent intranet site by using the junction point feature.


Usage
-----
For using the junction point feature you obviously need at least two sites, that are hierarchically related. You can then mark one or more layout pages of the parent site as 'junction point' (you can think of power sockets in the wall when talking about junction points). As second step you can switch to any top level layout page of a sub site and connect that page with one of the defined junction points (just like putting a jack into the power socket). Both configurations can be done by using the 'Junction Point' section of the manage pages dialogue.

After the connection between the junction point page and the top level layout of the sub site has been established, the junction point page is replaced by the connected layout, including all of its  sub layouts. Whenever you navigate to one of the connected page or one of its sub layouts, that layout is rendered in the context of the junction point page.


Main concept
------------
The navigation tree of each site in liferay is spanned by the parent relationship named 'parentLayoutId'. Top level pages of a site have the value '0' in that relationship. Junction points allow to change this '0' value to point to some other layout page of another site. To support some consistent notion of a hierarchy this extension of the parent relationship is restricted to parent sites only.

If a layout should be rendered either by naming its 'plid' or by using its friendly URL, the hook checks, if the requested layout is a top level layout, that is 'junctioned' to some other layout in another site. If this is the case, the top level navigation nodes are changed to represent the other site. On the other side, if a layout is requested, that is marked as 'junction point', that layout is exchanged by the layout, that is connected to that junction point. Since there can be more than one layout connected to one junction point, this change is only done, if the relationship is unique or if one of the connected layouts has been visited recently.


Implementation details
----------------------
The main functionality of junction points like storing the expando values, navigating over the extended layout tree or getting all ancestor layouts is implemented by the service trio 'JunctionPoint', 'JunctionPointUtil', and 'JunctionPointImpl'. This trio is wired by the 'ext-spring.xml' configuration file.

A 'JunctionPointApplicationStartupEvent' is used to make sure, that the two required custom attributes 'JUNCTION_POINT_LAYOUT' and 'JUNCTION_POINT_CONNECTION' are created when starting the portal server. The required permissions are created as well. Both attributes are defined as 'hidden' attributes, because a separate JSP page is supplied for editing the values. The event class is configured in the 'portal.properties' of the hook.

The separate JSP page is configured in the 'portal.properties' as well by using the property 'layout.form.update'. This property allows to add additional sections to the manage layouts dialogue. The corresponding file can be found under  'docroot/WEB-INF/jsp/html/portlet/layouts_admin/layout/junction_points.jsp'. Please note that this custom jsp is executed in the context of the portal and not in the context of the hook. Therefore the jsp just includes another jsp file by using the 'liferay-util:include' tag. The included jsp file is then executed in the context of the hook and can therefore access the classes of the hook.

The 'JunctionPointFilter' is configured as servlet filter in the 'liferay-hook.xml'. It is registered for the url pattern 'c/portal/layout' and thus listens for rendering layout pages. The filter inspects if the junction point feature should be used for the current layout. If so, the request parameters 'p_l_id' and 'p_v_l_s_g_id' are modified. Both values are used by liferay for so called 'virtual layout pages' that exists in liferay since version 6.2. They are used to embed one layout in the context of another site.

A 'JunctionPointServicePreAction' is used to replace the current layout and the associated top level layouts with 'JunctionPointLayoutWrapper' instances. The replacement is performed for the request attributes as well as the theme display fields. In addition to the layout replacement the site administration URL in the theme display is corrected.

The class 'JunctionPointLayoutWrapper' acts as wrapper around layouts to integrate the junction point feature into the upwards and downwards navigation in the layout tree. Most methods are straight forwards implemented, except for the method 'getParentLayoutId'. This method is called by the 'BreadcrumbTag', to check, if a parent layout exists. In case of a junctioned top level layout the value -1 is returned. This leads to a subsequent call of 'LayoutLocalServiceUtil.getParentLayout' by the BreadcrumbTag class. In other contexts, the correct value of '0' has to be returned. Therefore a hack is used to detect the BreadcrumbTag as calling class.

The last piece of the junction point puzzle is the 'JunctionPointLayoutLocalServiceWrapper' class that adapts the method 'getParentLayout' to use the new feature. This class is activated as service in the 'liferay-hook.xml' configuration.
