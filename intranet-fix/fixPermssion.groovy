import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil
import com.liferay.portal.kernel.repository.model.FileEntry
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil
import com.liferay.portal.model.*
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.service.RoleLocalServiceUtil
import com.liferay.portal.security.permission.ActionKeys
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portal.kernel.exception.PortalException
import java.text.SimpleDateFormat

def databag = [:]

/* Important variable*/
//databag.companyId = 10154;
databag.companyId = 10157; // Production
//databag.groupnames = ["Rotterdam", "Noorden","Veluwe" ,"Zwolle - Kampen","Oosten","Zuid - West","Midden - Noord"]
databag.groupnames = ["Personeelszaken"]
//databag.groupnames = ["Midden - Noord"]
databag.foldernames = ["Arbeidscontracten","tmp","undefined"]
databag.dryrun = false

// Setup the file log
final def SCRIPT_ID = "CLEAN_PERMISSIONS"
def startdate = new Date();
def startdateText = new SimpleDateFormat("yyyyMMdd-HHmm").format(startdate)
outputFile = new File("""${System.getProperty("liferay.home")}/scripting/out-${SCRIPT_ID}-${startdateText}.txt""")
outputFile.getParentFile().mkdirs()

outputFile << "START_DATE_TIME: " + new SimpleDateFormat().format(startdate) + "\n\n" 

/* Get role */
databag.siteMemberRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.SITE_MEMBER)
databag.guestRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.GUEST)
databag.userRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.USER)

def processFile(fileEntry,databag){
	outputFile <<  "************\n"
	outputFile <<  " * FILE_INFO: ${fileEntry.fileEntryId} - ${fileEntry.title}\n"

	// Clean the permissions
	[databag.siteMemberRole,databag.guestRole].each{ def role ->
		def currentPermissions = ResourcePermissionLocalServiceUtil.fetchResourcePermission(
			fileEntry.companyId,
			DLFileEntry.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(fileEntry.fileEntryId),
			role.roleId)

		currentPermissions.each { def permission ->
			
			if(databag.dryrun){
				outputFile <<  " * * DELETE_PERMISSION for ${role.name} : ${permission.actionIds} [*]\n"
			}else{
				outputFile <<  " * * DELETE_PERMISSION for ${role.name} : ${permission.actionIds}\n"
				ResourcePermissionLocalServiceUtil.deleteResourcePermission(permission)
			}
		}

	}
	

	// Check the permission for site member
	if(!(ResourcePermissionLocalServiceUtil.hasResourcePermission(
			fileEntry.companyId,
			DLFileEntry.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(fileEntry.fileEntryId),
			databag.userRole.roleId,
			ActionKeys.VIEW
		))){
		if(databag.dryrun){
			outputFile <<  " * * ADD_PERMISSION for ${databag.userRole.name}: VIEW [*]\n"
		}
		else{
			outputFile <<  " * * ADD_PERMISSION for ${databag.userRole.name}: VIEW\n"
			ResourcePermissionLocalServiceUtil.setResourcePermissions(
				fileEntry.companyId,
				DLFileEntry.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(fileEntry.fileEntryId),
				databag.userRole.roleId,
				[ActionKeys.VIEW] as String[]
			)
		}
	}
}

def processFolder(folder,databag){
	outputFile << "************\n"
	
	// Get files and folders
	def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
	def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
	outputFile << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
	outputFile << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

	// Process each item
	files.each{ file ->
		processFile(file,databag)
	}

	folders.each { childFolder ->
		processFolder(childFolder,databag)
	}
}

// check all the grupos
databag.groupnames.each{ def groupname ->
	// Get group
	def group = GroupLocalServiceUtil.getGroup(databag.companyId,groupname)
	outputFile << ("=====================================\n\n")
	outputFile << ("GROUP_ID: " + group.groupId + "\n\n")
	outputFile << ("RUN_DRY: " + databag.dryrun + "\n\n")

	// We go throughout all the folder

	databag.foldernames.each { def foldername ->
		try{
			def folder = DLAppServiceUtil.getFolder(group.groupId,0,foldername)
			processFolder(folder,databag)
		}
		catch(PortalException ex){
			outputFile << "************\n"
			outputFile << ("ERROR: process folder ${foldername}: ${ex.message} \n")
		}
		
	}
}


def enddate = new Date();
outputFile << "\nEND_DATE_TIME: " + new SimpleDateFormat().format(enddate) + "\n" 



