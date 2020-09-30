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
import com.mongodb.BasicDBObject
import models.DataDefinition
import nl.viking.db.MorphiaFactory
import org.apache.commons.lang3.StringUtils

import java.text.SimpleDateFormat

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

// Mongo Datasource
def getDataSource = { dd, pckg ->
    def groupId = 10197
    return DataDefinition.find("pckg,name,groupId", pckg, dd, groupId).get().dataSource
}

def loadDataDefinition( dd, pckg, id) {
    def groupId = 10197
    DataDefinition dataDef = DataDefinition.find("pckg,name,groupId", pckg, dd, groupId).get()
    dataDefIns = dataDef.dataSource?.getRecord([recordId:id]).record
    return dataDefIns
}

// Load Data for User
def loadNawGegevens(userInfo, databag) {
    def filters = [:]
    filters["id"] =  userInfo.records[0].id
    return databag.nawgegevensDS.getList([filters: filters]).records
}

// User Info
def getUserInfo(userId, recordId, tablename, databag) {
    databag.log << "GET USER INFO: ${userId} ${recordId}\n"
    def filter = new BasicDBObject()
    filter.put("id", recordId)
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
            "faciliteiten.bankpas": 1,
            "faciliteiten.device": 1,
            "faciliteiten.leaseauto": 1,
            "faciliteiten.mobiel": 1,
            "faciliteiten.openbaarVervoer": 1,
            "faciliteiten.toegang": 1,
            "opleidingen": 1,
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
            message: "Missing parameter: userId or recordId"
    ]
}

def getServiceContext(databag) {
    ServiceContext serviceContext = new ServiceContext()
    PrincipalThreadLocal.setName(databag.adminUserId);
    PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(UserLocalServiceUtil.getUser(databag.adminUserId));
    PermissionThreadLocal.setPermissionChecker(permissionChecker);
    return serviceContext;
}

def moveRootFileEntryField(cvFileEntry, nawdata, index, fieldName, destinyFolder, destinyGroupId, databag) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        if (index == -1) {
            nawdata[fieldName][0].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata[fieldName][0].url = new StringBuilder().append(databag.portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")
        } else {
            nawdata[fieldName][0][index].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata[fieldName][0][index].url = new StringBuilder().append(databag.portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")
        }
        databag.nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved: " << cvFileEntry << "\n"
    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveField(nawdata, fieldName, destinyGroupId, destinyFolderId, databag) {
    databag.log << " PROCESSING NawGegevens ${fieldName} \n"
    try {er4
        if (nawdata[fieldName][0].fileEntryId != null) {
            def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0].fileEntryId)
            def cvFolder = cvFileEntry.getFolder()
            def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
            if (cvParentFolder.groupId != destinyGroupId) {
                databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                try {
                    //parent folder found
                    databag.log << " zoeken: " << cvFolder.getName() << "\n"
                    foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                    databag.log << "parent folder gevonden: " << foundParentDestinyFolder <<"\n"
                    try {
                        //child folder found
                        foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                        databag.log << "folder found: " << foundDestinyFolder <<"\n"

                        moveRootFileEntryField(cvFileEntry, nawdata, -1, fieldName, foundParentDestinyFolder, destinyGroupId, databag)
                    } catch (PortalException pexDF) {
                        databag.log << "folder A: " << pexDF.message << "\n"
                        createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                        databag.log << "created folder A: " << createDestinyFolder << "\n"

                        moveRootFileEntryField(cvFileEntry, nawdata, -1, fieldName, createDestinyFolder, destinyGroupId, databag)
                    }
                } catch (PortalException pexPF) {
                    //folder not found
                    databag.log << "folder B:" << pexPF.message << "\n"
                    createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                    databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                    createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                    databag.log << "created folder B: " << createDestinyFolder << "\n"

                    moveRootFileEntryField(cvFileEntry, nawdata, -1, fieldName, createDestinyFolder, destinyGroupId, databag)
                }
            }
        }
    } catch(Exception ex) {
        databag.log << " moveField :: Error :: ${ex.message} \n"
    }
}

def moveArrayField(nawdata, fieldName, destinyGroupId, destinyFolderId, databag) {
    databag.log << " PROCESSING NawGegevens ${fieldName} \n"
    for(int entry=0; entry<nawdata[fieldName][0]?.size(); entry++) {
        try {
            if (nawdata[fieldName][0][entry].fileEntryId != null) {
                def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry].fileEntryId)
                def cvFolder = cvFileEntry.getFolder()
                def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                if (cvParentFolder.groupId != destinyGroupId) {
                    databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                    try {
                        //parent folder found
                        databag.log << " zoeken: " << cvFolder.getName() << "\n"
                        foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                        databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                        try {
                            //child folder found
                            foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                            databag.log << "folder found: " << foundDestinyFolder << "\n"

                            moveRootFileEntryField(cvFileEntry, nawdata, entry, fieldName, foundDestinyFolder, destinyGroupId, databag)
                        } catch (PortalException pexDF) {
                            databag.log << "folder A:" << pexDF.message << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder A: " << createDestinyFolder << "\n"

                            moveRootFileEntryField(cvFileEntry, nawdata, entry, fieldName, createDestinyFolder, destinyGroupId, databag)
                        }
                    } catch (PortalException pexPF) {
                        //folder not found
                        databag.log << "folder B:" << pexPF.message << "\n"
                        createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                        databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                        createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                        databag.log << "created folder B: " << createDestinyFolder << "\n"

                        moveRootFileEntryField(cvFileEntry, nawdata, entry, fieldName, createDestinyFolder, destinyGroupId, databag)
                    }
                }
            }
        } catch (Exception ex) {
            databag.log << " moveField :: Error :: ${ex.message} \n"
        }
    }
}

def moveVOGFileEntryField(cvFileEntry, nawdata, vogdata, index, fieldName, subfieldName, destinyFolder, destinyGroupId, databag) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        nawdata[fieldName][0].scopeGroupId = destinyGroupId
        nawdata[fieldName][0][subfieldName][0][index].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata[fieldName][0][subfieldName][0][index].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        databag.nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"
        vogdata.scopeGroupId = destinyGroupId
        vogdata[subfieldName][index].fileEntryId = cvNewFileEntry.fileEntryId
        vogdata[subfieldName][index].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        databag.vogDS.updateRecord(vogdata)
        databag.log << "move file entry something moved vog: \n " << cvFileEntry << "\n"

    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveVOGArrayField(nawdata, vogdata, fieldName, subfieldName, destinyGroupId, destinyFolderId, databag) {
    databag.log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"
    for(int entry=0; entry<nawdata[fieldName][0][subfieldName][0]?.size(); entry++) {
        try {
            if (nawdata[fieldName][0][subfieldName][0][entry].fileEntryId != null) {
                def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][subfieldName][0][entry].fileEntryId)
                def cvFolder = cvFileEntry.getFolder()
                def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                if (cvParentFolder.groupId != destinyGroupId) {
                    databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                    try {
                        //parent folder found
                        databag.log << " zoeken: " << cvFolder.getName() << "\n"
                        foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                        databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                        try {
                            //child folder found
                            foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                            databag.log << "folder found C: " << foundDestinyFolder << "\n"

                            moveVOGFileEntryField(cvFileEntry, nawdata, vogdata, entry, fieldName, subfieldName, foundDestinyFolder, destinyGroupId, databag)
                        } catch (PortalException pexDF) {
                            databag.log << "folder C:" << pexDF.message << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder C: " << createDestinyFolder << "\n"

                            moveVOGFileEntryField(cvFileEntry, nawdata, vogdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag)
                        }
                    } catch (PortalException pexPF) {
                        //folder not found
                        databag.log << "folder D:" << pexPF.message << "\n"
                        createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                        databag.log << "created parent folder D: " << createParentDestinyFolder << "\n"
                        createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                        databag.log << "created folder D: " << createDestinyFolder << "\n"

                        moveVOGFileEntryField(cvFileEntry, nawdata, vogdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag)
                    }
                }
            }
        } catch (Exception ex) {
            databag.log << " moveField :: Error :: ${ex.message} \n"
        }
    }
}

def moveExternFileEntryField(cvFileEntry,
                             nawdata,
                             externdata,
                             index,
                             fieldName,
                             subfieldName,
                             destinyFolder,
                             destinyGroupId,
                             databag,
                             nawgegevensDS,
                             externDS) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        nawdata[fieldName][0][index].scopeGroupId = destinyGroupId
        nawdata[fieldName][0][index][subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata[fieldName][0][index][subfieldName].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

        externdata.scopeGroupId = destinyGroupId
        externdata[subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
        externdata[subfieldName].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        externDS.updateRecord(externdata)
        databag.log << "move file entry something moved extern: \n " << cvFileEntry << "\n"

    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveExternArrayField(nawdata, fieldName, subfieldName, dataDefinition, packageName, destinyGroupId, destinyFolderId, databag, externDS) {
    databag.log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata[fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata[fieldName][0][entry]["id"]
                def externdata = loadDataDefinition(dataDefinition, packageName, externId)

                if (nawdata[fieldName][0][entry][subfieldName]?.fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry][subfieldName].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != destinyGroupId) {
                        databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                        try {
                            //parent folder found
                            databag.log << " zoeken: " << cvFolder.getName() << "\n"
                            foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                            databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                                databag.log << "folder found: " << foundDestinyFolder << "\n"

                                moveExternFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, foundDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            } catch (PortalException pexDF) {
                                databag.log << "folder A:" << pexDF.message << "\n"
                                createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                                databag.log << "created folder A: " << createDestinyFolder << "\n"

                                moveExternFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            databag.log << "folder B:" << pexPF.message << "\n"
                            createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder B: " << createDestinyFolder << "\n"

                            moveExternFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                        }
                    }
                }
            } catch (Exception ex) {
                databag.log << " moveExternArrayField :: Error :: ${ex.message} \n"
            }
        }

}

def moveFaciliteitenFileEntryField(cvFileEntry,
                             nawdata,
                             externdata,
                             index,
                             fieldName,
                             subfieldName,
                             destinyFolder,
                             destinyGroupId,
                             databag,
                             nawgegevensDS,
                             externDS) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        nawdata["faciliteiten"][fieldName][0][index].scopeGroupId = destinyGroupId
        nawdata["faciliteiten"][fieldName][0][index][subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata["faciliteiten"][fieldName][0][index][subfieldName].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

        externdata.scopeGroupId = destinyGroupId
        externdata[subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
        externdata[subfieldName].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        externDS.updateRecord(externdata)
        databag.log << "move file entry something moved faciliteiten: \n " << cvFileEntry << "\n"

    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveFaciliteitenArrayField(nawdata, fieldName, subfieldName, dataDefinition, packageName, destinyGroupId, destinyFolderId, databag, externDS) {
    databag.log << " PROCESSING NawGegevens Faciliteiten ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata["faciliteiten"][fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata["faciliteiten"][fieldName][0][entry]["id"]
                def externdata = loadDataDefinition(dataDefinition, packageName, externId)

                if (nawdata["faciliteiten"][fieldName][0][entry][subfieldName].fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["faciliteiten"][fieldName][0][entry][subfieldName].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != destinyGroupId) {
                        databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                        try {
                            //parent folder found
                            databag.log << " zoeken: " << cvFolder.getName() << "\n"
                            foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                            databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                                databag.log << "folder found: " << foundDestinyFolder << "\n"

                                moveFaciliteitenFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, foundDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            } catch (PortalException pexDF) {
                                databag.log << "folder A:" << pexDF.message << "\n"
                                createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                                databag.log << "created folder A: " << createDestinyFolder << "\n"

                                moveFaciliteitenFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            databag.log << "folder B:" << pexPF.message << "\n"
                            createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder B: " << createDestinyFolder << "\n"

                            moveFaciliteitenFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                        }
                    }
                }
            } catch (Exception ex) {
                databag.log << " moveFaciliteitenArrayField :: Error :: ${ex.message} \n"
            }
        }

}

def moveFaciliteitenArrayFileEntryField(cvFileEntry,
                                   nawdata,
                                   externdata,
                                   entry,
                                   fieldName,
                                   subfieldName,
                                   subEntry,
                                   destinyFolder,
                                   destinyGroupId,
                                   databag,
                                   nawgegevensDS,
                                   externDS) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        nawdata["faciliteiten"][fieldName][0][entry].scopeGroupId = destinyGroupId
        nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

        externdata.scopeGroupId = destinyGroupId
        externdata[subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
        externdata[subfieldName][subEntry].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        externDS.updateRecord(externdata)
        databag.log << "move file entry something moved faciliteiten: \n " << cvFileEntry << "\n"

    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveFaciliteitenArrayArrayField(nawdata, fieldName, subfieldName, dataDefinition, packageName, destinyGroupId, destinyFolderId, databag, externDS) {
    databag.log << " PROCESSING NawGegevens Faciliteiten ${fieldName} ${subfieldName} \n"

    for (int entry = 0; entry < nawdata["faciliteiten"][fieldName][0]?.size(); entry++) {
        try {
            def externId = nawdata["faciliteiten"][fieldName][0][entry]["id"]
            def externdata = loadDataDefinition(dataDefinition, packageName, externId)

            for (int subEntry = 0; subEntry < nawdata["faciliteiten"][fieldName][0][entry][subfieldName]?.size(); subEntry++) {
                databag.log << "\nfor: faciliteiten " << fieldName << " " << entry << " " << subfieldName << " " << subEntry << nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry] << "\n"
                if (nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != destinyGroupId) {
                        databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                        try {
                            //parent folder found
                            databag.log << " zoeken: " << cvFolder.getName() << "\n"
                            foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                            databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                                databag.log << "folder found: " << foundDestinyFolder << "\n"

                                moveFaciliteitenArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, foundDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            } catch (PortalException pexDF) {
                                databag.log << "folder A:" << pexDF.message << "\n"
                                createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                                databag.log << "created folder A: " << createDestinyFolder << "\n"

                                moveFaciliteitenArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            databag.log << "folder B:" << pexPF.message << "\n"
                            createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder B: " << createDestinyFolder << "\n"

                            moveFaciliteitenArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                        }
                    }
                }
            }

        } catch (Exception ex) {
            databag.log << " moveFaciliteitenArrayArrayField :: Error :: ${ex.message} \n"
            databag.log << " moveFaciliteitenArrayArrayField :: params :: ${nawdata} \n"
            databag.log << " moveFaciliteitenArrayArrayField :: params :: ${fieldName} \n"
            databag.log << " moveFaciliteitenArrayArrayField :: params :: ${subfieldName} \n"
            databag.log << " moveFaciliteitenArrayArrayField :: params :: ${dataDefinition} \n"
        }
    }

}

def moveExternArrayFileEntryField(cvFileEntry,
                             nawdata,
                             externdata,
                             entry,
                             fieldName,
                             subfieldName,
                             subEntry,
                             destinyFolder,
                             destinyGroupId,
                             databag,
                             nawgegevensDS,
                             externDS) {
    def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext(databag))
    if (cvNewFileEntry != null) {
        nawdata[fieldName][0][entry].scopeGroupId = destinyGroupId
        nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
        nawdata[fieldName][0][entry][subfieldName][subEntry].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        nawgegevensDS.updateRecord(nawdata)
        databag.log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

        externdata.scopeGroupId = destinyGroupId
        externdata[subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
        externdata[subfieldName][subEntry].url = new StringBuilder().append(databag.portalUrl)
                .append("/documents/")
                .append(destinyGroupId + "/")
                .append(cvNewFileEntry.fileEntryId + "/")
                .append(cvNewFileEntry.title + "/")
                .append(cvNewFileEntry.uuid + "?")
                .append("version=" + cvNewFileEntry.version)
                .append("&download=true&mod=1")

        externDS.updateRecord(opleidingdata)
        databag.log << "move file entry something moved array extern: \n " << cvFileEntry << "\n"

    } else {
        databag.log << "move file entry nothing moved: =)\n"
    }
}

def moveExternArrayArrayField(nawdata, fieldName, subfieldName, dataDefinition, packageName, destinyGroupId, destinyFolderId, databag, externDS) {
    databag.log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"
    for (int entry = 0; entry < nawdata[fieldName][0]?.size(); entry++) {
        try {
            def externId = nawdata[fieldName][0][entry]["id"]
            def externdata = loadDataDefinition(dataDefinition, packageName, externId)
            for (int subEntry = 0; subEntry < nawdata[fieldName][0][entry][subfieldName]?.size(); subEntry++) {
                databag.log << "\nfor:" << fieldName << " " << entry << " " << subfieldName << " " << subEntry << nawdata[fieldName][0][entry][subfieldName][subEntry] << "\n"
                if (nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId != null) {
                    databag.log << "entry:" << "\n"
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != destinyGroupId) {
                        databag.log << " personeel zaken ${cvParentFolder.folderId} != ${destinyGroupId} \n"
                        try {
                            //parent folder found
                            databag.log << " zoeken: " << cvFolder.getName() << "\n"
                            foundParentDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName())
                            databag.log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                foundDestinyFolder = DLAppServiceUtil.getFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName())
                                databag.log << "folder found: " << foundDestinyFolder << "\n"

                                moveExternArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, foundDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            } catch (PortalException pexDF) {
                                databag.log << "folder A:" << pexDF.message << "\n"
                                createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                                databag.log << "created folder A: " << createDestinyFolder << "\n"

                                moveExternArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            databag.log << "folder B:" << pexPF.message << "\n"
                            createParentDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, destinyFolderId, cvParentFolder.getName(), cvParentFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created parent folder B: " << createParentDestinyFolder << "\n"
                            createDestinyFolder = DLAppServiceUtil.addFolder(destinyGroupId, createParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription(), getServiceContext(databag))
                            databag.log << "created folder B: " << createDestinyFolder << "\n"

                            moveExternArrayFileEntryField(cvFileEntry, nawdata, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, destinyGroupId, databag, databag.nawgegevensDS, externDS)
                        }
                    }
                }
            }
        } catch (Exception ex) {
            databag.log << " moveExternArrayArrayField :: Error :: ${ex.message} \n"
            databag.log << " moveExternArrayArrayField :: params :: ${nawdata} \n"
            databag.log << " moveExternArrayArrayField :: params :: ${fieldName} \n"
            databag.log << " moveExternArrayArrayField :: params :: ${subfieldName} \n"
            databag.log << " moveExternArrayArrayField :: params :: ${dataDefinition} \n"
        }
    }
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

if ("userId" in params || "recordId" in params) {
    def userInfo = getUserInfo(params["userId"], params["recordId"], TABLENAME, databag)
    databag.log << "* user record before: ${userInfo.records.size() } \n"
    databag.log << "* user record id: ${userInfo.records.id} \n"
    databag.nawgegevensDS = getDataSource("Nawgegevens", "ContractProcess")
    databag.vogDS = getDataSource("VOG", "ContractProcess")
    databag.opleidingDS = getDataSource("Opleidingen", "ContractProcess")
    databag.beroepDS = getDataSource("Beroepsregistratie", "ContractProcess")
    databag.certificaatDS = getDataSource("Certificaten", "ContractProcess")
    databag.toegangDS = getDataSource("Toegang", "ContractProcess")
    databag.bankpasDS = getDataSource("Bankpas", "ContractProcess")
    databag.deviceDS = getDataSource("Device", "ContractProcess")
    databag.mobielDS = getDataSource("Mobiel", "ContractProcess")
    databag.openbaarDS = getDataSource("OpenbaarVervoer", "ContractProcess")
    databag.leaseautoDS = getDataSource("Leaseauto", "ContractProcess")
    databag.contractDS = getDataSource("Contract", "ContractProcess")
    databag.corresDS = getDataSource("Correspondentie", "ContractProcess")
    databag.functieDS = getDataSource("Functioneren","ContractProcess")
    databag.dossierDS = getDataSource("DossierEntry","ContractProcess")

    def nawdata = loadNawGegevens(userInfo, databag)

    moveField(nawdata,"cv", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveField(nawdata,"kopieBankpas", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveField(nawdata,"rijbewijs", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveField(nawdata,"getuigschrift", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveField(nawdata,"laatsteLoonstrook", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveField(nawdata,"praktijkovereenkomst", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveArrayField(nawdata,"kopieVoorEnAchterGeldigeId", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveArrayField(nawdata,"uploadLoonbelastingsverklaring", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)

    vogId = nawdata["vog"][0]["id"][0]
    def vogdata = loadDataDefinition("VOG", "ContractProcess", vogId)

    moveVOGArrayField(nawdata, vogdata, "vog", "uploadFactuurAanvraag", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)
    moveVOGArrayField(nawdata, vogdata, "vog", "uploadVog", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag)

    moveExternArrayField(nawdata, "opleidingen", "uploadDiploma", "Opleidingen", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.opleidingDS)
    moveExternArrayField(nawdata, "beroepsregistraties", "uploadBeroepsregistratie", "Beroepsregistratie", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.beroepDS)
    moveExternArrayField(nawdata, "beroepsregistraties", "uploadAanvraag", "Beroepsregistratie", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.beroepDS)
    moveExternArrayField(nawdata, "certificaten", "uploadCertificaat", "Certificaten", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.certificaatDS)
    moveExternArrayField(nawdata, "certificaten", "uploadOvereenkomst", "Certificaten", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.certificaatDS)

    moveFaciliteitenArrayField(nawdata, "toegang", "ontvangstformulier", "Toegang", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.toegangDS)
    moveFaciliteitenArrayField(nawdata, "toegang", "retourformulier", "Toegang", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.toegangDS)
    moveFaciliteitenArrayField(nawdata, "bankpas", "ontvangstformulier", "Bankpas", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.bankpasDS)
    moveFaciliteitenArrayField(nawdata, "bankpas", "retourformulier", "Bankpas", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.bankpasDS)
    moveFaciliteitenArrayField(nawdata, "device", "ontvangstformulier", "Device", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.deviceDS)
    moveFaciliteitenArrayField(nawdata, "device", "retourformulier", "Device", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.deviceDS)
    moveFaciliteitenArrayField(nawdata, "mobiel", "ontvangstformulier", "Mobiel", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.mobielDS)
    moveFaciliteitenArrayField(nawdata, "mobiel", "retourformulier", "Mobiel", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.mobielDS)
    moveFaciliteitenArrayField(nawdata, "openbaarVervoer", "ontvangstformulier", "OpenbaarVervoer", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.openbaarDS)
    moveFaciliteitenArrayField(nawdata, "openbaarVervoer", "retourformulier", "OpenbaarVervoer", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.openbaarDS)
    moveFaciliteitenArrayField(nawdata, "leaseauto", "gebruikersOvereenkomst", "Leaseauto", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.leaseautoDS)
    moveFaciliteitenArrayField(nawdata, "leaseauto", "vrijstellingBelasting", "Leaseauto", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.leaseautoDS)
    moveFaciliteitenArrayArrayField(nawdata, "leaseauto","contractLeasemaatschappij","Leaseauto", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.leaseautoDS)
    moveFaciliteitenArrayArrayField(nawdata, "leaseauto","contractMedewerker","Leaseauto", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.leaseautoDS)

    moveExternArrayField(nawdata, "contracten", "conceptContract", "Contract", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.contractDS)
    moveExternArrayArrayField(nawdata, "contracten", "contractUpload", "Contract", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.contractDS)

    moveExternArrayField(nawdata, "correspondentie", "fileUpload", "Correspondentie", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.corresDS)
    moveExternArrayField(nawdata, "functioneren", "fileUpload", "Functioneren", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.functieDS)

    moveExternArrayField(nawdata, "dossierCheck", "fileNaam", "DossierEntry", "ContractProcess", PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, databag, databag.dossierDS)

    return nawdata
} else {
    return getUserInfoError()
}

final def enddate = new Date();
databag.log << "\nEND_DATE_TIME: " + new SimpleDateFormat().format(enddate) + "\n"
