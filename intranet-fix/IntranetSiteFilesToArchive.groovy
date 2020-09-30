//the function can access:
// - DataDefinition dataDefinition -> object with the information of the data definition
// - DataHelper h -> object with general information and status of the request and the response
// - Map params -> object containing the data
// - wsMethod.methodName(dataDefinition) -> you can use it to run a method that you have already created. ex: wsMethod.myMethod(dataDefinition, arg1, arg2...)
// Example:

dataDefinition.logger.info "dataDefinition: $dataDefinition"
dataDefinition.logger.info "params: $params"
//Your code here

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


class IntranetSiteFilesToArchive {
    //constants
    static final SCRIPT_ID = "INTRANET_ARCHIVE"
    static final startdate = new Date();
    static final startdateText = new SimpleDateFormat("yyyyMMdd-HHmm").format(startdate)
    static final TABLENAME = "group10197.ContractProcess.Nawgegevens"
    static final PERSONEELZAKEN_GROUP_ID = 1822422
    static final DESTINY_FOLDER_ID = 2111502
    static final PACKAGE_NAME = "ContractProcess"
    static final DIRECTEUR = 1463592
    static final LEIDINGGEVENDE = 1463609
    static final PERSONEELAFDELING = 1463673
    static final LOKAALHRM = 2327539
    Long adminUserId = 1879301
    Object log
    String companyId
    Object adminUser
    String portalUrl
    String companyGroupId
    Object companies

    IntranetSiteFilesToArchive() {
        this.companies = CompanyLocalServiceUtil.getCompanies()
        log = new File("""${System.getProperty("liferay.home")}/scripting/out-${SCRIPT_ID}-${startdateText}.txt""")
        log.getParentFile().mkdirs()
        log << "START_DATE_TIME: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss S").format(startdate) + "\n\n"
        log << "LIFERAY_COMPANY: \n"
        log << "* id     : " + this.companies[0].getCompanyId() +"\n"
        log << "* url    : " + this.companies[0].getPortalURL(this.companies[0].getGroupId()) +"\n"
        log << "* name   : " + this.companies[0].getName() +"\n"
        log << "* groupId: " + this.companies[0].getGroupId() +"\n"
        this.companyId = this.companies[0].getCompanyId()
        this.portalUrl = this.companies[0].getPortalURL(this.companies[0].getGroupId())
        this.companyGroupId = this.companies[0].getGroupId()
        this.adminUser = UserLocalServiceUtil.getUser(this.adminUserId)
    }

    def processFolder(folder, databag){
        log << " PROCESSING Folder ******* ${folder}\n"

        // Get folders
        def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
        log << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
        log << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

        folders.each { childFolder ->
            moveFolders(childFolder)
        }
        databag.log << " END PROCESSING Folder ******* ${folder}\n"
    }

    def moveFolders(cvFolderEntry, index, fieldName, destinyFolder) {
        def cvNewFolder = DLAppServiceUtil.moveFolder(cvFolderEntry, destinyFolder, getServiceContext())

        /*
        moveFolder(long folderId,
                                long parentFolderId,
                                ServiceContext serviceContext)
                         throws PortalException,
                                SystemException
         */
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
    }

    // --------------------------
    // get service context for liferay operations
    // --------------------------
    def getServiceContext() {
        ServiceContext serviceContext = new ServiceContext()
        PrincipalThreadLocal.setName(adminUserId);
        PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(UserLocalServiceUtil.getUser(adminUserId));
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
        return serviceContext;
    }

    // --------------------------
    // main run call
    // --------------------------
    def run(userId, recordId) {

    }
}
