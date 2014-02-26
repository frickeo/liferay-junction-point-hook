/*
 * Copyright © 2014 HanseMerkur Krankenversicherung AG All Rights Reserved.
 *
 */
package de.hansemerkur.liferay.junctionpoint.hook.event;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portlet.expando.NoSuchTableException;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;

import de.hansemerkur.liferay.junctionpoint.util.JunctionPoint;

/**
 * This startup event creates two required custom attributes if they are
 * missing.
 * 
 * @author frickeo
 * 
 */
public class JunctionPointApplicationStartupEvent extends SimpleAction {

	@Override
	public void run(String[] companyIds) throws ActionException {
		// Create the junction point attributes for each company
		for (String string : companyIds) {
			long companyId = Long.valueOf(string);
			try {
				// create two attributes that belongs to layouts
				ExpandoColumn junctionPointLayout = addExpandoAttributes(
						companyId, Layout.class.getName(),
						JunctionPoint.JUNCTION_POINT_LAYOUT,
						ExpandoColumnConstants.BOOLEAN);
				ExpandoColumn junctionPointConnection = addExpandoAttributes(
						companyId, Layout.class.getName(),
						JunctionPoint.JUNCTION_POINT_CONNECTION,
						ExpandoColumnConstants.STRING);

				// das Attribute JunctionPointLayout auf "hidden" setzen
				UnicodeProperties jpLayoutProperties = junctionPointConnection
						.getTypeSettingsProperties();
				jpLayoutProperties.setProperty(
						ExpandoColumnConstants.PROPERTY_HIDDEN,
						Boolean.TRUE.toString());
				junctionPointConnection
						.setTypeSettingsProperties(jpLayoutProperties);
				ExpandoColumnLocalServiceUtil
						.updateExpandoColumn(junctionPointLayout);
				// das Attribute JunctionPointConnection auf "hidden" setzen
				UnicodeProperties jpConnectionProperties = junctionPointConnection
						.getTypeSettingsProperties();
				jpConnectionProperties.setProperty(
						ExpandoColumnConstants.PROPERTY_HIDDEN,
						Boolean.TRUE.toString());
				junctionPointConnection
						.setTypeSettingsProperties(jpConnectionProperties);
				ExpandoColumnLocalServiceUtil
						.updateExpandoColumn(junctionPointConnection);
			} catch (SystemException e) {
				// Behandlung einer SystemException
				throw new ActionException(e);
			} catch (PortalException e) {
				// Behandlung einer PortalException
				throw new ActionException(e);
			}
		}
	}

	/**
	 * Legt für die Company zwei neue Expando-Attribute an, falls es diese noch
	 * nicht gibt.
	 * 
	 * @param companyId
	 *            die CompanyId
	 * @throws SystemException
	 * @throws PortalException
	 */
	protected ExpandoColumn addExpandoAttributes(long companyId,
			String className, String columnName, int type)
			throws PortalException, SystemException {
		// lade die ExpandoTable für Groups oder lege sie an
		ExpandoTable expandoTable;
		try {
			expandoTable = ExpandoTableLocalServiceUtil.getDefaultTable(
					companyId, className);
		} catch (NoSuchTableException e) {
			expandoTable = ExpandoTableLocalServiceUtil.addDefaultTable(
					companyId, className);
		}

		// prüfe, ob das Attribute für cms-mapping existiert
		ExpandoColumn expandoColumn = createExpandoColumn(expandoTable,
				columnName, type);
		return expandoColumn;
	}

	/**
	 * Leget eine ExpandoColumn an und gibt ihr das Leserecht für 'Guest' und
	 * das Schreibrecht für 'Power User' in Layouts
	 * 
	 * @param expandoTable
	 *            Die ExpandoTabelle, zu der die Spalte gehört
	 * @param name
	 *            Der Name
	 * @param type
	 *            der Typ
	 * @throws PortalException
	 * @throws SystemException
	 */
	private ExpandoColumn createExpandoColumn(ExpandoTable expandoTable,
			String name, int type) throws PortalException, SystemException {
		ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(
				expandoTable.getTableId(), name);
		if (expandoColumn == null) {
			expandoColumn = ExpandoColumnLocalServiceUtil.addColumn(
					expandoTable.getTableId(), name, type);
			expandoColumn.persist();
		}
		// stelle sicher, dass die Resource existiert. Durch eine Race-Condition
		// in Liferay ist
		// jedoch nicht sichergestellt, dass Guest auch lesen darf.
		ResourceLocalServiceUtil.addResources(expandoTable.getCompanyId(), 0,
				0, ExpandoColumn.class.getName(), expandoColumn.getColumnId(),
				false, false, false);
		// stelle sicher, dass Guest lesen darf
		Role role = RoleLocalServiceUtil.getRole(expandoTable.getCompanyId(),
				RoleConstants.GUEST);
		ResourcePermissionLocalServiceUtil.setResourcePermissions(
				expandoTable.getCompanyId(), ExpandoColumn.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(expandoColumn.getColumnId()), role.getRoleId(),
				new String[] { "VIEW" });

		// stelle sicher, dass Power User in Layouts schreiben darf
		// (siehe https://issues.liferay.com/browse/LPS-44592)
		if (expandoTable.getClassName().equals(Layout.class.getName())) {
			Role powerUserRole = RoleLocalServiceUtil.getRole(
					expandoTable.getCompanyId(), RoleConstants.POWER_USER);
			ResourcePermissionLocalServiceUtil.setResourcePermissions(
					expandoTable.getCompanyId(), ExpandoColumn.class.getName(),
					ResourceConstants.SCOPE_INDIVIDUAL,
					String.valueOf(expandoColumn.getColumnId()),
					powerUserRole.getRoleId(),
					new String[] { "VIEW", "UPDATE" });
		}

		return expandoColumn;
	}
}
