import com.liferay.portal.kernel.exception.PortalException
import com.liferay.portal.model.*
import com.liferay.portal.security.auth.PrincipalThreadLocal
import com.liferay.portal.security.permission.*
import com.liferay.portal.service.CompanyLocalServiceUtil
import com.liferay.portal.service.ServiceContext
import com.liferay.portal.service.UserLocalServiceUtil
import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil
import com.liferay.portal.service.RoleLocalServiceUtil
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.security.permission.ActionKeys
import com.mongodb.BasicDBObject
import models.DataDefinition
import nl.viking.db.MorphiaFactory
import org.apache.commons.lang3.StringUtils

import java.text.SimpleDateFormat
import groovy.time.TimeDuration
import groovy.time.TimeCategory

class AzureAD {

}
// --------------------------
// main call
// --------------------------
AzureAD iuds = new AzureAD()

println("works")

