//the function can access:
// - DataDefinition dataDefinition -> object with the information of the data definition
// - DataHelper h -> object with general information and status of the request and the response
// - Map params -> object containing the data
// - wsMethod.methodName(dataDefinition) -> you can use it to run a method that you have already created. ex: wsMethod.myMethod(dataDefinition, arg1, arg2...)
// Example:

dataDefinition.logger.info "dataDefinition: $dataDefinition"
dataDefinition.logger.info "params: $params"
//Your code here

import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil
import com.liferay.portal.kernel.repository.model.FileEntry
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil
import com.liferay.portal.model.*
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.service.RoleLocalServiceUtil
import com.liferay.portal.security.permission.ActionKeys
import com.liferay.portal.service.RepositoryLocalServiceUtil
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portal.service.CompanyLocalServiceUtil
import com.liferay.portal.kernel.exception.PortalException
import com.liferay.portal.service.ServiceContext
import com.liferay.portal.service.ResourceLocalServiceUtil
import com.liferay.portal.security.auth.PrincipalThreadLocal
import java.text.SimpleDateFormat
import com.mongodb.BasicDBObject
import nl.viking.db.MorphiaFactory
import models.DataDefinition
import com.liferay.portal.security.permission.*
import com.liferay.portal.service.UserLocalServiceUtil
import org.apache.commons.lang3.StringUtils

//constants
final def SCRIPT_ID = "INTRANET_MOVE"
final def startdate = new Date();
final def startdateText = new SimpleDateFormat("yyyyMMdd-HHmm").format(startdate)
final def TABLENAME = "group10197.ContractProcess.Nawgegevens"
final def PERSONEELZAKEN_GROUP_ID = 1822422
final def DESTINY_FOLDER_ID = 2111502

def databag = [:]

databag.debugModeOn = true
databag.adminUserId = 2360389

def companies = CompanyLocalServiceUtil.getCompanies()
databag.companies = companies

databag.log = new File("""${System.getProperty("liferay.home")}/scripting/out-${SCRIPT_ID}-${startdateText}.txt""")
databag.log.getParentFile().mkdirs()

def getUserInfo(userId, liferayUserId, tablename, databag) {
    databag.log << "GET USER INFO: ${liferayUserId}\n"
    def filter = new BasicDBObject()
    filter.put("liferayUserId", Long.parseLong(liferayUserId))
    databag.log << " * filter ${filter}\n"
    def projection = new BasicDBObject([
            "id": 1,
            "roepnaam" : 1,
            "weergaveAchternaam" : 1,
            "gewensteTelefoonnummer" : 1,
            "userId" : 1,
            "liferayUserId": 1,
            "scopeGroupId": 1,
            "kopiebankpas" : 1,
            "laatsteLoonstrook" : 1,
            "kopieVoorEnAchterGeldigeId" : 1,
            "uploadLoonbelastingsverklaring": 1,
            "rijbewijs": 1,
            "cv": 1,
            "getuigschrift": 1,
            "praktijkovereenkomst": 1,
            "faciliteiten.bankpas": 1,
            "beroepregistraties": 1,
            "certificaten": 1,
            "contracten": 1,
            "correspondentie": 1,
            "faciliteiten.device": 1,
            "faciliteiten.openbaarVervoer": 1,
            "opleidingen": 1,
            "faciliteiten.toegang": 1,
            "vog": 1
    ])
    def nawgegevensMorphiaCollection = MorphiaFactory.ds().DB.getCollection(tablename)
    def query = nawgegevensMorphiaCollection.find(filter,projection)
    databag.log << " * after ${query}\n"
    def queryResult = query.toList()
    databag.log << " * after transformlist ${queryResult}\n"
    queryResult.each { def nawGegevens ->
        nawGegevens.roepnaam = StringUtils.stripAccents(nawGegevens.roepnaam).replace("'", "")
        nawGegevens.weergaveAchternaam = StringUtils.stripAccents(nawGegevens.weergaveAchternaam).replace("'", "")
    }
    databag.log << "* USER_DETAIL: ${queryResult}\n"
    return [
            success: true,
            records: queryResult
    ]
}

def getUserInfoError() {
    return [
            success: false,
            message: "Missing parametor: userId"
    ]
}


def isUserPersoneelZaken(userRecord, personeelGroupId) {
    if (userRecord.scopeGroupId != personeelGroupId) {
        return true
    } else {
        return false
    }
}


def moveUserData(nawdata, source, destiny) {
    return true
}

def processFile(fileEntry,databag) {
    databag.log << "************\n"
    databag.log << " * FILE_INFO: ${fileEntry.fileEntryId} - ${fileEntry.title} \n"
}


def processFolder(folder, databag){
    databag.log << " PROCESSING Folder ******* ${folder}\n"

    // Get files and folders
    def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
    def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
    databag.log << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
    databag.log << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

    // Process each item
    files.each{ file ->
        processFile(file,databag)
    }

    folders.each { childFolder ->
        processFolder(childFolder,databag)
    }
    databag.log << " END PROCESSING Folder ******* ${folder}\n"
}

def searchFileEntry (folder, fileName, databag) {
    def file = null
    databag.log << " SEARCHING Folder ******* ${folder}\n"

    // Get files and folders
    def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
    def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
    databag.log << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
    databag.log << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

    // Process each item
    found = false
    for (int i=0; i < files.size(); i++) {
        file = files[i]
        databag.log << " comparison ${file.title} :: ${fileName} \n"
        if (file.title.equals(fileName)) {
            databag.log << "************\n"
            databag.log << " * FOUND : ${file.fileEntryId} - ${file.title} \n"
            found = true
            break;
        } else {
            file = null
        }
    }

    if (!found) {
        for (int k = 0; k < folders.size(); k++) {
            file = searchFileEntry(folders[k], fileName, databag)
            if (file != null)
                break;
        }
    }
    databag.log << " END PROCESSING Folder ******* ${folder}\n"
    return file
}

def createFileStructureIfDoesntExists() {

}


def moveNawgegevens(nawdata, source, destinyGroupId, destinyFolderId, databag) {
    databag.log << " PROCESSING NawGegevens \n"
    // get parent folder and move to perseenalzaken cv
    databag.log << " ${nawdata["cv"]} \n";
    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["cv"][0].fileEntryId)
    databag.log << " ${cvFileEntry} \n";
    def cvFolder = cvFileEntry.getFolder()
    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())

    databag.log << " folder: ${cvFolder} \n\n"
    databag.log << " parent folder: ${cvParentFolder}\n\n"

    ServiceContext serviceContext = new ServiceContext()
    PrincipalThreadLocal.setName(databag.adminUserId);
    PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(UserLocalServiceUtil.getUser(databag.adminUserId));
    PermissionThreadLocal.setPermissionChecker(permissionChecker);

    def cvMovedFolder = cvParentFolder //DLAppServiceUtil.moveFolder(cvParentFolder.getFolderId(), destinyFolderId, serviceContext)

    /* def cvNewFileEntry = searchFileEntry(cvMovedFolder, cvFileEntry.title, databag)
     databag.log << " new file entry folder: ${cvNewFileEntry}\n\n"

     if (cvNewFileEntry != null) {
         nawdata["cv"][0].fileEntryId = cvNewFileEntry.fileEntryId
         nawdata["cv"][0].url = new StringBuilder().append(databag.portalUrl)
                 .append("/documents/")
                 .append(destinyGroupId+"/")
                 .append(cvNewFileEntry.fileEntryId+"/")
                 .append(cvNewFileEntry.title+"/")
                 .append(cvNewFileEntry.uuid+"?")
                 .append("version="+cvNewFileEntry.version)
                 .append("&download=true&mod=1")
     }*/

    nawdata = moveSimpleField(cvMovedFolder, nawdata, destinyGroupId, cvFileEntry.title, "cv", databag)

    cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["kopieBankpas"][0].fileEntryId)
    databag.log << " kopie ${cvFileEntry} \n";
    nawdata = moveSimpleField(cvMovedFolder, nawdata, destinyGroupId, cvFileEntry.title, "kopiebankpas", databag)

    cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["rijbewijs"][0].fileEntryId)
    databag.log << " rijbewijs ${cvFileEntry} \n";
    nawdata = moveSimpleField(cvMovedFolder, nawdata, destinyGroupId, cvFileEntry.title, "rijbewijs", databag)

    databag.log << " moved folder: ${cvMovedFolder} \n\n"
    databag.log << " updated naw: ${nawdata} \n\n"

    //print whole contents ids
    processFolder(cvMovedFolder, databag)

    def data = [:]
    data.put("newfileentry", cvNewFileEntry)
    data.put("fileentry",cvFileEntry)
    data.put("folder", cvFolder)
    data.put("parentfolder", cvParentFolder)
    data.put("movedfolder", cvMovedFolder)

    return data
}

def moveSimpleField(cvMovedFolder, nawdata, destinyGroupId, fileName, fieldName, databag) {
    def cvNewFileEntry = searchFileEntry(cvMovedFolder, fileName, databag)
    databag.log << " ${fileName} file entry folder: ${cvNewFileEntry}\n\n"

    if (cvNewFileEntry != null) {
        nawdata[fieldName][0].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata[fieldName][0].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId+"/")
                .append(cvNewFileEntry.fileEntryId+"/")
                .append(cvNewFileEntry.title+"/")
                .append(cvNewFileEntry.uuid+"?")
                .append("version="+cvNewFileEntry.version)
                .append("&download=true&mod=1")
    }
    return nawdata
}

def moveWithinArrayField(cvMovedFolder, nawdata, destinyGroupId, fileName, fieldName, databag) {
    def cvNewFileEntry = searchFileEntry(cvMovedFolder, fileName, databag)
    databag.log << " laatsteLoonstrook file entry folder: ${cvNewFileEntry}\n\n"

    if (cvNewFileEntry != null) {
        nawdata["kopieBankpas"][0].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata["kopieBankpas"][0].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId+"/")
                .append(cvNewFileEntry.fileEntryId+"/")
                .append(cvNewFileEntry.title+"/")
                .append(cvNewFileEntry.uuid+"?")
                .append("version="+cvNewFileEntry.version)
                .append("&download=true&mod=1")
    }
    return nawdata
}

def movekopieVoorEnAchterGeldigeId() {
    def cvNewFileEntry = searchFileEntry(cvMovedFolder, fileName, databag)
    databag.log << " laatsteLoonstrook file entry folder: ${cvNewFileEntry}\n\n"

    if (cvNewFileEntry != null) {
        nawdata["kopieVoorEnAchterGeldigeId"][0][0].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata["kopieVoorEnAchterGeldigeId"][0][0].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId+"/")
                .append(cvNewFileEntry.fileEntryId+"/")
                .append(cvNewFileEntry.title+"/")
                .append(cvNewFileEntry.uuid+"?")
                .append("version="+cvNewFileEntry.version)
                .append("&download=true&mod=1")
    }
    return nawdata
}

def moveBeroepsregistratie(userId) {
    return true
}

def moveCertificaten(userId) {
    return true
}

def moveContract(userId) {
    return true
}

def moveCorrespondentie(userId) {
    return true
}

def moveDevice(userId) {
    return true
}

def moveDossierEntry() {
    return true
}

def moveVOG() {
    return true
}

def moveToegang() {
    return true
}

def getDataSource() {
    def groupId = 10197
    return DataDefinition.find("pckg,name,groupId", "ContractProcess","Nawgegevens", groupId).get().dataSource
}

// --------------------------
// main call
// --------------------------

databag.log << "START_DATE_TIME: " + new SimpleDateFormat().format(startdate) + "\n\n"
databag.log << "PARAMETERS     : ${params}\n"
databag.log << "LIFERAY_COMPANY: \n"

databag.companies.each {
    def company ->
        databag.log << "* id     : " + company.getCompanyId() +"\n"
        databag.log << "* url    : " + company.getPortalURL(company.getGroupId()) +"\n"
        databag.log << "* name   : " + company.getName() +"\n"
        databag.log << "* groupId: " + company.getGroupId() +"\n"
        databag.companyId = company.getCompanyId()
        databag.portalUrl = company.getPortalURL(company.getGroupId())
        databag.companyGroupId = company.getGroupId()
}

def adminUser = UserLocalServiceUtil.getUser(databag.adminUserId)
PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(adminUser)
PermissionThreadLocal.setPermissionChecker(permissionChecker)

if ("userId" in params && "liferayUserId" in params) {
    def userInfo = getUserInfo(params["userId"], params["liferayUserId"], TABLENAME, databag)
    databag.log << "* user record before: ${userInfo.records.size() } \n"
    databag.log << "* user record id: ${userInfo.records.id} \n"
    def nawgegevensDS = getDataSource()
    def filters = [:]
    filters["id"] =  userInfo.records.id[0]
    def nawdata = nawgegevensDS.getList([filters: filters]).records

    if (isUserPersoneelZaken(userInfo.records, PERSONEELZAKEN_GROUP_ID)) {
        databag.log << " personeel zaken ${PERSONEELZAKEN_GROUP_ID} != ${userInfo.records.scopeGroupId[0]} \n"
        databag.log << " complete record : ${nawdata} \n"
        nawdata = moveNawgegevens(nawdata, userInfo.records.scopeGroupId[0], PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    }
    //databag.log << "* user record after: ${nawdata} \n"
    return nawdata
} else {
    return getUserInfoError()
}

final def enddate = new Date();
databag.log << "\nEND_DATE_TIME: " + new SimpleDateFormat().format(enddate) + "\n"
