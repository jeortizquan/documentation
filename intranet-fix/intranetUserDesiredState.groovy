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

class DataMapper {
    static final groupId = 10197

    def getDataSource ( dd, pckg ) {
        return DataDefinition.find("pckg,name,groupId", pckg, dd, groupId).get().dataSource
    }

    def loadDataDefinition( dd, pckg, id) {
        DataDefinition dataDef = DataDefinition.find("pckg,name,groupId", pckg, dd, groupId).get()
        return dataDef.dataSource?.getRecord([recordId:id]).record
    }
}

class IntranetUserDesiredState {
    //constants
    static final SCRIPT_ID = "INTRANET_MOVE"
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

    Boolean debugModeOn = true
    Long adminUserId = 1879301
    DataMapper dataMapper
    Object nawgegevensDS
    Object vogDS
    Object opleidingDS
    Object beroepDS
    Object certificaatDS
    Object toegangDS
    Object bankpasDS
    Object deviceDS
    Object mobielDS
    Object openbaarDS
    Object leaseautoDS
    Object contractDS
    Object corresDS
    Object functieDS
    Object dossierDS
    Object log
    String companyId
    Object adminUser
    String portalUrl
    String companyGroupId
    Object companies
    Object nawdata

    // --------------------------
    // Constructor
    // --------------------------
    IntranetUserDesiredState() {
        dataMapper = new DataMapper()

        this.nawgegevensDS = dataMapper.getDataSource("Nawgegevens", "ContractProcess")
        this.vogDS = dataMapper.getDataSource("VOG", "ContractProcess")
        this.opleidingDS = dataMapper.getDataSource("Opleidingen", "ContractProcess")
        this.beroepDS = dataMapper.getDataSource("Beroepsregistratie", "ContractProcess")
        this.certificaatDS = dataMapper.getDataSource("Certificaten", "ContractProcess")
        this.toegangDS = dataMapper.getDataSource("Toegang", "ContractProcess")
        this.bankpasDS = dataMapper.getDataSource("Bankpas", "ContractProcess")
        this.deviceDS = dataMapper.getDataSource("Device", "ContractProcess")
        this.mobielDS = dataMapper.getDataSource("Mobiel", "ContractProcess")
        this.openbaarDS = dataMapper.getDataSource("OpenbaarVervoer", "ContractProcess")
        this.leaseautoDS = dataMapper.getDataSource("Leaseauto", "ContractProcess")
        this.contractDS = dataMapper.getDataSource("Contract", "ContractProcess")
        this.corresDS = dataMapper.getDataSource("Correspondentie", "ContractProcess")
        this.functieDS = dataMapper.getDataSource("Functioneren", "ContractProcess")
        this.dossierDS = dataMapper.getDataSource("DossierEntry", "ContractProcess")
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
    // File operations
    // --------------------------
    def createChildDirectory(folderId, folderName, folderDescription) {
        def destinyChildFolder = DLAppServiceUtil.addFolder(PERSONEELZAKEN_GROUP_ID, folderId, folderName, folderDescription, getServiceContext())
        log << "created folder: " << destinyChildFolder << "\n"
        return destinyChildFolder
    }

    def createParentAndChildDirectory(parentFolderName, parentFolderDescription, folderName, folderDescription) {
        def parentDestinyFolder = DLAppServiceUtil.addFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, parentFolderName, parentFolderDescription, getServiceContext())
        log << "created parent folder: " << parentDestinyFolder << "\n"
        def destinyChildFolder = DLAppServiceUtil.addFolder(PERSONEELZAKEN_GROUP_ID, parentDestinyFolder.folderId, folderName, folderDescription, getServiceContext())
        log << "created folder: " << destinyChildFolder << "\n"
        return destinyChildFolder
    }

    def setPermissionsFileEntry(cvFileEntry, roleId, roleName) {
        if(!(ResourcePermissionLocalServiceUtil.hasResourcePermission(
                cvFileEntry.companyId,
                DLFileEntry.class.getName(),
                ResourceConstants.SCOPE_INDIVIDUAL,
                String.valueOf(cvFileEntry.fileEntryId),
                roleId,
                ActionKeys.VIEW
        ))){
            log <<  " * * ADD_PERMISSION for ${roleName}: VIEW\n"
            ResourcePermissionLocalServiceUtil.setResourcePermissions(
                    cvFileEntry.companyId,
                    DLFileEntry.class.getName(),
                    ResourceConstants.SCOPE_INDIVIDUAL,
                    String.valueOf(cvFileEntry.fileEntryId),
                    roleId,
                    [ActionKeys.VIEW] as String[]
            )
        }
    }

    // --------------------------
    // Main methods
    // --------------------------
    def updateFileEntryField(cvFileEntry, index, fieldName, destinyFolder, destinyGroupId) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            if (index == -1) {
                nawdata[fieldName][0].fileEntryId = cvNewFileEntry.fileEntryId
                nawdata[fieldName][0].url = new StringBuilder().append(portalUrl)
                        .append("/documents/")
                        .append(destinyGroupId + "/")
                        .append(cvNewFileEntry.fileEntryId + "/")
                        .append(cvNewFileEntry.title + "/")
                        .append(cvNewFileEntry.uuid + "?")
                        .append("version=" + cvNewFileEntry.version)
                        .append("&download=true&mod=1")
            } else {
                nawdata[fieldName][0][index].fileEntryId = cvNewFileEntry.fileEntryId
                nawdata[fieldName][0][index].url = new StringBuilder().append(portalUrl)
                        .append("/documents/")
                        .append(destinyGroupId + "/")
                        .append(cvNewFileEntry.fileEntryId + "/")
                        .append(cvNewFileEntry.title + "/")
                        .append(cvNewFileEntry.uuid + "?")
                        .append("version=" + cvNewFileEntry.version)
                        .append("&download=true&mod=1")
            }
            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry moved: " << cvFileEntry << "\n"
        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveField(fieldName) {
        log << " PROCESSING NawGegevens ${fieldName} \n"
        try {
            if (nawdata[fieldName][0]?.fileEntryId != null) {
                def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0].fileEntryId)
                def cvFolder = cvFileEntry.getFolder()
                def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                    log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                    try {
                        //parent folder found
                        log << " zoeken: " << cvFolder.getName() << "\n"
                        def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                        log << "parent folder gevonden: " << foundParentDestinyFolder <<"\n"
                        try {
                            //child folder found
                            def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                            log << "folder found: " << foundDestinyFolder <<"\n"

                            updateFileEntryField(cvFileEntry,-1, fieldName, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                        } catch (PortalException pexDF) {
                            log << "folder A: ${fieldName} :: " << pexDF.message << "\n"
                            def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                            updateFileEntryField(cvFileEntry, -1, fieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                        }
                    } catch (PortalException pexPF) {
                        //folder not found
                        log << "folder B: ${fieldName} :: " << pexPF.message << "\n"
                        def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                        updateFileEntryField(cvFileEntry,-1, fieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                    }
                }
            } else {
                log << " no file entry :: ${fieldName} \n"
            }
        } catch(Exception ex) {
            log << " moveField :: ${fieldName} :: Error :: ${ex.message} \n"
        }
    }

    def moveArrayField(fieldName) {
        log << " PROCESSING NawGegevens ${fieldName} \n"
        for(int entry=0; entry<nawdata[fieldName][0]?.size(); entry++) {
            try {
                if (nawdata[fieldName][0][entry]?.fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                        log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                        try {
                            //parent folder found
                            log << " zoeken: " << cvFolder.getName() << "\n"
                            def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                            log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                log << "folder found: " << foundDestinyFolder << "\n"

                                updateFileEntryField(cvFileEntry, entry, fieldName, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                            } catch (PortalException pexDF) {
                                log << "folder A: ${fieldName} :: ${entry} :: " << pexDF.message << "\n"
                                def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                updateFileEntryField(cvFileEntry, entry, fieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            log << "folder B: ${fieldName} :: ${entry} :: " << pexPF.message << "\n"
                            def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                            updateFileEntryField(cvFileEntry, entry, fieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID)
                        }
                    }
                } else {
                    log << " no file entry :: ${fieldName} \n"
                }
            } catch (Exception ex) {
                log << " moveArrayField :: ${fieldName} :: Error :: ${ex.message} \n"
            }
        }
    }

    def updateExternFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, destinyFolder, destinyGroupId, externDS) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            nawdata[fieldName][0][entry].scopeGroupId = destinyGroupId
            nawdata[fieldName][0][entry][subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata[fieldName][0][entry][subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

            externdata.scopeGroupId = destinyGroupId
            externdata[subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            externdata[subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            externDS.updateRecord(externdata)
            log << "move file entry moved extern: \n " << cvFileEntry << "\n"

        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveExternArrayField(fieldName, subfieldName, dataDefinition, externDS) {
        log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata[fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata[fieldName][0][entry]["id"]
                def externdata = dataMapper.loadDataDefinition(dataDefinition, PACKAGE_NAME, externId)

                if (nawdata[fieldName][0][entry][subfieldName]?.fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry][subfieldName].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                        log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                        try {
                            //parent folder found
                            log << " zoeken: " << cvFolder.getName() << "\n"
                            def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                            log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                log << "folder found: " << foundDestinyFolder << "\n"

                                updateExternFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            } catch (PortalException pexDF) {
                                log << "folder E: ${fieldName} :: ${subfieldName}" << pexDF.message << "\n"
                                def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                updateExternFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            log << "folder F: ${fieldName} :: ${subfieldName}" << pexPF.message << "\n"
                            def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                            updateExternFileEntryField(cvFileEntry,  externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                        }
                    }
                } else {
                    log << " no file entry :: ${fieldName} :: ${subfieldName} \n"
                }
            } catch (Exception ex) {
                log << " moveExternArrayField :: ${fieldName} :: ${subfieldName} :: Error :: ${ex.message} \n"
            }
        }
    }

    def updateExternSimpleArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, destinyFolder, destinyGroupId, externDS) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            nawdata[fieldName][0][entry].scopeGroupId = destinyGroupId
            nawdata[fieldName][0][entry][subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata[fieldName][0][entry][subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

            externdata.scopeGroupId = destinyGroupId
            externdata[subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            externdata[subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            externDS.updateRecord(externdata)
            log << "move file entry moved extern: \n " << cvFileEntry << "\n"

        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveExternSimpleArrayField(fieldName, subfieldName, dataDefinition, externDS) {
        log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata[fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata[fieldName][0][entry]["id"]
                def externdata = dataMapper.loadDataDefinition(dataDefinition, PACKAGE_NAME, externId)

                if (nawdata[fieldName][0][entry][subfieldName]?.fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry][subfieldName].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                        log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                        try {
                            //parent folder found
                            log << " zoeken: " << cvFolder.getName() << "\n"
                            def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                            log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                log << "folder found: " << foundDestinyFolder << "\n"

                                updateExternSimpleArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            } catch (PortalException pexDF) {
                                log << "folder E: ${fieldName} :: ${subfieldName}" << pexDF.message << "\n"
                                def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                updateExternSimpleArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            log << "folder F: ${fieldName} :: ${subfieldName}" << pexPF.message << "\n"
                            def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                            updateExternSimpleArrayFileEntryField(cvFileEntry,  externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                        }
                    }
                } else {
                    log << " no file entry :: ${fieldName} :: ${subfieldName} \n"
                }
            } catch (Exception ex) {
                log << " moveExternArrayArrayField :: ${fieldName} :: ${subfieldName} :: Error :: ${ex.message} \n"
            }
        }
    }

    def updateFaciliteitenFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName,
                                         destinyFolder, destinyGroupId, externDS) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            nawdata["faciliteiten"][fieldName][0][entry].scopeGroupId = destinyGroupId
            nawdata["faciliteiten"][fieldName][0][entry][subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata["faciliteiten"][fieldName][0][entry][subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

            externdata.scopeGroupId = destinyGroupId
            externdata[subfieldName].fileEntryId = cvNewFileEntry.fileEntryId
            externdata[subfieldName].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            externDS.updateRecord(externdata)
            log << "move file entry moved faciliteiten: \n " << cvFileEntry << "\n"

        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveFaciliteitenArrayField(fieldName, subfieldName, dataDefinition, externDS) {
        log << " PROCESSING NawGegevens Faciliteiten ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata["faciliteiten"][fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata["faciliteiten"][fieldName][0][entry]["id"]
                def externdata = dataMapper.loadDataDefinition(dataDefinition, PACKAGE_NAME, externId)

                if (nawdata["faciliteiten"][fieldName][0][entry][subfieldName]?.fileEntryId != null) {
                    def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["faciliteiten"][fieldName][0][entry][subfieldName].fileEntryId)
                    def cvFolder = cvFileEntry.getFolder()
                    def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                    if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                        log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                        try {
                            //parent folder found
                            log << " zoeken: " << cvFolder.getName() << "\n"
                            def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                            log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                            try {
                                //child folder found
                                def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                log << "folder found: " << foundDestinyFolder << "\n"

                                updateFaciliteitenFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            } catch (PortalException pexDF) {
                                log << "folder G: ${fieldName} :: ${subfieldName}" << pexDF.message << "\n"
                                def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                updateFaciliteitenFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            }
                        } catch (PortalException pexPF) {
                            //folder not found
                            log << "folder H: ${fieldName} :: ${subfieldName}" << pexPF.message << "\n"
                            def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                            updateFaciliteitenFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                        }
                    }
                } else {
                    log << " no file entry :: faciliteiten :: ${fieldName} :: ${subfieldName} \n"
                }
            } catch (Exception ex) {
                log << " moveFaciliteitenArrayField :: Error :: ${ex.message} \n"
            }
        }

    }

    def updateFaciliteitenArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry,
                                              destinyFolder, destinyGroupId, externDS) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            nawdata["faciliteiten"][fieldName][0][entry].scopeGroupId = destinyGroupId
            nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

            externdata.scopeGroupId = destinyGroupId
            externdata[subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
            externdata[subfieldName][subEntry].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            externDS.updateRecord(externdata)
            log << "move file entry moved faciliteiten: \n " << cvFileEntry << "\n"

        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveFaciliteitenArrayArrayField(fieldName, subfieldName, dataDefinition, externDS) {
        log << " PROCESSING NawGegevens Faciliteiten ${fieldName} ${subfieldName} \n"

        for (int entry = 0; entry < nawdata["faciliteiten"][fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata["faciliteiten"][fieldName][0][entry]["id"]
                def externdata = dataMapper.loadDataDefinition(dataDefinition, PACKAGE_NAME, externId)

                for (int subEntry = 0; subEntry < nawdata["faciliteiten"][fieldName][0][entry][subfieldName]?.size(); subEntry++) {
                    log << "\nfor: faciliteiten " << fieldName << " " << entry << " " << subfieldName << " " << subEntry << nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry] << "\n"
                    if (nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry]?.fileEntryId != null) {
                        def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata["faciliteiten"][fieldName][0][entry][subfieldName][subEntry].fileEntryId)
                        def cvFolder = cvFileEntry.getFolder()
                        def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                        if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                            log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                            try {
                                //parent folder found
                                log << " zoeken: " << cvFolder.getName() << "\n"
                                def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                                log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                                try {
                                    //child folexitder found
                                    def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                    log << "folder found: " << foundDestinyFolder << "\n"

                                    updateFaciliteitenArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                                } catch (PortalException pexDF) {
                                    log << "folder I: ${fieldName} :: ${subfieldName}" << pexDF.message << "\n"
                                    def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                    updateFaciliteitenArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                                }
                            } catch (PortalException pexPF) {
                                //folder not found
                                log << "folder J: ${fieldName} :: ${subfieldName}" << pexPF.message << "\n"
                                def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                                updateFaciliteitenArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            }
                        }
                    } else {
                        log << " no file entry :: array faciliteiten :: ${fieldName} :: ${subfieldName} \n"
                    }
                }
            } catch (Exception ex) {
                log << " moveFaciliteitenArrayArrayField :: ${fieldName} :: ${subfieldName} :: Error :: ${ex.message} \n"
            }
        }

    }

    def updateExternArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry,
                                        destinyFolder, destinyGroupId, externDS) {
        def cvNewFileEntry = DLAppServiceUtil.moveFileEntry(cvFileEntry.fileEntryId, destinyFolder.folderId, getServiceContext())
        log << ">> old entry id: " << cvFileEntry.fileEntryId << " :: new entry id: " << cvNewFileEntry.fileEntryId << "\n"
        setPermissionsFileEntry(cvNewFileEntry, DIRECTEUR, "DIRECTEUR")
        setPermissionsFileEntry(cvNewFileEntry, LEIDINGGEVENDE, "LEIDING GEVENDE")
        setPermissionsFileEntry(cvNewFileEntry, PERSONEELAFDELING, "PERSONEEL AFDELING")
        setPermissionsFileEntry(cvNewFileEntry, LOKAALHRM, "LOKAAL HRM")
        if (cvNewFileEntry != null) {
            nawdata[fieldName][0][entry].scopeGroupId = destinyGroupId
            nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
            nawdata[fieldName][0][entry][subfieldName][subEntry].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            nawgegevensDS.updateRecord(nawdata)
            log << "move file entry something moved nawgegevens: \n " << cvFileEntry << "\n"

            externdata.scopeGroupId = destinyGroupId
            externdata[subfieldName][subEntry].fileEntryId = cvNewFileEntry.fileEntryId
            externdata[subfieldName][subEntry].url = new StringBuilder().append(portalUrl)
                    .append("/documents/")
                    .append(destinyGroupId + "/")
                    .append(cvNewFileEntry.fileEntryId + "/")
                    .append(cvNewFileEntry.title + "/")
                    .append(cvNewFileEntry.uuid + "?")
                    .append("version=" + cvNewFileEntry.version)
                    .append("&download=true&mod=1")

            externDS.updateRecord(externdata)
            log << "move file entry moved array extern: \n " << cvFileEntry << "\n"

        } else {
            log << "move file entry nothing moved: =)\n"
        }
    }

    def moveExternArrayArrayField(fieldName, subfieldName, dataDefinition, externDS) {
        log << " PROCESSING NawGegevens ${fieldName} ${subfieldName} \n"
        for (int entry = 0; entry < nawdata[fieldName][0]?.size(); entry++) {
            try {
                def externId = nawdata[fieldName][0][entry]["id"]
                def externdata = dataMapper.loadDataDefinition(dataDefinition, PACKAGE_NAME, externId)
                for (int subEntry = 0; subEntry < nawdata[fieldName][0][entry][subfieldName]?.size(); subEntry++) {
                    log << "\nfor:" << fieldName << " " << entry << " " << subfieldName << " " << subEntry << nawdata[fieldName][0][entry][subfieldName][subEntry] << "\n"
                    if (nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId != null) {
                        log << "entry:" << "\n"
                        def cvFileEntry = DLAppServiceUtil.getFileEntry(nawdata[fieldName][0][entry][subfieldName][subEntry].fileEntryId)
                        def cvFolder = cvFileEntry.getFolder()
                        def cvParentFolder = DLAppServiceUtil.getFolder(cvFolder.getParentFolderId())
                        if (cvParentFolder.groupId != PERSONEELZAKEN_GROUP_ID) {
                            log << " personeel zaken ${cvParentFolder.folderId} != ${PERSONEELZAKEN_GROUP_ID} \n"
                            try {
                                //parent folder found
                                log << " zoeken: " << cvFolder.getName() << "\n"
                                def foundParentDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, DESTINY_FOLDER_ID, cvParentFolder.getName())
                                log << "parent folder gevonden: " << foundParentDestinyFolder << "\n"
                                try {
                                    //child folder found
                                    def foundDestinyFolder = DLAppServiceUtil.getFolder(PERSONEELZAKEN_GROUP_ID, foundParentDestinyFolder.folderId, cvFolder.getName())
                                    log << "folder found: " << foundDestinyFolder << "\n"

                                    updateExternArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, foundDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                                } catch (PortalException pexDF) {
                                    log << "folder K: ${fieldName} :: ${subfieldName}" << pexDF.message << "\n"
                                    def createDestinyFolder = createChildDirectory(foundParentDestinyFolder.folderId, cvFolder.getName(), cvFolder.getDescription())

                                    updateExternArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                                }
                            } catch (PortalException pexPF) {
                                //folder not found
                                log << "folder L: ${fieldName} :: ${subfieldName}" << pexPF.message << "\n"
                                def createDestinyFolder = createParentAndChildDirectory(cvParentFolder.getName(), cvParentFolder.getDescription(), cvFolder.getName(), cvFolder.getDescription())

                                updateExternArrayFileEntryField(cvFileEntry, externdata, entry, fieldName, subfieldName, subEntry, createDestinyFolder, PERSONEELZAKEN_GROUP_ID, externDS)
                            }
                        }
                    } else {
                        log << " no file entry :: extern array array :: ${fieldName} :: ${subfieldName} \n"
                    }
                }
            } catch (Exception ex) {
                log << " moveExternArrayArrayField :: ${fieldName} :: ${subfieldName} :: Error :: ${ex.message} \n"
            }
        }
    }

    def checkVOGFieldSimpleArrayOrCompound(fieldName, subfieldName) {
        return nawdata[fieldName][0][subfieldName][0] instanceof java.util.ArrayList
    }

    // --------------------------
    // main run call
    // --------------------------
    def run(userId, recordId) {
        def userInfo = getUserInfo(userId, recordId)

        log << "* user record before: ${userInfo.records.size() } \n"
        log << "* user record id: ${userInfo.records.id} \n"

        this.nawdata = loadNawGegevens(userInfo)

        moveField("cv")
        moveField("kopieBankpas")
        moveField("rijbewijs")
        moveField("getuigschrift")
        moveField("laatsteLoonstrook")
        moveField("praktijkovereenkomst")

        moveArrayField("kopieVoorEnAchterGeldigeId")
        moveArrayField("uploadLoonbelastingsverklaring")

        if (checkVOGFieldSimpleArrayOrCompound("vog","uploadFactuurAanvraag")) {
            moveExternArrayArrayField("vog", "uploadFactuurAanvraag", "VOG", vogDS)
        } else {
            moveExternSimpleArrayField("vog", "uploadFactuurAanvraag", "VOG", vogDS)
        }
        if (checkVOGFieldSimpleArrayOrCompound("vog","uploadVog")) {
            moveExternArrayArrayField("vog", "uploadVog", "VOG", vogDS)
        } else {
            moveExternSimpleArrayField("vog", "uploadVog", "VOG", vogDS)
        }

        moveExternArrayField("opleidingen", "uploadDiploma", "Opleidingen", opleidingDS)
        moveExternArrayField("beroepsregistraties", "uploadAanvraag", "Beroepsregistratie", beroepDS)
        moveExternArrayField("certificaten", "uploadCertificaat", "Certificaten", certificaatDS)
        moveExternArrayField("certificaten", "uploadOvereenkomst", "Certificaten", certificaatDS)
        moveExternArrayField("contracten", "conceptContract", "Contract",  contractDS)
        moveExternArrayField("correspondentie", "fileUpload", "Correspondentie", corresDS)
        moveExternArrayField("functioneren", "fileUpload", "Functioneren", functieDS)
        moveExternArrayField("dossierCheck", "fileNaam", "DossierEntry", dossierDS)

        moveFaciliteitenArrayField("toegang", "ontvangstformulier", "Toegang", toegangDS)
        moveFaciliteitenArrayField("toegang", "retourformulier", "Toegang", toegangDS)
        moveFaciliteitenArrayField("bankpas", "ontvangstformulier", "Bankpas", bankpasDS)
        moveFaciliteitenArrayField("bankpas", "retourformulier", "Bankpas", bankpasDS)
        moveFaciliteitenArrayField("device", "ontvangstformulier", "Device", deviceDS)
        moveFaciliteitenArrayField("device", "retourformulier", "Device", deviceDS)
        moveFaciliteitenArrayField("mobiel", "ontvangstformulier", "Mobiel", mobielDS)
        moveFaciliteitenArrayField("mobiel", "retourformulier", "Mobiel", mobielDS)
        moveFaciliteitenArrayField("openbaarVervoer", "ontvangstformulier", "OpenbaarVervoer", openbaarDS)
        moveFaciliteitenArrayField("openbaarVervoer", "retourformulier", "OpenbaarVervoer", openbaarDS)
        moveFaciliteitenArrayField("leaseauto", "gebruikersOvereenkomst", "Leaseauto", leaseautoDS)
        moveFaciliteitenArrayField("leaseauto", "vrijstellingBelasting", "Leaseauto", leaseautoDS)

        moveFaciliteitenArrayArrayField("leaseauto","contractLeasemaatschappij","Leaseauto", leaseautoDS)
        moveFaciliteitenArrayArrayField("leaseauto","contractMedewerker","Leaseauto", leaseautoDS)

        moveExternArrayArrayField("contracten", "contractUpload", "Contract", contractDS)
        moveExternArrayArrayField("beroepsregistraties", "uploadBeroepsregistratie", "Beroepsregistratie", beroepDS)

        log << "\nafter:" << nawdata << "\n"
        final def enddate = new Date();
        log << "\nEND_DATE_TIME: " + new SimpleDateFormat("dd-M-yyyy hh:mm:ss S").format(enddate) + "\n"
        TimeDuration elapsedTime = TimeCategory.minus(enddate,startdate)
        log << "\nELAPSED_TIME: " + elapsedTime + "\n"
        return nawdata
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
    // Load Data for User
    // --------------------------
    def loadNawGegevens(userInfo) {
        def filters = [:]
        filters["id"] =  userInfo.records[0].id
        return nawgegevensDS.getList([filters: filters]).records
    }

    // --------------------------
    // Get User Information
    // --------------------------
    def getUserInfo(userId, recordId) {
        log << "GET USER INFO: ${userId} ${recordId}\n"
        def filter = new BasicDBObject()
        filter.put("id", recordId)
        log << " * filter ${filter}\n"
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
        def nawgegevensMorphiaCollection = MorphiaFactory.ds().DB.getCollection(TABLENAME)
        def query = nawgegevensMorphiaCollection.find(filter, projection)
        def queryResult = query.toList()
        queryResult.each { def nawGegevens ->
            nawGegevens.roepnaam = StringUtils.stripAccents(nawGegevens.roepnaam).replace("'", "")
            nawGegevens.weergaveAchternaam = StringUtils.stripAccents(nawGegevens.weergaveAchternaam).replace("'", "")
        }
        log << "* USER_DETAIL: ${queryResult}\n"
        return [
                success: true,
                records: queryResult
        ]
    }

    // --------------------------
    // User info error
    // --------------------------
    def getUserInfoError() {
        final def enddate = new Date();
        log << "\nEND_DATE_TIME: " + new SimpleDateFormat("dd-MM-yyyy hh:mm:ss S").format(enddate) + "\n"
        TimeDuration elapsedTime = TimeCategory.minus(enddate,startdate)
        log << "\nELAPSED_TIME: " + elapsedTime + "\n"
        return [
                success: false,
                message: "Missing parameter: userId or recordId"
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
IntranetUserDesiredState iuds = new IntranetUserDesiredState()
iuds.log << "Setting permissions:" << iuds.adminUser << "\n"

PrincipalThreadLocal.setName(iuds.adminUserId)
PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(iuds.adminUser)
PermissionThreadLocal.setPermissionChecker(permissionChecker)
if ("info" in params) {
    return iuds.information(params["info"])
} else {
    if ("userId" in params || "recordId" in params) {
        return iuds.run(params["userId"], params["recordId"])
    } else {
        return iuds.getUserInfoError()
    }
}