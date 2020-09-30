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

import java.text.SimpleDateFormat

def databag = [:]

databag.debugModeOn = true

def companies = CompanyLocalServiceUtil.getCompanies()
databag.companies = companies

companies.each {
    def company ->
        out.println("company Id: " + company.getCompanyId())
        out.println("company url: " + company.getPortalURL(company.getGroupId()))
        out.println("company name: " + company.getName())
        out.println("company group Id: " + company.getGroupId())
}
out.println("<em><strong>destiny:</strong></em>")
long repositoryId = 1822216
def folderCount = DLAppServiceUtil.getFoldersCount(repositoryId, 0)
def folders = DLAppServiceUtil.getFolders(repositoryId, 0)
out.println("folderCount: " + folderCount)
databag.folders = folders

def processFolder(folder, databag) {
    out.println(" *************************** ")
    out.println(" FOLDER: " + folder.getName())
    // Get files and folders
    def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
    def subfolders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
    out.println(" * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n")
    out.println(" * FOLDER_SIZE: Files: ${files.size()} / Folder: ${subfolders.size()}\n")
    files.each {
        file-> processFile(file, databag)
    }
    subfolders.each {
        childFolder -> processFolder(childFolder, databag)
    }
    out.println(" *************************** ")
}

def processFile(fileEntry, databag) {
    out.println(" * * PROCESSING FILE: ${fileEntry.getFileEntryId()} :: ${fileEntry.getTitle()} \n")
}

/*
ServiceContext serviceContext = new ServiceContext()
serviceContext.setCompanyId(databag.companies.get(0).getCompanyId());
serviceContext.setScopeGroupId(repositoryId);
databag.serviceContext = serviceContext
DLAppServiceUtil.moveFolder(3143778, 1889024, serviceContext)
*/


databag.folders.each {
    def folder ->
        try {
            processFolder(folder, databag)
        }
        catch(PortalException ex) {
            out.println("************\n")
            out.println("ERROR: process folder ${foldername}: ${ex.message} \n")
        }
}

out.println("<em><strong>source:</strong></em>")
long sourceRepositoryId = 1958984
def sourceFolderCount = DLAppServiceUtil.getFoldersCount(sourceRepositoryId, 0)
def sourceFolders = DLAppServiceUtil.getFolders(sourceRepositoryId, 0)
out.println("folderSourceCount: " + sourceFolderCount)
databag.sourceFolders = sourceFolders

databag.sourceFolders.each {
    def folder ->
        try {
            processFolder(folder, databag)
        }
        catch(PortalException ex) {
            out.println("************\n")
            out.println("ERROR: process folder ${foldername}: ${ex.message} \n")
        }
}

/*
ServiceContext serviceContext = new ServiceContext()
serviceContext.setCompanyId(databag.companies.get(0).getCompanyId());
serviceContext.setScopeGroupId(repositoryId);
databag.serviceContext = serviceContext
def folderResult = DLAppServiceUtil.moveFolder(3144199, 3143778, serviceContext)
out.println(":: result :: :: ::")
out.println(" * FOLDER_RESULT: ${folderResult.folderId} - ${folderResult.name}\n")
out.println(":: result :: :: ::")
*/

/*

ServiceContext serviceContext = new ServiceContext()
serviceContext.setCompanyId(databag.companies.get(0).getCompanyId());
//to move a folder to a root folder
//first need to put repositoryId, and 0 in the parentFolder
repositoryId=1958984
serviceContext.setScopeGroupId(repositoryId);
databag.serviceContext = serviceContext
def folderResult = DLAppServiceUtil.moveFolder(3145703, 0, serviceContext)
out.println(":: result :: :: ::")
out.println(" * FOLDER_RESULT: ${folderResult.folderId} - ${folderResult.name}\n")
out.println(":: result :: :: ::")

 */