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
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.security.permission.ActionKeys
import com.liferay.portal.service.GroupLocalServiceUtil
import groovy.time.TimeCategory
import groovy.time.TimeDuration

import java.text.SimpleDateFormat

class IntranetSiteFilesToArchive {
    //constants
    static final SCRIPT_ID = "INTRANET_ARCHIVE"
    static final startdate = new Date();
    static final startdateText = new SimpleDateFormat("yyyyMMdd-HHmmss").format(startdate)
    static final PERSONEELZAKEN_GROUP_ID = 1822422L
    static final DESTINY_FOLDER_NAME = "SiteArchive"
    static final DESTINY_FOLDER_DESCRIPTION = "Archive van intranet bestanden"
    static final DIRECTEUR = 1463592L
    static final LEIDINGGEVENDE = 1463609L
    static final PERSONEELAFDELING = 1463673L
    static final LOKAALHRM = 2327539L
    static final GROUP_NAMES = ["Rotterdam", "Noorden", "Veluwe", "Zwolle - Kampen", "Oosten", "Zuid - West", "Midden - Noord"]
    static final FOLDER_NAMES = ["Arbeidscontracten"]

    Long adminUserId = 1879301
    Object log
    long companyId
    Object adminUser
    Object parentDestinyFolder
    Object destinyFolder
    String portalUrl
    String companyGroupId
    Object companies

    // --------------------------
    // Constructor
    // --------------------------
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

    // --------------------------
    // rename a file entry
    // --------------------------
    def renameFile(fileEntry) {
        log << "************\n"
        log << " * File Info: ${fileEntry.fileEntryId} - ${fileEntry.title} \n"
        try {
            def cvNewFileEntry = DLAppServiceUtil.getFileEntry(fileEntry.fileEntryId)
            log << " * entry : \n" << cvNewFileEntry << "\n"
            log << "reposid: "<< cvNewFileEntry.getRepositoryId() << "\n"
            log << "foldeid: "<< cvNewFileEntry.getFolderId() << "\n"
            log << "source : "<< cvNewFileEntry.getTitle().replaceFirst("."+fileEntry.getExtension(),"-archive."+fileEntry.getExtension()) << "\n"
            log << "mime   : "<< cvNewFileEntry.getMimeType() << "\n"
            log << "title  : "<< cvNewFileEntry.getTitle().replaceFirst("."+fileEntry.getExtension(),"-archive."+fileEntry.getExtension()) << "\n"
            log << "description: "<< cvNewFileEntry.getDescription() <<"\n"
            log << "change : " <<"\n"
            log << "version: " << cvNewFileEntry.getVersion()<<"\n"
            log << "size: "<< cvNewFileEntry.getSize() << "\n"

            def cvRenamedFileEntry = DLAppServiceUtil.addFileEntry(
                    cvNewFileEntry.getRepositoryId(),
                    cvNewFileEntry.getFolderId(),
                    cvNewFileEntry.getTitle().replaceFirst("."+fileEntry.getExtension(),"-archive."+fileEntry.getExtension()),
                    cvNewFileEntry.getMimeType(),
                    cvNewFileEntry.getTitle().replaceFirst("."+fileEntry.getExtension(),"-archive."+fileEntry.getExtension()),
                    cvNewFileEntry.getDescription(),
                    "",
                    cvNewFileEntry.getContentStream(),
                    cvNewFileEntry.getSize(),
                    getServiceContext()
            )
            log << " * newentry : \n" << cvRenamedFileEntry << "\n"

            DLAppServiceUtil.deleteFileEntry(fileEntry.fileEntryId)
            log << " * removed old entry : ${fileEntry.fileEntryId}\n" << "\n"

        } catch (PortalException portalException) {
        log << "error :: renameFile :: ${portalException.message}\n"
        }

    }

    // --------------------------
    // traverse structure folder
    // to rename archive files
    // --------------------------
    def traverseFolder(folder){
        log << "TRAVERSING Folder ******* ${folder}\n"

        // Get files and folders
        def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
        def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
        log << " * Folder Info: ${folder.folderId} - ${folder.name}\n"
        log << " * Folder Size: Files: ${files.size()} / Folder: ${folders.size()}\n"

        // Process each file
        files.each{ file ->
            renameFile(file)
        }

        folders.each { childFolder ->
            traverseFolder(childFolder)
        }
        log << "END TRAVERSING Folder ******* ${folder}\n"
    }

    // --------------------------
    // move to archive folder
    // --------------------------
    def moveFolder(cvFolderEntry, destinyFolder) {
        log << "MOVING Folder ******* ${cvFolderEntry}\n"
        try {
            log << "moving folder: " << cvFolderEntry.getName() << "\nto:\ndestiny: " << destinyFolder.getName() << "\n"

            def cvNewFolderEntry = DLAppServiceUtil.moveFolder(cvFolderEntry.folderId, destinyFolder.folderId, getServiceContext())
            log << "old folder id: " << cvFolderEntry.folderId << "\nto:\nnew folder id: " << cvNewFolderEntry.folderId << "\n"
            setPermissionsFolderEntry(cvNewFolderEntry, DIRECTEUR, "DIRECTEUR")
            setPermissionsFolderEntry(cvNewFolderEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
            setPermissionsFolderEntry(cvNewFolderEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
            setPermissionsFolderEntry(cvNewFolderEntry, LOKAALHRM, "LOKAAL HRM")

        } catch(PortalException portalException) {
            log << " moveFolders :: Error :: ${portalException} \n"
        }
        log << "END MOVING Folder ******* ${cvFolderEntry}\n"
    }

    // --------------------------
    // load parent destiny archive folder
    // --------------------------
    def loadParentDestinyFolder() {
        try {
            return DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, 0, DESTINY_FOLDER_NAME)
        } catch(PortalException portalException) {
            log << "parent destiny :: error :: {$portalException.message}"
            return DLAppServiceUtil.addFolder(PERSONEELZAKEN_GROUP_ID, 0, DESTINY_FOLDER_NAME, DESTINY_FOLDER_DESCRIPTION, getServiceContext())
        }
    }

    def loadDestinyFolder(folderId, groupName) {
        try {
            def destinyChildFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, folderId, groupName)
            log << "found child folder: " << destinyChildFolder << "\n"
            return destinyChildFolder
        } catch(PortalException portalException) {
            log << "child destiny :: error :: {$portalException.message}"
            def destinyChildFolder = DLAppServiceUtil.addFolder(PERSONEELZAKEN_GROUP_ID, folderId, groupName, "Archive "+groupName, getServiceContext())
            log << "created child folder: " << destinyChildFolder << "\n"
            return destinyChildFolder
        }
    }

    // --------------------------
    // set folder permissions
    // --------------------------
    def setPermissionsFolderEntry(cvFolderEntry, roleId, roleName) {
        if(!(ResourcePermissionLocalServiceUtil.hasResourcePermission(
                cvFolderEntry.companyId,
                DLFileEntry.class.getName(),
                ResourceConstants.SCOPE_INDIVIDUAL,
                String.valueOf(cvFolderEntry.folderId),
                roleId,
                ActionKeys.VIEW
        ))){
            log <<  " * * ADD_PERMISSION for ${roleName}: VIEW\n"
            ResourcePermissionLocalServiceUtil.setResourcePermissions(
                    cvFolderEntry.companyId,
                    DLFileEntry.class.getName(),
                    ResourceConstants.SCOPE_INDIVIDUAL,
                    String.valueOf(cvFolderEntry.folderId),
                    roleId,
                    [ActionKeys.VIEW] as String[]
            )
        }
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
    def run() {
        parentDestinyFolder = loadParentDestinyFolder()
        GROUP_NAMES.each { def groupName ->
            // Get group
            def group = GroupLocalServiceUtil.getGroup(this.companyId, groupName)
            log << "=====================================\n\n"
            log << "GROUP_ID: " << group.groupId << "\n\n"
            log << "parent destiny:" << parentDestinyFolder << "\n\n"

            FOLDER_NAMES.each { def folderName ->
                try {
                    def folder = DLAppServiceUtil.getFolder(group.groupId, 0L, folderName)
                    destinyFolder = loadDestinyFolder(this.parentDestinyFolder.folderId, groupName)

                    traverseFolder(folder)

                    moveFolder(folder, destinyFolder)
                }
                catch (PortalException portalException) {
                    log << "************\n"
                    log << ("ERROR: process folder ${folder}: ${portalException.message} \n")
                }
            }
        }
        final def enddate = new Date();
        log << "\nEND_DATE_TIME: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss S").format(enddate) + "\n"
        TimeDuration elapsedTime = TimeCategory.minus(enddate,startdate)
        log << "\nELAPSED_TIME: " + elapsedTime + "\n"
        return [
                success: true,
                message: "Process executed ${elapsedTime}"
        ]
    }

    // --------------------------
    // Information Message test
    // --------------------------
    def information(informationMessage) {
        println("information message :: ${informationMessage}")
        final def enddate = new Date();
        log << "\nEND_DATE_TIME: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss S").format(enddate) + "\n"
        TimeDuration elapsedTime = TimeCategory.minus(enddate,startdate)
        log << "\nELAPSED_TIME: " + elapsedTime + "\n"
        return [
                success: true,
                message: informationMessage
        ]
    }
}

// --------------------------
// main call
// --------------------------
IntranetSiteFilesToArchive iuds = new IntranetSiteFilesToArchive()
iuds.log << "Setting permissions:" << iuds.adminUser << "\n"

PrincipalThreadLocal.setName(iuds.adminUserId)
PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(iuds.adminUser)
PermissionThreadLocal.setPermissionChecker(permissionChecker)

if ("info" in params) {
    return iuds.information(params["info"])
} else {
    return iuds.run()
}